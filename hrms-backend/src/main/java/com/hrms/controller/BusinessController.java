package com.hrms.controller;

import com.hrms.service.BusinessService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 营业分析Controller
 */
@RestController
@RequestMapping("/business")
public class BusinessController {

    @Autowired
    private BusinessService businessService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(businessService.overview());
    }

    @GetMapping("/monthly-sales")
    public Result<Map<String, Object>> monthlySales() {
        return Result.success(businessService.monthlySales());
    }

    @GetMapping("/top-customers")
    public Result<List<Map<String, Object>>> topCustomers() {
        return Result.success(businessService.topCustomers());
    }

    @GetMapping("/top-products")
    public Result<List<Map<String, Object>>> topProducts() {
        return Result.success(businessService.topProducts());
    }

    @GetMapping("/country-distribution")
    public Result<List<Map<String, Object>>> countryDistribution() {
        return Result.success(businessService.countryDistribution());
    }

    @GetMapping("/inventory-value")
    public Result<List<Map<String, Object>>> inventoryValue() {
        return Result.success(businessService.inventoryValue());
    }

    @GetMapping("/profit-ranking")
    public Result<Map<String, Object>> profitRanking() {
        return Result.success(businessService.profitRanking());
    }
}
