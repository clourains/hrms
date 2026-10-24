package com.hrms.controller;

import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Product;
import com.hrms.service.ProductService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品Controller
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping("/page")
    public Result<PageResult<Product>> page(@RequestBody BizQueryDTO queryDTO) {
        return Result.success(productService.pageQuery(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        return Result.success(productService.getById(id));
    }

    /**
     * 产品下拉列表
     *
     * @param includeDisabled 传 true 时连下架产品一起返回（出入库、订单明细用，P021）
     */
    @GetMapping("/list")
    public Result<List<Product>> list(@RequestParam(value = "includeDisabled", required = false) Boolean includeDisabled) {
        boolean include = Boolean.TRUE.equals(includeDisabled);
        return Result.success(productService.listAll(include));
    }

    @PostMapping
    public Result<Void> save(@RequestBody Product product) {
        return productService.save(product) ? Result.success("新增成功", null) : Result.error("新增失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Product product) {
        return productService.update(product) ? Result.success("更新成功", null) : Result.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        return productService.deleteById(id) ? Result.success("删除成功", null) : Result.error("删除失败");
    }
}
