package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Product;
import org.apache.ibatis.annotations.Mapper;

/**
 * 产品Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

}
