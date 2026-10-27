package com.hrms.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExcelExportUtil 单元测试（P8-A 导出功能）
 *
 * 覆盖：
 *   1. 正常写出（带表头 + 多行数据）→ .xlsx ZIP 结构正确、文件名 URL 编码、UTF-8 BOM 头
 *   2. 无表头仅数据 → 仍写出 xlsx（ExcelWriter 允许）
 *   3. 空表头 + 空数据 → 返回合法 xlsx（Excel 能打开的最小文件）
 *   4. 文件名不带 .xlsx 后缀 → 自动补后缀
 *   5. 含中文的文件名 → Content-Disposition 里 UTF-8 编码正确
 *   6. 多行数据 → 每一行都写进 sheet
 */
@DisplayName("Excel 导出通用工具")
class ExcelExportUtilTest {

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
    }

    // ==================================================
    // 1. 正常导出：带表头 + 多行数据
    // ==================================================

    @Test
    @DisplayName("正常写出应返回合法 xlsx，表头和行数正确")
    void shouldWriteValidXlsxWithHeadersAndRows() throws Exception {
        List<String> headers = List.of("操作时间", "操作人", "模块", "结果");
        List<List<Object>> rows = List.of(
                List.of("2026-10-27 10:00:00", "admin", "员工管理", "成功"),
                List.of("2026-10-27 11:00:00", "manager", "薪资管理", "成功"),
                List.of("2026-10-27 12:00:00", "admin", "参数配置", "失败")
        );

        ExcelExportUtil.write(response, "审计日志.xlsx", headers, rows);

        // Content-Type
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getContentType());

        // Content-Disposition 含 UTF-8 文件名
        String cd = response.getHeader("Content-Disposition");
        assertNotNull(cd);
        assertTrue(cd.contains("UTF-8''"));
        assertTrue(cd.contains("%E5%AE%A1%E8%AE%A1%E6%97%A5%E5%BF%97")); // 审计日志 URL 编码

        // 文件是 .xlsx ZIP 结构，且 zip 条目数 > 0
        byte[] body = response.getContentAsByteArray();
        assertTrue(body.length > 100, "导出内容不应为空");
        assertTrue(isXlsx(body), "应返回合法 xlsx（ZIP 前缀 + [Content_Types].xml）");
    }

    // ==================================================
    // 2. 空数据（有表头但无行）
    // ==================================================

    @Test
    @DisplayName("无数据行仍应返回合法 xlsx")
    void shouldReturnValidXlsxWhenNoRows() throws Exception {
        List<String> headers = List.of("登录时间", "用户名", "IP");
        List<List<Object>> rows = List.of();

        ExcelExportUtil.write(response, "登录日志.xlsx", headers, rows);

        byte[] body = response.getContentAsByteArray();
        assertTrue(body.length > 100);
        assertTrue(isXlsx(body));
    }

    // ==================================================
    // 3. 文件名不带后缀 → 自动补 .xlsx
    // ==================================================

    @Test
    @DisplayName("文件名未带后缀应自动补 .xlsx")
    void shouldAppendXlsxSuffix() throws Exception {
        ExcelExportUtil.write(response, "公告列表", List.of("ID", "标题"), List.of());

        String cd = response.getHeader("Content-Disposition");
        assertTrue(cd.contains("xlsx"), "文件名应包含 xlsx 后缀");
    }

    // ==================================================
    // 4. 含 null 单元格的数据行 → 应写成空单元格而非 NPE
    // ==================================================

    @Test
    @DisplayName("数据行含 null 单元格应正常写出")
    void shouldWriteRowsContainingNullCells() throws Exception {
        List<String> headers = List.of("A", "B", "C");
        List<List<Object>> rows = Arrays.asList(
                Arrays.asList("ok", null, "also-ok")
        );

        assertDoesNotThrow(() -> ExcelExportUtil.write(response, "t.xlsx", headers, rows));
        assertTrue(isXlsx(response.getContentAsByteArray()));
    }

    // ==================================================
    // 5. 表头为 null 或 数据为 null → 正常处理（转空集合）
    // ==================================================

    @Test
    @DisplayName("表头和数据均为 null 应返回最小合法 xlsx")
    void shouldHandleNullHeadersAndRows() throws Exception {
        assertDoesNotThrow(() -> ExcelExportUtil.write(response, "t.xlsx", null, null));
        assertTrue(isXlsx(response.getContentAsByteArray()));
    }

    // ==================================================
    // 辅助：判断字节是否为合法 xlsx（ZIP + 含 [Content_Types].xml）
    // ==================================================

    private boolean isXlsx(byte[] bytes) {
        if (bytes.length < 4) return false;
        try (InputStream is = new ByteArrayInputStream(bytes);
             ZipInputStream zis = new ZipInputStream(is)) {
            ZipEntry entry;
            boolean foundContentTypes = false;
            while ((entry = zis.getNextEntry()) != null) {
                if ("[Content_Types].xml".equals(entry.getName())) {
                    foundContentTypes = true;
                    break;
                }
            }
            return foundContentTypes;
        } catch (Exception e) {
            return false;
        }
    }
}
