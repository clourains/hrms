package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.PageResult;
import com.hrms.dto.RecruitQueryDTO;
import com.hrms.entity.JobRequisition;
import com.hrms.service.JobRequisitionService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 招聘需求Controller
 *
 * 招聘模块整体仅管理员（HR）可访问，与业务模块保持同一套权限口径（P045）。
 * 路由统一挂在 /recruit 下，均为新增路径，不影响既有模块。
 */
@RequireRole("admin")
@RestController
@RequestMapping("/recruit/requisition")
public class JobRequisitionController {

    @Autowired
    private JobRequisitionService requisitionService;

    @PostMapping("/page")
    public Result<PageResult<JobRequisition>> page(@RequestBody RecruitQueryDTO queryDTO) {
        return Result.success(requisitionService.pageQuery(queryDTO));
    }

    /** 在招需求下拉（候选人登记时选岗位） */
    @GetMapping("/list")
    public Result<List<JobRequisition>> list() {
        return Result.success(requisitionService.listOpen());
    }

    /** 招聘看板统计 */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> statistics() {
        return Result.success(requisitionService.statistics());
    }

    @GetMapping("/{id}")
    public Result<JobRequisition> getById(@PathVariable Long id) {
        return Result.success(requisitionService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody JobRequisition requisition) {
        return requisitionService.save(requisition) ? Result.success("新增成功", null) : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody JobRequisition requisition) {
        return requisitionService.update(requisition) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return requisitionService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }

    /** 变更需求状态：1招聘中 2已暂停 3已完成 4已关闭 */
    @PutMapping("/{id}/status/{status}")
    public Result<Void> changeStatus(@PathVariable Long id, @PathVariable Integer status) {
        return requisitionService.changeStatus(id, status)
                ? Result.success("状态已更新", null) : Result.error("状态更新失败");
    }
}
