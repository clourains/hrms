-- 发票管理 + 线下交易台账
-- 发票：由销售订单开票，状态 0待开票 1开票中 2已开票 3已作废
CREATE TABLE IF NOT EXISTS `biz_invoice` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '发票ID',
    `invoice_no` varchar(50) NOT NULL COMMENT '发票号',
    `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
    `order_no` varchar(50) DEFAULT NULL COMMENT '关联订单号',
    `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
    `customer_name` varchar(200) DEFAULT NULL COMMENT '客户名称',
    `title` varchar(200) NOT NULL COMMENT '发票抬头',
    `tax_no` varchar(100) DEFAULT NULL COMMENT '税号',
    `currency` varchar(10) DEFAULT 'USD' COMMENT '币种',
    `amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '开票金额',
    `invoice_type` varchar(30) DEFAULT '普通发票' COMMENT '发票类型',
    `status` tinyint DEFAULT '0' COMMENT '状态 0待开票 1开票中 2已开票 3已作废',
    `issue_date` date DEFAULT NULL COMMENT '开票日期',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invoice_no` (`invoice_no`),
    KEY `idx_invoice_order` (`order_id`),
    KEY `idx_invoice_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发票记录';

-- 线下交易：展会/面谈等线下成交台账，状态 0待确认 1已确认 9已取消
CREATE TABLE IF NOT EXISTS `biz_offline_deal` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '线下交易ID',
    `deal_no` varchar(50) NOT NULL COMMENT '交易编号',
    `customer_id` bigint DEFAULT NULL COMMENT '客户ID',
    `customer_name` varchar(200) NOT NULL COMMENT '客户名称',
    `customer_country` varchar(80) DEFAULT NULL COMMENT '国家',
    `item_name` varchar(300) NOT NULL COMMENT '商品/服务',
    `currency` varchar(10) DEFAULT 'USD' COMMENT '币种',
    `amount` decimal(14,2) NOT NULL DEFAULT '0.00' COMMENT '成交金额',
    `deal_date` date NOT NULL COMMENT '成交日期',
    `pay_method` tinyint DEFAULT '1' COMMENT '收款方式 1银行转账 2现金 3微信 4支付宝 9其他',
    `voucher` varchar(200) DEFAULT NULL COMMENT '凭证（单号/链接）',
    `status` tinyint DEFAULT '0' COMMENT '状态 0待确认 1已确认 9已取消',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_deal_no` (`deal_no`),
    KEY `idx_deal_status` (`status`),
    KEY `idx_deal_date` (`deal_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线下交易台账';

-- 演示数据：从已有订单取前 3 张生成发票，覆盖待开票/开票中/已开票三种状态
INSERT INTO `biz_invoice`
(`invoice_no`, `order_id`, `order_no`, `customer_id`, `customer_name`, `title`, `tax_no`,
 `currency`, `amount`, `invoice_type`, `status`, `issue_date`, `remark`, `deleted`, `create_time`, `update_time`)
SELECT CONCAT('INV2026', LPAD(o.id, 4, '0')),
       o.id,
       o.order_no,
       o.customer_id,
       c.company_name,
       CONCAT(c.company_name, ' Ltd.'),
       CONCAT('TAX', LPAD(o.id, 6, '0')),
       o.currency,
       o.total_amount_usd,
       '普通发票',
       MOD(o.id, 3),
       DATE_ADD(o.order_date, INTERVAL 3 DAY),
       '由销售订单生成',
       0,
       NOW(),
       NOW()
FROM `biz_order` o
         JOIN `biz_customer` c ON c.id = o.customer_id
WHERE o.deleted = 0
  AND c.deleted = 0
ORDER BY o.id
LIMIT 3;

-- 演示数据：3 条线下成交记录（已确认状态）
INSERT INTO `biz_offline_deal`
(`deal_no`, `customer_id`, `customer_name`, `customer_country`, `item_name`,
 `currency`, `amount`, `deal_date`, `pay_method`, `voucher`, `status`, `remark`, `deleted`, `create_time`, `update_time`)
SELECT CONCAT('OD202610', LPAD(c.id, 3, '0')),
       c.id,
       c.company_name,
       c.country,
       '样品采购 - 便携储能电源',
       'USD',
       1200.00 + c.id * 300,
       '2026-10-15',
       1 + MOD(c.id, 4),
       CONCAT('VOUCHER-', LPAD(c.id, 4, '0')),
       1,
       '线下展会现场成交',
       0,
       NOW(),
       NOW()
FROM `biz_customer` c
WHERE c.deleted = 0
ORDER BY c.id
LIMIT 3;
