package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Inventory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 库存Mapper
 */
@Mapper
public interface InventoryMapper extends BaseMapper<Inventory> {

    /**
     * 按产品+仓库取库存并加行锁（P013）
     *
     * 出入库原来是"先查后改"，两个请求同时出库同一行会各自读到同样的旧值，导致超卖。
     * 这里用 SELECT ... FOR UPDATE 把行锁住，事务提交前其它事务只能排队。
     */
    @Select("SELECT * FROM biz_inventory WHERE product_id = #{productId} AND warehouse_id = #{warehouseId} FOR UPDATE")
    Inventory selectForUpdate(@Param("productId") Long productId, @Param("warehouseId") Long warehouseId);
}
