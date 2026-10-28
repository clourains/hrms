package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 境外客户Mapper
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 客户编码是否已被占用（含逻辑删除记录）：唯一索引仍然占着，导入判重必须算上
     */
    @Select("SELECT COUNT(*) FROM biz_customer WHERE customer_code = #{code}")
    int countByCodeIncludeDeleted(@Param("code") String code);
}
