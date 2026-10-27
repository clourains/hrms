package com.hrms.util;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import jakarta.servlet.http.HttpServletResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 导出通用工具（P8-A）
 *
 * 统一处理 Content-Type、文件名 URL 编码、writer 生命周期。
 * 用法：
 *   ExcelExportUtil.write(response, "审计日志.xlsx",
 *     List.of("时间","操作人","模块",...),
 *     rows); // rows: List<List<Object>>
 */
public final class ExcelExportUtil {

    private ExcelExportUtil() {}

    /**
     * 写出 Excel 到 response 并立即 flush+close
     */
    public static void write(HttpServletResponse response, String filename,
                             List<String> headers, List<List<Object>> rows) throws Exception {
        // getWriter(true) 在内存中创建 XSSFWorkbook（.xlsx）。
        // 注意不能用无参 getWriter()（默认 HSSFWorkbook=.xls 二进制，与 Content-Type 不符），
        // 也不能用 getWriter(String 文件路径)（会真的落盘并在下次复用时读到旧内容）。
        ExcelWriter writer = ExcelUtil.getWriter(true);
        if (headers != null && !headers.isEmpty()) {
            writer.writeHeadRow(headers);
        }
        if (rows != null) {
            for (List<Object> row : rows) {
                if (row == null) continue;
                // Hutool 的 writeRow 会对 null 单元格抛 NPE，这里手动把 null 转 ""
                List<Object> safeRow = new java.util.ArrayList<>(row.size());
                for (Object cell : row) {
                    safeRow.add(cell == null ? "" : cell);
                }
                writer.writeRow(safeRow);
            }
        }
        writer.autoSizeColumnAll();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String safeName = filename.endsWith(".xlsx") ? filename : filename + ".xlsx";
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + URLEncoder.encode(safeName, StandardCharsets.UTF_8));

        writer.flush(response.getOutputStream(), true);
        writer.close();
    }
}
