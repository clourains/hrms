package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.dto.BizQueryDTO;
import com.hrms.dto.PageResult;
import com.hrms.entity.Customer;
import com.hrms.entity.Invoice;
import com.hrms.entity.TradeOrder;
import com.hrms.mapper.CustomerMapper;
import com.hrms.mapper.InvoiceMapper;
import com.hrms.mapper.TradeOrderMapper;
import com.hrms.service.InvoiceService;
import com.hrms.util.SortUtil;
import com.hrms.util.Validators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 发票Service实现类
 */
@Service
public class InvoiceServiceImpl implements InvoiceService {

    /** 发票列表可排序字段白名单 */
    private static final Map<String, SortUtil.OrderCol> INVOICE_SORT = new HashMap<>();

    static {
        INVOICE_SORT.put("invoiceNo", SortUtil.OrderCol.natural("invoice_no"));
        INVOICE_SORT.put("orderNo", SortUtil.OrderCol.natural("order_no"));
        INVOICE_SORT.put("amount", SortUtil.OrderCol.plain("amount"));
        INVOICE_SORT.put("issueDate", SortUtil.OrderCol.plain("issue_date"));
        INVOICE_SORT.put("createTime", SortUtil.OrderCol.plain("create_time"));
    }

    private static final int ST_PENDING = 0;  // 待开票
    private static final int ST_ISSUING = 1;  // 开票中
    private static final int ST_ISSUED = 2;   // 已开票
    private static final int ST_VOID = 3;     // 已作废

    @Autowired
    private InvoiceMapper invoiceMapper;

    @Autowired
    private TradeOrderMapper tradeOrderMapper;

    @Autowired
    private CustomerMapper customerMapper;

    @Override
    public PageResult<Invoice> pageQuery(BizQueryDTO queryDTO) {
        Page<Invoice> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            wrapper.and(w -> w.like(Invoice::getInvoiceNo, queryDTO.getKeyword())
                    .or().like(Invoice::getOrderNo, queryDTO.getKeyword())
                    .or().like(Invoice::getCustomerName, queryDTO.getKeyword()));
        }
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Invoice::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getCustomerId() != null) {
            wrapper.eq(Invoice::getCustomerId, queryDTO.getCustomerId());
        }
        if (queryDTO.getStartDate() != null) {
            wrapper.ge(Invoice::getIssueDate, queryDTO.getStartDate());
        }
        if (queryDTO.getEndDate() != null) {
            wrapper.le(Invoice::getIssueDate, queryDTO.getEndDate());
        }

        String orderBy = SortUtil.build(queryDTO.getSortField(), queryDTO.getSortOrder(), INVOICE_SORT);
        if (orderBy != null) {
            wrapper.last(orderBy);
        } else {
            wrapper.orderByDesc(Invoice::getCreateTime).orderByDesc(Invoice::getId);
        }
        Page<Invoice> result = invoiceMapper.selectPage(page, wrapper);
        return new PageResult<>(result.getTotal(), result.getRecords());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice createFromOrder(Invoice invoice) {
        Validators.notNull(invoice.getOrderId(), "关联订单");
        TradeOrder order = tradeOrderMapper.selectById(invoice.getOrderId());
        if (order == null) {
            throw new RuntimeException("订单不存在或已删除");
        }

        invoice.setOrderNo(order.getOrderNo());
        invoice.setCustomerId(order.getCustomerId());
        if (!StringUtils.hasText(invoice.getCustomerName())) {
            Customer customer = customerMapper.selectById(order.getCustomerId());
            invoice.setCustomerName(customer == null ? "" : customer.getCompanyName());
        }
        if (!StringUtils.hasText(invoice.getTitle())) {
            invoice.setTitle(invoice.getCustomerName());
        }
        if (!StringUtils.hasText(invoice.getCurrency())) {
            invoice.setCurrency(order.getCurrency() == null ? "USD" : order.getCurrency());
        }
        if (invoice.getAmount() == null) {
            invoice.setAmount(order.getTotalAmountUsd());
        }
        Validators.positive(invoice.getAmount(), "开票金额");
        if (invoice.getInvoiceType() == null) {
            invoice.setInvoiceType("普通发票");
        }
        if (!StringUtils.hasText(invoice.getInvoiceNo())) {
            invoice.setInvoiceNo(generateInvoiceNo());
        }

        // 新开的票一定是「待开票」，状态只能通过流转接口推进
        invoice.setStatus(ST_PENDING);
        invoice.setIssueDate(null);
        invoice.setCreateTime(LocalDateTime.now());
        invoice.setUpdateTime(LocalDateTime.now());
        try {
            invoiceMapper.insert(invoice);
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("发票号「" + invoice.getInvoiceNo() + "」已存在，请更换");
        }
        return invoice;
    }

    @Override
    public boolean update(Invoice invoice) {
        Validators.notNull(invoice.getId(), "发票ID");
        Invoice origin = invoiceMapper.selectById(invoice.getId());
        if (origin == null) {
            throw new RuntimeException("发票不存在或已删除");
        }
        if (origin.getStatus() != null && origin.getStatus() != ST_PENDING) {
            throw new RuntimeException("只有待开票的发票可以修改");
        }
        Validators.text(invoice.getTitle(), "发票抬头");
        Validators.positive(invoice.getAmount(), "开票金额");
        // 编辑时不允许把原值清空，否则等于绕过必填校验
        Validators.notCleared(origin.getInvoiceNo(), invoice.getInvoiceNo(), "发票号");

        invoice.setOrderId(origin.getOrderId());
        invoice.setOrderNo(origin.getOrderNo());
        invoice.setCustomerId(origin.getCustomerId());
        invoice.setStatus(origin.getStatus());
        invoice.setIssueDate(origin.getIssueDate());
        invoice.setCreateTime(origin.getCreateTime());
        invoice.setUpdateTime(LocalDateTime.now());
        try {
            return invoiceMapper.updateById(invoice) > 0;
        } catch (DuplicateKeyException e) {
            throw new RuntimeException("发票号「" + invoice.getInvoiceNo() + "」已存在，请更换");
        }
    }

    @Override
    public boolean updateStatus(Long id, Integer status) {
        Invoice invoice = invoiceMapper.selectById(id);
        if (invoice == null) {
            throw new RuntimeException("发票不存在或已删除");
        }
        int current = invoice.getStatus() == null ? ST_PENDING : invoice.getStatus();
        if (!isAllowed(current, status)) {
            throw new RuntimeException("发票当前为「" + statusText(current) + "」，不能变更为「" + statusText(status) + "」");
        }

        invoice.setStatus(status);
        // 标记已开票时补上开票日期，作废则保留原日期便于对账
        if (status == ST_ISSUED && invoice.getIssueDate() == null) {
            invoice.setIssueDate(LocalDate.now());
        }
        invoice.setUpdateTime(LocalDateTime.now());
        return invoiceMapper.updateById(invoice) > 0;
    }

    @Override
    public boolean deleteById(Long id) {
        return invoiceMapper.deleteById(id) > 0;
    }

    /**
     * 状态流转：待开票→开票中→已开票；未作废的票都可以作废，已作废是终态
     */
    private boolean isAllowed(int current, int target) {
        if (current == ST_VOID) {
            return false;
        }
        if (target == ST_VOID) {
            return true;
        }
        return (current == ST_PENDING && target == ST_ISSUING)
                || (current == ST_ISSUING && target == ST_ISSUED);
    }

    private String statusText(int status) {
        return switch (status) {
            case ST_PENDING -> "待开票";
            case ST_ISSUING -> "开票中";
            case ST_ISSUED -> "已开票";
            case ST_VOID -> "已作废";
            default -> "未知状态";
        };
    }

    /**
     * 生成发票号：INV + 年月日 + 4 位当日序号（如 INV20261029-0001）
     */
    private String generateInvoiceNo() {
        String prefix = "INV" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        String max = invoiceMapper.maxInvoiceNoIncludeDeleted(prefix);
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
