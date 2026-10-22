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
        try {
            return Result.success(businessService.overview());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/monthly-sales")
    public Result<Map<String, Object>> monthlySales() {
        try {
            return Result.success(businessService.monthlySales());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/top-customers")
    public Result<List<Map<String, Object>>> topCustomers() {
        try {
            return Result.success(businessService.topCustomers());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/top-products")
    public Result<List<Map<String, Object>>> topProducts() {
        try {
            return Result.success(businessService.topProducts());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/country-distribution")
    public Result<List<Map<String, Object>>> countryDistribution() {
        try {
            return Result.success(businessService.countryDistribution());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/inventory-value")
    public Result<List<Map<String, Object>>> inventoryValue() {
        try {
            return Result.success(businessService.inventoryValue());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
