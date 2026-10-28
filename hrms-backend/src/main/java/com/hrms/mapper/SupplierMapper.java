package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 供应商Mapper
 */
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {

    /**
     * 供应商编码是否已被占用（含逻辑删除记录）
     */
    @Select("SELECT COUNT(*) FROM biz_supplier WHERE supplier_code = #{code}")
    int countByCodeIncludeDeleted(@Param("code") String code);
}
