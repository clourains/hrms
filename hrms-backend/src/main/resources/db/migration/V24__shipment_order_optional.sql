-- 运单批量导入（Excel / 开放接口）允许无关联订单：17TRACK 运单号往往先于订单生成
ALTER TABLE `biz_shipment`
    MODIFY COLUMN `order_id` bigint DEFAULT NULL COMMENT '关联订单ID（运单批量导入可为空）';
