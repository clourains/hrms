package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.Probation;
import com.hrms.service.ProbationService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 试用期转正Controller
 */
@RequireRole("admin")
@RestController
@RequestMapping("/recruit/probation")
public class ProbationController {

    @Autowired
    private ProbationService probationService;

    @PostMapping("/page")
    public Result<PageResult<Probation>> page(@RequestBody RecruitQueryDTO queryDTO) {
        return Result.success(probationService.pageQuery(queryDTO));
    }

    /** 试用期概览：试用中、30 天内到期、已逾期、本月转正 */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.success(probationService.statistics());
    }

    @GetMapping("/{id}")
    public Result<Probation> getById(@PathVariable Long id) {
        return Result.success(probationService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Probation probation) {
        return probationService.save(probation) ? Result.success("登记成功", null) : Result.error("登记失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Probation probation) {
        return probationService.update(probation) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return probationService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }

    /**
     * 转正，body: { score: 88, evaluation: "..." }
     */
    @PutMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer score = body.get("score") == null || "".equals(body.get("score"))
                ? null : Integer.valueOf(body.get("score").toString());
        String evaluation = body.get("evaluation") == null ? null : body.get("evaluation").toString();
        return probationService.confirm(id, score, evaluation)
                ? Result.success("已办理转正", null) : Result.error("转正失败");
    }

    /**
     * 不予转正，body: { evaluation: "原因（必填）" }
     */
    @PutMapping("/{id}/fail")
    public Result<Void> fail(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String evaluation = body.get("evaluation") == null ? null : body.get("evaluation").toString();
        return probationService.fail(id, evaluation)
                ? Result.success("已记录为未通过", null) : Result.error("操作失败");
    }

    /**
     * 延长试用期，body: { newEnd: "2027-01-31", evaluation: "..." }
     */
    @PutMapping("/{id}/extend")
    public Result<Void> extend(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String end = body.get("newEnd") == null ? null : body.get("newEnd").toString();
        String evaluation = body.get("evaluation") == null ? null : body.get("evaluation").toString();
        LocalDate newEnd = (end == null || end.trim().isEmpty()) ? null : LocalDate.parse(end);
        return probationService.extend(id, newEnd, evaluation)
                ? Result.success("试用期已延长", null) : Result.error("操作失败");
    }
}
