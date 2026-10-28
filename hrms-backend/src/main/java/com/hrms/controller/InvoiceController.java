package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Invoice;
import com.hrms.service.InvoiceService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 发票管理Controller
 *
 * 业务模块整体仅管理员可访问（P045）
 */
@RequireRole("admin")
@RestController
@RequestMapping("/invoice")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @PostMapping("/page")
    public Result<PageResult<Invoice>> page(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(invoiceService.pageQuery(queryDTO));
    }

    /**
     * 从销售订单开票
     */
    @PostMapping
    public Result<Invoice> create(@RequestBody Invoice invoice) {
        return Result.success("开票申请已创建", invoiceService.createFromOrder(invoice));
    }

    @PutMapping
    public Result<Void> update(@RequestBody Invoice invoice) {
        return invoiceService.update(invoice) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @PutMapping("/status/{id}/{status}")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        return invoiceService.updateStatus(id, status) ? Result.success("操作成功", null) : Result.error("操作失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return invoiceService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }
}
