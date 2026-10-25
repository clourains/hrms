package com.hrms.controller;

import com.hrms.annotation.RequireRole;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Payment;
import com.hrms.service.PaymentService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 收付款Controller
 *
 * 业务模块整体仅管理员可访问（P045）
 */
@RequireRole("admin")
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(paymentService.pageQuery(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<Payment> getById(@PathVariable Long id) {
        return Result.success(paymentService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Payment payment) {
        return paymentService.save(payment) ? Result.success("登记成功", null) : Result.error("登记失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return paymentService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }
}
