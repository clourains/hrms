package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.entity.AuditLogRecord;
import com.hrms.entity.LoginLog;
import com.hrms.entity.Notice;
import com.hrms.mapper.AuditLogRecordMapper;
import com.hrms.mapper.LoginLogMapper;
import com.hrms.mapper.NoticeMapper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 导出接口单元测试（P8-A：审计日志 / 登录日志 / 公告列表导出 Excel）
 *
 * 以 Mockito 替换 Mapper、MockHttpServletResponse 承接响应体，
 * 再用 POI 把导出的 xlsx 读回来，断言表头与每一行的单元格内容，
 * 覆盖：列顺序、中文文件名编码、成功/失败与状态的中文映射、null 字段空串兜底、空结果集。
 */
@DisplayName("导出接口 Excel")
class ExportEndpointsTest {

    private AuditLogController auditLogController;
    private LoginLogController loginLogController;
    private NoticeController noticeController;

    private AuditLogRecordMapper auditLogRecordMapper;
    private LoginLogMapper loginLogMapper;
    private NoticeMapper noticeMapper;

    private MockHttpServletResponse response;
    private final DataFormatter formatter = new DataFormatter();

    @BeforeEach
    void setUp() {
        auditLogRecordMapper = mock(AuditLogRecordMapper.class);
        loginLogMapper = mock(LoginLogMapper.class);
        noticeMapper = mock(NoticeMapper.class);

        auditLogController = new AuditLogController();
        ReflectionTestUtils.setField(auditLogController, "auditLogRecordMapper", auditLogRecordMapper);

        loginLogController = new LoginLogController();
        ReflectionTestUtils.setField(loginLogController, "loginLogMapper", loginLogMapper);

        noticeController = new NoticeController();
        ReflectionTestUtils.setField(noticeController, "noticeMapper", noticeMapper);

        response = new MockHttpServletResponse();
    }

    // ==================================================
    // 审计日志导出
    // ==================================================

    @Test
    @DisplayName("审计日志导出：9 列表头 + 每行数据按列顺序写出")
    void auditExportShouldWriteHeaderAndRows() throws Exception {
        AuditLogRecord r1 = auditLog("admin", "员工管理", "新增员工", "empId=1", 1, null, 35L, "127.0.0.1");
        AuditLogRecord r2 = auditLog("manager", "薪资管理", "计算薪资", "month=2026-10", 0, "员工不存在", 12L, "10.0.0.2");
        when(auditLogRecordMapper.selectList(any())).thenReturn(List.of(r1, r2));

        auditLogController.export(null, null, null, null, null, response);

        List<List<String>> sheet = readSheet(response);
        assertEquals(3, sheet.size(), "1 行表头 + 2 行数据");

        assertEquals(List.of("操作时间", "操作人", "模块", "操作", "操作详情", "结果", "失败原因", "耗时", "IP"),
                sheet.get(0));
        assertEquals(List.of("2026-10-27 10:00:00", "admin", "员工管理", "新增员工", "empId=1", "成功", "", "35ms", "127.0.0.1"),
                sheet.get(1));
        assertEquals("失败", sheet.get(2).get(5));
        assertEquals("员工不存在", sheet.get(2).get(6));

        verify(auditLogRecordMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("审计日志导出：中文文件名按 UTF-8 编码写入 Content-Disposition")
    void auditExportShouldEncodeChineseFilename() throws Exception {
        when(auditLogRecordMapper.selectList(any())).thenReturn(List.of());

        auditLogController.export(null, null, null, null, null, response);

        String cd = response.getHeader("Content-Disposition");
        assertNotNull(cd);
        assertTrue(cd.contains("filename*=UTF-8''"), cd);
        assertTrue(cd.contains("%E5%AE%A1%E8%AE%A1%E6%97%A5%E5%BF%97.xlsx"), cd); // 审计日志.xlsx
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                response.getContentType());
    }

    @Test
    @DisplayName("审计日志导出：无数据时仅输出表头行")
    void auditExportShouldWriteHeaderOnlyWhenEmpty() throws Exception {
        when(auditLogRecordMapper.selectList(any())).thenReturn(List.of());

        auditLogController.export(null, null, null, null, null, response);

        List<List<String>> sheet = readSheet(response);
        assertEquals(1, sheet.size(), "空结果集只应有表头");
        assertEquals(9, sheet.get(0).size());
    }

    @Test
    @DisplayName("审计日志导出：全字段为 null 时不抛异常，输出空串")
    void auditExportShouldTolerateAllNullFields() throws Exception {
        when(auditLogRecordMapper.selectList(any())).thenReturn(List.of(new AuditLogRecord()));

        assertDoesNotThrow(() -> auditLogController.export(null, null, null, null, null, response));

        List<List<String>> sheet = readSheet(response);
        assertEquals(2, sheet.size());
        // success 为 null 时按“非成功”渲染为失败
        assertEquals("失败", sheet.get(1).get(5));
        assertEquals("", sheet.get(1).get(1));
    }

    // ==================================================
    // 登录日志导出
    // ==================================================

    @Test
    @DisplayName("登录日志导出：6 列表头 + 成功/失败中文映射")
    void loginLogExportShouldWriteHeaderAndRows() throws Exception {
        LoginLog ok = loginLog("admin", 1, "登录成功", "127.0.0.1", "Mozilla/5.0 Chrome");
        LoginLog fail = loginLog("admin", 0, "用户名或密码错误", "127.0.0.1", "curl/8.0");
        when(loginLogMapper.selectList(any())).thenReturn(List.of(ok, fail));

        loginLogController.export(null, null, null, null, response);

        List<List<String>> sheet = readSheet(response);
        assertEquals(3, sheet.size());
        assertEquals(List.of("登录时间", "用户名", "结果", "说明", "IP", "浏览器"), sheet.get(0));
        assertEquals(List.of("2026-10-27 10:00:00", "admin", "成功", "登录成功", "127.0.0.1", "Mozilla/5.0 Chrome"),
                sheet.get(1));
        assertEquals("失败", sheet.get(2).get(2));

        assertTrue(response.getHeader("Content-Disposition")
                .contains("%E7%99%BB%E5%BD%95%E6%97%A5%E5%BF%97.xlsx")); // 登录日志.xlsx
    }

    @Test
    @DisplayName("登录日志导出：浏览器 UA 为 null 时输出空串")
    void loginLogExportShouldTolerateNullUserAgent() throws Exception {
        when(loginLogMapper.selectList(any())).thenReturn(List.of(loginLog("admin", 1, "登录成功", "127.0.0.1", null)));

        loginLogController.export(null, null, null, null, response);

        List<List<String>> sheet = readSheet(response);
        assertEquals("", sheet.get(1).get(5));
    }

    // ==================================================
    // 公告列表导出
    // ==================================================

    @Test
    @DisplayName("公告导出：6 列表头 + 级别/状态中文映射")
    void noticeExportShouldWriteHeaderAndRows() throws Exception {
        Notice published = notice(1L, "国庆放假通知", "important", 1, "admin");
        Notice offline = notice(2L, "系统维护", null, 0, null);
        when(noticeMapper.selectList(any())).thenReturn(List.of(published, offline));

        noticeController.export(null, null, response);

        List<List<String>> sheet = readSheet(response);
        assertEquals(3, sheet.size());
        assertEquals(List.of("ID", "标题", "级别", "状态", "发布人", "发布时间"), sheet.get(0));
        assertEquals(List.of("1", "国庆放假通知", "important", "已发布", "admin", "2026-10-27 10:00:00"),
                sheet.get(1));
        // level 为 null 兜底 normal，status=0 → 已下线，publisher 为 null → 空串
        assertEquals(List.of("2", "系统维护", "normal", "已下线", "", "2026-10-27 10:00:00"),
                sheet.get(2));

        assertTrue(response.getHeader("Content-Disposition")
                .contains("%E5%85%AC%E5%91%8A%E5%88%97%E8%A1%A8.xlsx")); // 公告列表.xlsx
    }

    // ==================================================
    // 权限注解（导出接口不应被非管理员调用）
    // ==================================================

    @Test
    @DisplayName("三个导出控制器均声明了 @RequireRole 管理员限制")
    void exportControllersShouldDeclareAdminRole() throws Exception {
        assertAdminOnly(AuditLogController.class, "export");
        assertAdminOnly(LoginLogController.class, "export");
        assertAdminOnly(NoticeController.class, "export");
    }

    /**
     * 注解可在类上（整类限制）或 export 方法上（单接口限制），两种都算数
     */
    private void assertAdminOnly(Class<?> controller, String methodName) {
        RequireRole role = controller.getAnnotation(RequireRole.class);
        if (role == null) {
            for (Method m : controller.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    role = m.getAnnotation(RequireRole.class);
                    if (role != null) break;
                }
            }
        }
        assertNotNull(role, controller.getSimpleName() + " 缺少 @RequireRole");
        assertArrayEquals(new String[]{"admin"}, role.value(),
                controller.getSimpleName() + " 应仅允许 admin 导出");
    }

    // ==================================================
    // 辅助
    // ==================================================

    /** 把导出字节读成二维字符串表（首行为表头） */
    private List<List<String>> readSheet(MockHttpServletResponse resp) throws Exception {
        byte[] bytes = resp.getContentAsByteArray();
        assertTrue(bytes.length > 0, "导出内容不应为空");
        List<List<String>> rows = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                List<String> cells = new ArrayList<>();
                if (row != null) {
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        cells.add(formatter.formatCellValue(row.getCell(c)));
                    }
                }
                rows.add(cells);
            }
        }
        return rows;
    }

    private AuditLogRecord auditLog(String username, String module, String action, String detail,
                                    Integer success, String errorMsg, Long costMs, String ip) {
        AuditLogRecord r = new AuditLogRecord();
        r.setUsername(username);
        r.setModule(module);
        r.setAction(action);
        r.setDetail(detail);
        r.setSuccess(success);
        r.setErrorMsg(errorMsg);
        r.setCostMs(costMs);
        r.setIp(ip);
        r.setCreateTime(LocalDateTime.of(2026, 10, 27, 10, 0, 0));
        return r;
    }

    private LoginLog loginLog(String username, Integer success, String msg, String ip, String ua) {
        LoginLog l = new LoginLog();
        l.setUsername(username);
        l.setSuccess(success);
        l.setMsg(msg);
        l.setIp(ip);
        l.setUserAgent(ua);
        l.setCreateTime(LocalDateTime.of(2026, 10, 27, 10, 0, 0));
        return l;
    }

    private Notice notice(Long id, String title, String level, Integer status, String publisher) {
        Notice n = new Notice();
        n.setId(id);
        n.setTitle(title);
        n.setLevel(level);
        n.setStatus(status);
        n.setPublisher(publisher);
        n.setCreateTime(LocalDateTime.of(2026, 10, 27, 10, 0, 0));
        return n;
    }
}
