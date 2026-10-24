-- =============================================================
-- 库存盘点单（P019 剩余部分）
-- 一张单可盘多个 SKU，草稿可改、确认后批量调整库存并写流水
-- 只新增两张表，不改动 biz_inventory / biz_inventory_log 结构
-- =============================================================

CREATE TABLE IF NOT EXISTS `biz_stock_check` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '盘点单ID',
  `check_no` varchar(32) NOT NULL COMMENT '盘点单号 如 PD20261024-0001',
  `warehouse_id` bigint NOT NULL COMMENT '盘点仓库ID',
  `check_date` date NOT NULL COMMENT '盘点日期',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态 1草稿 2已确认 3已作废',
  `item_count` int NOT NULL DEFAULT '0' COMMENT '明细行数',
  `diff_rows` int NOT NULL DEFAULT '0' COMMENT '有差异的行数',
  `diff_sum` int NOT NULL DEFAULT '0' COMMENT '差异合计（盘盈为正、盘亏为负）',
  `operator` varchar(50) DEFAULT NULL COMMENT '制单人',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `confirm_time` datetime DEFAULT NULL COMMENT '确认时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_check_no` (`check_no`),
  KEY `idx_warehouse_date` (`warehouse_id`, `check_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点单';

CREATE TABLE IF NOT EXISTS `biz_stock_check_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `check_id` bigint NOT NULL COMMENT '盘点单ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `book_quantity` int NOT NULL DEFAULT '0' COMMENT '账面数量（确认时点）',
  `actual_quantity` int NOT NULL DEFAULT '0' COMMENT '实盘数量',
  `diff` int NOT NULL DEFAULT '0' COMMENT '差异 = 实盘 - 账面',
  `remark` varchar(200) DEFAULT NULL COMMENT '行备注',
  PRIMARY KEY (`id`),
  KEY `idx_check` (`check_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点单明细';
