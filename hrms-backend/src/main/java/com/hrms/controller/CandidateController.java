package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.HireDTO;
import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.Candidate;
import com.hrms.service.CandidateService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 招聘候选人Controller
 */
@RequireRole("admin")
@RestController
@RequestMapping("/recruit/candidate")
public class CandidateController {

    @Autowired
    private CandidateService candidateService;

    @PostMapping("/page")
    public Result<PageResult<Candidate>> page(@RequestBody RecruitQueryDTO queryDTO) {
        return Result.success(candidateService.pageQuery(queryDTO));
    }

    /** 各阶段人数漏斗 */
    @GetMapping("/funnel")
    public Result<List<Map<String, Object>>> funnel() {
        return Result.success(candidateService.stageFunnel());
    }

    @GetMapping("/{id}")
    public Result<Candidate> getById(@PathVariable Long id) {
        return Result.success(candidateService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Candidate candidate) {
        return candidateService.save(candidate) ? Result.success("登记成功", null) : Result.error("登记失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Candidate candidate) {
        return candidateService.update(candidate) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return candidateService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }

    /**
     * 手工调整阶段，body: { stage: 4, reason: "..." }
     */
    @PutMapping("/{id}/stage")
    public Result<Void> changeStage(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer stage = body.get("stage") == null ? null : Integer.valueOf(body.get("stage").toString());
        String reason = body.get("reason") == null ? null : body.get("reason").toString();
        return candidateService.changeStage(id, stage, reason)
                ? Result.success("阶段已更新", null) : Result.error("阶段更新失败");
    }

    /**
     * 录用入职：生成员工档案与试用期记录
     */
    @PostMapping("/{id}/hire")
    public Result<Long> hire(@PathVariable Long id, @RequestBody(required = false) HireDTO hireDTO) {
        Long employeeId = candidateService.hire(id, hireDTO);
        return Result.success("已录用并生成员工档案", employeeId);
    }
}
