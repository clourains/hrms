-- =====================================================
-- 境外贸易公司业务扩展 - 数据库升级脚本 v2
-- 兼容性: MySQL 5.7+ / 8.0+ (不依赖 MariaDB 语法)
-- 幂等安全: 可重复执行, 列/表已存在则跳过
-- 执行顺序: 先 init.sql (基础 HR 表), 再本脚本 (贸易业务)
-- =====================================================
USE hrms;

-- ========== Part 1: 修复 sys_employee 境外字段 ==========
-- 临时关掉外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- --- 1a. 清理被误建成 BLOB 的脏列 (如果存在) ---
-- MySQL < 8.0.29 不支持 DROP COLUMN IF EXISTS, 用动态 SQL
SET @drop_et = (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE sys_employee DROP COLUMN employee_type',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_employee'
    AND COLUMN_NAME = 'employee_type' AND DATA_TYPE = 'blob'
);
PREPARE stmt FROM @drop_et; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @drop_vt = (
  SELECT IF(
    COUNT(*) > 0,
    'ALTER TABLE sys_employee DROP COLUMN visa_type',
    'SELECT 1'
  )
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_employee'
    AND COLUMN_NAME = 'visa_type' AND DATA_TYPE = 'blob'
);
PREPARE stmt FROM @drop_vt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- --- 1b. 加回正确类型 (列不存在才加, 幂等) ---
SET @add_country = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE sys_employee ADD COLUMN country VARCHAR(50) DEFAULT "中国" COMMENT "所属国家"',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_employee' AND COLUMN_NAME = 'country'
);
PREPARE stmt FROM @add_country; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @add_wl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE sys_employee ADD COLUMN work_location VARCHAR(100) COMMENT "工作地点"',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_employee' AND COLUMN_NAME = 'work_location'
);
PREPARE stmt FROM @add_wl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @add_et = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE sys_employee ADD COLUMN employee_type TINYINT DEFAULT 1 COMMENT "员工类型 1国内 2境外"',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_employee' AND COLUMN_NAME = 'employee_type'
);
PREPARE stmt FROM @add_et; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @add_passport = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE sys_employee ADD COLUMN passport_no VARCHAR(20) COMMENT "护照号"',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_employee' AND COLUMN_NAME = 'passport_no'
);
PREPARE stmt FROM @add_passport; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @add_vt = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE sys_employee ADD COLUMN visa_type VARCHAR(50) COMMENT "签证类型"',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_employee' AND COLUMN_NAME = 'visa_type'
);
PREPARE stmt FROM @add_vt; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @add_ved = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE sys_employee ADD COLUMN visa_expire_date DATE COMMENT "签证到期日"',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_employee' AND COLUMN_NAME = 'visa_expire_date'
);
PREPARE stmt FROM @add_ved; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET FOREIGN_KEY_CHECKS = 1;
-- Part 1 done.

-- ========== Part 2: 业务表 ==========

-- 产品表
CREATE TABLE IF NOT EXISTS biz_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '产品ID',
    sku VARCHAR(50) NOT NULL COMMENT 'SKU编码',
    name VARCHAR(100) NOT NULL COMMENT '产品名称(中文)',
    name_en VARCHAR(200) COMMENT '产品名称(英文)',
    category VARCHAR(50) COMMENT '产品分类',
    unit VARCHAR(20) DEFAULT 'pcs' COMMENT '单位',
    cost_price DECIMAL(12,2) DEFAULT 0 COMMENT '成本价(CNY)',
    sale_price_usd DECIMAL(12,2) DEFAULT 0 COMMENT '销售价(USD)',
    description TEXT COMMENT '产品描述',
    status TINYINT DEFAULT 1 COMMENT '状态 1在售 0下架',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sku (sku),
    KEY idx_category (category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

-- 仓库表
CREATE TABLE IF NOT EXISTS biz_warehouse (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '仓库ID',
    warehouse_code VARCHAR(50) NOT NULL COMMENT '仓库编码',
    warehouse_name VARCHAR(100) NOT NULL COMMENT '仓库名称',
    warehouse_type TINYINT DEFAULT 1 COMMENT '类型 1国内 2海外',
    country VARCHAR(50) COMMENT '所在国家',
    city VARCHAR(100) COMMENT '所在城市',
    address VARCHAR(300) COMMENT '详细地址',
    manager VARCHAR(50) COMMENT '负责人',
    capacity INT DEFAULT 0 COMMENT '容量',
    remark VARCHAR(500) COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '状态 1启用 0停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (warehouse_code),
    KEY idx_type (warehouse_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

-- 库存表
CREATE TABLE IF NOT EXISTS biz_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '库存ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    warehouse_id BIGINT NOT NULL COMMENT '仓库ID',
    quantity INT DEFAULT 0 COMMENT '可用数量',
    locked_quantity INT DEFAULT 0 COMMENT '锁定数量(已下单未发货)',
    warning_quantity INT DEFAULT 10 COMMENT '预警数量',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_warehouse (product_id, warehouse_id),
    KEY idx_warehouse (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 库存变动日志
CREATE TABLE IF NOT EXISTS biz_inventory_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    change_type TINYINT NOT NULL COMMENT '变动类型 1入库 2出库 3锁定 4解锁',
    change_quantity INT NOT NULL COMMENT '变动数量(正数)',
    before_qty INT COMMENT '变动前数量',
    after_qty INT COMMENT '变动后数量',
    order_no VARCHAR(50) COMMENT '关联订单号(可选)',
    operator VARCHAR(50) COMMENT '操作人',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_product (product_id),
    KEY idx_warehouse (warehouse_id),
    KEY idx_time (created_at),
    KEY idx_order (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存变动日志';

-- 境外客户
CREATE TABLE IF NOT EXISTS biz_customer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '客户ID',
    customer_code VARCHAR(50) NOT NULL COMMENT '客户编码',
    company_name VARCHAR(200) NOT NULL COMMENT '公司名称',
    company_name_en VARCHAR(200) COMMENT '公司名称(英文)',
    country VARCHAR(50) COMMENT '国家',
    city VARCHAR(100) COMMENT '城市',
    contact_name VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(30) COMMENT '联系电话',
    contact_email VARCHAR(100) COMMENT '邮箱',
    website VARCHAR(200) COMMENT '网站',
    address VARCHAR(300) COMMENT '地址',
    tax_no VARCHAR(50) COMMENT '税号',
    remark VARCHAR(500) COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '状态 1正常 0停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (customer_code),
    KEY idx_country (country)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='境外客户';

-- 供应商
CREATE TABLE IF NOT EXISTS biz_supplier (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '供应商ID',
    supplier_code VARCHAR(50) NOT NULL COMMENT '供应商编码',
    supplier_name VARCHAR(200) NOT NULL COMMENT '供应商名称',
    country VARCHAR(50) COMMENT '所在国家',
    contact_name VARCHAR(50) COMMENT '联系人',
    contact_phone VARCHAR(30) COMMENT '联系电话',
    contact_email VARCHAR(100) COMMENT '邮箱',
    address VARCHAR(300) COMMENT '地址',
    main_product VARCHAR(200) COMMENT '主营产品',
    remark VARCHAR(500) COMMENT '备注',
    status TINYINT DEFAULT 1 COMMENT '状态 1正常 0停用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_code (supplier_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商';

-- 销售订单
CREATE TABLE IF NOT EXISTS biz_trade_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '订单ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单号',
    customer_id BIGINT NOT NULL COMMENT '客户ID',
    employee_id BIGINT COMMENT '业务员ID',
    order_date DATE NOT NULL COMMENT '订单日期',
    total_quantity INT DEFAULT 0 COMMENT '总数量',
    total_amount_usd DECIMAL(14,2) DEFAULT 0 COMMENT '总金额(USD)',
    exchange_rate DECIMAL(10,6) DEFAULT 1 COMMENT '汇率',
    total_amount_cny DECIMAL(14,2) DEFAULT 0 COMMENT '总金额(CNY)',
    payment_status TINYINT DEFAULT 0 COMMENT '付款状态 0未付 1部分 2付清',
    status TINYINT DEFAULT 0 COMMENT '订单状态 0待确认 1已确认 2备货中 3已发货 4已签收 5已完成 9已取消',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_customer (customer_id),
    KEY idx_employee (employee_id),
    KEY idx_status (status),
    KEY idx_order_date (order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单';

-- 订单明细
CREATE TABLE IF NOT EXISTS biz_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    warehouse_id BIGINT COMMENT '发货仓库ID',
    quantity INT NOT NULL COMMENT '数量',
    unit_price_usd DECIMAL(12,2) DEFAULT 0 COMMENT '单价(USD)',
    subtotal_usd DECIMAL(14,2) DEFAULT 0 COMMENT '小计(USD)',
    remark VARCHAR(500) COMMENT '备注',
    KEY idx_order (order_id),
    KEY idx_product (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细';

-- 物流发货
CREATE TABLE IF NOT EXISTS biz_shipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '发货单ID',
    shipment_no VARCHAR(50) NOT NULL COMMENT '发货单号',
    order_id BIGINT NOT NULL COMMENT '关联订单ID',
    transport_type VARCHAR(30) DEFAULT '海运' COMMENT '运输方式 海运/空运/陆运',
    carrier VARCHAR(100) COMMENT '承运商',
    tracking_no VARCHAR(100) COMMENT '追踪号',
    container_no VARCHAR(50) COMMENT '集装箱号',
    port_from VARCHAR(100) COMMENT '起运港',
    port_to VARCHAR(100) COMMENT '目的港',
    ship_date DATE COMMENT '发货日期(ETD)',
    eta_date DATE COMMENT '预计到达(ETA)',
    freight_amount DECIMAL(12,2) DEFAULT 0 COMMENT '运费',
    status TINYINT DEFAULT 1 COMMENT '状态 1待发货 2运输中 3清关中 4已签收',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_shipment_no (shipment_no),
    KEY idx_order (order_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物流发货';

-- 收付款
CREATE TABLE IF NOT EXISTS biz_payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '收付款ID',
    payment_no VARCHAR(50) NOT NULL COMMENT '收付款单号',
    order_id BIGINT COMMENT '关联订单ID',
    order_no VARCHAR(50) COMMENT '关联订单号',
    type TINYINT NOT NULL COMMENT '类型 1收款 2付款',
    amount DECIMAL(14,2) NOT NULL COMMENT '金额',
    currency VARCHAR(10) DEFAULT 'USD' COMMENT '币种',
    exchange_rate DECIMAL(10,6) DEFAULT 1 COMMENT '汇率',
    amount_cny DECIMAL(14,2) DEFAULT 0 COMMENT '折合人民币',
    payment_date DATE COMMENT '收付款日期',
    method VARCHAR(50) COMMENT '方式 电汇/信用证/PayPal/西联/现金',
    bank_name VARCHAR(100) COMMENT '银行名称',
    bank_account VARCHAR(50) COMMENT '银行账号',
    remark VARCHAR(500) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_payment_no (payment_no),
    KEY idx_order (order_id),
    KEY idx_type (type),
    KEY idx_date (payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收付款';

-- 汇率
CREATE TABLE IF NOT EXISTS biz_exchange_rate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    currency_from VARCHAR(10) NOT NULL DEFAULT 'USD',
    currency_to VARCHAR(10) NOT NULL DEFAULT 'CNY',
    rate DECIMAL(12,6) NOT NULL COMMENT '汇率',
    rate_date DATE NOT NULL COMMENT '汇率日期',
    source VARCHAR(100) COMMENT '数据来源',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_date (rate_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='汇率';

-- ========== Part 3: 演示数据 ==========

-- --- 汇率 ---
INSERT IGNORE INTO biz_exchange_rate (currency_from, currency_to, rate, rate_date, source) VALUES
('USD', 'CNY', 7.250000, CURDATE(), 'manual'),
('USD', 'CNY', 7.280000, DATE_SUB(CURDATE(), INTERVAL 7 DAY), 'manual');

-- --- 仓库 ---
INSERT IGNORE INTO biz_warehouse (warehouse_code, warehouse_name, warehouse_type, country, city, address, manager, capacity) VALUES
('WH-CN-01', '国内主仓', 1, '中国', '深圳', '深圳市南山区科技园A座', '张三', 10000),
('WH-CN-02', '国内分仓', 1, '中国', '上海', '上海市浦东新区保税区B区', '李四', 5000),
('WH-US-01', '美国洛杉矶仓', 2, '美国', 'Los Angeles', '1000 Harbor Blvd, LA', 'Mike', 8000),
('WH-US-02', '美国纽约仓', 2, '美国', 'New York', '500 Container Ave, NY', 'John', 3000);

-- --- 产品 ---
INSERT IGNORE INTO biz_product (sku, name, name_en, category, unit, cost_price, sale_price_usd, description) VALUES
('LED-001', 'LED灯泡E27 9W', 'LED Bulb E27 9W', '电子', 'pcs', 8.50, 2.99, '节能LED灯泡, 色温6500K'),
('LED-002', 'LED灯管T8 18W', 'LED Tube T8 18W', '电子', 'pcs', 35.00, 9.99, 'LED日光灯管, 1.2米'),
('LED-003', 'LED面板灯 30x30', 'LED Panel 30x30', '电子', 'pcs', 68.00, 18.50, '嵌入式LED面板灯'),
('HOME-001', '智能插座', 'Smart Plug', '智能家居', 'pcs', 45.00, 12.99, 'WiFi智能插座, App远程控制'),
('HOME-002', '人体感应灯', 'Motion Sensor Light', '智能家居', 'pcs', 32.00, 8.99, 'USB充电, IP65防水'),
('OFF-001', '无线鼠标', 'Wireless Mouse', '办公用品', 'pcs', 25.00, 6.99, '2.4G无线, 1600DPI'),
('OFF-002', '机械键盘', 'Mechanical Keyboard', '办公用品', 'pcs', 180.00, 45.00, '红轴, RGB背光, 104键'),
('TOOL-001', '电动螺丝刀套装', 'Electric Screwdriver Kit', '工具', 'set', 120.00, 35.00, '3.6V锂电, 含12批头');

-- --- 客户 ---
INSERT IGNORE INTO biz_customer (customer_code, company_name, company_name_en, country, city, contact_name, contact_phone, contact_email, website) VALUES
('CUS-US-001', '美国灯饰进口有限公司', 'US Lighting Import Co., Ltd.', '美国', 'Los Angeles', 'David Wilson', '+1-310-555-1234', 'david@uslighting.com', 'www.uslighting.com'),
('CUS-US-002', '纽约智能家居公司', 'NY Smart Home Inc.', '美国', 'New York', 'Jennifer Chen', '+1-212-555-5678', 'jennifer@nysmarthome.com', 'www.nysmarthome.com'),
('CUS-US-003', '芝加哥办公用品商', 'Chicago Office Supply Co.', '美国', 'Chicago', 'Robert Lee', '+1-312-555-9012', 'robert@chicagooffice.com', ''),
('CUS-CA-001', '多伦多贸易公司', 'Toronto Trading Ltd.', '加拿大', 'Toronto', 'Michael Brown', '+1-416-555-3456', 'michael@torontotrading.ca', 'www.torontotrading.ca');

-- --- 供应商 ---
INSERT IGNORE INTO biz_supplier (supplier_code, supplier_name, country, contact_name, contact_phone, main_product) VALUES
('SUP-001', '深圳LED制造集团', '中国', '陈经理', '0755-8888-0001', 'LED照明系列'),
('SUP-002', '东莞智能家居科技', '中国', '王总', '0769-8888-0002', '智能家居产品'),
('SUP-003', '广州办公用品批发', '中国', '林主管', '020-8888-0003', '办公用品');

-- --- 库存 ---
INSERT IGNORE INTO biz_inventory (product_id, warehouse_id, quantity, locked_quantity, warning_quantity) VALUES
(1, 1, 5000, 0, 500), (1, 3, 1500, 0, 200),
(2, 1, 2000, 0, 200), (2, 3, 800, 0, 100),
(3, 1, 800, 0, 100), (3, 3, 300, 0, 50),
(4, 1, 1500, 0, 200), (4, 2, 800, 0, 100), (4, 3, 400, 0, 50),
(5, 1, 2000, 0, 200), (5, 3, 600, 0, 50),
(6, 1, 3000, 0, 300),
(7, 1, 1500, 0, 200), (7, 3, 500, 0, 50),
(8, 1, 600, 0, 50), (8, 2, 300, 0, 30);

-- --- 销售订单 (用 2025-10-15 作基准) ---
INSERT IGNORE INTO biz_trade_order (order_no, customer_id, order_date, total_quantity, total_amount_usd, exchange_rate, total_amount_cny, payment_status, status, remark) VALUES
('SO-2025-1001', 1, '2025-10-10', 3500, 10850.00, 7.25, 78662.50, 2, 5, 'LED灯泡和灯管, 已完成签收'),
('SO-2025-1002', 2, '2025-10-12', 800, 35600.00, 7.25, 258100.00, 2, 4, '机械键盘大批量订单'),
('SO-2025-1003', 3, '2025-10-14', 1000, 7250.00, 7.25, 52562.50, 1, 3, '智能家居套装, 运输中'),
('SO-2025-1004', 1, '2025-10-15', 2000, 5980.00, 7.25, 43355.00, 0, 2, '新订单, 备货中'),
('SO-2025-1005', 4, '2025-10-15', 500, 17500.00, 7.25, 126875.00, 0, 1, '加拿大客户首单, 待确认');

-- --- 订单明细 ---
INSERT IGNORE INTO biz_order_item (order_id, product_id, warehouse_id, quantity, unit_price_usd, subtotal_usd) VALUES
(1, 1, 1, 3000, 2.99, 8970.00),
(1, 2, 1, 500, 9.99, 1498.50),
(2, 7, 1, 800, 45.00, 36000.00),
(3, 4, 1, 800, 12.99, 10392.00),
(3, 5, 1, 200, 8.99, 1798.00),
(4, 1, 1, 2000, 2.99, 5980.00),
(5, 3, 1, 200, 18.50, 3700.00),
(5, 8, 1, 500, 35.00, 17500.00);

-- --- 物流发货 ---
INSERT IGNORE INTO biz_shipment (shipment_no, order_id, transport_type, carrier, tracking_no, container_no, port_from, port_to, ship_date, eta_date, freight_amount, status) VALUES
('SHP-2025-1001', 1, '海运', '马士基 MAERSK', 'MSKU1234567890', 'MSKU1234567', '深圳盐田港', '洛杉矶长滩港', '2025-10-12', '2025-11-05', 2800.00, 4),
('SHP-2025-1002', 2, '空运', 'DHL', 'DHL9876543210', NULL, '上海浦东机场', '纽约JFK机场', '2025-10-14', '2025-10-16', 4500.00, 4),
('SHP-2025-1003', 3, '海运', '中远海运 COSCO', 'COSCO1122334455', 'COSU9876543', '深圳盐田港', '洛杉矶长滩港', '2025-10-15', '2025-11-08', 2500.00, 2);

-- --- 收付款 ---
INSERT IGNORE INTO biz_payment (payment_no, order_id, order_no, type, amount, currency, exchange_rate, amount_cny, payment_date, method, bank_name) VALUES
('PAY-IN-2025-1001', 1, 'SO-2025-1001', 1, 10850.00, 'USD', 7.25, 78662.50, '2025-10-11', '电汇', '中国银行'),
('PAY-IN-2025-1002', 2, 'SO-2025-1002', 1, 35600.00, 'USD', 7.25, 258100.00, '2025-10-13', '信用证', '工商银行'),
('PAY-IN-2025-1003', 3, 'SO-2025-1003', 1, 5000.00, 'USD', 7.25, 36250.00, '2025-10-14', 'PayPal', 'PayPal'),
('PAY-OUT-2025-1001', NULL, NULL, 2, 85000.00, 'CNY', 1.00, 85000.00, '2025-10-10', '电汇', '建设银行');
