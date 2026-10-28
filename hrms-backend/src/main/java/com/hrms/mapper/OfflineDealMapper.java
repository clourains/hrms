package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.OfflineDeal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 线下交易Mapper
 */
@Mapper
public interface OfflineDealMapper extends BaseMapper<OfflineDeal> {

    /**
     * 取当前前缀下用到的最大交易编号（含逻辑删除记录）
     */
    @Select("SELECT MAX(deal_no) FROM biz_offline_deal WHERE deal_no LIKE CONCAT(#{prefix}, '%')")
    String maxDealNoIncludeDeleted(@Param("prefix") String prefix);

    /**
     * 交易编号是否已被占用（含逻辑删除记录）
     */
    @Select("SELECT COUNT(*) FROM biz_offline_deal WHERE deal_no = #{dealNo}")
    int countByNoIncludeDeleted(@Param("dealNo") String dealNo);
}
