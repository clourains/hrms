package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Payment;
import com.hrms.entity.TradeOrder;
import com.hrms.mapper.PaymentMapper;
import com.hrms.mapper.TradeOrderMapper;
import com.hrms.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 收付款Service实现类
 */
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Override
    public PageResult<Map<String, Object>> pageQuery(BizQueryDTO queryDTO) {
        Page<Payment> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.like(Payment::getPaymentNo, queryDTO.getKeyword());
        }
        if (queryDTO.getType() != null) {
            wrapper.eq(Payment::getType, queryDTO.getType());
        }
        if (queryDTO.getOrderId() != null) {
            wrapper.eq(Payment::getOrderId, queryDTO.getOrderId());
        }
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(Payment::getPaymentDate, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(Payment::getPaymentDate, queryDTO.getEndDate());
        }
        wrapper.orderByDesc(Payment::getPaymentDate).orderByDesc(Payment::getId);
        Page<Payment> result = paymentMapper.selectPage(page, wrapper);

        List<Map<String, Object>> list = result.getRecords().stream().map(payment -> {
            Map<String, Object> map = new HashMap<>();
            map.put("payment", payment);
            if (payment.getOrderId() != null) {
                TradeOrder order = tradeOrderMapper.selectById(payment.getOrderId());
                map.put("orderNo", order == null ? "" : order.getOrderNo());
            } else {
                map.put("orderNo", "");
            }
            return map;
        }).collect(Collectors.toList());
        return new PageResult<>(result.getTotal(), list);
    }

    @Override
    public Payment getById(Long id) {
        return paymentMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Payment payment) {
        if (!StringUtils.hasText(payment.getPaymentNo())) {
            payment.setPaymentNo("PAY" + System.currentTimeMillis());
        }
        if (payment.getAmount() != null && payment.getExchangeRate() != null) {
            payment.setAmountCny(payment.getAmount().multiply(payment.getExchangeRate()));
        }
        payment.setCreateTime(LocalDateTime.now());
        boolean saved = paymentMapper.insert(payment) > 0;

        // 联动更新订单付款状态（仅收款且关联订单）
        if (saved && payment.getType() != null && payment.getType() == 1 && payment.getOrderId() != null) {
            TradeOrder order = tradeOrderMapper.selectById(payment.getOrderId());
            if (order != null) {
                QueryWrapper<Payment> sumWrapper = new QueryWrapper<>();
                sumWrapper.select("IFNULL(SUM(amount),0) AS total")
                        .eq("order_id", payment.getOrderId())
                        .eq("type", 1);
                Map<String, Object> sumMap = paymentMapper.selectMaps(sumWrapper).stream().findFirst().orElse(null);
                BigDecimal paid = BigDecimal.ZERO;
                if (sumMap != null && sumMap.get("total") != null) {
                    paid = new BigDecimal(sumMap.get("total").toString());
                }
                TradeOrder update = new TradeOrder();
                update.setId(order.getId());
                if (order.getTotalAmountUsd() != null && paid.compareTo(order.getTotalAmountUsd()) >= 0) {
                    update.setPaymentStatus(2);
                } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                    update.setPaymentStatus(1);
                }
                update.setUpdateTime(LocalDateTime.now());
                tradeOrderMapper.updateById(update);
            }
        }
        return saved;
    }

    @Override
    public boolean deleteById(Long id) {
        return paymentMapper.deleteById(id) > 0;
    }
}
