package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.OfflineDeal;
import com.hrms.service.OfflineDealService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 线下交易台账Controller
 *
 * 业务模块整体仅管理员可访问（P045）
 */
@RequireRole("admin")
@RestController
@RequestMapping("/offline-deal")
public class OfflineDealController {

    @Autowired
    private OfflineDealService offlineDealService;

    @PostMapping("/page")
    public Result<PageResult<OfflineDeal>> page(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(offlineDealService.pageQuery(queryDTO));
    }

    @PostMapping
    public Result<Void> save(@RequestBody OfflineDeal deal) {
        return offlineDealService.save(deal) ? Result.success("新增成功", null) : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody OfflineDeal deal) {
        return offlineDealService.update(deal) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @PutMapping("/status/{id}/{status}")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        return offlineDealService.updateStatus(id, status) ? Result.success("操作成功", null) : Result.error("操作失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return offlineDealService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }
}
