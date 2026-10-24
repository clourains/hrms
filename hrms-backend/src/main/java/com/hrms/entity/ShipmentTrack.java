package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 物流轨迹节点（P018）
 *
 * 发货单原来只有 4 档状态，看不出"现在到哪、哪天到"。
 * 每推进一步就落一条节点，前端按时间线展示。
 */
@Data
@TableName("biz_shipment_track")
public class ShipmentTrack implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long shipmentId;

    /**
     * 对应发货状态 1待发货 2运输中 3清关中 4已签收
     */
    private Integer status;

    /**
     * 节点名称，如 已揽收 / 离港 / 到港 / 清关完成
     */
    private String node;

    private String location;

    private String content;

    private LocalDateTime trackTime;

    private String operator;

    private LocalDateTime createTime;
}
