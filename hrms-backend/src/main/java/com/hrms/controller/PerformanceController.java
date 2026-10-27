package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.AssessmentCycle;
import com.hrms.entity.Department;
import com.hrms.entity.Employee;
import com.hrms.entity.Performance;
import com.hrms.entity.PerformanceIndicator;
import com.hrms.mapper.AssessmentCycleMapper;
import com.hrms.mapper.DepartmentMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.PerformanceIndicatorMapper;
import com.hrms.service.PerformanceService;
import com.hrms.util.ExcelExportUtil;
import com.hrms.util.Result;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 绩效Controller
 */
@RestController
@RequestMapping("/performance")
public class PerformanceController {

    @Autowired
    private PerformanceService performanceService;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private DepartmentMapper departmentMapper;

    @Autowired
    private AssessmentCycleMapper assessmentCycleMapper;

    @Autowired
    private PerformanceIndicatorMapper performanceIndicatorMapper;

    /**
     * 考核周期列表
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/cycles")
    public Result<List<AssessmentCycle>> cycles() {
        try {
            List<AssessmentCycle> list = assessmentCycleMapper.selectList(
                    new LambdaQueryWrapper<AssessmentCycle>().orderByDesc(AssessmentCycle::getPeriod));
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 创建考核周期
     */
    @RequireRole("admin")
    @AuditLog(module = "绩效管理", action = "创建考核周期")
    @PostMapping("/cycle")
    public Result<Void> createCycle(@RequestBody AssessmentCycle cycle) {
        try {
            if (cycle.getPeriod() == null || cycle.getPeriod().isEmpty()) {
                return Result.error("考核月份不能为空");
            }
            Long exists = assessmentCycleMapper.selectCount(
                    new LambdaQueryWrapper<AssessmentCycle>().eq(AssessmentCycle::getPeriod, cycle.getPeriod()));
            if (exists != null && exists > 0) {
                return Result.error("该月份的考核周期已存在");
            }
            assessmentCycleMapper.insert(cycle);
            return Result.success("创建成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 绩效指标配置
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/indicators")
    public Result<List<PerformanceIndicator>> indicators() {
        try {
            List<PerformanceIndicator> list = performanceIndicatorMapper.selectList(
                    new LambdaQueryWrapper<PerformanceIndicator>().orderByAsc(PerformanceIndicator::getSort));
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 保存绩效指标权重（整体更新已有指标）
     */
    @RequireRole("admin")
    @AuditLog(module = "绩效管理", action = "指标配置")
    @PutMapping("/indicators")
    public Result<Void> saveIndicators(@RequestBody List<PerformanceIndicator> indicators) {
        try {
            for (PerformanceIndicator it : indicators) {
                if (it.getId() == null) continue;
                performanceIndicatorMapper.updateById(it);
            }
            return Result.success("保存成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 导出绩效报表（当前考核周期）
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/export")
    public void export(@RequestParam String period, HttpServletResponse response) throws Exception {
        try {
            List<Performance> list = performanceService.listByPeriod(period);
            Map<Long, Employee> empMap = new HashMap<>();
            for (Employee e : employeeMapper.selectList(null)) {
                empMap.put(e.getId(), e);
            }
            Map<Long, String> deptMap = new HashMap<>();
            for (Department d : departmentMapper.selectList(null)) {
                deptMap.put(d.getId(), d.getDeptName());
            }
            List<List<Object>> rows = new ArrayList<>();
            for (Performance p : list) {
                Employee e = empMap.get(p.getEmpId());
                rows.add(Arrays.asList(
                        e == null ? "" : e.getEmpNo(),
                        e == null ? "" : e.getName(),
                        e == null ? "" : deptMap.getOrDefault(e.getDeptId(), ""),
                        p.getPeriod(),
                        p.getWorkQuality(),
                        p.getWorkEfficiency(),
                        p.getWorkAttitude(),
                        p.getTeamwork(),
                        p.getInnovation(),
                        p.getTotalScore(),
                        p.getGrade(),
                        p.getComment()
                ));
            }
            ExcelExportUtil.write(response, "绩效报表_" + period + ".xlsx",
                    List.of("工号", "姓名", "部门", "考核周期", "工作质量", "工作效率", "工作态度",
                            "团队协作", "创新改进", "总分", "等级", "评语"),
                    rows);
        } catch (Exception ex) {
            response.setStatus(500);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":500,\"message\":\"导出失败\",\"data\":null}");
        }
    }

    /**
     * 根据ID获取绩效记录
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/{id}")
    public Result<Performance> getById(@PathVariable Long id) {
        try {
            Performance performance = performanceService.getById(id);
            return Result.success(performance);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取员工绩效列表
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/listByEmpId")
    public Result<List<Performance>> listByEmpId(@RequestParam Long empId) {
        try {
            List<Performance> list = performanceService.listByEmpId(empId);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据周期查询绩效
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/listByPeriod")
    public Result<List<Performance>> listByPeriod(@RequestParam String period) {
        try {
            List<Performance> list = performanceService.listByPeriod(period);
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 绩效评估
     */
    @RequireRole("admin")
    @AuditLog(module = "绩效管理", action = "考评")
    @PostMapping("/evaluate")
    public Result<Void> evaluate(@RequestBody Performance performance) {
        try {
            boolean result = performanceService.evaluate(performance);
            if (result) {
                return Result.success("评估成功", null);
            } else {
                return Result.error("评估失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 员工自评：写入自评总分/总结/改进计划，流程流转到待上级评分
     */
    @RequireRole({"admin", "manager"})
    @AuditLog(module = "绩效管理", action = "员工自评")
    @PutMapping("/selfReview/{id}")
    public Result<Void> selfReview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Performance p = performanceService.getById(id);
            if (p == null) {
                return Result.error("绩效记录不存在");
            }
            BigDecimal selfScore = toDecimal(body.get("selfScore"));
            if (selfScore != null && (selfScore.compareTo(BigDecimal.ZERO) < 0
                    || selfScore.compareTo(new BigDecimal("100")) > 0)) {
                return Result.error("自评总分需在 0-100 之间");
            }
            p.setSelfScore(selfScore);
            p.setSelfSummary(toText(body.get("selfSummary")));
            p.setImprovementPlan(toText(body.get("improvementPlan")));
            p.setFlowStatus("pending_manager");
            performanceService.update(p);
            return Result.success("自评已提交", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 上级评分：写入五个维度得分与评语，流程流转到待面谈
     */
    @RequireRole({"admin", "manager"})
    @AuditLog(module = "绩效管理", action = "上级评分")
    @PutMapping("/managerReview/{id}")
    public Result<Void> managerReview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Performance p = performanceService.getById(id);
            if (p == null) {
                return Result.error("绩效记录不存在");
            }
            BigDecimal quality = toDecimal(body.get("workQuality"));
            BigDecimal efficiency = toDecimal(body.get("workEfficiency"));
            BigDecimal attitude = toDecimal(body.get("workAttitude"));
            BigDecimal teamwork = toDecimal(body.get("teamwork"));
            BigDecimal innovation = toDecimal(body.get("innovation"));
            for (BigDecimal v : new BigDecimal[]{quality, efficiency, attitude, teamwork, innovation}) {
                if (v != null && (v.compareTo(BigDecimal.ZERO) < 0
                        || v.compareTo(new BigDecimal("100")) > 0)) {
                    return Result.error("各维度得分需在 0-100 之间");
                }
            }
            p.setWorkQuality(quality);
            p.setWorkEfficiency(efficiency);
            p.setWorkAttitude(attitude);
            p.setTeamwork(teamwork);
            p.setInnovation(innovation);
            p.setComment(toText(body.get("comment")));
            p.setEvaluator(com.hrms.util.UserContext.get());
            p.setFlowStatus("pending_interview");
            // 先由 Service 按权重重算总分/等级，再回填上级评分总分（同一 0-100 口径）
            performanceService.update(p);
            p.setManagerScore(p.getTotalScore());
            performanceService.update(p);
            return Result.success("评分已提交", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 绩效面谈：记录面谈内容与时间，流程结束
     */
    @RequireRole({"admin", "manager"})
    @AuditLog(module = "绩效管理", action = "绩效面谈")
    @PutMapping("/interview/{id}")
    public Result<Void> interview(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Performance p = performanceService.getById(id);
            if (p == null) {
                return Result.error("绩效记录不存在");
            }
            String note = toText(body.get("interviewNote"));
            if (note == null || note.trim().isEmpty()) {
                return Result.error("请填写面谈记录");
            }
            p.setInterviewNote(note);
            p.setInterviewTime(parseDateTime(toText(body.get("interviewTime"))));
            p.setFlowStatus("completed");
            performanceService.update(p);
            return Result.success("面谈记录已保存", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    private BigDecimal toDecimal(Object value) {
        if (value == null || value.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toText(Object value) {
        return value == null ? null : value.toString();
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : values) {
            if (v != null) {
                total = total.add(v);
            }
        }
        return total;
    }

    private LocalDateTime parseDateTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        String value = text.trim();
        try {
            return LocalDateTime.parse(value.replace('T', ' '),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
            // 允许分钟精度
        }
        try {
            return LocalDateTime.parse(value.replace('T', ' '),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception ignored) {
            return LocalDateTime.now();
        }
    }

    /**
     * 下载绩效导入模板
     */
    @RequireRole("admin")
    @GetMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws Exception {
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.writeHeadRow(Arrays.asList("工号*", "姓名*", "考核周期*(yyyy-MM)", "工作质量",
                "工作效率", "工作态度", "团队协作", "创新改进", "评语"));
        writer.writeRow(Arrays.asList("E1001", "张三", "2026-10", 85, 88, 90, 86, 82, "工作认真负责"));
        writer.autoSizeColumnAll();
        String fileName = URLEncoder.encode("绩效导入模板.xlsx", StandardCharsets.UTF_8.name());
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        writer.flush(response.getOutputStream());
        writer.close();
    }

    /**
     * Excel 导入绩效：逐行校验，失败行不影响成功行
     */
    @RequireRole("admin")
    @AuditLog(module = "绩效管理", action = "Excel导入")
    @PostMapping("/import")
    public Result<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (file == null || file.isEmpty()) {
                return Result.error("请选择要导入的 Excel 文件");
            }
            List<Employee> employees = employeeMapper.selectList(null);
            Map<String, Employee> byNo = employees.stream()
                    .filter(e -> e.getEmpNo() != null && !e.getEmpNo().isEmpty())
                    .collect(Collectors.toMap(Employee::getEmpNo, e -> e, (a, b) -> a));
            Map<String, Employee> byName = employees.stream()
                    .filter(e -> e.getName() != null && !e.getName().isEmpty())
                    .collect(Collectors.toMap(Employee::getName, e -> e, (a, b) -> a));

            List<List<Object>> rows;
            try (ExcelReader reader = ExcelUtil.getReader(file.getInputStream())) {
                rows = reader.read();
            }
            if (rows == null || rows.size() <= 1) {
                return Result.error("模板中没有可导入的数据");
            }

            int success = 0;
            List<Map<String, Object>> errors = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                String empNo = cell(row, 0);
                String name = cell(row, 1);
                String period = cell(row, 2);
                if (empNo.isEmpty() && name.isEmpty() && period.isEmpty()) {
                    continue;
                }
                String reason = null;
                if (!period.matches("\\d{4}-\\d{2}")) {
                    reason = "考核周期格式应为 yyyy-MM";
                }
                Employee emp = byNo.get(empNo);
                if (emp == null && !name.isEmpty()) {
                    emp = byName.get(name);
                }
                if (reason == null && emp == null) {
                    reason = "未匹配到员工（工号/姓名）";
                }
                BigDecimal[] scores = new BigDecimal[5];
                for (int c = 0; c < 5; c++) {
                    BigDecimal score = toDecimal(cell(row, 3 + c));
                    if (score == null) {
                        score = BigDecimal.ZERO;
                    }
                    if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
                        reason = "第 " + (i + 1) + " 行「" + HEADER_SCORE[c] + "」需在 0-100 之间";
                        break;
                    }
                    scores[c] = score;
                }
                if (reason == null) {
                    try {
                        Performance p = new Performance();
                        p.setEmpId(emp.getId());
                        p.setPeriod(period);
                        p.setWorkQuality(scores[0]);
                        p.setWorkEfficiency(scores[1]);
                        p.setWorkAttitude(scores[2]);
                        p.setTeamwork(scores[3]);
                        p.setInnovation(scores[4]);
                        p.setComment(cell(row, 8));
                        p.setEvaluator("Excel导入");
                        p.setStatus(2);
                        p.setFlowStatus("completed");
                        performanceService.save(p);
                        success++;
                    } catch (Exception e) {
                        reason = e.getMessage();
                    }
                }
                if (reason != null) {
                    Map<String, Object> error = new HashMap<>();
                    error.put("row", i + 1);
                    error.put("empNo", empNo);
                    error.put("name", name);
                    error.put("reason", reason);
                    errors.add(error);
                }
            }
            result.put("success", success);
            result.put("failed", errors.size());
            result.put("errors", errors);
            return Result.success("导入完成", result);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    private static final String[] HEADER_SCORE = {"工作质量", "工作效率", "工作态度", "团队协作", "创新改进"};

    private String cell(List<Object> row, int index) {
        if (row == null || row.size() <= index || row.get(index) == null) {
            return "";
        }
        return row.get(index).toString().trim();
    }

    /**
     * 绩效统计
     */
    @RequireRole({"admin", "manager"})
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics(@RequestParam String period) {
        try {
            Map<String, Object> statistics = performanceService.statistics(period);
            return Result.success(statistics);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 新增绩效记录
     */
    @RequireRole("admin")
    @AuditLog(module = "绩效管理", action = "新增")
    @PostMapping
    public Result<Void> save(@RequestBody Performance performance) {
        try {
            // 新建记录从「待自评」开始，导入的存量成绩会显式标记 completed
            if (performance.getFlowStatus() == null || performance.getFlowStatus().isEmpty()) {
                performance.setFlowStatus("pending_self");
            }
            boolean result = performanceService.save(performance);
            if (result) {
                return Result.success("新增成功", null);
            } else {
                return Result.error("新增失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新绩效记录
     */
    @RequireRole("admin")
    @AuditLog(module = "绩效管理", action = "修改")
    @PutMapping
    public Result<Void> update(@RequestBody Performance performance) {
        try {
            boolean result = performanceService.update(performance);
            if (result) {
                return Result.success("更新成功", null);
            } else {
                return Result.error("更新失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 根据ID删除绩效记录
     */
    @RequireRole("admin")
    @AuditLog(module = "绩效管理", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            boolean result = performanceService.deleteById(id);
            if (result) {
                return Result.success("删除成功", null);
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
