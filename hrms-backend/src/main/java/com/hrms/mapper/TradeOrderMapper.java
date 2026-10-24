package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.TradeOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 销售订单Mapper
 */
@Mapper
public interface TradeOrderMapper extends BaseMapper<TradeOrder> {

    /**
     * 取当天已用到的最大订单号，连已逻辑删除的一起算。
     *
     * 订单号是「SO + 日期 + 4 位当日序号」。如果按未删除条数算下一个号，
     * 删掉一张单后序号会退回去，再撞上已删记录占着的 uk_order_no。
     * 所以取当前真正用到的最大号，原生 SQL 绕过 @TableLogic。
     */
    @Select("SELECT MAX(order_no) FROM biz_order WHERE order_no LIKE CONCAT(#{prefix}, '%')")
    String maxOrderNoIncludeDeleted(@Param("prefix") String prefix);

    /**
     * 单号是否已被占用（含已删除记录）
     */
    @Select("SELECT COUNT(*) FROM biz_order WHERE order_no = #{orderNo}")
    int countByNoIncludeDeleted(@Param("orderNo") String orderNo);
}
