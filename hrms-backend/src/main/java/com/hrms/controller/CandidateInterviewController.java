package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.CandidateInterview;
import com.hrms.service.CandidateInterviewService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 面试安排Controller
 */
@RequireRole("admin")
@RestController
@RequestMapping("/recruit/interview")
public class CandidateInterviewController {

    @Autowired
    private CandidateInterviewService interviewService;

    @PostMapping("/page")
    public Result<PageResult<CandidateInterview>> page(@RequestBody RecruitQueryDTO queryDTO) {
        return Result.success(interviewService.pageQuery(queryDTO));
    }

    /** 某候选人的全部面试记录 */
    @GetMapping("/candidate/{candidateId}")
    public Result<List<CandidateInterview>> listByCandidate(@PathVariable Long candidateId) {
        return Result.success(interviewService.listByCandidate(candidateId));
    }

    @GetMapping("/{id}")
    public Result<CandidateInterview> getById(@PathVariable Long id) {
        return Result.success(interviewService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody CandidateInterview interview) {
        return interviewService.save(interview) ? Result.success("面试已安排", null) : Result.error("安排失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody CandidateInterview interview) {
        return interviewService.update(interview) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return interviewService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }

    /**
     * 登记面试结果，body: { result: 1通过/2未通过/3爽约, score: 85, comment: "..." }
     */
    @PutMapping("/{id}/result")
    public Result<Void> submitResult(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer result = body.get("result") == null ? null : Integer.valueOf(body.get("result").toString());
        Integer score = body.get("score") == null || "".equals(body.get("score"))
                ? null : Integer.valueOf(body.get("score").toString());
        String comment = body.get("comment") == null ? null : body.get("comment").toString();
        return interviewService.submitResult(id, result, score, comment)
                ? Result.success("面试结果已登记", null) : Result.error("登记失败");
    }
}
