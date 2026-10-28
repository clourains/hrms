package com.hrms.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.Shipment;
import com.hrms.mapper.ShipmentMapper;
import com.hrms.service.ShipmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 运单轨迹自动同步任务
 *
 * 每 6 小时扫描在途/清关中的发货单，按运单号拉取 17TRACK 节点并落库（去重），
 * 无需人工逐单点「同步实时轨迹」。单票失败只记日志，不影响其它运单。
 */
@Component
public class ShipmentTrackAutoSyncJob {

    private static final Logger log = LoggerFactory.getLogger(ShipmentTrackAutoSyncJob.class);

    /** 单次最多同步的运单数，避免一次打满 17TRACK 配额 */
    private static final int BATCH_LIMIT = 100;

    @Autowired
    private ShipmentMapper shipmentMapper;

    @Autowired
    private ShipmentService shipmentService;

    @Scheduled(
            initialDelayString = "${app.shipment.sync-initial-delay-ms:60000}",
            fixedDelayString = "${app.shipment.sync-interval-ms:21600000}"
    )
    public void syncInTransitTracks() {
        List<Shipment> list = shipmentMapper.selectList(new LambdaQueryWrapper<Shipment>()
                .isNotNull(Shipment::getTrackingNo)
                .ne(Shipment::getTrackingNo, "")
                .in(Shipment::getStatus, 2, 3)
                .orderByAsc(Shipment::getShipDate)
                .last("LIMIT " + BATCH_LIMIT));
        if (list.isEmpty()) {
            return;
        }
        int synced = 0;
        int inserted = 0;
        for (Shipment shipment : list) {
            if (!StringUtils.hasText(shipment.getTrackingNo())) {
                continue;
            }
            try {
                int added = shipmentService.syncLiveTracks(shipment.getId());
                if (added > 0) {
                    synced++;
                    inserted += added;
                }
            } catch (Exception e) {
                log.warn("自动同步轨迹失败 shipmentId={} trackingNo={}：{}",
                        shipment.getId(), shipment.getTrackingNo(), e.getMessage());
            }
        }
        log.info("运单轨迹自动同步完成：扫描 {} 票，更新 {} 票，新增 {} 个节点", list.size(), synced, inserted);
    }
}
