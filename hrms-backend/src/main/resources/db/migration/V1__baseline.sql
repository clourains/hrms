-- ============================================================
-- Flyway V1 基线（M2-4）
-- 由 mysqldump 从运行库导出的最终结构 + 种子数据快照。
-- 全新环境：Flyway 执行本文件一键建库建数据；
-- 已有环境：baseline-on-migrate 自动 baseline 到 1，跳过本文件。
-- 历史增量脚本（init.sql / trade_upgrade.sql / stock_check / fix）
-- 保留于上级目录仅供追溯，内容已过时，以本基线为准。
-- ============================================================
-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: hrms
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `hrms`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `hrms` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `hrms`;

--
-- Table structure for table `biz_customer`
--

DROP TABLE IF EXISTS `biz_customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_customer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户ID',
  `customer_code` varchar(50) NOT NULL COMMENT '客户编码',
  `company_name` varchar(200) NOT NULL COMMENT '公司名称',
  `contact_name` varchar(100) DEFAULT NULL COMMENT '联系人',
  `country` varchar(50) DEFAULT NULL COMMENT '国家',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `address` varchar(300) DEFAULT NULL COMMENT '地址',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(50) DEFAULT NULL COMMENT '电话',
  `website` varchar(200) DEFAULT NULL COMMENT '网站',
  `level` varchar(10) DEFAULT 'C' COMMENT '客户等级 A/B/C',
  `credit_limit_usd` decimal(14,2) DEFAULT '0.00' COMMENT '信用额度(USD)',
  `status` tinyint DEFAULT '1' COMMENT '状态 0停用 1合作中',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_code` (`customer_code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='境外客户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_customer`
--

LOCK TABLES `biz_customer` WRITE;
/*!40000 ALTER TABLE `biz_customer` DISABLE KEYS */;
INSERT INTO `biz_customer` VALUES (1,'CUS-US-001','ABC Trading LLC','John Smith','美国','洛杉矶',NULL,'john@abctrading.com','+1-310-555-0101',NULL,'A',500000.00,0,NULL,0,'2026-10-15 11:31:20','2026-10-22 10:12:37'),(2,'CUS-US-002','Global Imports Inc','Emily Davis','美国','纽约',NULL,'emily@globalimports.com','+1-212-555-0202',NULL,'A',800000.00,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(3,'CUS-US-003','West Coast Distribution','Michael Brown','美国','西雅图',NULL,'mike@wcdist.com','+1-206-555-0303',NULL,'B',300000.00,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(4,'CUS-US-004','Sunshine Retail Group','Jessica Wilson','美国','迈阿密',NULL,'jessica@sunshineretail.com','+1-305-555-0404',NULL,'B',200000.00,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(5,'CUS-CA-001','Maple Leaf Trading Co','David Taylor','加拿大','多伦多',NULL,'david@mapleleaf.ca','+1-416-555-0505',NULL,'B',150000.00,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(6,'CUS-MX-001','Comercial del Norte SA','Carlos Garcia','墨西哥','墨西哥城',NULL,'carlos@cdnorte.mx','+52-55-555-0606',NULL,'C',100000.00,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20');
/*!40000 ALTER TABLE `biz_customer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_exchange_rate`
--

DROP TABLE IF EXISTS `biz_exchange_rate`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_exchange_rate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `currency` varchar(10) NOT NULL COMMENT '币种',
  `rate_to_cny` decimal(10,4) NOT NULL COMMENT '兑人民币汇率',
  `rate_date` date NOT NULL COMMENT '日期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_currency_date` (`currency`,`rate_date`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='汇率表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_exchange_rate`
--

LOCK TABLES `biz_exchange_rate` WRITE;
/*!40000 ALTER TABLE `biz_exchange_rate` DISABLE KEYS */;
INSERT INTO `biz_exchange_rate` VALUES (1,'USD',7.2100,'2026-05-01','2026-10-15 11:31:20'),(2,'USD',7.1900,'2026-06-01','2026-10-15 11:31:20'),(3,'USD',7.1800,'2026-07-01','2026-10-15 11:31:20'),(4,'USD',7.1600,'2026-08-01','2026-10-15 11:31:20'),(5,'USD',7.1500,'2026-09-01','2026-10-15 11:31:20'),(6,'USD',7.1400,'2026-10-01','2026-10-15 11:31:20');
/*!40000 ALTER TABLE `biz_exchange_rate` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_inventory`
--

DROP TABLE IF EXISTS `biz_inventory`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_inventory` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '库存ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `quantity` int DEFAULT '0' COMMENT '库存数量',
  `locked_quantity` int DEFAULT '0' COMMENT '锁定数量(已下单未发货)',
  `warning_quantity` int DEFAULT '100' COMMENT '库存预警值',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_warehouse` (`product_id`,`warehouse_id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_inventory`
--

LOCK TABLES `biz_inventory` WRITE;
/*!40000 ALTER TABLE `biz_inventory` DISABLE KEYS */;
INSERT INTO `biz_inventory` VALUES (1,1,1,50000,0,5000,'2026-10-15 11:31:20'),(2,1,3,12000,500,2000,'2026-10-15 11:31:20'),(3,2,1,20000,0,2000,'2026-10-15 11:31:20'),(4,2,3,5000,200,1000,'2026-10-15 11:31:20'),(5,3,1,15000,0,1500,'2026-10-15 11:31:20'),(6,3,3,8000,300,1000,'2026-10-15 11:31:20'),(7,3,4,3000,0,500,'2026-10-15 11:31:20'),(8,4,1,8000,0,800,'2026-10-15 11:31:20'),(9,4,3,4000,100,500,'2026-10-15 11:31:20'),(10,5,1,30000,0,3000,'2026-10-15 11:31:20'),(11,5,4,6000,200,1000,'2026-10-15 11:31:20'),(12,6,1,10000,0,1000,'2026-10-15 11:31:20'),(13,6,4,2500,0,500,'2026-10-15 11:31:20'),(14,7,1,6000,0,600,'2026-10-15 11:31:20'),(15,7,3,2000,0,300,'2026-10-15 11:31:20'),(16,8,1,9000,0,900,'2026-10-15 11:31:20'),(17,8,4,3500,150,500,'2026-10-15 11:31:20');
/*!40000 ALTER TABLE `biz_inventory` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_inventory_log`
--

DROP TABLE IF EXISTS `biz_inventory_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_inventory_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `change_type` tinyint NOT NULL COMMENT '类型 1入库 2出库 3锁定 4解锁',
  `quantity` int NOT NULL COMMENT '变动数量',
  `before_quantity` int DEFAULT NULL COMMENT '变动前数量',
  `before_locked_quantity` int DEFAULT NULL COMMENT '变动前锁定数量',
  `after_quantity` int DEFAULT NULL COMMENT '变动后数量',
  `after_locked_quantity` int DEFAULT NULL COMMENT '变动后锁定数量',
  `order_no` varchar(50) DEFAULT NULL COMMENT '关联单号',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_warehouse` (`warehouse_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存变动记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_inventory_log`
--

LOCK TABLES `biz_inventory_log` WRITE;
/*!40000 ALTER TABLE `biz_inventory_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_inventory_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_order`
--

DROP TABLE IF EXISTS `biz_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` varchar(50) NOT NULL COMMENT '订单编号',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `order_date` date NOT NULL COMMENT '下单日期',
  `total_quantity` int DEFAULT '0' COMMENT '总数量',
  `total_amount_usd` decimal(14,2) DEFAULT '0.00' COMMENT '总金额(USD)',
  `currency` varchar(10) DEFAULT 'USD' COMMENT '币种',
  `exchange_rate` decimal(10,4) DEFAULT '7.2000' COMMENT '汇率(兑CNY)',
  `status` tinyint DEFAULT '0' COMMENT '状态 0待确认 1已确认 2备货中 3已发货 4已签收 5已完成 9已取消',
  `payment_status` tinyint DEFAULT '0' COMMENT '付款状态 0未付款 1部分付款 2已付清',
  `sales_user_id` bigint DEFAULT NULL COMMENT '销售负责人ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_order_date` (`order_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售订单表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_order`
--

LOCK TABLES `biz_order` WRITE;
/*!40000 ALTER TABLE `biz_order` DISABLE KEYS */;
INSERT INTO `biz_order` VALUES (1,'SO202605-001',1,'2026-05-06',20000,59800.00,'USD',7.2100,5,2,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(2,'SO202605-002',2,'2026-05-18',5000,99950.00,'USD',7.2100,5,2,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(3,'SO202606-001',3,'2026-06-08',10000,89900.00,'USD',7.1900,5,2,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(4,'SO202606-002',4,'2026-06-22',8000,71920.00,'USD',7.1900,4,1,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(5,'SO202607-001',1,'2026-07-05',15000,299850.00,'USD',7.1800,5,2,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(6,'SO202607-002',5,'2026-07-19',3000,89970.00,'USD',7.1800,4,0,1,NULL,0,'2026-10-15 11:31:20','2026-10-24 17:39:16'),(7,'SO202608-001',2,'2026-08-09',12000,239880.00,'USD',7.1600,3,1,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(8,'SO202608-002',6,'2026-08-25',5000,74950.00,'USD',7.1600,2,0,1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(9,'SO202609-001',3,'2026-09-11',10000,129900.00,'USD',7.1500,3,0,1,NULL,1,'2026-10-15 11:31:20','2026-10-24 17:36:36'),(10,'SO202610-001',1,'2026-10-03',25000,74750.00,'USD',7.1400,1,0,1,NULL,1,'2026-10-15 11:31:20','2026-10-24 17:36:30');
/*!40000 ALTER TABLE `biz_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_order_item`
--

DROP TABLE IF EXISTS `biz_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `product_id` bigint NOT NULL COMMENT '产品ID',
  `warehouse_id` bigint DEFAULT NULL COMMENT '发货仓库ID',
  `quantity` int NOT NULL COMMENT '数量',
  `unit_price_usd` decimal(12,2) NOT NULL COMMENT '单价(USD)',
  `total_price_usd` decimal(14,2) NOT NULL COMMENT '小计(USD)',
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单明细表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_order_item`
--

LOCK TABLES `biz_order_item` WRITE;
/*!40000 ALTER TABLE `biz_order_item` DISABLE KEYS */;
INSERT INTO `biz_order_item` VALUES (1,1,1,1,20000,2.99,59800.00),(2,2,3,1,5000,19.99,99950.00),(3,3,5,1,10000,8.99,89900.00),(4,4,2,1,8000,8.99,71920.00),(5,5,4,1,15000,19.99,299850.00),(6,6,7,1,3000,29.99,89970.00),(7,7,3,1,12000,19.99,239880.00),(8,8,8,1,5000,14.99,74950.00);
/*!40000 ALTER TABLE `biz_order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_payment`
--

DROP TABLE IF EXISTS `biz_payment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_payment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收付款ID',
  `payment_no` varchar(50) NOT NULL COMMENT '流水号',
  `order_id` bigint DEFAULT NULL COMMENT '关联订单ID',
  `type` tinyint NOT NULL COMMENT '类型 1收款 2付款',
  `amount` decimal(14,2) NOT NULL COMMENT '金额',
  `currency` varchar(10) DEFAULT 'USD' COMMENT '币种',
  `exchange_rate` decimal(10,4) DEFAULT '7.2000' COMMENT '汇率',
  `amount_cny` decimal(14,2) DEFAULT NULL COMMENT '折合人民币',
  `payment_date` date NOT NULL COMMENT '收付日期',
  `method` varchar(50) DEFAULT NULL COMMENT '方式 电汇/信用证/PayPal/西联',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_order` (`order_id`),
  KEY `idx_payment_date` (`payment_date`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='收付款表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_payment`
--

LOCK TABLES `biz_payment` WRITE;
/*!40000 ALTER TABLE `biz_payment` DISABLE KEYS */;
INSERT INTO `biz_payment` VALUES (1,'PAY202605-001',1,1,59800.00,'USD',7.2100,431158.00,'2026-05-20','电汇',NULL,'2026-10-15 11:31:20'),(2,'PAY202606-001',2,1,99950.00,'USD',7.1900,718640.50,'2026-06-05','电汇',NULL,'2026-10-15 11:31:20'),(3,'PAY202606-002',3,1,89900.00,'USD',7.1900,646381.00,'2026-06-25','电汇',NULL,'2026-10-15 11:31:20'),(4,'PAY202607-001',4,1,40000.00,'USD',7.1800,287200.00,'2026-07-02','信用证',NULL,'2026-10-15 11:31:20'),(5,'PAY202608-001',5,1,299850.00,'USD',7.1600,2146926.00,'2026-08-01','电汇',NULL,'2026-10-15 11:31:20'),(7,'PAY202609-001',2,2,85000.00,'USD',7.1500,607750.00,'2026-09-05','电汇',NULL,'2026-10-15 11:31:20');
/*!40000 ALTER TABLE `biz_payment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_product`
--

DROP TABLE IF EXISTS `biz_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '产品ID',
  `sku` varchar(50) NOT NULL COMMENT 'SKU编码',
  `name` varchar(100) NOT NULL COMMENT '产品名称(中文)',
  `name_en` varchar(200) DEFAULT NULL COMMENT '产品名称(英文)',
  `category` varchar(50) DEFAULT NULL COMMENT '产品分类',
  `unit` varchar(20) DEFAULT 'pcs' COMMENT '单位',
  `cost_price` decimal(12,2) DEFAULT NULL COMMENT '成本价(CNY)',
  `sale_price_usd` decimal(12,2) DEFAULT NULL COMMENT '销售价(USD)',
  `description` varchar(500) DEFAULT NULL COMMENT '产品描述',
  `status` tinyint DEFAULT '1' COMMENT '状态 0下架 1在售',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku` (`sku`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='产品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_product`
--

LOCK TABLES `biz_product` WRITE;
/*!40000 ALTER TABLE `biz_product` DISABLE KEYS */;
INSERT INTO `biz_product` VALUES (1,'SKU-LED-001','LED灯泡 9W','LED Bulb 9W E26','照明产品','pcs',8.50,2.99,NULL,0,0,'2026-10-15 11:31:20','2026-10-20 10:56:32'),(2,'SKU-LED-002','LED灯带 5米','LED Strip Light 5M RGB','照明产品','roll',35.00,12.99,NULL,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(3,'SKU-ELE-001','蓝牙耳机','Bluetooth Earbuds TWS','电子产品','pcs',45.00,19.99,NULL,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(4,'SKU-ELE-002','智能手表','Smart Watch Fitness Tracker','电子产品','pcs',85.00,39.99,NULL,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(5,'SKU-HOM-001','不锈钢保温杯','Stainless Steel Tumbler 30oz','家居用品','pcs',15.00,8.99,NULL,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(6,'SKU-HOM-002','硅胶厨具套装','Silicone Kitchen Utensil Set','家居用品','set',28.00,15.99,NULL,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(7,'SKU-TOY-001','遥控车','RC Car 4WD Off-Road','玩具','pcs',55.00,29.99,NULL,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(8,'SKU-TOY-002','积木套装','Building Blocks Set 500pcs','玩具','set',40.00,24.99,NULL,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20');
/*!40000 ALTER TABLE `biz_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_shipment`
--

DROP TABLE IF EXISTS `biz_shipment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_shipment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '发货ID',
  `shipment_no` varchar(50) NOT NULL COMMENT '发货单号',
  `order_id` bigint NOT NULL COMMENT '订单ID',
  `ship_date` date DEFAULT NULL COMMENT '发货日期',
  `transport_type` varchar(20) DEFAULT NULL COMMENT '运输方式 海运/空运/快递/铁路',
  `carrier` varchar(100) DEFAULT NULL COMMENT '承运商',
  `tracking_no` varchar(100) DEFAULT NULL COMMENT '追踪号',
  `container_no` varchar(50) DEFAULT NULL COMMENT '柜号',
  `port_from` varchar(100) DEFAULT NULL COMMENT '起运港',
  `port_to` varchar(100) DEFAULT NULL COMMENT '目的港',
  `etd` date DEFAULT NULL COMMENT '预计离港日',
  `eta` date DEFAULT NULL COMMENT '预计到港日',
  `status` tinyint DEFAULT '1' COMMENT '状态 1待发货 2运输中 3清关中 4已签收',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除 0正常 1已删除',
  `freight_cost_usd` decimal(12,2) DEFAULT NULL COMMENT '运费(USD)',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shipment_no` (`shipment_no`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='发货物流表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_shipment`
--

LOCK TABLES `biz_shipment` WRITE;
/*!40000 ALTER TABLE `biz_shipment` DISABLE KEYS */;
INSERT INTO `biz_shipment` VALUES (1,'SH202605-001',1,'2026-05-10','海运','马士基','MSKU1234567','MSKU1234567','深圳盐田港','洛杉矶港','2026-05-10','2026-05-28',4,0,3500.00,NULL,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(2,'SH202605-002',2,'2026-05-22','空运','联邦快递','FX987654321',NULL,'深圳宝安机场','纽约肯尼迪机场','2026-05-22','2026-05-25',4,0,12000.00,NULL,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(3,'SH202606-001',3,'2026-06-12','海运','中远海运','COSU7654321','COSU7654321','深圳蛇口港','西雅图港','2026-06-12','2026-06-30',4,0,3200.00,NULL,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(4,'SH202607-001',5,'2026-07-08','海运','长荣海运','EGLV2468101','EGLU2468101','深圳盐田港','洛杉矶港','2026-07-08','2026-07-26',4,0,4500.00,NULL,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(5,'SH202608-001',7,'2026-08-12','海运','马士基','MSKU8888888','MSKU8888888','深圳盐田港','纽约港','2026-08-12','2026-09-02',2,0,5200.00,NULL,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(6,'SH202609-001',9,'2026-09-15','快递','DHL','DHL555666777',NULL,'深圳','迈阿密','2026-09-15','2026-09-22',3,0,2800.00,NULL,'2026-10-15 11:31:20','2026-10-15 11:31:20');
/*!40000 ALTER TABLE `biz_shipment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_shipment_track`
--

DROP TABLE IF EXISTS `biz_shipment_track`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_shipment_track` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '轨迹ID',
  `shipment_id` bigint NOT NULL COMMENT '发货单ID',
  `status` tinyint NOT NULL COMMENT '对应发货状态 1待发货 2运输中 3清关中 4已签收',
  `node` varchar(50) NOT NULL COMMENT '节点名称',
  `location` varchar(100) DEFAULT NULL COMMENT '节点地点',
  `content` varchar(300) DEFAULT NULL COMMENT '节点说明',
  `track_time` datetime NOT NULL COMMENT '节点时间',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_shipment` (`shipment_id`),
  KEY `idx_track_time` (`track_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='物流轨迹节点';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_shipment_track`
--

LOCK TABLES `biz_shipment_track` WRITE;
/*!40000 ALTER TABLE `biz_shipment_track` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_shipment_track` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_stock_check`
--

DROP TABLE IF EXISTS `biz_stock_check`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_stock_check` (
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
  KEY `idx_warehouse_date` (`warehouse_id`,`check_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存盘点单';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_stock_check`
--

LOCK TABLES `biz_stock_check` WRITE;
/*!40000 ALTER TABLE `biz_stock_check` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_stock_check` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_stock_check_item`
--

DROP TABLE IF EXISTS `biz_stock_check_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_stock_check_item` (
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_stock_check_item`
--

LOCK TABLES `biz_stock_check_item` WRITE;
/*!40000 ALTER TABLE `biz_stock_check_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `biz_stock_check_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_supplier`
--

DROP TABLE IF EXISTS `biz_supplier`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '供应商ID',
  `supplier_code` varchar(50) NOT NULL COMMENT '供应商编码',
  `company_name` varchar(200) NOT NULL COMMENT '公司名称',
  `contact_name` varchar(100) DEFAULT NULL COMMENT '联系人',
  `country` varchar(50) DEFAULT '中国' COMMENT '国家',
  `city` varchar(50) DEFAULT NULL COMMENT '城市',
  `phone` varchar(50) DEFAULT NULL COMMENT '电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `category` varchar(100) DEFAULT NULL COMMENT '供应产品类别',
  `status` tinyint DEFAULT '1' COMMENT '状态 0停用 1合作中',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_code` (`supplier_code`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='供应商表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_supplier`
--

LOCK TABLES `biz_supplier` WRITE;
/*!40000 ALTER TABLE `biz_supplier` DISABLE KEYS */;
INSERT INTO `biz_supplier` VALUES (1,'SUP-CN-001','深圳市光明电子有限公司','王工','中国','深圳','0755-12345678',NULL,'电子产品',0,NULL,0,'2026-10-15 11:31:20','2026-10-22 10:12:23'),(2,'SUP-CN-002','义乌市美家居用品厂','陈老板','中国','义乌','0579-87654321',NULL,'家居用品',1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(3,'SUP-CN-003','东莞市华玩具制造厂','刘经理','中国','东莞','0769-11223344',NULL,'玩具',1,NULL,0,'2026-10-15 11:31:20','2026-10-15 11:31:20');
/*!40000 ALTER TABLE `biz_supplier` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `biz_warehouse`
--

DROP TABLE IF EXISTS `biz_warehouse`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `biz_warehouse` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '仓库ID',
  `warehouse_code` varchar(50) NOT NULL COMMENT '仓库编码',
  `warehouse_name` varchar(100) NOT NULL COMMENT '仓库名称',
  `type` tinyint DEFAULT '1' COMMENT '类型 1国内仓 2海外仓',
  `country` varchar(50) DEFAULT NULL COMMENT '所在国家',
  `city` varchar(50) DEFAULT NULL COMMENT '所在城市',
  `address` varchar(200) DEFAULT NULL COMMENT '详细地址',
  `manager` varchar(50) DEFAULT NULL COMMENT '仓库负责人',
  `capacity` int DEFAULT NULL COMMENT '容量(立方米)',
  `status` tinyint DEFAULT '1' COMMENT '状态 0停用 1启用',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='仓库表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `biz_warehouse`
--

LOCK TABLES `biz_warehouse` WRITE;
/*!40000 ALTER TABLE `biz_warehouse` DISABLE KEYS */;
INSERT INTO `biz_warehouse` VALUES (1,'WH-CN-SZ','深圳总仓',1,'中国','深圳','深圳市宝安区福永街道物流园A区','张三',5000,1,0,'2026-10-15 11:31:20','2026-10-24 17:38:07'),(2,'WH-CN-YW','义乌仓',1,'中国','义乌','义乌市国际商贸城仓储区B12','李四',3000,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(3,'WH-US-LA','洛杉矶海外仓',2,'美国','洛杉矶','1234 Warehouse St, Los Angeles, CA','Mike Johnson',8000,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20'),(4,'WH-US-NY','纽约海外仓',2,'美国','纽约','567 Storage Ave, Newark, NJ','Sarah Smith',6000,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20');
/*!40000 ALTER TABLE `biz_warehouse` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_attendance`
--

DROP TABLE IF EXISTS `sys_attendance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_attendance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '考勤ID',
  `emp_id` bigint NOT NULL COMMENT '员工ID',
  `attend_date` date NOT NULL COMMENT '考勤日期',
  `check_in_time` datetime DEFAULT NULL COMMENT '上班打卡时间',
  `check_out_time` datetime DEFAULT NULL COMMENT '下班打卡时间',
  `status` tinyint DEFAULT '1' COMMENT '状态 1正常 2迟到 3早退 4缺勤 5请假 6出差',
  `work_hours` decimal(4,2) DEFAULT '0.00' COMMENT '工作时长(小时)',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_date` (`emp_id`,`attend_date`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_attend_date` (`attend_date`)
) ENGINE=InnoDB AUTO_INCREMENT=603 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='考勤表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_attendance`
--

LOCK TABLES `sys_attendance` WRITE;
/*!40000 ALTER TABLE `sys_attendance` DISABLE KEYS */;
INSERT INTO `sys_attendance` VALUES (1,14,'2026-10-15','2026-10-15 09:33:00','0000-00-00 00:00:00',5,8.65,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(2,9,'2026-10-15','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.05,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(3,13,'2026-10-15','2026-10-15 20:35:00','0000-00-00 00:00:00',1,9.95,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(4,12,'2026-10-15','2026-10-15 18:23:00','0000-00-00 00:00:00',1,9.17,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(5,8,'2026-10-15','2026-10-15 23:21:00','0000-00-00 00:00:00',1,8.65,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(6,11,'2026-10-15','2026-10-15 18:12:00','0000-00-00 00:00:00',1,9.88,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(7,10,'2026-10-15','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.46,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(8,7,'2026-10-15','2026-10-15 09:20:00','0000-00-00 00:00:00',1,8.81,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(9,6,'2026-10-15','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.82,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(10,20,'2026-10-15','2026-10-15 18:43:00','0000-00-00 00:00:00',1,9.02,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(11,19,'2026-10-15','2026-10-15 10:28:00','0000-00-00 00:00:00',1,8.22,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(12,5,'2026-10-15','0000-00-00 00:00:00','0000-00-00 00:00:00',5,8.72,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(13,18,'2026-10-15','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.01,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(14,17,'2026-10-15','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.87,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(15,4,'2026-10-15','2026-10-15 14:29:00','0000-00-00 00:00:00',1,8.55,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(16,16,'2026-10-15','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(17,15,'2026-10-15','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.96,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(18,3,'2026-10-15','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.52,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(19,2,'2026-10-15','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.95,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(20,1,'2026-10-15','2026-10-15 17:02:00','0000-00-00 00:00:00',1,9.81,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(21,14,'2026-10-14','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.12,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(22,9,'2026-10-14','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.42,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(23,13,'2026-10-14','2026-10-14 22:35:00','0000-00-00 00:00:00',1,9.43,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(24,12,'2026-10-14','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.14,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(25,8,'2026-10-14','0000-00-00 00:00:00','0000-00-00 00:00:00',2,9.27,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(26,11,'2026-10-14','2026-10-14 21:55:00','2026-10-14 18:59:00',1,8.27,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(27,10,'2026-10-14','2026-10-14 19:29:00','0000-00-00 00:00:00',2,9.86,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(28,7,'2026-10-14','2026-10-14 09:26:00','0000-00-00 00:00:00',1,9.50,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(29,6,'2026-10-14','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.03,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(30,20,'2026-10-14','2026-10-14 19:39:00','0000-00-00 00:00:00',1,8.83,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(31,19,'2026-10-14','2026-10-14 12:48:00','0000-00-00 00:00:00',1,9.10,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(32,5,'2026-10-14','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.07,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(33,18,'2026-10-14','0000-00-00 00:00:00','0000-00-00 00:00:00',3,9.70,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(34,17,'2026-10-14','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.36,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(35,4,'2026-10-14','2026-10-14 10:02:00','0000-00-00 00:00:00',1,9.53,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(36,16,'2026-10-14','0000-00-00 00:00:00','0000-00-00 00:00:00',2,9.57,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(37,15,'2026-10-14','2026-10-14 11:44:00','2026-10-14 19:46:00',1,9.55,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(38,3,'2026-10-14','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.71,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(39,2,'2026-10-14','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.30,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(40,1,'2026-10-14','2026-10-14 10:43:00','0000-00-00 00:00:00',1,9.02,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(41,14,'2026-10-13','2026-10-13 14:03:00','0000-00-00 00:00:00',1,9.92,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(42,9,'2026-10-13','2026-10-13 11:39:00','2026-10-13 20:10:00',1,8.79,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(43,13,'2026-10-13','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.92,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(44,12,'2026-10-13','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.72,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(45,8,'2026-10-13','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.79,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(46,11,'2026-10-13','2026-10-13 22:44:00','0000-00-00 00:00:00',5,9.48,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(47,10,'2026-10-13','2026-10-13 14:51:00','2026-10-13 22:11:00',1,9.95,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(48,7,'2026-10-13','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.61,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(49,6,'2026-10-13','2026-10-13 17:51:00','0000-00-00 00:00:00',1,9.66,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(50,20,'2026-10-13','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.79,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(51,19,'2026-10-13','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.85,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(52,5,'2026-10-13','2026-10-13 14:26:00','0000-00-00 00:00:00',1,9.67,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(53,18,'2026-10-13','2026-10-13 19:53:00','2026-10-13 23:22:00',1,9.25,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(54,17,'2026-10-13','2026-10-13 19:24:00','0000-00-00 00:00:00',1,9.18,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(55,4,'2026-10-13','2026-10-13 17:21:00','0000-00-00 00:00:00',1,8.05,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(56,16,'2026-10-13','2026-10-13 18:53:00','0000-00-00 00:00:00',1,9.06,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(57,15,'2026-10-13','0000-00-00 00:00:00','2026-10-13 18:13:00',3,9.41,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(58,3,'2026-10-13','2026-10-13 12:16:00','0000-00-00 00:00:00',1,8.31,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(59,2,'2026-10-13','2026-10-13 14:20:00','0000-00-00 00:00:00',1,8.27,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(60,1,'2026-10-13','2026-10-13 13:39:00','2026-10-13 19:45:00',1,8.66,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(61,14,'2026-10-12','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.16,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(62,9,'2026-10-12','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.66,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(63,13,'2026-10-12','2026-10-12 12:36:00','0000-00-00 00:00:00',1,9.57,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(64,12,'2026-10-12','2026-10-12 20:11:00','0000-00-00 00:00:00',1,9.16,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(65,8,'2026-10-12','2026-10-12 15:02:00','0000-00-00 00:00:00',1,9.21,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(66,11,'2026-10-12','2026-10-12 09:50:00','2026-10-12 20:54:00',1,9.80,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(67,10,'2026-10-12','2026-10-12 12:07:00','0000-00-00 00:00:00',1,9.02,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(68,7,'2026-10-12','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.43,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(69,6,'2026-10-12','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.21,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(70,20,'2026-10-12','2026-10-12 19:46:00','0000-00-00 00:00:00',1,9.43,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(71,19,'2026-10-12','0000-00-00 00:00:00','2026-10-12 18:44:00',1,8.54,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(72,5,'2026-10-12','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.48,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(73,18,'2026-10-12','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.10,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(74,17,'2026-10-12','2026-10-12 12:01:00','0000-00-00 00:00:00',1,9.41,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(75,4,'2026-10-12','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.69,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(76,16,'2026-10-12','2026-10-12 10:27:00','0000-00-00 00:00:00',1,8.72,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(77,15,'2026-10-12','2026-10-12 17:20:00','0000-00-00 00:00:00',1,8.25,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(78,3,'2026-10-12','2026-10-12 14:55:00','0000-00-00 00:00:00',1,8.59,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(79,2,'2026-10-12','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.23,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(80,1,'2026-10-12','2026-10-12 10:04:00','0000-00-00 00:00:00',1,9.03,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(81,14,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(82,9,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(83,13,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(84,12,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(85,8,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(86,11,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(87,10,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(88,7,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(89,6,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(90,20,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(91,19,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(92,5,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(93,18,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(94,17,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(95,4,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(96,16,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(97,15,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(98,3,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(99,2,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(100,1,'2026-10-11',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(101,14,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(102,9,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(103,13,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(104,12,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(105,8,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(106,11,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(107,10,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(108,7,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(109,6,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(110,20,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(111,19,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(112,5,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(113,18,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(114,17,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(115,4,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(116,16,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(117,15,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(118,3,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(119,2,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(120,1,'2026-10-10',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(121,14,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.12,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(122,9,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.82,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(123,13,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.54,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(124,12,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',2,9.26,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(125,8,'2026-10-09','0000-00-00 00:00:00','2026-10-09 21:59:00',5,8.92,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(126,11,'2026-10-09','2026-10-09 10:20:00','0000-00-00 00:00:00',1,9.25,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(127,10,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',2,8.14,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(128,7,'2026-10-09','2026-10-09 13:57:00','0000-00-00 00:00:00',1,9.59,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(129,6,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.70,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(130,20,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.81,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(131,19,'2026-10-09','2026-10-09 21:32:00','0000-00-00 00:00:00',1,9.93,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(132,5,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.16,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(133,18,'2026-10-09','2026-10-09 16:44:00','0000-00-00 00:00:00',1,8.42,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(134,17,'2026-10-09','2026-10-09 18:28:00','0000-00-00 00:00:00',5,8.52,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(135,4,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.12,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(136,16,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.63,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(137,15,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.46,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(138,3,'2026-10-09','2026-10-09 14:37:00','0000-00-00 00:00:00',1,8.25,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(139,2,'2026-10-09','0000-00-00 00:00:00','2026-10-09 20:35:00',1,9.48,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(140,1,'2026-10-09','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.80,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(141,14,'2026-10-08','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.61,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(142,9,'2026-10-08','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.53,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(143,13,'2026-10-08','2026-10-08 18:30:00','0000-00-00 00:00:00',5,9.97,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(144,12,'2026-10-08','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.17,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(145,8,'2026-10-08','2026-10-08 09:16:00','0000-00-00 00:00:00',1,9.09,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(146,11,'2026-10-08','2026-10-08 22:14:00','0000-00-00 00:00:00',1,8.33,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(147,10,'2026-10-08','2026-10-08 12:58:00','0000-00-00 00:00:00',1,9.47,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(148,7,'2026-10-08','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.06,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(149,6,'2026-10-08','2026-10-08 13:04:00','0000-00-00 00:00:00',2,8.56,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(150,20,'2026-10-08','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.30,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(151,19,'2026-10-08','2026-10-08 16:17:00','0000-00-00 00:00:00',1,9.75,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(152,5,'2026-10-08','2026-10-08 13:31:00','0000-00-00 00:00:00',1,9.01,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(153,18,'2026-10-08','2026-10-08 11:43:00','0000-00-00 00:00:00',1,8.31,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(154,17,'2026-10-08','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.39,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(155,4,'2026-10-08','0000-00-00 00:00:00','2026-10-08 18:24:00',2,8.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(156,16,'2026-10-08','2026-10-08 17:47:00','0000-00-00 00:00:00',1,8.62,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(157,15,'2026-10-08','2026-10-08 20:16:00','0000-00-00 00:00:00',1,8.76,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(158,3,'2026-10-08','2026-10-08 21:45:00','0000-00-00 00:00:00',1,9.51,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(159,2,'2026-10-08','2026-10-08 09:23:00','0000-00-00 00:00:00',1,8.81,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(160,1,'2026-10-08','2026-10-08 21:54:00','0000-00-00 00:00:00',1,9.77,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(161,14,'2026-10-07','2026-10-07 09:27:00','2026-10-07 21:26:00',1,8.68,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(162,9,'2026-10-07','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.51,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(163,13,'2026-10-07','2026-10-07 11:00:00','0000-00-00 00:00:00',1,8.35,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(164,12,'2026-10-07','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.21,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(165,8,'2026-10-07','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.40,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(166,11,'2026-10-07','2026-10-07 23:27:00','0000-00-00 00:00:00',2,9.28,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(167,10,'2026-10-07','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.74,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(168,7,'2026-10-07','2026-10-07 12:42:00','0000-00-00 00:00:00',1,8.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(169,6,'2026-10-07','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.39,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(170,20,'2026-10-07','2026-10-07 22:16:00','0000-00-00 00:00:00',1,8.51,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(171,19,'2026-10-07','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.84,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(172,5,'2026-10-07','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.40,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(173,18,'2026-10-07','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.88,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(174,17,'2026-10-07','0000-00-00 00:00:00','2026-10-07 20:50:00',1,9.13,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(175,4,'2026-10-07','2026-10-07 09:36:00','0000-00-00 00:00:00',1,9.48,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(176,16,'2026-10-07','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.88,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(177,15,'2026-10-07','2026-10-07 11:25:00','0000-00-00 00:00:00',1,9.01,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(178,3,'2026-10-07','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.02,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(179,2,'2026-10-07','2026-10-07 20:40:00','2026-10-07 23:46:00',1,8.77,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(180,1,'2026-10-07','2026-10-07 10:58:00','0000-00-00 00:00:00',1,8.52,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(181,14,'2026-10-06','2026-10-06 13:55:00','0000-00-00 00:00:00',1,9.87,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(182,9,'2026-10-06','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.07,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(183,13,'2026-10-06','0000-00-00 00:00:00','0000-00-00 00:00:00',2,8.28,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(184,12,'2026-10-06','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.95,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(185,8,'2026-10-06','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.02,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(186,11,'2026-10-06','2026-10-06 18:19:00','0000-00-00 00:00:00',1,8.77,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(187,10,'2026-10-06','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.88,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(188,7,'2026-10-06','2026-10-06 17:59:00','0000-00-00 00:00:00',1,8.90,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(189,6,'2026-10-06','0000-00-00 00:00:00','2026-10-06 22:56:00',1,8.95,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(190,20,'2026-10-06','0000-00-00 00:00:00','0000-00-00 00:00:00',2,9.73,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(191,19,'2026-10-06','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.33,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(192,5,'2026-10-06','2026-10-06 15:02:00','0000-00-00 00:00:00',1,9.59,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(193,18,'2026-10-06','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.42,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(194,17,'2026-10-06','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.38,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(195,4,'2026-10-06','2026-10-06 18:49:00','0000-00-00 00:00:00',1,9.18,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(196,16,'2026-10-06','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.34,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(197,15,'2026-10-06','2026-10-06 19:01:00','0000-00-00 00:00:00',1,9.32,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(198,3,'2026-10-06','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.15,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(199,2,'2026-10-06','2026-10-06 17:08:00','0000-00-00 00:00:00',2,9.20,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(200,1,'2026-10-06','2026-10-06 10:21:00','2026-10-06 18:12:00',2,8.88,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(201,14,'2026-10-05','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.84,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(202,9,'2026-10-05','0000-00-00 00:00:00','0000-00-00 00:00:00',2,9.83,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(203,13,'2026-10-05','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.12,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(204,12,'2026-10-05','2026-10-05 20:23:00','0000-00-00 00:00:00',3,8.59,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(205,8,'2026-10-05','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.20,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(206,11,'2026-10-05','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.25,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(207,10,'2026-10-05','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.80,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(208,7,'2026-10-05','2026-10-05 20:29:00','2026-10-05 20:15:00',1,8.32,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(209,6,'2026-10-05','2026-10-05 21:44:00','0000-00-00 00:00:00',1,9.03,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(210,20,'2026-10-05','2026-10-05 16:34:00','2026-10-05 18:35:00',3,8.52,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(211,19,'2026-10-05','2026-10-05 16:35:00','0000-00-00 00:00:00',1,8.89,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(212,5,'2026-10-05','2026-10-05 10:28:00','0000-00-00 00:00:00',1,8.26,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(213,18,'2026-10-05','2026-10-05 15:35:00','0000-00-00 00:00:00',1,8.11,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(214,17,'2026-10-05','2026-10-05 14:51:00','0000-00-00 00:00:00',1,8.75,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(215,4,'2026-10-05','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.87,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(216,16,'2026-10-05','2026-10-05 12:00:00','0000-00-00 00:00:00',1,9.42,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(217,15,'2026-10-05','0000-00-00 00:00:00','2026-10-05 21:07:00',1,8.38,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(218,3,'2026-10-05','2026-10-05 19:10:00','0000-00-00 00:00:00',1,8.57,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(219,2,'2026-10-05','2026-10-05 12:45:00','0000-00-00 00:00:00',1,8.58,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(220,1,'2026-10-05','2026-10-05 21:21:00','0000-00-00 00:00:00',1,8.50,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(221,14,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(222,9,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(223,13,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(224,12,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(225,8,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(226,11,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(227,10,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(228,7,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(229,6,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(230,20,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(231,19,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(232,5,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(233,18,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(234,17,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(235,4,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(236,16,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(237,15,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(238,3,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(239,2,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(240,1,'2026-10-04',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(241,14,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(242,9,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(243,13,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(244,12,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(245,8,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(246,11,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(247,10,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(248,7,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(249,6,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(250,20,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(251,19,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(252,5,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(253,18,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(254,17,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(255,4,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(256,16,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(257,15,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(258,3,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(259,2,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(260,1,'2026-10-03',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(261,14,'2026-10-02','2026-10-02 18:35:00','2026-10-02 18:11:00',1,9.37,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(262,9,'2026-10-02','2026-10-02 19:51:00','0000-00-00 00:00:00',1,8.43,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(263,13,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.97,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(264,12,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.12,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(265,8,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.24,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(266,11,'2026-10-02','2026-10-02 15:23:00','0000-00-00 00:00:00',1,9.31,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(267,10,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.54,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(268,7,'2026-10-02','2026-10-02 15:35:00','0000-00-00 00:00:00',1,9.81,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(269,6,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.85,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(270,20,'2026-10-02','2026-10-02 15:28:00','0000-00-00 00:00:00',1,8.33,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(271,19,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.21,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(272,5,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.21,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(273,18,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.62,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(274,17,'2026-10-02','2026-10-02 11:57:00','0000-00-00 00:00:00',1,8.35,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(275,4,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.70,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(276,16,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.16,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(277,15,'2026-10-02','2026-10-02 17:25:00','0000-00-00 00:00:00',1,8.21,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(278,3,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.78,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(279,2,'2026-10-02','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.72,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(280,1,'2026-10-02','2026-10-02 13:51:00','0000-00-00 00:00:00',1,9.40,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(281,14,'2026-10-01','0000-00-00 00:00:00','0000-00-00 00:00:00',2,9.98,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(282,9,'2026-10-01','2026-10-01 21:23:00','0000-00-00 00:00:00',1,8.39,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(283,13,'2026-10-01','0000-00-00 00:00:00','2026-10-01 20:17:00',1,9.92,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(284,12,'2026-10-01','2026-10-01 20:20:00','0000-00-00 00:00:00',1,9.94,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(285,8,'2026-10-01','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.92,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(286,11,'2026-10-01','2026-10-01 14:25:00','0000-00-00 00:00:00',1,9.68,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(287,10,'2026-10-01','2026-10-01 13:52:00','0000-00-00 00:00:00',1,8.99,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(288,7,'2026-10-01','2026-10-01 09:02:00','0000-00-00 00:00:00',1,8.75,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(289,6,'2026-10-01','2026-10-01 23:05:00','0000-00-00 00:00:00',1,8.93,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(290,20,'2026-10-01','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.42,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(291,19,'2026-10-01','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.92,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(292,5,'2026-10-01','2026-10-01 15:41:00','0000-00-00 00:00:00',1,9.09,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(293,18,'2026-10-01','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.13,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(294,17,'2026-10-01','2026-10-01 15:38:00','0000-00-00 00:00:00',1,8.90,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(295,4,'2026-10-01','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.95,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(296,16,'2026-10-01','0000-00-00 00:00:00','2026-10-01 19:47:00',1,9.25,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(297,15,'2026-10-01','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.20,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(298,3,'2026-10-01','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.37,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(299,2,'2026-10-01','0000-00-00 00:00:00','2026-10-01 21:31:00',1,8.97,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(300,1,'2026-10-01','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.72,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(301,14,'2026-09-30','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.25,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(302,9,'2026-09-30','2026-09-30 22:35:00','0000-00-00 00:00:00',3,9.34,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(303,13,'2026-09-30','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.53,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(304,12,'2026-09-30','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.42,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(305,8,'2026-09-30','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.51,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(306,11,'2026-09-30','2026-09-30 21:56:00','0000-00-00 00:00:00',1,9.38,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(307,10,'2026-09-30','2026-09-30 20:26:00','0000-00-00 00:00:00',1,8.54,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(308,7,'2026-09-30','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.76,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(309,6,'2026-09-30','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.84,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(310,20,'2026-09-30','2026-09-30 20:16:00','0000-00-00 00:00:00',1,9.56,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(311,19,'2026-09-30','2026-09-30 19:16:00','2026-09-30 18:58:00',1,9.68,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(312,5,'2026-09-30','2026-09-30 17:09:00','2026-09-30 21:36:00',1,9.90,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(313,18,'2026-09-30','2026-09-30 10:53:00','2026-09-30 18:03:00',3,8.84,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(314,17,'2026-09-30','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.33,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(315,4,'2026-09-30','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.37,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(316,16,'2026-09-30','2026-09-30 20:11:00','0000-00-00 00:00:00',1,9.06,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(317,15,'2026-09-30','2026-09-30 17:08:00','0000-00-00 00:00:00',1,8.02,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(318,3,'2026-09-30','2026-09-30 19:39:00','0000-00-00 00:00:00',5,9.62,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(319,2,'2026-09-30','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.26,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(320,1,'2026-09-30','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.90,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(321,14,'2026-09-29','2026-09-29 21:17:00','0000-00-00 00:00:00',1,8.35,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(322,9,'2026-09-29','2026-09-29 10:05:00','0000-00-00 00:00:00',1,8.75,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(323,13,'2026-09-29','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.32,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(324,12,'2026-09-29','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.37,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(325,8,'2026-09-29','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.32,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(326,11,'2026-09-29','2026-09-29 12:52:00','0000-00-00 00:00:00',1,9.40,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(327,10,'2026-09-29','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.26,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(328,7,'2026-09-29','0000-00-00 00:00:00','0000-00-00 00:00:00',3,9.43,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(329,6,'2026-09-29','2026-09-29 10:53:00','0000-00-00 00:00:00',1,9.39,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(330,20,'2026-09-29','2026-09-29 09:09:00','0000-00-00 00:00:00',1,8.84,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(331,19,'2026-09-29','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.01,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(332,5,'2026-09-29','2026-09-29 22:58:00','0000-00-00 00:00:00',1,8.06,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(333,18,'2026-09-29','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.86,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(334,17,'2026-09-29','2026-09-29 22:23:00','0000-00-00 00:00:00',1,8.69,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(335,4,'2026-09-29','2026-09-29 19:53:00','0000-00-00 00:00:00',1,8.84,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(336,16,'2026-09-29','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.41,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(337,15,'2026-09-29','2026-09-29 20:32:00','2026-09-29 20:37:00',2,8.41,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(338,3,'2026-09-29','2026-09-29 14:04:00','0000-00-00 00:00:00',1,9.40,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(339,2,'2026-09-29','2026-09-29 23:15:00','0000-00-00 00:00:00',1,8.20,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(340,1,'2026-09-29','2026-09-29 14:17:00','0000-00-00 00:00:00',1,8.68,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(341,14,'2026-09-28','2026-09-28 23:32:00','0000-00-00 00:00:00',1,8.49,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(342,9,'2026-09-28','2026-09-28 12:16:00','0000-00-00 00:00:00',1,8.84,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(343,13,'2026-09-28','2026-09-28 19:09:00','0000-00-00 00:00:00',1,9.42,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(344,12,'2026-09-28','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.68,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(345,8,'2026-09-28','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.45,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(346,11,'2026-09-28','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.79,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(347,10,'2026-09-28','2026-09-28 09:01:00','0000-00-00 00:00:00',1,8.63,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(348,7,'2026-09-28','2026-09-28 16:42:00','0000-00-00 00:00:00',1,8.72,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(349,6,'2026-09-28','2026-09-28 22:28:00','0000-00-00 00:00:00',1,8.19,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(350,20,'2026-09-28','2026-09-28 14:59:00','0000-00-00 00:00:00',1,8.55,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(351,19,'2026-09-28','2026-09-28 09:21:00','0000-00-00 00:00:00',1,8.25,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(352,5,'2026-09-28','2026-09-28 17:34:00','0000-00-00 00:00:00',1,8.68,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(353,18,'2026-09-28','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.26,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(354,17,'2026-09-28','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.78,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(355,4,'2026-09-28','2026-09-28 11:39:00','0000-00-00 00:00:00',1,9.20,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(356,16,'2026-09-28','2026-09-28 16:38:00','0000-00-00 00:00:00',1,8.69,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(357,15,'2026-09-28','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.24,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(358,3,'2026-09-28','2026-09-28 23:57:00','0000-00-00 00:00:00',1,9.86,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(359,2,'2026-09-28','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.51,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(360,1,'2026-09-28','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.96,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(361,14,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(362,9,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(363,13,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(364,12,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(365,8,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(366,11,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(367,10,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(368,7,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(369,6,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(370,20,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(371,19,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(372,5,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(373,18,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(374,17,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(375,4,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(376,16,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(377,15,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(378,3,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(379,2,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(380,1,'2026-09-27',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(381,14,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(382,9,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(383,13,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(384,12,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(385,8,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(386,11,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(387,10,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(388,7,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(389,6,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(390,20,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(391,19,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(392,5,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(393,18,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(394,17,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(395,4,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(396,16,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(397,15,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(398,3,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(399,2,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(400,1,'2026-09-26',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(401,14,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.07,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(402,9,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.86,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(403,13,'2026-09-25','2026-09-25 20:59:00','2026-09-25 20:49:00',2,9.13,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(404,12,'2026-09-25','2026-09-25 15:47:00','0000-00-00 00:00:00',1,9.53,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(405,8,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.09,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(406,11,'2026-09-25','2026-09-25 14:09:00','0000-00-00 00:00:00',1,9.63,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(407,10,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.70,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(408,7,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.23,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(409,6,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.43,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(410,20,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.93,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(411,19,'2026-09-25','2026-09-25 10:27:00','0000-00-00 00:00:00',1,8.55,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(412,5,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',2,8.44,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(413,18,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.66,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(414,17,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',3,9.86,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(415,4,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.11,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(416,16,'2026-09-25','2026-09-25 12:16:00','2026-09-25 18:08:00',1,8.76,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(417,15,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.22,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(418,3,'2026-09-25','0000-00-00 00:00:00','2026-09-25 18:26:00',1,9.12,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(419,2,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.13,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(420,1,'2026-09-25','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.20,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(421,14,'2026-09-24','2026-09-24 17:27:00','0000-00-00 00:00:00',1,8.50,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(422,9,'2026-09-24','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.73,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(423,13,'2026-09-24','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.58,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(424,12,'2026-09-24','2026-09-24 18:44:00','0000-00-00 00:00:00',3,8.86,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(425,8,'2026-09-24','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.93,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(426,11,'2026-09-24','0000-00-00 00:00:00','0000-00-00 00:00:00',2,9.17,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(427,10,'2026-09-24','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.12,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(428,7,'2026-09-24','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.55,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(429,6,'2026-09-24','2026-09-24 14:36:00','0000-00-00 00:00:00',2,9.90,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(430,20,'2026-09-24','0000-00-00 00:00:00','0000-00-00 00:00:00',5,10.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(431,19,'2026-09-24','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.66,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(432,5,'2026-09-24','2026-09-24 19:42:00','2026-09-24 20:54:00',5,8.78,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(433,18,'2026-09-24','2026-09-24 16:39:00','0000-00-00 00:00:00',1,8.68,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(434,17,'2026-09-24','0000-00-00 00:00:00','2026-09-24 23:41:00',1,9.87,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(435,4,'2026-09-24','2026-09-24 13:10:00','0000-00-00 00:00:00',1,9.07,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(436,16,'2026-09-24','2026-09-24 15:31:00','0000-00-00 00:00:00',1,8.58,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(437,15,'2026-09-24','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.55,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(438,3,'2026-09-24','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.50,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(439,2,'2026-09-24','0000-00-00 00:00:00','0000-00-00 00:00:00',3,9.96,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(440,1,'2026-09-24','2026-09-24 13:21:00','0000-00-00 00:00:00',1,9.59,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(441,14,'2026-09-23','2026-09-23 18:06:00','0000-00-00 00:00:00',1,9.07,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(442,9,'2026-09-23','2026-09-23 22:36:00','0000-00-00 00:00:00',1,9.65,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(443,13,'2026-09-23','2026-09-23 21:09:00','0000-00-00 00:00:00',1,8.15,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(444,12,'2026-09-23','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.53,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(445,8,'2026-09-23','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.45,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(446,11,'2026-09-23','2026-09-23 22:12:00','0000-00-00 00:00:00',1,8.72,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(447,10,'2026-09-23','2026-09-23 18:28:00','0000-00-00 00:00:00',1,8.45,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(448,7,'2026-09-23','0000-00-00 00:00:00','0000-00-00 00:00:00',2,9.88,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(449,6,'2026-09-23','2026-09-23 11:38:00','0000-00-00 00:00:00',1,9.65,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(450,20,'2026-09-23','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.65,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(451,19,'2026-09-23','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.11,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(452,5,'2026-09-23','0000-00-00 00:00:00','2026-09-23 23:29:00',1,9.33,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(453,18,'2026-09-23','0000-00-00 00:00:00','2026-09-23 23:45:00',1,8.98,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(454,17,'2026-09-23','2026-09-23 17:56:00','0000-00-00 00:00:00',1,9.04,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(455,4,'2026-09-23','2026-09-23 13:07:00','0000-00-00 00:00:00',1,8.65,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(456,16,'2026-09-23','2026-09-23 17:06:00','0000-00-00 00:00:00',1,9.86,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(457,15,'2026-09-23','0000-00-00 00:00:00','2026-09-23 21:13:00',1,8.54,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(458,3,'2026-09-23','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.88,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(459,2,'2026-09-23','0000-00-00 00:00:00','0000-00-00 00:00:00',2,9.84,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(460,1,'2026-09-23','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.51,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(461,14,'2026-09-22','0000-00-00 00:00:00','0000-00-00 00:00:00',5,9.21,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(462,9,'2026-09-22','2026-09-22 15:04:00','0000-00-00 00:00:00',1,9.59,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(463,13,'2026-09-22','2026-09-22 09:22:00','0000-00-00 00:00:00',1,8.22,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(464,12,'2026-09-22','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.15,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(465,8,'2026-09-22','2026-09-22 16:52:00','0000-00-00 00:00:00',1,9.99,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(466,11,'2026-09-22','2026-09-22 18:47:00','0000-00-00 00:00:00',1,8.63,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(467,10,'2026-09-22','2026-09-22 20:57:00','2026-09-22 23:40:00',1,9.94,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(468,7,'2026-09-22','2026-09-22 15:07:00','0000-00-00 00:00:00',1,8.56,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(469,6,'2026-09-22','0000-00-00 00:00:00','0000-00-00 00:00:00',3,8.75,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(470,20,'2026-09-22','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.60,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(471,19,'2026-09-22','2026-09-22 16:57:00','0000-00-00 00:00:00',1,8.09,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(472,5,'2026-09-22','0000-00-00 00:00:00','0000-00-00 00:00:00',3,9.90,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(473,18,'2026-09-22','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.78,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(474,17,'2026-09-22','2026-09-22 14:38:00','0000-00-00 00:00:00',1,9.02,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(475,4,'2026-09-22','2026-09-22 11:25:00','0000-00-00 00:00:00',1,8.87,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(476,16,'2026-09-22','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.95,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(477,15,'2026-09-22','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.87,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(478,3,'2026-09-22','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.80,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(479,2,'2026-09-22','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.04,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(480,1,'2026-09-22','2026-09-22 16:28:00','0000-00-00 00:00:00',1,9.16,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(481,14,'2026-09-21','2026-09-21 16:40:00','2026-09-21 23:36:00',1,9.08,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(482,9,'2026-09-21','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.01,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(483,13,'2026-09-21','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.34,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(484,12,'2026-09-21','2026-09-21 09:34:00','0000-00-00 00:00:00',1,8.29,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(485,8,'2026-09-21','2026-09-21 23:30:00','0000-00-00 00:00:00',1,8.46,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(486,11,'2026-09-21','2026-09-21 13:27:00','0000-00-00 00:00:00',3,8.98,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(487,10,'2026-09-21','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.74,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(488,7,'2026-09-21','2026-09-21 11:48:00','0000-00-00 00:00:00',1,8.69,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(489,6,'2026-09-21','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.14,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(490,20,'2026-09-21','2026-09-21 15:21:00','0000-00-00 00:00:00',1,9.64,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(491,19,'2026-09-21','2026-09-21 14:18:00','0000-00-00 00:00:00',1,8.40,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(492,5,'2026-09-21','2026-09-21 22:04:00','0000-00-00 00:00:00',1,9.45,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(493,18,'2026-09-21','0000-00-00 00:00:00','2026-09-21 21:47:00',1,8.73,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(494,17,'2026-09-21','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.71,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(495,4,'2026-09-21','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.46,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(496,16,'2026-09-21','0000-00-00 00:00:00','2026-09-21 18:20:00',1,9.39,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(497,15,'2026-09-21','2026-09-21 23:39:00','0000-00-00 00:00:00',1,9.12,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(498,3,'2026-09-21','2026-09-21 18:02:00','0000-00-00 00:00:00',1,9.52,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(499,2,'2026-09-21','2026-09-21 13:42:00','0000-00-00 00:00:00',1,8.52,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(500,1,'2026-09-21','0000-00-00 00:00:00','2026-09-21 22:08:00',1,9.38,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(501,14,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(502,9,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(503,13,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(504,12,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(505,8,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(506,11,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(507,10,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(508,7,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(509,6,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(510,20,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(511,19,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(512,5,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(513,18,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(514,17,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(515,4,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(516,16,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(517,15,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(518,3,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(519,2,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(520,1,'2026-09-20',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(521,14,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(522,9,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(523,13,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(524,12,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(525,8,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(526,11,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(527,10,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(528,7,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(529,6,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(530,20,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(531,19,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(532,5,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(533,18,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(534,17,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(535,4,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(536,16,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(537,15,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(538,3,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(539,2,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(540,1,'2026-09-19',NULL,NULL,4,0.00,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(541,14,'2026-09-18','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.59,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(542,9,'2026-09-18','2026-09-18 16:49:00','0000-00-00 00:00:00',1,9.79,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(543,13,'2026-09-18','2026-09-18 20:03:00','0000-00-00 00:00:00',1,9.77,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(544,12,'2026-09-18','2026-09-18 14:29:00','0000-00-00 00:00:00',1,8.38,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(545,8,'2026-09-18','2026-09-18 22:25:00','0000-00-00 00:00:00',1,8.83,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(546,11,'2026-09-18','2026-09-18 14:13:00','0000-00-00 00:00:00',1,9.81,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(547,10,'2026-09-18','2026-09-18 17:39:00','2026-09-18 19:54:00',1,9.88,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(548,7,'2026-09-18','2026-09-18 12:29:00','0000-00-00 00:00:00',1,9.60,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(549,6,'2026-09-18','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.96,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(550,20,'2026-09-18','2026-09-18 16:50:00','0000-00-00 00:00:00',1,8.41,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(551,19,'2026-09-18','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.60,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(552,5,'2026-09-18','2026-09-18 17:52:00','2026-09-18 23:49:00',1,8.46,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(553,18,'2026-09-18','2026-09-18 23:38:00','2026-09-18 22:59:00',1,8.68,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(554,17,'2026-09-18','2026-09-18 13:03:00','0000-00-00 00:00:00',1,8.21,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(555,4,'2026-09-18','2026-09-18 16:02:00','0000-00-00 00:00:00',1,8.52,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(556,16,'2026-09-18','2026-09-18 17:07:00','0000-00-00 00:00:00',1,8.22,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(557,15,'2026-09-18','2026-09-18 15:34:00','0000-00-00 00:00:00',1,9.91,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(558,3,'2026-09-18','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.32,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(559,2,'2026-09-18','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.24,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(560,1,'2026-09-18','2026-09-18 23:04:00','0000-00-00 00:00:00',1,8.67,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(561,14,'2026-09-17','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.60,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(562,9,'2026-09-17','2026-09-17 22:35:00','2026-09-17 18:20:00',1,9.12,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(563,13,'2026-09-17','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.24,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(564,12,'2026-09-17','2026-09-17 17:30:00','2026-09-17 21:08:00',1,8.24,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(565,8,'2026-09-17','2026-09-17 17:45:00','0000-00-00 00:00:00',1,8.44,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(566,11,'2026-09-17','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.70,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(567,10,'2026-09-17','2026-09-17 10:07:00','0000-00-00 00:00:00',1,8.77,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(568,7,'2026-09-17','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.69,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(569,6,'2026-09-17','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.21,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(570,20,'2026-09-17','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.20,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(571,19,'2026-09-17','2026-09-17 11:37:00','0000-00-00 00:00:00',1,8.63,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(572,5,'2026-09-17','2026-09-17 18:29:00','0000-00-00 00:00:00',2,9.26,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(573,18,'2026-09-17','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.92,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(574,17,'2026-09-17','2026-09-17 22:17:00','0000-00-00 00:00:00',1,8.86,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(575,4,'2026-09-17','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.74,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(576,16,'2026-09-17','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.08,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(577,15,'2026-09-17','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.07,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(578,3,'2026-09-17','2026-09-17 17:12:00','0000-00-00 00:00:00',3,9.13,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(579,2,'2026-09-17','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.01,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(580,1,'2026-09-17','2026-09-17 15:42:00','0000-00-00 00:00:00',3,9.15,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(581,14,'2026-09-16','2026-09-16 12:17:00','0000-00-00 00:00:00',1,8.34,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(582,9,'2026-09-16','2026-09-16 17:35:00','0000-00-00 00:00:00',1,9.82,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(583,13,'2026-09-16','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.16,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(584,12,'2026-09-16','2026-09-16 19:19:00','0000-00-00 00:00:00',3,8.37,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(585,8,'2026-09-16','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.16,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(586,11,'2026-09-16','2026-09-16 22:47:00','0000-00-00 00:00:00',1,8.84,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(587,10,'2026-09-16','0000-00-00 00:00:00','0000-00-00 00:00:00',3,8.67,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(588,7,'2026-09-16','0000-00-00 00:00:00','0000-00-00 00:00:00',2,8.86,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(589,6,'2026-09-16','2026-09-16 19:22:00','0000-00-00 00:00:00',1,8.40,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(590,20,'2026-09-16','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.56,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(591,19,'2026-09-16','0000-00-00 00:00:00','0000-00-00 00:00:00',2,9.60,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(592,5,'2026-09-16','0000-00-00 00:00:00','0000-00-00 00:00:00',1,8.23,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(593,18,'2026-09-16','0000-00-00 00:00:00','2026-09-16 18:43:00',1,9.01,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(594,17,'2026-09-16','2026-09-16 11:48:00','0000-00-00 00:00:00',2,8.51,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(595,4,'2026-09-16','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.88,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(596,16,'2026-09-16','2026-09-16 10:43:00','0000-00-00 00:00:00',1,8.63,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(597,15,'2026-09-16','2026-09-16 19:07:00','0000-00-00 00:00:00',1,9.88,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(598,3,'2026-09-16','2026-09-16 20:34:00','0000-00-00 00:00:00',1,8.19,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(599,2,'2026-09-16','2026-09-16 20:47:00','0000-00-00 00:00:00',1,9.85,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(600,1,'2026-09-16','0000-00-00 00:00:00','0000-00-00 00:00:00',1,9.89,'',0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(601,3,'2026-10-26','2026-10-26 00:59:59',NULL,0,0.00,NULL,0,'2026-10-26 00:59:59','2026-10-26 00:59:59'),(602,4,'2026-10-26','2026-10-26 12:25:56',NULL,1,0.00,NULL,0,'2026-10-26 12:25:56','2026-10-26 12:25:56');
/*!40000 ALTER TABLE `sys_attendance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_department`
--

DROP TABLE IF EXISTS `sys_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `dept_name` varchar(100) NOT NULL COMMENT '部门名称',
  `dept_code` varchar(50) NOT NULL COMMENT '部门编码',
  `parent_id` bigint DEFAULT '0' COMMENT '上级部门ID',
  `leader` varchar(50) DEFAULT NULL COMMENT '部门负责人',
  `phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` tinyint DEFAULT '1' COMMENT '状态 0停用 1正常',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_department`
--

LOCK TABLES `sys_department` WRITE;
/*!40000 ALTER TABLE `sys_department` DISABLE KEYS */;
INSERT INTO `sys_department` VALUES (1,'总公司','HQ',0,'张总','13800000001','ceo@company.com',1,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(2,'技术部','TECH',1,'李技术','13800000002','tech@company.com',2,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(3,'市场部','MARKET',1,'王市场','13800000003','market@company.com',3,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(4,'人事部','HR',1,'赵人事','13800000004','hr@company.com',4,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(5,'财务部','FINANCE',1,'钱财务','13800000005','finance@company.com',5,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(6,'行政部','ADMIN',1,'孙行政','13800000006','admin@company.com',6,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(7,'研发一组','TECH-1',2,'周组长','13800000007','tech1@company.com',1,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(8,'研发二组','TECH-2',2,'吴组长','13800000008','tech2@company.com',2,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(9,'运维组','TECH-3',2,'郑组长','13800000009','tech3@company.com',3,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(10,'CS-Test2','CS-TEST2',2,'tester2','13800000000','cs@test.com',9,1,1,'2026-10-26 12:12:41','2026-10-26 12:12:40');
/*!40000 ALTER TABLE `sys_department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_emp_training`
--

DROP TABLE IF EXISTS `sys_emp_training`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_emp_training` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `emp_id` bigint NOT NULL COMMENT '员工ID',
  `training_id` bigint NOT NULL COMMENT '培训ID',
  `score` decimal(5,2) DEFAULT NULL COMMENT '培训成绩',
  `status` tinyint DEFAULT '1' COMMENT '状态 1报名 2完成 3未通过',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_training` (`emp_id`,`training_id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_training_id` (`training_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工培训关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_emp_training`
--

LOCK TABLES `sys_emp_training` WRITE;
/*!40000 ALTER TABLE `sys_emp_training` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_emp_training` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_employee`
--

DROP TABLE IF EXISTS `sys_employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `emp_no` varchar(50) NOT NULL COMMENT '工号',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `gender` tinyint DEFAULT NULL COMMENT '性别 0女 1男',
  `birthday` date DEFAULT NULL COMMENT '出生日期',
  `id_card` varchar(18) DEFAULT NULL COMMENT '身份证号',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `address` varchar(255) DEFAULT NULL COMMENT '住址',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `dept_id` bigint DEFAULT NULL COMMENT '部门ID',
  `position` varchar(50) DEFAULT NULL COMMENT '职位',
  `job_level` varchar(50) DEFAULT NULL COMMENT '职级',
  `entry_date` date DEFAULT NULL COMMENT '入职日期',
  `leave_date` date DEFAULT NULL COMMENT '离职日期',
  `work_years` int DEFAULT '0' COMMENT '工龄(年)',
  `education` varchar(20) DEFAULT NULL COMMENT '学历',
  `major` varchar(50) DEFAULT NULL COMMENT '专业',
  `salary` decimal(10,2) DEFAULT '0.00' COMMENT '基础薪资',
  `status` tinyint DEFAULT '1' COMMENT '状态 0离职 1在职 2试用期 3休假',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `country` varchar(50) DEFAULT '中国' COMMENT '所属国家',
  `work_location` varchar(100) DEFAULT NULL COMMENT '工作地点',
  `employee_type` tinyint DEFAULT '1' COMMENT '员工类型 1国内 2境外',
  `passport_no` varchar(20) DEFAULT NULL COMMENT '护照号',
  `visa_type` varchar(50) DEFAULT NULL COMMENT '签证类型',
  `visa_expire_date` date DEFAULT NULL COMMENT '签证到期日',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_no` (`emp_no`),
  KEY `idx_dept_id` (`dept_id`),
  KEY `idx_emp_no` (`emp_no`)
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_employee`
--

LOCK TABLES `sys_employee` WRITE;
/*!40000 ALTER TABLE `sys_employee` DISABLE KEYS */;
INSERT INTO `sys_employee` VALUES (1,'EMP001','张总',1,'1980-01-15','110101198001150001','13800000001','ceo@company.com','北京市朝阳区',NULL,1,'总经理','P10','2010-01-01',NULL,14,'博士','工商管理',50000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(2,'EMP002','李技术',1,'1985-03-20','110101198503200002','13800000002','tech@company.com','北京市海淀区',NULL,2,'技术总监','P9','2012-03-15',NULL,12,'硕士','计算机科学',40000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(3,'EMP003','王市场',0,'1986-05-10','110101198605100003','13800000003','market@company.com','北京市西城区',NULL,3,'市场总监','P9','2013-06-01',NULL,11,'硕士','市场营销',38000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(4,'EMP004','赵人事',0,'1988-08-25','110101198808250004','13800000004','hr@company.com','北京市东城区',NULL,4,'人事总监','P8','2014-02-10',NULL,10,'本科','人力资源',32000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(5,'EMP005','钱财务',0,'1987-11-30','110101198711300005','13800000005','finance@company.com','北京市丰台区',NULL,5,'财务总监','P8','2013-09-20',NULL,11,'本科','会计学',35000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(6,'EMP006','孙行政',1,'1990-02-14','110101199002140006','13800000006','admin@company.com','北京市石景山区',NULL,6,'行政主管','P7','2015-04-01',NULL,9,'本科','行政管理',25000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(7,'EMP007','周组长',1,'1990-07-08','110101199007080007','13800000007','tech1@company.com','北京市通州区',NULL,7,'研发组长','P7','2015-08-15',NULL,9,'硕士','软件工程',28000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(8,'EMP008','吴组长',0,'1991-04-22','110101199104220008','13800000008','tech2@company.com','北京市大兴区',NULL,8,'研发组长','P7','2016-01-10',NULL,8,'本科','计算机科学',26000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(9,'EMP009','郑组长',1,'1989-12-05','110101198912050009','13800000009','tech3@company.com','北京市昌平区',NULL,9,'运维组长','P6','2014-11-20',NULL,10,'本科','网络工程',24000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(10,'EMP010','陈开发',1,'1993-06-18','110101199306180010','13800000010','chen@company.com','北京市朝阳区',NULL,7,'高级开发工程师','P6','2017-03-01',NULL,7,'本科','软件工程',22000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(11,'EMP011','林开发',0,'1994-09-25','110101199409250011','13800000011','lin@company.com','北京市海淀区',NULL,7,'中级开发工程师','P5','2018-05-10',NULL,6,'本科','计算机科学',18000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(12,'EMP012','黄开发',1,'1995-11-12','110101199511120012','13800000012','huang@company.com','北京市西城区',NULL,8,'高级开发工程师','P6','2017-07-20',NULL,7,'硕士','软件工程',23000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(13,'EMP013','徐测试',0,'1996-03-08','110101199603080013','13800000013','xu@company.com','北京市东城区',NULL,8,'测试工程师','P5','2018-09-01',NULL,6,'本科','软件工程',16000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(14,'EMP014','朱运维',1,'1994-08-15','110101199408150014','13800000014','zhu@company.com','北京市丰台区',NULL,9,'运维工程师','P5','2017-11-10',NULL,7,'本科','网络工程',17000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(15,'EMP015','马销售',1,'1992-01-20','110101199201200015','13800000015','ma@company.com','北京市朝阳区',NULL,3,'销售经理','P6','2016-04-15',NULL,8,'本科','市场营销',20000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(16,'EMP016','何销售',0,'1995-07-30','110101199507300016','13800000016','he@company.com','北京市海淀区',NULL,3,'销售专员','P4','2019-02-20',NULL,5,'本科','市场营销',12000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(17,'EMP017','高招聘',0,'1993-12-10','110101199312100017','13800000017','gao@company.com','北京市西城区',NULL,4,'招聘专员','P5','2018-06-01',NULL,6,'本科','人力资源',14000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(18,'EMP018','梁薪酬',1,'1991-05-25','110101199105250018','13800000018','liang@company.com','北京市东城区',NULL,4,'薪酬专员','P5','2016-08-10',NULL,8,'本科','人力资源',15000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(19,'EMP019','宋会计',0,'1990-10-05','110101199010050019','13800000019','song@company.com','北京市丰台区',NULL,5,'会计','P5','2015-12-01',NULL,9,'本科','会计学',16000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(20,'EMP020','唐出纳',0,'1996-02-28','110101199602280020','13800000020','tang@company.com','北京市石景山区',NULL,5,'出纳','P4','2019-08-15',NULL,5,'本科','金融学',11000.00,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19','中国',NULL,1,NULL,NULL,NULL),(21,'US001','Mike Johnson',1,NULL,NULL,'+1-310-555-1001',NULL,NULL,NULL,2,'海外仓主管',NULL,'2023-03-15',NULL,0,NULL,NULL,6500.00,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20','美国','洛杉矶海外仓',2,'P12345678','L1工作签证','2027-03-14'),(22,'US002','Sarah Smith',0,NULL,NULL,'+1-201-555-1002',NULL,NULL,NULL,2,'海外仓主管',NULL,'2023-06-01',NULL,0,NULL,NULL,6200.00,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20','美国','纽约海外仓',2,'P87654321','L1工作签证','2026-12-31'),(23,'US003','Tom Wilson',1,NULL,NULL,'+1-310-555-1003',NULL,NULL,NULL,6,'美国销售代表',NULL,'2024-01-10',NULL,0,NULL,NULL,5500.00,1,0,'2026-10-15 11:31:20','2026-10-15 11:31:20','美国','洛杉矶办事处',2,'P11223344','B1商务签证','2026-06-30'),(24,'US004','Lisa Chen',0,NULL,NULL,'+1-646-555-1004',NULL,NULL,NULL,6,'美国客户经理',NULL,'2024-08-20','2026-10-26',0,NULL,NULL,5800.00,1,0,'2026-10-15 11:31:20','2026-10-26 12:36:03','美国','纽约办事处',2,'P55667788','H1B工作签证','2027-08-19'),(25,'CS-900','CS-TestEmp2',1,NULL,NULL,'13900000001',NULL,NULL,NULL,2,'QA-2',NULL,'2026-10-26',NULL,0,NULL,NULL,0.00,1,1,'2026-10-26 12:25:56','2026-10-26 12:25:56','中国',NULL,1,NULL,NULL,NULL);
/*!40000 ALTER TABLE `sys_employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_performance`
--

DROP TABLE IF EXISTS `sys_performance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_performance` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '绩效ID',
  `emp_id` bigint NOT NULL COMMENT '员工ID',
  `period` varchar(7) NOT NULL COMMENT '考核周期 yyyy-MM',
  `work_quality` decimal(5,2) DEFAULT '0.00' COMMENT '工作质量(0-100)',
  `work_efficiency` decimal(5,2) DEFAULT '0.00' COMMENT '工作效率(0-100)',
  `work_attitude` decimal(5,2) DEFAULT '0.00' COMMENT '工作态度(0-100)',
  `teamwork` decimal(5,2) DEFAULT '0.00' COMMENT '团队协作(0-100)',
  `innovation` decimal(5,2) DEFAULT '0.00' COMMENT '创新能力(0-100)',
  `total_score` decimal(5,2) DEFAULT '0.00' COMMENT '综合得分',
  `grade` varchar(10) DEFAULT NULL COMMENT '等级 S/A/B/C/D',
  `evaluator` varchar(50) DEFAULT NULL COMMENT '考核人',
  `comment` varchar(500) DEFAULT NULL COMMENT '评语',
  `status` tinyint DEFAULT '0' COMMENT '状态 0草稿 1已提交 2已确认',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_period` (`period`),
  KEY `idx_emp_period` (`emp_id`,`period`)
) ENGINE=InnoDB AUTO_INCREMENT=129 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='绩效表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_performance`
--

LOCK TABLES `sys_performance` WRITE;
/*!40000 ALTER TABLE `sys_performance` DISABLE KEYS */;
INSERT INTO `sys_performance` VALUES (1,1,'2026-05',95.83,82.62,87.98,80.05,64.10,83.83,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(2,1,'2026-06',87.41,86.53,75.34,82.47,61.29,80.12,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(3,1,'2026-07',98.14,87.36,76.97,89.78,62.08,84.55,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(4,1,'2026-08',79.57,82.51,78.21,81.71,79.84,80.39,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(5,1,'2026-09',89.88,88.40,76.96,86.57,78.44,84.71,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(6,1,'2026-10',99.31,79.12,89.74,71.07,74.33,84.37,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(7,2,'2026-05',98.19,84.13,88.38,77.87,84.76,87.65,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(8,2,'2026-06',92.49,88.78,97.02,85.76,94.47,91.76,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(9,2,'2026-07',80.45,93.63,97.32,73.04,88.97,87.29,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(10,2,'2026-08',95.03,90.66,98.50,89.05,72.42,90.34,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(11,2,'2026-09',96.08,78.46,95.06,74.97,74.75,85.11,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(12,2,'2026-10',88.30,93.56,77.43,73.92,72.63,82.93,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-26 12:28:43'),(13,3,'2026-05',82.36,99.35,91.39,80.30,86.24,88.69,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(14,3,'2026-06',91.54,80.22,88.75,91.84,94.64,88.66,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(15,3,'2026-07',92.94,95.63,99.45,79.80,84.50,91.68,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(16,3,'2026-08',85.59,84.97,98.37,75.34,63.00,83.07,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(17,3,'2026-09',96.83,76.44,84.75,79.18,72.63,83.04,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(18,3,'2026-10',96.56,80.35,76.72,79.28,71.92,82.25,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-26 12:28:43'),(19,15,'2026-05',93.23,95.50,98.18,72.59,82.72,90.12,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(20,15,'2026-06',99.63,99.71,99.73,99.22,91.61,98.41,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(21,15,'2026-07',87.81,77.75,87.79,93.46,73.14,83.94,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(22,15,'2026-08',85.92,85.83,76.15,89.42,63.43,81.10,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(23,15,'2026-09',86.45,83.44,89.89,88.99,73.24,84.79,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(24,15,'2026-10',99.79,94.88,79.20,80.58,69.07,86.96,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(25,16,'2026-05',77.11,82.26,83.28,82.92,65.61,78.78,'B','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(26,16,'2026-06',85.26,71.90,94.75,92.83,75.20,83.44,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(27,16,'2026-07',96.64,74.14,75.65,91.46,77.48,83.17,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(28,16,'2026-08',80.52,77.67,80.65,80.88,64.76,77.52,'B','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(29,16,'2026-09',87.74,86.49,99.33,76.51,65.80,84.77,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(30,16,'2026-10',75.32,81.71,85.47,97.72,72.71,81.92,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(31,4,'2026-05',71.31,73.85,87.79,75.16,71.42,75.84,'B','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(32,4,'2026-06',73.44,87.86,90.79,81.20,93.97,84.76,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(33,4,'2026-07',92.02,92.72,89.62,89.58,77.82,89.22,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(34,4,'2026-08',87.61,82.23,81.92,74.87,94.31,84.22,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(35,4,'2026-09',82.45,74.04,85.68,91.97,73.30,81.05,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(36,4,'2026-10',91.08,81.19,93.95,90.10,62.69,84.78,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(37,17,'2026-05',81.21,89.14,76.71,82.84,92.74,84.27,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(38,17,'2026-06',81.77,74.69,90.12,86.64,93.57,84.17,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(39,17,'2026-07',73.96,93.48,87.93,77.14,82.35,83.37,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(40,17,'2026-08',84.37,84.36,98.90,80.35,89.98,87.51,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(41,17,'2026-09',77.45,90.15,90.32,71.49,74.36,81.84,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(42,17,'2026-10',97.07,78.41,92.36,88.96,62.65,85.08,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(43,18,'2026-05',84.50,75.70,87.47,97.74,64.46,81.87,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(44,18,'2026-06',95.87,97.89,76.55,85.61,74.55,87.77,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(45,18,'2026-07',85.51,80.16,78.56,90.88,61.86,80.04,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(46,18,'2026-08',75.32,91.83,77.67,80.49,75.00,80.65,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(47,18,'2026-09',72.81,75.49,90.85,88.65,67.20,78.62,'B','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(48,18,'2026-10',74.90,76.00,87.76,98.53,67.85,80.23,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(49,5,'2026-05',78.04,90.03,88.36,90.06,85.91,86.09,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(50,5,'2026-06',90.87,77.72,79.99,76.76,78.49,81.43,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(51,5,'2026-07',98.98,77.34,83.15,96.86,77.46,86.86,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(52,5,'2026-08',94.25,86.34,82.45,95.71,73.68,87.05,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(53,5,'2026-09',81.48,92.23,88.92,86.81,64.63,83.93,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(54,5,'2026-10',99.41,85.14,89.54,81.85,68.02,86.53,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(55,19,'2026-05',98.85,73.64,93.02,77.22,61.43,82.52,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(56,19,'2026-06',84.46,78.64,99.85,73.18,79.19,83.60,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(57,19,'2026-07',82.69,84.10,77.04,99.95,86.12,85.02,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(58,19,'2026-08',92.10,83.33,75.27,91.67,80.28,84.70,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(59,19,'2026-09',91.87,97.23,83.79,71.04,64.15,84.31,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(60,19,'2026-10',84.68,72.71,99.58,89.36,69.72,83.13,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(61,20,'2026-05',83.57,82.87,94.68,89.46,90.88,87.60,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(62,20,'2026-06',83.96,90.40,75.09,99.33,90.73,87.12,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(63,20,'2026-07',83.71,89.53,97.09,83.97,83.70,87.88,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(64,20,'2026-08',99.68,97.43,90.11,78.39,80.44,91.12,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(65,20,'2026-09',72.43,89.60,75.59,74.73,85.11,79.60,'B','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(66,20,'2026-10',73.41,82.51,93.61,84.11,64.16,79.94,'B','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(67,6,'2026-05',75.49,86.73,81.01,85.86,92.29,83.48,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(68,6,'2026-06',70.79,80.93,93.57,88.63,90.67,83.54,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(69,6,'2026-07',85.54,98.86,81.41,81.84,67.17,84.73,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(70,6,'2026-08',95.19,87.53,85.07,77.82,63.34,83.87,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(71,6,'2026-09',90.84,75.61,96.28,90.86,92.28,88.34,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(72,6,'2026-10',85.79,95.93,93.57,73.64,73.22,86.17,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(73,7,'2026-05',85.76,84.79,97.23,99.02,65.90,86.82,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(74,7,'2026-06',98.22,75.93,79.17,77.22,84.62,83.65,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(75,7,'2026-07',93.85,95.93,98.42,72.71,82.44,90.40,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(76,7,'2026-08',98.07,92.62,99.09,86.69,91.14,94.17,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(77,7,'2026-09',93.42,77.02,95.71,83.20,84.99,86.98,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(78,7,'2026-10',77.53,73.37,95.24,91.29,64.19,80.10,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(79,10,'2026-05',84.11,99.77,88.77,93.31,68.16,87.94,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(80,10,'2026-06',95.02,84.12,96.28,95.32,83.33,90.84,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(81,10,'2026-07',94.01,70.07,90.28,71.45,74.29,80.94,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(82,10,'2026-08',96.89,77.70,89.87,76.13,68.31,83.29,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(83,10,'2026-09',87.23,74.77,76.79,96.43,66.68,80.32,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(84,10,'2026-10',79.33,99.47,99.48,98.48,88.30,92.61,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(85,11,'2026-05',75.84,86.43,78.86,73.85,66.24,77.35,'B','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(86,11,'2026-06',85.20,99.96,86.84,81.13,75.21,87.11,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(87,11,'2026-07',71.81,99.93,95.17,71.26,87.62,85.80,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(88,11,'2026-08',94.59,91.92,79.87,93.43,71.24,87.30,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(89,11,'2026-09',77.89,80.56,99.27,93.96,62.80,82.98,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(90,11,'2026-10',70.12,93.39,97.17,72.83,88.40,84.50,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(91,8,'2026-05',93.25,83.19,96.84,71.46,81.79,86.47,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(92,8,'2026-06',99.03,99.09,98.64,94.56,69.01,93.79,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(93,8,'2026-07',94.92,81.44,85.36,97.87,74.02,86.95,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(94,8,'2026-08',76.50,96.46,93.98,74.52,76.66,84.71,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(95,8,'2026-09',97.81,76.22,81.40,89.73,78.24,84.98,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(96,8,'2026-10',88.95,87.86,77.03,88.59,89.95,86.39,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(97,12,'2026-05',82.59,85.91,84.82,81.18,84.00,83.87,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(98,12,'2026-06',79.32,84.88,88.70,77.55,81.49,82.65,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(99,12,'2026-07',79.44,91.97,92.94,81.71,87.98,86.89,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(100,12,'2026-08',94.78,91.94,79.46,90.94,93.45,90.23,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(101,12,'2026-09',90.51,86.56,92.73,96.67,71.13,87.98,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(102,12,'2026-10',97.68,89.78,88.21,89.92,85.72,90.85,'S','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(103,13,'2026-05',90.48,76.27,99.91,80.67,87.60,86.91,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(104,13,'2026-06',96.30,70.50,86.35,76.57,85.69,83.31,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(105,13,'2026-07',70.38,95.86,81.78,93.12,61.36,81.09,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(106,13,'2026-08',96.49,78.94,96.03,79.38,61.35,84.17,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(107,13,'2026-09',77.66,74.83,75.96,91.27,75.07,78.27,'B','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(108,13,'2026-10',70.77,95.09,77.62,70.43,86.50,80.53,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(109,9,'2026-05',92.28,83.27,99.59,87.75,60.25,86.01,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(110,9,'2026-06',77.81,78.42,90.55,78.05,76.67,80.38,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(111,9,'2026-07',87.27,83.50,88.06,77.84,85.91,84.87,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(112,9,'2026-08',97.52,80.99,76.98,78.90,68.62,82.15,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(113,9,'2026-09',80.23,99.00,95.25,74.51,71.25,85.72,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(114,9,'2026-10',74.68,94.47,90.27,88.21,67.07,83.63,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(115,14,'2026-05',75.67,80.16,78.15,88.45,84.36,80.51,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(116,14,'2026-06',89.08,72.76,88.78,84.40,86.16,83.80,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(117,14,'2026-07',78.88,77.16,82.62,94.26,64.48,79.35,'B','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(118,14,'2026-08',76.45,90.71,95.17,98.88,73.75,86.72,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(119,14,'2026-09',72.28,76.04,94.49,78.79,64.41,77.46,'B','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(120,14,'2026-10',92.56,81.44,91.26,73.25,80.68,84.84,'A','系统评估','本月工作表现',2,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(125,999,'2026-10',80.00,80.00,80.00,80.00,80.00,400.00,'A','admin',NULL,1,1,'2026-10-26 12:29:43','2026-10-26 12:29:42'),(126,999,'2026-10',80.00,80.00,80.00,80.00,80.00,400.00,'A','admin',NULL,1,1,'2026-10-26 12:29:43','2026-10-26 12:29:42'),(127,999,'2026-10',80.00,80.00,80.00,80.00,80.00,400.00,'A','admin',NULL,1,1,'2026-10-26 12:30:38','2026-10-26 12:30:37'),(128,999,'2026-10',80.00,80.00,80.00,80.00,80.00,400.00,'A','admin',NULL,1,1,'2026-10-26 12:30:38','2026-10-26 12:30:37');
/*!40000 ALTER TABLE `sys_performance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_recruitment`
--

DROP TABLE IF EXISTS `sys_recruitment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_recruitment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招聘ID',
  `position` varchar(100) NOT NULL COMMENT '招聘职位',
  `dept_id` bigint DEFAULT NULL COMMENT '所属部门',
  `headcount` int DEFAULT '1' COMMENT '招聘人数',
  `salary_range` varchar(50) DEFAULT NULL COMMENT '薪资范围',
  `education` varchar(20) DEFAULT NULL COMMENT '学历要求',
  `experience` varchar(50) DEFAULT NULL COMMENT '经验要求',
  `description` text COMMENT '职位描述',
  `requirement` text COMMENT '任职要求',
  `status` tinyint DEFAULT '1' COMMENT '状态 0关闭 1招聘中 2已完成',
  `publisher` varchar(50) DEFAULT NULL COMMENT '发布人',
  `publish_time` datetime DEFAULT NULL COMMENT '发布时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dept_id` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='招聘表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_recruitment`
--

LOCK TABLES `sys_recruitment` WRITE;
/*!40000 ALTER TABLE `sys_recruitment` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_recruitment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_salary`
--

DROP TABLE IF EXISTS `sys_salary`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_salary` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '薪资ID',
  `emp_id` bigint NOT NULL COMMENT '员工ID',
  `salary_month` varchar(7) NOT NULL COMMENT '薪资月份 yyyy-MM',
  `base_salary` decimal(10,2) DEFAULT '0.00' COMMENT '基本工资',
  `performance_bonus` decimal(10,2) DEFAULT '0.00' COMMENT '绩效奖金',
  `overtime_pay` decimal(10,2) DEFAULT '0.00' COMMENT '加班费',
  `allowance` decimal(10,2) DEFAULT '0.00' COMMENT '补贴',
  `social_security` decimal(10,2) DEFAULT '0.00' COMMENT '社保扣除',
  `housing_fund` decimal(10,2) DEFAULT '0.00' COMMENT '公积金扣除',
  `tax` decimal(10,2) DEFAULT '0.00' COMMENT '个税扣除',
  `other_deduction` decimal(10,2) DEFAULT '0.00' COMMENT '其他扣除',
  `net_salary` decimal(10,2) DEFAULT '0.00' COMMENT '实发工资',
  `status` tinyint DEFAULT '0' COMMENT '状态 0未发放 1已发放',
  `pay_time` datetime DEFAULT NULL COMMENT '发放时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`emp_id`),
  KEY `idx_salary_month` (`salary_month`),
  KEY `idx_emp_month` (`emp_id`,`salary_month`)
) ENGINE=InnoDB AUTO_INCREMENT=129 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪资表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_salary`
--

LOCK TABLES `sys_salary` WRITE;
/*!40000 ALTER TABLE `sys_salary` DISABLE KEYS */;
INSERT INTO `sys_salary` VALUES (1,1,'2026-05',50000.00,14160.39,744.44,500.00,5250.00,6000.00,4500.00,0.00,42250.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(2,1,'2026-06',50000.00,14740.58,636.98,500.00,5250.00,6000.00,4500.00,0.00,42250.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(3,1,'2026-07',50000.00,7627.43,402.77,500.00,5250.00,6000.00,4500.00,0.00,42250.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(4,1,'2026-08',50000.00,7255.97,919.69,500.00,5250.00,6000.00,4500.00,0.00,42250.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(5,1,'2026-09',50000.00,14216.65,849.25,500.00,5250.00,6000.00,4500.00,0.00,42250.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(6,1,'2026-10',50000.00,9812.65,858.57,500.00,5250.00,6000.00,4500.00,0.00,42250.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(7,2,'2026-05',40000.00,10792.38,669.53,500.00,4200.00,4800.00,3500.00,0.00,34000.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(8,2,'2026-06',40000.00,10404.20,994.02,500.00,4200.00,4800.00,3500.00,0.00,34000.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(9,2,'2026-07',40000.00,8548.38,860.66,500.00,4200.00,4800.00,3500.00,0.00,34000.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(10,2,'2026-08',40000.00,8781.44,406.41,500.00,4200.00,4800.00,3500.00,0.00,34000.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(11,2,'2026-09',40000.00,5911.95,975.75,500.00,4200.00,4800.00,3500.00,0.00,34000.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(12,2,'2026-10',40000.00,5294.03,881.53,500.00,4200.00,4800.00,3500.00,0.00,34000.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-26 12:28:43'),(13,3,'2026-05',38000.00,10810.07,967.30,500.00,3990.00,4560.00,3300.00,0.00,32350.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(14,3,'2026-06',38000.00,4327.31,445.01,500.00,3990.00,4560.00,3300.00,0.00,32350.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(15,3,'2026-07',38000.00,3928.27,749.37,500.00,3990.00,4560.00,3300.00,0.00,32350.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(16,3,'2026-08',38000.00,9091.31,233.01,500.00,3990.00,4560.00,3300.00,0.00,32350.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(17,3,'2026-09',38000.00,4380.53,682.89,500.00,3990.00,4560.00,3300.00,0.00,32350.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(18,3,'2026-10',38000.00,5208.27,877.82,500.00,3990.00,4560.00,3300.00,0.00,32350.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-26 12:28:43'),(19,4,'2026-05',32000.00,8532.43,532.51,500.00,3360.00,3840.00,2700.00,0.00,27400.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(20,4,'2026-06',32000.00,4243.09,217.37,500.00,3360.00,3840.00,2700.00,0.00,27400.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(21,4,'2026-07',32000.00,7026.73,337.51,500.00,3360.00,3840.00,2700.00,0.00,27400.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(22,4,'2026-08',32000.00,8920.12,456.32,500.00,3360.00,3840.00,2700.00,0.00,27400.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(23,4,'2026-09',32000.00,7041.78,632.44,500.00,3360.00,3840.00,2700.00,0.00,27400.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(24,4,'2026-10',32000.00,5512.70,909.48,500.00,3360.00,3840.00,2700.00,0.00,27400.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(25,5,'2026-05',35000.00,6743.35,588.23,500.00,3675.00,4200.00,3000.00,0.00,29875.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(26,5,'2026-06',35000.00,7358.04,991.05,500.00,3675.00,4200.00,3000.00,0.00,29875.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(27,5,'2026-07',35000.00,5612.53,535.81,500.00,3675.00,4200.00,3000.00,0.00,29875.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(28,5,'2026-08',35000.00,8915.65,260.90,500.00,3675.00,4200.00,3000.00,0.00,29875.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(29,5,'2026-09',35000.00,10384.64,134.89,500.00,3675.00,4200.00,3000.00,0.00,29875.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(30,5,'2026-10',35000.00,8567.35,214.86,500.00,3675.00,4200.00,3000.00,0.00,29875.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(31,6,'2026-05',25000.00,7012.78,868.21,500.00,2625.00,3000.00,2000.00,0.00,21625.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(32,6,'2026-06',25000.00,5666.93,562.30,500.00,2625.00,3000.00,2000.00,0.00,21625.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(33,6,'2026-07',25000.00,7056.71,869.81,500.00,2625.00,3000.00,2000.00,0.00,21625.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(34,6,'2026-08',25000.00,5575.02,465.60,500.00,2625.00,3000.00,2000.00,0.00,21625.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(35,6,'2026-09',25000.00,4914.99,18.18,500.00,2625.00,3000.00,2000.00,0.00,21625.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(36,6,'2026-10',25000.00,5709.59,155.05,500.00,2625.00,3000.00,2000.00,0.00,21625.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(37,7,'2026-05',28000.00,7557.12,782.28,500.00,2940.00,3360.00,2300.00,0.00,24100.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(38,7,'2026-06',28000.00,4832.47,467.87,500.00,2940.00,3360.00,2300.00,0.00,24100.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(39,7,'2026-07',28000.00,4202.94,849.02,500.00,2940.00,3360.00,2300.00,0.00,24100.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(40,7,'2026-08',28000.00,5563.60,920.46,500.00,2940.00,3360.00,2300.00,0.00,24100.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(41,7,'2026-09',28000.00,3482.00,847.56,500.00,2940.00,3360.00,2300.00,0.00,24100.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(42,7,'2026-10',28000.00,7685.63,819.50,500.00,2940.00,3360.00,2300.00,0.00,24100.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(43,8,'2026-05',26000.00,5096.94,942.41,500.00,2730.00,3120.00,2100.00,0.00,22450.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(44,8,'2026-06',26000.00,4011.91,530.37,500.00,2730.00,3120.00,2100.00,0.00,22450.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(45,8,'2026-07',26000.00,6953.86,595.30,500.00,2730.00,3120.00,2100.00,0.00,22450.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(46,8,'2026-08',26000.00,5016.19,537.37,500.00,2730.00,3120.00,2100.00,0.00,22450.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(47,8,'2026-09',26000.00,4122.93,852.26,500.00,2730.00,3120.00,2100.00,0.00,22450.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(48,8,'2026-10',26000.00,4589.97,356.65,500.00,2730.00,3120.00,2100.00,0.00,22450.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(49,9,'2026-05',24000.00,5449.02,106.10,500.00,2520.00,2880.00,1900.00,0.00,20800.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(50,9,'2026-06',24000.00,5399.29,805.97,500.00,2520.00,2880.00,1900.00,0.00,20800.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(51,9,'2026-07',24000.00,3145.32,358.48,500.00,2520.00,2880.00,1900.00,0.00,20800.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(52,9,'2026-08',24000.00,3967.45,557.34,500.00,2520.00,2880.00,1900.00,0.00,20800.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(53,9,'2026-09',24000.00,6273.73,363.12,500.00,2520.00,2880.00,1900.00,0.00,20800.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(54,9,'2026-10',24000.00,4293.79,883.33,500.00,2520.00,2880.00,1900.00,0.00,20800.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(55,10,'2026-05',22000.00,3225.30,515.13,500.00,2310.00,2640.00,1700.00,0.00,19150.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(56,10,'2026-06',22000.00,6056.92,837.47,500.00,2310.00,2640.00,1700.00,0.00,19150.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(57,10,'2026-07',22000.00,4653.70,275.87,500.00,2310.00,2640.00,1700.00,0.00,19150.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(58,10,'2026-08',22000.00,5308.04,704.25,500.00,2310.00,2640.00,1700.00,0.00,19150.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(59,10,'2026-09',22000.00,3969.39,897.92,500.00,2310.00,2640.00,1700.00,0.00,19150.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(60,10,'2026-10',22000.00,3446.10,722.26,500.00,2310.00,2640.00,1700.00,0.00,19150.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(61,11,'2026-05',18000.00,4542.11,641.70,500.00,1890.00,2160.00,1300.00,0.00,15850.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(62,11,'2026-06',18000.00,5124.32,692.00,500.00,1890.00,2160.00,1300.00,0.00,15850.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(63,11,'2026-07',18000.00,4283.09,372.73,500.00,1890.00,2160.00,1300.00,0.00,15850.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(64,11,'2026-08',18000.00,4659.89,853.88,500.00,1890.00,2160.00,1300.00,0.00,15850.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(65,11,'2026-09',18000.00,4990.09,869.05,500.00,1890.00,2160.00,1300.00,0.00,15850.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(66,11,'2026-10',18000.00,4272.62,827.05,500.00,1890.00,2160.00,1300.00,0.00,15850.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(67,12,'2026-05',23000.00,2643.71,892.45,500.00,2415.00,2760.00,1800.00,0.00,19975.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(68,12,'2026-06',23000.00,3395.31,513.19,500.00,2415.00,2760.00,1800.00,0.00,19975.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(69,12,'2026-07',23000.00,6217.47,718.55,500.00,2415.00,2760.00,1800.00,0.00,19975.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(70,12,'2026-08',23000.00,2474.20,33.71,500.00,2415.00,2760.00,1800.00,0.00,19975.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(71,12,'2026-09',23000.00,2552.65,173.50,500.00,2415.00,2760.00,1800.00,0.00,19975.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(72,12,'2026-10',23000.00,5532.47,993.06,500.00,2415.00,2760.00,1800.00,0.00,19975.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(73,13,'2026-05',16000.00,4342.98,306.72,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(74,13,'2026-06',16000.00,4678.54,890.06,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(75,13,'2026-07',16000.00,3405.38,150.72,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(76,13,'2026-08',16000.00,1795.45,853.21,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(77,13,'2026-09',16000.00,1865.03,854.48,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(78,13,'2026-10',16000.00,1676.58,556.21,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(79,14,'2026-05',17000.00,4111.54,877.74,500.00,1785.00,2040.00,1200.00,0.00,15025.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(80,14,'2026-06',17000.00,2586.93,671.09,500.00,1785.00,2040.00,1200.00,0.00,15025.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(81,14,'2026-07',17000.00,3647.82,851.15,500.00,1785.00,2040.00,1200.00,0.00,15025.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(82,14,'2026-08',17000.00,3526.14,132.05,500.00,1785.00,2040.00,1200.00,0.00,15025.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(83,14,'2026-09',17000.00,1866.34,848.49,500.00,1785.00,2040.00,1200.00,0.00,15025.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(84,14,'2026-10',17000.00,2025.23,932.82,500.00,1785.00,2040.00,1200.00,0.00,15025.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(85,15,'2026-05',20000.00,3508.52,87.20,500.00,2100.00,2400.00,1500.00,0.00,17500.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(86,15,'2026-06',20000.00,3218.34,261.34,500.00,2100.00,2400.00,1500.00,0.00,17500.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(87,15,'2026-07',20000.00,3571.70,180.61,500.00,2100.00,2400.00,1500.00,0.00,17500.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(88,15,'2026-08',20000.00,4897.19,79.65,500.00,2100.00,2400.00,1500.00,0.00,17500.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(89,15,'2026-09',20000.00,2901.43,887.84,500.00,2100.00,2400.00,1500.00,0.00,17500.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(90,15,'2026-10',20000.00,5052.42,152.01,500.00,2100.00,2400.00,1500.00,0.00,17500.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(91,16,'2026-05',12000.00,2329.83,897.76,500.00,1260.00,1440.00,700.00,0.00,10900.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(92,16,'2026-06',12000.00,1383.68,689.38,500.00,1260.00,1440.00,700.00,0.00,10900.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(93,16,'2026-07',12000.00,1721.47,18.27,500.00,1260.00,1440.00,700.00,0.00,10900.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(94,16,'2026-08',12000.00,2254.84,142.77,500.00,1260.00,1440.00,700.00,0.00,10900.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(95,16,'2026-09',12000.00,2148.75,548.25,500.00,1260.00,1440.00,700.00,0.00,10900.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(96,16,'2026-10',12000.00,2532.70,131.72,500.00,1260.00,1440.00,700.00,0.00,10900.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(97,17,'2026-05',14000.00,4179.64,568.48,500.00,1470.00,1680.00,900.00,0.00,12550.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(98,17,'2026-06',14000.00,3819.79,615.62,500.00,1470.00,1680.00,900.00,0.00,12550.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(99,17,'2026-07',14000.00,2759.29,580.45,500.00,1470.00,1680.00,900.00,0.00,12550.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(100,17,'2026-08',14000.00,2648.38,487.91,500.00,1470.00,1680.00,900.00,0.00,12550.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(101,17,'2026-09',14000.00,1685.66,46.36,500.00,1470.00,1680.00,900.00,0.00,12550.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(102,17,'2026-10',14000.00,3992.03,489.55,500.00,1470.00,1680.00,900.00,0.00,12550.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(103,18,'2026-05',15000.00,3511.72,884.22,500.00,1575.00,1800.00,1000.00,0.00,13375.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(104,18,'2026-06',15000.00,2728.12,394.21,500.00,1575.00,1800.00,1000.00,0.00,13375.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(105,18,'2026-07',15000.00,3728.76,531.98,500.00,1575.00,1800.00,1000.00,0.00,13375.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(106,18,'2026-08',15000.00,2793.44,559.78,500.00,1575.00,1800.00,1000.00,0.00,13375.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(107,18,'2026-09',15000.00,3016.45,848.06,500.00,1575.00,1800.00,1000.00,0.00,13375.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(108,18,'2026-10',15000.00,3671.62,75.17,500.00,1575.00,1800.00,1000.00,0.00,13375.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(109,19,'2026-05',16000.00,2253.56,795.67,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(110,19,'2026-06',16000.00,2770.03,441.17,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(111,19,'2026-07',16000.00,1948.62,221.21,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(112,19,'2026-08',16000.00,4093.51,232.48,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(113,19,'2026-09',16000.00,4239.11,426.17,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(114,19,'2026-10',16000.00,3701.43,4.97,500.00,1680.00,1920.00,1100.00,0.00,14200.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(115,20,'2026-05',11000.00,1220.50,258.95,500.00,1155.00,1320.00,600.00,0.00,10075.00,1,'2026-05-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(116,20,'2026-06',11000.00,1386.95,875.31,500.00,1155.00,1320.00,600.00,0.00,10075.00,1,'2026-06-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(117,20,'2026-07',11000.00,3267.58,300.39,500.00,1155.00,1320.00,600.00,0.00,10075.00,1,'2026-07-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(118,20,'2026-08',11000.00,2301.50,829.53,500.00,1155.00,1320.00,600.00,0.00,10075.00,1,'2026-08-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(119,20,'2026-09',11000.00,2220.32,57.59,500.00,1155.00,1320.00,600.00,0.00,10075.00,1,'2026-09-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(120,20,'2026-10',11000.00,2772.49,628.36,500.00,1155.00,1320.00,600.00,0.00,10075.00,1,'2026-10-25 00:00:00',NULL,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(125,999,'2026-10',5000.00,500.00,100.00,100.00,300.00,200.00,50.00,0.00,5150.00,1,NULL,NULL,1,'2026-10-26 12:29:43','2026-10-26 12:29:42'),(126,999,'2026-10',5000.00,500.00,100.00,100.00,300.00,200.00,50.00,0.00,5150.00,1,NULL,NULL,1,'2026-10-26 12:29:43','2026-10-26 12:29:42'),(127,999,'2026-10',5000.00,500.00,100.00,100.00,300.00,200.00,50.00,0.00,5150.00,1,NULL,NULL,1,'2026-10-26 12:30:38','2026-10-26 12:30:37'),(128,999,'2026-10',5000.00,500.00,100.00,100.00,300.00,200.00,50.00,0.00,5150.00,1,NULL,NULL,1,'2026-10-26 12:30:38','2026-10-26 12:30:37');
/*!40000 ALTER TABLE `sys_salary` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_training`
--

DROP TABLE IF EXISTS `sys_training`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_training` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '培训ID',
  `title` varchar(200) NOT NULL COMMENT '培训标题',
  `trainer` varchar(50) DEFAULT NULL COMMENT '讲师',
  `training_type` varchar(20) DEFAULT NULL COMMENT '培训类型',
  `start_date` date DEFAULT NULL COMMENT '开始日期',
  `end_date` date DEFAULT NULL COMMENT '结束日期',
  `location` varchar(100) DEFAULT NULL COMMENT '培训地点',
  `description` text COMMENT '培训内容',
  `max_participants` int DEFAULT '50' COMMENT '最大人数',
  `participants` int DEFAULT '0' COMMENT '已报名人数',
  `status` tinyint DEFAULT '1' COMMENT '状态 0取消 1报名中 2进行中 3已结束',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_training`
--

LOCK TABLES `sys_training` WRITE;
/*!40000 ALTER TABLE `sys_training` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_training` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
  `role` varchar(20) DEFAULT 'user' COMMENT '角色 admin/user',
  `emp_id` bigint DEFAULT NULL COMMENT '关联员工ID',
  `status` tinyint DEFAULT '1' COMMENT '状态 0停用 1正常',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','$2a$10$faFQdajSz9aq1JugNq6gpuUYtxxdHlllZ2o6lvg5XmSbZlzubs/LG','系统管理员',NULL,'admin',1,1,0,'2026-10-15 11:31:19','2026-10-26 11:55:57'),(2,'hr001','e10adc3949ba59abbe56e057f20f883e','赵人事',NULL,'admin',4,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19'),(3,'emp001','$2a$10$0dADr413vm9BYjWSVG3rausHFcrbqJiAZ5MaeVUbNB./nhhBr1oz.','张总',NULL,'user',1,1,0,'2026-10-15 11:31:19','2026-10-26 11:55:57'),(4,'emp002','$2a$10$MjnuA98XyHvFH5F2dUQd8uza4p4aXfik1jF6vPkok9uE9JT93I/oa','李技术',NULL,'user',2,1,0,'2026-10-15 11:31:19','2026-10-26 12:25:56'),(5,'emp003','e10adc3949ba59abbe56e057f20f883e','陈开发',NULL,'user',10,1,0,'2026-10-15 11:31:19','2026-10-15 11:31:19');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-26 19:18:22
