-- ============================================================
-- 2026-10-24 缺陷修复配套的表结构变更
-- 对应问题清单：P016 / P018 / P020 / P034
-- 执行方式：mysql -uroot -p -h127.0.0.1 -P3306 hrms < fix_2026-10-24.sql
-- 均为增量变更，可重复执行（执行前请确认字段/索引是否已存在）
-- ============================================================

-- P020：变动记录补"变动前后锁定数量"，锁定/解锁记录才能看出锁了多少
ALTER TABLE biz_inventory_log
    ADD COLUMN before_locked_quantity INT NULL COMMENT '变动前锁定数量' AFTER before_quantity,
    ADD COLUMN after_locked_quantity  INT NULL COMMENT '变动后锁定数量' AFTER after_quantity;

-- P034：变动记录按时间倒序分页缺索引，补 create_time 索引
ALTER TABLE biz_inventory_log
    ADD INDEX idx_create_time (create_time);

-- P016：发货单改为逻辑删除，与仓库/产品/订单口径一致
ALTER TABLE biz_shipment
    ADD COLUMN deleted TINYINT DEFAULT 0 COMMENT '逻辑删除 0正常 1已删除' AFTER status;

-- P018：物流轨迹节点表，记录每个状态节点的时间/地点/说明
CREATE TABLE IF NOT EXISTS biz_shipment_track (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '轨迹ID',
    shipment_id BIGINT       NOT NULL COMMENT '发货单ID',
    status      TINYINT      NOT NULL COMMENT '对应发货状态 1待发货 2运输中 3清关中 4已签收',
    node        VARCHAR(50)  NOT NULL COMMENT '节点名称',
    location    VARCHAR(100) COMMENT '节点地点',
    content     VARCHAR(300) COMMENT '节点说明',
    track_time  DATETIME     NOT NULL COMMENT '节点时间',
    operator    VARCHAR(50)  COMMENT '操作人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_shipment (shipment_id),
    KEY idx_track_time (track_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流轨迹节点';
