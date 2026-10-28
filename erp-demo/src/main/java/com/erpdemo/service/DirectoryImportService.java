package com.erpdemo.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * 监控目录自动导入：把 17TRACK 导出的 CSV 放进目录，扫描后自动建出库单并按需确认出库。
 */
@Service
public class DirectoryImportService {

    private static final Logger log = LoggerFactory.getLogger(DirectoryImportService.class);
    private static final DateTimeFormatter ARCHIVE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 表头别名（已统一去空格、小写、去下划线/连字符） */
    private static final Map<String, List<String>> ALIASES = new LinkedHashMap<>();

    static {
        ALIASES.put("trackingNo", List.of("运单号", "物流单号", "快递单号", "单号",
                "trackingnumber", "trackingno", "number"));
        ALIASES.put("carrier", List.of("承运商", "物流商", "快递公司", "运输商", "物流公司", "carrier"));
        ALIASES.put("transportType", List.of("运输方式", "运输类型", "transport"));
        ALIASES.put("portFrom", List.of("起运港", "发货港", "始发地", "起运地", "发货地", "origin"));
        ALIASES.put("portTo", List.of("目的港", "目的地", "destination"));
        ALIASES.put("shipDate", List.of("发货日期", "出库日期", "shipdate"));
        ALIASES.put("orderNo", List.of("关联订单号", "订单号", "orderno", "ordernumber"));
    }

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy-M-d"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("yyyy.M.d"));

    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final OutboundService outboundService;
    private final String dirConfig;
    private final boolean autoConfirm;
    private final AtomicBoolean scanning = new AtomicBoolean(false);

    public DirectoryImportService(JdbcTemplate jdbcTemplate,
                                  OutboundService outboundService,
                                  @Value("${erp.import.dir}") String dirConfig,
                                  @Value("${erp.import.auto-confirm:true}") boolean autoConfirm) {
        this.jdbcTemplate = jdbcTemplate;
        this.outboundService = outboundService;
        this.dirConfig = dirConfig;
        this.autoConfirm = autoConfirm;
    }

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(importDir());
        Files.createDirectories(archiveDir());
        log.info("监控目录已就绪 dir={} archive={} autoConfirm={}", importDir(), archiveDir(), autoConfirm);
    }

    @Scheduled(fixedDelayString = "${erp.import.interval-ms:10000}")
    public void scheduledScan() {
        Map<String, Object> summary = scan();
        if (((Number) summary.get("files")).intValue() > 0) {
            log.info("目录扫描完成 files={} imported={} skipped={} errors={}",
                    summary.get("files"), summary.get("imported"),
                    summary.get("skipped"), summary.get("errors"));
        }
    }

    /**
     * 立即扫描一次，返回本次汇总结果。
     */
    public Map<String, Object> scan() {
        Map<String, Object> summary = new LinkedHashMap<>();
        int files = 0;
        int imported = 0;
        int skipped = 0;
        int errors = 0;

        if (!scanning.compareAndSet(false, true)) {
            summary.put("files", 0);
            summary.put("imported", 0);
            summary.put("skipped", 0);
            summary.put("errors", 0);
            return summary;
        }
        try {
            Path dir = importDir();
            if (!Files.isDirectory(dir)) {
                log.warn("监控目录不存在 dir={}", dir);
            } else {
                List<Path> csvFiles;
                try (Stream<Path> stream = Files.list(dir)) {
                    csvFiles = stream
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                            .sorted()
                            .toList();
                }
                for (Path file : csvFiles) {
                    int[] result = processFile(file);
                    files++;
                    imported += result[0];
                    skipped += result[1];
                    errors += result[2];
                }
            }
        } catch (IOException e) {
            log.warn("扫描监控目录失败 dir={} error={}", importDir(), e.toString());
            summary.put("error", e.toString());
        } finally {
            scanning.set(false);
        }

        summary.put("files", files);
        summary.put("imported", imported);
        summary.put("skipped", skipped);
        summary.put("errors", errors);
        return summary;
    }

    public Path importDir() {
        return Paths.get(dirConfig);
    }

    public Path archiveDir() {
        return importDir().resolve("archive");
    }

    public Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("dir", importDir().toString());
        info.put("autoConfirm", autoConfirm);
        info.put("history", history(10));
        return info;
    }

    public List<Map<String, Object>> history(int limit) {
        return jdbcTemplate.query("""
                        SELECT id, file_name, rows_total, rows_imported, rows_skipped, status, message, create_time
                        FROM erp_import_history
                        ORDER BY id DESC
                        LIMIT ?
                        """,
                this::mapHistory, limit);
    }

    private Map<String, Object> mapHistory(ResultSet rs, int rowNum) throws SQLException {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", rs.getLong("id"));
        item.put("fileName", rs.getString("file_name"));
        item.put("rowsTotal", rs.getInt("rows_total"));
        item.put("rowsImported", rs.getInt("rows_imported"));
        item.put("rowsSkipped", rs.getInt("rows_skipped"));
        item.put("status", rs.getString("status"));
        item.put("message", rs.getString("message"));
        item.put("createTime", rs.getObject("create_time", LocalDateTime.class));
        return item;
    }

    /**
     * 处理单个 CSV 文件：解析、建单、确认、写导入记录并归档。
     *
     * @return [imported, skipped, errors]
     */
    private int[] processFile(Path file) {
        String fileName = file.getFileName().toString();
        int total = 0;
        int imported = 0;
        int skipped = 0;
        int errors = 0;
        String status;
        String message = null;

        try {
            String content = readText(file);
            List<List<String>> rows = parseCsv(content);
            if (rows.isEmpty()) {
                throw new IllegalStateException("文件内容为空");
            }
            Map<String, Integer> columns = mapHeader(rows.get(0));
            Integer trackingIndex = columns.get("trackingNo");
            if (trackingIndex == null) {
                throw new IllegalStateException("未识别到运单号列，请检查表头");
            }

            Set<String> seenInFile = new HashSet<>();
            for (int i = 1; i < rows.size(); i++) {
                List<String> row = rows.get(i);
                if (row == null || row.stream().allMatch(String::isBlank)) {
                    continue;
                }
                total++;
                String trackingNo = cell(row, trackingIndex);
                if (trackingNo.isEmpty()) {
                    skipped++;
                    continue;
                }
                if (!seenInFile.add(trackingNo) || existsInDb(trackingNo)) {
                    skipped++;
                    continue;
                }
                try {
                    createOutbound(row, columns, trackingNo, fileName);
                    imported++;
                } catch (Exception e) {
                    skipped++;
                    log.warn("导入运单失败 file={} trackingNo={} error={}", fileName, trackingNo, e.toString());
                }
            }

            if (skipped > 0) {
                status = "PARTIAL";
            } else if (imported == 0) {
                status = "PARTIAL";
                message = total == 0 ? "文件中没有数据行" : "没有可导入的运单";
            } else {
                status = "SUCCESS";
            }
            if (message == null || message.isEmpty()) {
                message = String.format("总 %d 行，导入 %d 行，跳过 %d 行", total, imported, skipped);
            }
        } catch (Exception e) {
            status = "ERROR";
            message = truncate(e.getMessage() == null ? e.toString() : e.getMessage(), 500);
            errors++;
            log.warn("导入文件失败 file={} error={}", fileName, message);
        }

        // 扫描结束统一记一条导入记录
        String finalMessage = message;
        if (status != null && !"ERROR".equals(status) && finalMessage == null) {
            finalMessage = String.format("总 %d 行，导入 %d 行，跳过 %d 行", total, imported, skipped);
        }
        jdbcTemplate.update("""
                        INSERT INTO erp_import_history
                            (file_name, rows_total, rows_imported, rows_skipped, status, message)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                fileName, total, imported, skipped, status, finalMessage);

        moveToArchive(file);
        return new int[]{imported, skipped, errors};
    }

    private void createOutbound(List<String> row, Map<String, Integer> columns,
                                String trackingNo, String fileName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("trackingNo", trackingNo);
        body.put("carrier", cell(row, columns.get("carrier")));
        body.put("transportType", cell(row, columns.get("transportType")));
        body.put("portFrom", cell(row, columns.get("portFrom")));
        body.put("portTo", cell(row, columns.get("portTo")));
        body.put("orderNo", cell(row, columns.get("orderNo")));
        LocalDate shipDate = parseShipDate(cell(row, columns.get("shipDate")));
        body.put("shipDate", shipDate == null ? null : shipDate.toString());

        Map<String, Object> created = outboundService.create(body);
        if (autoConfirm) {
            Object id = created.get("id");
            if (id instanceof Number number) {
                outboundService.confirm(number.longValue(), "目录导入:" + fileName);
            }
        }
    }

    private boolean existsInDb(String trackingNo) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM erp_outbound WHERE tracking_no = ?", Integer.class, trackingNo);
        return count != null && count > 0;
    }

    private LocalDate parseShipDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(text, format);
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一种格式
            }
        }
        try {
            return LocalDateTime.parse(text, DATETIME_FORMAT).toLocalDate();
        } catch (DateTimeParseException ignored) {
            // 解析失败按今天处理
        }
        return LocalDate.now();
    }

    private String readText(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String utf8 = decodeStrict(bytes, StandardCharsets.UTF_8);
        if (utf8 != null && !utf8.contains("\uFFFD")) {
            return stripBom(utf8);
        }
        String gbk = decodeStrict(bytes, Charset.forName("GBK"));
        if (gbk != null) {
            return stripBom(gbk);
        }
        throw new IOException("文件编码无法识别（需 UTF-8 或 GBK）");
    }

    private String decodeStrict(byte[] bytes, Charset charset) {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            CharBuffer buffer = decoder.decode(ByteBuffer.wrap(bytes));
            return buffer.toString();
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private String stripBom(String text) {
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    /**
     * 支持引号包裹（"" 转义）、逗号分隔、CRLF/CR/LF 换行的 CSV 解析。
     */
    private List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else if (c == '\r') {
                // 统一忽略，按 \n 切行
            } else {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Integer> mapHeader(List<String> header) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        List<String> normalized = header.stream().map(this::normalize).toList();

        for (int i = 0; i < normalized.size(); i++) {
            String name = normalized.get(i);
            if (name.isEmpty()) {
                continue;
            }
            for (Map.Entry<String, List<String>> entry : ALIASES.entrySet()) {
                if (columns.containsKey(entry.getKey())) {
                    continue;
                }
                if (entry.getValue().contains(name)) {
                    columns.put(entry.getKey(), i);
                    break;
                }
            }
        }

        // 表头带前后缀时做包含匹配（取最长别名，避免「单号」命中「订单号」）
        for (int i = 0; i < normalized.size(); i++) {
            String name = normalized.get(i);
            if (name.isEmpty() || columns.containsValue(i)) {
                continue;
            }
            String matchedField = null;
            int matchedLength = 0;
            for (Map.Entry<String, List<String>> entry : ALIASES.entrySet()) {
                if (columns.containsKey(entry.getKey())) {
                    continue;
                }
                for (String alias : entry.getValue()) {
                    if (alias.length() >= 3 && name.contains(alias) && alias.length() > matchedLength) {
                        matchedField = entry.getKey();
                        matchedLength = alias.length();
                    }
                }
            }
            if (matchedField != null) {
                columns.putIfAbsent(matchedField, i);
            }
        }
        return columns;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s_\\-]", "").toLowerCase(Locale.ROOT);
    }

    private String cell(List<String> row, Integer index) {
        if (index == null || index < 0 || index >= row.size()) {
            return "";
        }
        String value = row.get(index);
        return value == null ? "" : value.trim();
    }

    private void moveToArchive(Path file) {
        String timestamp = LocalDateTime.now().format(ARCHIVE_FORMAT);
        String originalName = file.getFileName().toString();
        try {
            Path target = archiveDir().resolve(timestamp + "_" + originalName);
            int seq = 1;
            while (Files.exists(target)) {
                target = archiveDir().resolve(timestamp + "_" + seq++ + "_" + originalName);
            }
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("导入文件已归档 file={} target={}", originalName, target.getFileName());
        } catch (IOException e) {
            log.warn("归档文件失败 file={} error={}", originalName, e.toString());
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
