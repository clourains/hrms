package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.OfflineDeal;
import com.hrms.mapper.OfflineDealMapper;
import com.hrms.service.OfflineDealService;
import com.hrms.util.SortUtil;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 线下交易Service实现类
 */
@Service
public class OfflineDealServiceImpl implements OfflineDealService {

    /** 线下交易列表可排序字段白名单 */
    private static final Map<String, SortUtil.OrderCol> DEAL_SORT = new HashMap<>();

    static {
        DEAL_SORT.put("dealNo", SortUtil.OrderCol.natural("deal_no"));
        DEAL_SORT.put("amount", SortUtil.OrderCol.plain("amount"));
        DEAL_SORT.put("dealDate", SortUtil.OrderCol.plain("deal_date"));
        DEAL_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    private static final int ST_PENDING = 0;   // 待确认
    private static final int ST_CONFIRMED = 1; // 已确认
    private static final int ST_CANCELLED = 9; // 已取消

    @Autowired
    private OfflineDealMapper offlineDealMapper;

    @Override
    public PageResult<OfflineDeal> pageQuery(BizQueryDTO queryDTO) {
        Page<OfflineDeal> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<OfflineDeal> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(OfflineDeal::getDealNo, queryDTO.getKeyword())
                    .or().like(OfflineDeal::getCustomerName, queryDTO.getKeyword())
                    .or().like(OfflineDeal::getItemName, queryDTO.getKeyword()));
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(OfflineDeal::getStatus, queryDTO.getStatus());
        }
        if (StringUtils.hasText(queryDTO.getCountry())) {
            wrapper.eq(OfflineDeal::getCustomerCountry, queryDTO.getCountry());
        }
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(OfflineDeal::getDealDate, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(OfflineDeal::getDealDate, queryDTO.getEndDate());
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), DEAL_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            wrapper.orderByDesc(OfflineDeal::getDealDate).orderByDesc(OfflineDeal::getId);
        }
        Page<OfflineDeal> result = offlineDealMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    public boolean save(OfflineDeal deal) {
        checkFields(deal);
        if (!StringUtils.hasText(deal.getDealNo())) {
            deal.setDealNo(generateDealNo());
        }
        if (!StringUtils.hasText(deal.getCurrency())) {
            deal.setCurrency("USD");
        }
        if (deal.getPayMethod() == null) {
            deal.setPayMethod(1);
        }
        // 新建一律待确认，确认/取消走状态流转接口
        deal.setStatus(ST_PENDING);
        deal.setCreateTime(LocalDateTime.now());
        deal.setUpdateTime(LocalDateTime.now());
        try {
            return offlineDealMapper.insert(deal) > 0;
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("交易编号「" + deal.getDealNo() + "」已存在，请稍后重试");
        }
    }

    @Override
    public boolean update(OfflineDeal deal) {
        Validators.notNull(deal.getId(), "交易ID");
        OfflineDeal origin = offlineDealMapper.selectById(deal.getId());
        if (origin == null) {
            throw new RuntimeException("交易记录不存在或已删除");
        }
        if (origin.getStatus() != null && origin.getStatus() != ST_PENDING) {
            throw new RuntimeException("只有待确认的线下交易可以修改");
        }
        checkFields(deal);

        deal.setDealNo(origin.getDealNo());
        deal.setStatus(origin.getStatus());
        deal.setCreateTime(origin.getCreateTime());
        deal.setUpdateTime(LocalDateTime.now());
        return offlineDealMapper.updateById(deal) > 0;
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        OfflineDeal deal = offlineDealMapper.selectById(id);
        if (deal == null) {
            throw new RuntimeException("交易记录不存在或已删除");
        }
        int current = deal.getStatus() == null ? ST_PENDING : deal.getStatus();
        boolean allowed = (current == ST_PENDING && (status == ST_CONFIRMED || status == ST_CANCELLED))
                || (current == ST_CONFIRMED && status == ST_CANCELLED);
        if (!allowed) {
            throw new RuntimeException("交易当前为「" + statusText(current) + "」，不能变更为「" + statusText(status) + "」");
        }

        deal.setStatus(status);
        deal.setUpdateTime(LocalDateTime.now());
        return offlineDealMapper.updateById(deal) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return offlineDealMapper.deleteById(id) > 0;
    }

    private void checkFields(OfflineDeal deal) {
        Validators.text(deal.getCustomerName(), "客户名称");
        Validators.text(deal.getItemName(), "商品/服务");
        Validators.notNull(deal.getDealDate(), "成交日期");
        Validators.notNull(deal.getAmount(), "成交金额");
        Validators.positive(deal.getAmount(), "成交金额");
    }

    private String statusText(int status) {
        return switch (status) {
            case ST_PENDING -> "待确认";
            case ST_CONFIRMED -> "已确认";
            case ST_CANCELLED -> "已取消";
            default -> "未知状态";
        };
    }

    /**
     * 生成交易编号：OD + 年月日 + 4 位当日序号（如 OD20261029-0001）
     */
    private String generateDealNo() {
        String prefix = "OD" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        String max = offlineDealMapper.maxDealNoIncludeDeleted(prefix);
        int seq = 0;
        if (max != null && max.length() > prefix.length()) {
            String tail = max.substring(prefix.length()).trim();
            if (tail.matches("\\d+")) {
                seq = Integer.parseInt(tail);
            }
        }
        return prefix + String.format("%04d", seq + 1);
    }
}
