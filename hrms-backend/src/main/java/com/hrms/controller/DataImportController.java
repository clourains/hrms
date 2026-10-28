package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.ImportResult;
import com.hrms.entity.ImportLog;
import com.hrms.service.DataImportService;
import com.hrms.service.ImportAuditService;
import com.hrms.util.ExcelUtil;
import com.hrms.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Excel 批量导入：模板下载 + 上传导入
 *
 * module 取值：customer / supplier / product / order / invoice / offline-deal
 */
@RequireRole("admin")
@RestController
@RequestMapping("/import")
public class DataImportController {

    @Autowired
    private DataImportService dataImportService;

    @Autowired
    private ImportAuditService importAuditService;

    /**
     * 下载对应模块的导入模板（表头 + 一行示例）
     */
    @GetMapping("/template/{module}")
    public void template(@PathVariable String module, HttpServletResponse response) throws IOException {
        String label = dataImportService.moduleLabel(module);
        ExcelUtil.writeTemplate(response, label + "导入模板.xlsx", label,
                dataImportService.headers(module), dataImportService.sampleRow(module));
    }

    /**
     * 上传 Excel 并导入，返回逐行结果（成功/跳过/失败）
     */
    @PostMapping("/upload/{module}")
    public Result<ImportResult> upload(@PathVariable String module,
                                       @RequestParam("file") MultipartFile file) {
        try {
            ImportResult result = dataImportService.importData(module, ExcelUtil.read(file));
            // 留痕：谁在什么时候上传了什么文件、导入成功/跳过/失败多少条（不记录具体字段值）
            importAuditService.record(module, "EXCEL", file.getOriginalFilename(), result);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error("Excel 文件读取失败，请确认文件未损坏后重试");
        }
    }

    /**
     * 最近导入记录（按模块/来源过滤），用于核对是谁导入的、结果如何
     * source：EXCEL / API，留空查全部
     */
    @GetMapping("/logs")
    public Result<List<ImportLog>> logs(@RequestParam(required = false) String module,
                                       @RequestParam(required = false) String source,
                                       @RequestParam(defaultValue = "20") int limit) {
        return Result.success(importAuditService.recent(module, source, limit));
    }
}
