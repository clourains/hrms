-- 修正演示仓库容量与实际库存严重不匹配（原容量 5000/3000/8000/6000，库存远超容量）
-- 调整后各仓使用率约 74%~77%，不再显示「超容」
UPDATE biz_warehouse SET capacity = 200000 WHERE id = 1;
UPDATE biz_warehouse SET capacity = 12000  WHERE id = 2;
UPDATE biz_warehouse SET capacity = 42000  WHERE id = 3;
UPDATE biz_warehouse SET capacity = 20000  WHERE id = 4;
