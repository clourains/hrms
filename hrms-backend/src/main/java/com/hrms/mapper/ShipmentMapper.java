package com.hrms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.entity.Shipment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 发货物流Mapper
 */
@Mapper
public interface ShipmentMapper extends BaseMapper<Shipment> {

    /**
     * 单号占用检查要连已逻辑删除的一起算，否则删掉的单号会被重新生成、撞唯一键
     */
    @Select("SELECT COUNT(*) FROM biz_shipment WHERE shipment_no = #{shipmentNo}")
    int countByNoIncludeDeleted(@Param("shipmentNo") String shipmentNo);
}
