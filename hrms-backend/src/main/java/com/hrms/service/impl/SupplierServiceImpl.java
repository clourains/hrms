package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Supplier;
import com.hrms.mapper.SupplierMapper;
import com.hrms.service.SupplierService;
import com.hrms.util.CodeReleaser;
import com.hrms.util.SortUtil;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 供应商Service实现类
 */
@Service
public class SupplierServiceImpl implements SupplierService {

    /** 供应商列表可排序字段白名单（P004） */
    private static final Map<String, SortUtil.OrderCol> SUPPLIER_SORT = new HashMap<>();

    static {
        SUPPLIER_SORT.put("supplierCode", SortUtil.OrderCol.natural("supplier_code"));
        SUPPLIER_SORT.put("companyName", SortUtil.OrderCol.natural("company_name"));
        SUPPLIER_SORT.put("country", SortUtil.OrderCol.natural("country"));
        SUPPLIER_SORT.put("category", SortUtil.OrderCol.natural("category"));
        SUPPLIER_SORT.put("status", SortUtil.OrderCol.plain("status"));
        SUPPLIER_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    @Autowired
    private SupplierMapper supplierMapper;

    @Override
    public PageResult<Supplier> pageQuery(BizQueryDTO queryDTO) {
        Page<Supplier> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Supplier::getCompanyName, queryDTO.getKeyword())
                    .or().like(Supplier::getSupplierCode, queryDTO.getKeyword())
                    .or().like(Supplier::getCategory, queryDTO.getKeyword()));
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Supplier::getStatus, queryDTO.getStatus());
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), SUPPLIER_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            wrapper.orderByDesc(Supplier::getCreateTime);
        }
        Page<Supplier> result = supplierMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public Supplier getById(Long id) {
        return supplierMapper.selectById(id);
    }

    @Override
    public boolean save(Supplier supplier) {
        Validators.text(supplier.getSupplierCode(), "供应商编码");
        Validators.text(supplier.getCompanyName(), "公司名称");
        Validators.text(supplier.getContactName(), "联系人");
        Validators.text(supplier.getPhone(), "电话");
        Validators.text(supplier.getCountry(), "国家");
        Validators.text(supplier.getCity(), "城市");
        Validators.text(supplier.getCategory(), "供应类别");
        if (supplier.getStatus() == null) {
            supplier.setStatus(1);
        }
        supplier.setCreateTime(LocalDateTime.now());
        supplier.setUpdateTime(LocalDateTime.now());
        return supplierMapper.insert(supplier) > 0;
    }

    @Override
    public boolean update(Supplier supplier) {
        if (supplier.getId() == null) {
            throw new RuntimeException("供应商不存在");
        }
        Supplier origin = supplierMapper.selectById(supplier.getId());
        if (origin == null) {
            throw new RuntimeException("供应商不存在或已删除");
        }
        Validators.notCleared(origin.getSupplierCode(), supplier.getSupplierCode(), "供应商编码");
        Validators.notCleared(origin.getCompanyName(), supplier.getCompanyName(), "公司名称");
        Validators.notCleared(origin.getContactName(), supplier.getContactName(), "联系人");
        Validators.notCleared(origin.getPhone(), supplier.getPhone(), "电话");
        Validators.notCleared(origin.getCountry(), supplier.getCountry(), "国家");
        Validators.notCleared(origin.getCity(), supplier.getCity(), "城市");
        Validators.notCleared(origin.getCategory(), supplier.getCategory(), "供应类别");
        supplier.setUpdateTime(LocalDateTime.now());
        return supplierMapper.updateById(supplier) > 0;
    }

    /**
     * 删除供应商（P043）
     *
     * 供应商目前没有被任何业务表引用（订单只关联客户，产品没有供应商字段），
     * 所以不需要做引用检查；但编码唯一索引 uk_supplier_code 同样会被已删记录占着，
     * 删除前先把编码让出来，否则同一编码再也建不出来。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw new RuntimeException("供应商不存在或已删除");
        }
        Supplier release = new Supplier();
        release.setId(id);
        release.setSupplierCode(CodeReleaser.release(supplier.getSupplierCode(), id, 50));
        release.setUpdateTime(LocalDateTime.now());
        supplierMapper.updateById(release);

        return supplierMapper.deleteById(id) > 0;
    }

    @Override
    public List<Supplier> listAll() {
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Supplier::getStatus, 1).orderByAsc(Supplier::getSupplierCode);
        return supplierMapper.selectList(wrapper);
    }
}
