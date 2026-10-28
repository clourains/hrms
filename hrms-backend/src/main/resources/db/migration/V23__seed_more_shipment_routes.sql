-- V23：补充演示发货单与真实轨迹节点
-- 目的：驾驶舱航线增加「上海→纽约」「上海→汉堡」「宁波→洛杉矶」等不同航线，
--       每票都有多段真实节点，方便按货名/客户/订单号搜索后查看航线。

-- 1) 新增发货单
INSERT INTO `biz_shipment`
  (`shipment_no`, `order_id`, `ship_date`, `transport_type`, `carrier`, `tracking_no`,
   `container_no`, `port_from`, `port_to`, `etd`, `eta`, `status`, `deleted`,
   `freight_cost_usd`, `remark`)
VALUES
  ('SH202610-001', 7, '2026-10-05', '海运', '中远海运', 'COSU9876543',
   'CSLU1234567', '上海港', '纽约港', '2026-10-05', '2026-11-12', 2, 0,
   3850.00, '上海→纽约 美东直航'),
  ('SH202610-002', 4, '2026-10-12', '空运', '联邦快递', 'FX888777666',
   NULL, '上海港', '纽约肯尼迪机场', '2026-10-13', '2026-10-15', 4, 0,
   2100.00, '上海→纽约 空运急件'),
  ('SH202610-003', 8, '2026-09-28', '海运', '马士基', 'MSKU7654321',
   'MSKU9081726', '上海港', '汉堡港', '2026-09-30', '2026-11-05', 3, 0,
   4200.00, '上海→汉堡 欧线'),
  ('SH202610-004', 6, '2026-10-15', '海运', '长荣海运', 'EGLV1357924',
   'EGHU3344556', '宁波港', '洛杉矶港', '2026-10-16', '2026-10-30', 3, 0,
   3100.00, '宁波→洛杉矶 美西线');

-- 2) SH202610-001 上海港 → 纽约港（海运，运输中）
INSERT INTO `biz_shipment_track`
  (`shipment_id`, `status`, `node`, `location`, `content`, `track_time`, `operator`, `create_time`)
SELECT s.`id`, t.`status`, t.`node`, t.`location`, t.`content`, t.`track_time`, '系统', NOW()
FROM `biz_shipment` s
JOIN (
  SELECT 1 AS `status`, '已接单' AS `node`, '上海港' AS `location`, '发货单创建，货物进仓待装船' AS `content`, '2026-10-05 09:00:00' AS `track_time`
  UNION ALL SELECT 2, '已发运', '上海港', '中远海运集装箱装船，船舶离港', '2026-10-07 20:30:00'
  UNION ALL SELECT 2, '航行中', '巴拿马城', '船舶通过巴拿马运河，驶向美东', '2026-10-24 08:00:00'
) t ON 1 = 1
WHERE s.`shipment_no` = 'SH202610-001';

-- 3) SH202610-002 上海港 → 纽约肯尼迪机场（空运，已签收）
INSERT INTO `biz_shipment_track`
  (`shipment_id`, `status`, `node`, `location`, `content`, `track_time`, `operator`, `create_time`)
SELECT s.`id`, t.`status`, t.`node`, t.`location`, t.`content`, t.`track_time`, '系统', NOW()
FROM `biz_shipment` s
JOIN (
  SELECT 1 AS `status`, '已接单' AS `node`, '上海' AS `location`, '货代已提货，安排空运舱位' AS `content`, '2026-10-12 10:00:00' AS `track_time`
  UNION ALL SELECT 2, '已发运', '上海', '浦东机场起飞，直飞纽约', '2026-10-13 22:15:00'
  UNION ALL SELECT 3, '清关中', '纽约肯尼迪机场', '货物到港，海关清关', '2026-10-14 08:40:00'
  UNION ALL SELECT 4, '已签收', '纽约', '客户已签收，回单已上传', '2026-10-15 14:20:00'
) t ON 1 = 1
WHERE s.`shipment_no` = 'SH202610-002';

-- 4) SH202610-003 上海港 → 汉堡港（海运，清关中）
INSERT INTO `biz_shipment_track`
  (`shipment_id`, `status`, `node`, `location`, `content`, `track_time`, `operator`, `create_time`)
SELECT s.`id`, t.`status`, t.`node`, t.`location`, t.`content`, t.`track_time`, '系统', NOW()
FROM `biz_shipment` s
JOIN (
  SELECT 1 AS `status`, '已接单' AS `node`, '上海港' AS `location`, '发货单创建，货物进仓待装船' AS `content`, '2026-09-28 09:30:00' AS `track_time`
  UNION ALL SELECT 2, '已发运', '上海港', '马士基集装箱装船，船舶离港', '2026-09-30 21:00:00'
  UNION ALL SELECT 2, '中转', '新加坡', '新加坡港中转，换装母船', '2026-10-08 06:00:00'
  UNION ALL SELECT 3, '清关中', '汉堡港', '抵达汉堡港，等待清关', '2026-10-26 09:20:00'
) t ON 1 = 1
WHERE s.`shipment_no` = 'SH202610-003';

-- 5) SH202610-004 宁波港 → 洛杉矶港（海运，清关中）
INSERT INTO `biz_shipment_track`
  (`shipment_id`, `status`, `node`, `location`, `content`, `track_time`, `operator`, `create_time`)
SELECT s.`id`, t.`status`, t.`node`, t.`location`, t.`content`, t.`track_time`, '系统', NOW()
FROM `biz_shipment` s
JOIN (
  SELECT 1 AS `status`, '已接单' AS `node`, '宁波港' AS `location`, '发货单创建，货物进仓待装船' AS `content`, '2026-10-15 10:00:00' AS `track_time`
  UNION ALL SELECT 2, '已发运', '宁波港', '长荣海运集装箱装船，船舶离港', '2026-10-16 19:40:00'
  UNION ALL SELECT 3, '清关中', '洛杉矶港', '抵达洛杉矶港，等待清关', '2026-10-28 07:30:00'
) t ON 1 = 1
WHERE s.`shipment_no` = 'SH202610-004';
