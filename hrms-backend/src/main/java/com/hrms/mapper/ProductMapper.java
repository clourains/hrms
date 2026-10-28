package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 产品Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * SKU 是否已被占用（含逻辑删除记录）：P043 逻辑删除仍占唯一索引
     */
    @Select("SELECT COUNT(*) FROM biz_product WHERE sku = #{sku}")
    int countBySkuIncludeDeleted(@Param("sku") String sku);
}
