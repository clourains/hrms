package com.hrms.util;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel 读写工具
 *
 * 读：第一行当表头，把每个单元格按「表头名 → 文本值」放进 Map，
 *     并附上 "__row" 记录 Excel 行号（出错时能指回用户看到的第几行）。
 * 写：用于下载导入模板（表头 + 一行示例）。
 */
public final class ExcelUtil {

    /** 行号在 Map 里的保留键 */
    public static final String ROW_KEY = "__row";

    /** 单文件大小上限：5MB，防止超大文件拖垮解析线程 */
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    /** 单次最大数据行数 */
    private static final int MAX_ROWS = 5000;

    private ExcelUtil() {
    }

    /**
     * 读取上传文件的首个工作表
     */
    public static List<Map<String, String>> read(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要导入的 Excel 文件");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
            throw new RuntimeException("仅支持 .xlsx / .xls 格式的 Excel 文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Excel 文件不能超过 5MB，请拆分后再导入");
        }

        try (InputStream in = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new RuntimeException("Excel 第一个工作表为空");
            }

            List<String> headers = new ArrayList<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                headers.add(formatter.formatCellValue(headerRow.getCell(c)).trim());
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                boolean blank = true;
                for (int c = 0; c < headers.size(); c++) {
                    String key = headers.get(c);
                    if (key.isEmpty()) {
                        continue;
                    }
                    Cell cell = row.getCell(c);
                    String value = cell == null ? "" : formatter.formatCellValue(cell).trim();
                    if (!value.isEmpty()) {
                        blank = false;
                    }
                    values.put(key, value);
                }
                if (blank) {
                    continue;
                }
                values.put(ROW_KEY, String.valueOf(r + 1));
                rows.add(values);
                if (rows.size() > MAX_ROWS) {
                    throw new RuntimeException("单次最多导入 " + MAX_ROWS + " 行，请拆分后再导入");
                }
            }
            return rows;
        }
    }

    /**
     * 输出导入模板（表头 + 示例行）
     */
    public static void writeTemplate(HttpServletResponse response, String fileName, String sheetName,
                                     List<String> headers, List<String> sampleRow) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                header.createCell(i).setCellValue(headers.get(i));
            }
            if (sampleRow != null && !sampleRow.isEmpty()) {
                Row sample = sheet.createRow(1);
                for (int i = 0; i < sampleRow.size(); i++) {
                    sample.createCell(i).setCellValue(sampleRow.get(i));
                }
            }
            for (int i = 0; i < headers.size(); i++) {
                sheet.setColumnWidth(i, Math.min(6000, Math.max(3000, headers.get(i).length() * 500)));
            }

            String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
            workbook.write(response.getOutputStream());
            response.flushBuffer();
        }
    }
}
