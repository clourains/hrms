package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Invoice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 发票Mapper
 */
@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {

    /**
     * 取当前前缀下用到的最大发票号（含逻辑删除记录），避免自动取号时撞唯一索引
     */
    @Select("SELECT MAX(invoice_no) FROM biz_invoice WHERE invoice_no LIKE CONCAT(#{prefix}, '%')")
    String maxInvoiceNoIncludeDeleted(@Param("prefix") String prefix);

    /**
     * 发票号是否已被占用（含逻辑删除记录）
     */
    @Select("SELECT COUNT(*) FROM biz_invoice WHERE invoice_no = #{invoiceNo}")
    int countByNoIncludeDeleted(@Param("invoiceNo") String invoiceNo);
}
