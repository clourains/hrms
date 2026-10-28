# HRMS 运单推送接口 — ERP 接入文档

面向 ERP（Java / Spring Boot）出库流程，实现「出库确认 → 自动把运单推进 HRMS → HRMS 自动同步 17TRACK 轨迹 → 驾驶舱实时航线」全链路自动化。

---

## 1. 前置条件

| 项 | 说明 |
|---|---|
| 接口地址 | `http://<hrms-host>:8080/api`（生产建议 HTTPS + 反向代理，不直接暴露 8080） |
| 鉴权 | 请求头 `X-API-Key`，密钥在 HRMS「业务管理 → 物流跟踪 → 批量导入运单 → 开放接口」获取 |
| 网络 | ERP 服务端能访问 HRMS 的 8080 端口；不要把密钥写进前端或浏览器脚本 |
| 依赖 | Spring Web（RestTemplate）、Jackson、spring-jdbc（outbox 使用 JdbcTemplate） |

知识边界：17TRACK 的 API 只能「按运单号查轨迹」，不能拉订单清单。因此运单号必须先由 ERP（本接口）、Excel 或手工进入 HRMS，之后轨迹由 HRMS 每 6 小时自动同步，无需 ERP 再推送。

---

## 2. 接口契约

### 2.1 请求

```http
POST {base}/open-api/import/shipment
X-API-Key: hrms_xxx
Content-Type: application/json; charset=UTF-8
```

```json
{
  "items": [
    {
      "trackingNo": "1Z999AA10123456784",
      "carrier": "UPS",
      "transportType": "快递",
      "portFrom": "上海港",
      "portTo": "美国纽约",
      "shipDate": "2026-10-20",
      "orderNo": "SO202610-001",
      "eta": "2026-10-28",
      "freightCostUsd": "120.50",
      "remark": "ERP出库单:OUT202610-001"
    }
  ]
}
```

### 2.2 字段

| 接口字段 | 中文表头（也接受） | 必填 | 说明 |
|---|---|---|---|
| `trackingNo` | 运单号 | 是 | 幂等键，重复推送自动跳过 |
| `carrier` | 承运商 | 否 | UPS / DHL / COSCO 等 |
| `transportType` | 运输方式 | 否 | 海运 / 空运 / 铁路 / 快递 / 卡车 |
| `portFrom` | 起运港 | 否 | 建议填 HRMS 坐标字典内的港口/城市，如「上海港」 |
| `portTo` | 目的港 | 否 | 同上；填不认识的名称不会生成航线点位 |
| `shipDate` | 发货日期 | 否 | `yyyy-MM-dd` |
| `orderNo` | 关联订单号 | 否 | 填了必须是 HRMS 已存在的订单号，否则该行失败 |
| `eta` | 预计到港 | 否 | `yyyy-MM-dd` |
| `freightCostUsd` | 运费(USD) | 否 | 数字字符串 |
| `remark` | 备注 | 否 | 建议写 ERP 出库单号，便于对账 |

`items` 每批建议 ≤ 50 条。

### 2.3 响应

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 1,
    "success": 1,
    "skipped": 0,
    "failed": 0,
    "errors": []
  }
}
```

| code | 含义 | 处理 |
|---|---|---|
| 200 且 `failed=0` | 全部受理（`success` 新建，`skipped` 已存在） | 标记完成 |
| 200 且 `failed>0` | 部分行数据不合规 | 按 `errors[].row/message` 修正后重推，无需重试 |
| 401/403 | 密钥错误或已失效 | 更新 ERP 配置中的密钥 |
| 5xx / 超时 | HRMS 或网络异常 | 进入重试（见第 5 节） |

`skipped=1` 是正常结果：说明该运单已在 HRMS 中，重试不会产生重复发货单。

---

## 3. 配置（ERP 端 application.yml）

```yaml
hrms:
  push:
    base-url: http://hrms-host:8080/api
    api-key: ${HRMS_API_KEY}     # 环境变量注入，勿提交明文
spring:
  task:
    scheduling:
      enabled: true              # 开启 @Scheduled（outbox 补偿任务）
```

---

## 4. 建表（ERP 库执行一次）

```sql
CREATE TABLE erp_shipment_push_outbox (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  outbound_no   VARCHAR(64)  NOT NULL COMMENT 'ERP 出库单号',
  tracking_no   VARCHAR(100) NOT NULL COMMENT '运单号（唯一幂等键）',
  payload       TEXT         NOT NULL COMMENT '推送报文 JSON',
  status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/DONE/FAILED',
  attempts      INT          NOT NULL DEFAULT 0,
  last_error    VARCHAR(500) NULL,
  next_retry_at DATETIME     NOT NULL,
  create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tracking (tracking_no),
  KEY idx_status_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 5. 接入步骤

### 5.1 放入代码

将 [HrmsShipmentPushClient.java](./HrmsShipmentPushClient.java)、[ShipmentPushItem.java](./ShipmentPushItem.java)、[ShipmentPushOutbox.java](./ShipmentPushOutbox.java) 复制到 ERP 工程（包名按需调整），确保组件扫描能扫到。

### 5.2 在出库确认处登记（同一事务）

```java
@Transactional
public void confirmOutbound(OutboundOrder outbound, Waybill waybill) {
    // 原有出库逻辑：扣减库存、更新出库单状态、写入运单号……
    outbox.enqueue(outbound.getOutboundNo(),
            ShipmentPushItem.builder()
                    .trackingNo(waybill.getNo())
                    .carrier(waybill.getCarrier())
                    .transportType(outbound.getTransportType())
                    .portFrom(outbound.getWarehousePort())
                    .portTo(outbound.getDestCity())
                    .shipDate(outbound.getShipDate().toString())
                    .orderNo(outbound.getSalesOrderNo())
                    .remark("ERP出库单:" + outbound.getOutboundNo())
                    .build());
}
```

要点：
- **不要在出库事务里直接发 HTTP**，否则外部接口抖动会拖住出库并可能造成长事务。
- outbox 与出库记录同事务提交：要么一起成功，要么一起回滚，不会丢单。
- `tracking_no` 唯一索引保证同一运单不会重复入队。

### 5.3 自动推送

`ShipmentPushOutbox.pushPending()` 每 30 秒扫描 `PENDING` 且到期的记录并推送：

| 结果 | 处理 |
|---|---|
| `success=1` 或 `skipped=1` | `status=DONE` |
| `failed>0`（数据不合规） | `status=FAILED`，人工修正后重推 |
| 网络/5xx/超时 | 按 2、4、8、16 分钟退避，最多 5 次；仍失败则 `FAILED` |

---

## 6. 幂等与重试

- HRMS 侧以 `trackingNo` 去重，重复推送返回 `skipped`。
- ERP 侧 `uk_tracking` 唯一索引 + outbox 状态机，避免同一运单重复入队。
- 因此「网络超时后重试」是安全的：最坏结果只是多一次 `skipped`。

---

## 7. 在 HRMS 查看推送结果

进入「业务管理 → 物流跟踪 → 批量导入运单 → 开放接口」弹窗，下方「最近调用记录」显示每次调用的时间、模块、提交/成功/跳过/失败数量、失败明细、调用 IP，只统计外部 API 调用。

失败明细示例：`第 1 行：订单号「NO-SUCH-ORDER」不存在，可清空该列后重新导入`。

---

## 8. 联调清单

1. 推送 1 条测试运单 → 返回 `success=1`，HRMS 发货单列表出现该单（状态「运输中」）。
2. 同一条再推 → `skipped=1`。
3. 构造订单号不存在的运单 → `failed=1`，HRMS「最近调用记录」能看到失败明细。
4. 真实运单号 6 小时内（或点「同步实时轨迹」）应出现 17TRACK 轨迹节点。
5. 驾驶舱业务视图可看到该票航线；若 `portTo` 不在坐标字典内，则不会有该航段点位。
6. 停掉 HRMS 模拟网络故障 → outbox 记录进入重试，恢复后自动补发为 `DONE`。

### 上线前

- [ ] `HRMS_API_KEY` 已通过环境变量/配置中心注入，未提交到 Git
- [ ] HRMS 已部署在 HTTPS/内网白名单之后
- [ ] 出库确认逻辑已改为「写 outbox」，无同步 HTTP 调用
- [ ] 监控：`FAILED` 记录数、`attempts>=5` 告警

---

## 9. FAQ

**Q：推送成功多久能看到图上航线？**
A：发货单会立即创建，但航线依赖真实轨迹节点；有轨迹节点后才上图（避免假航线）。

**Q：目的是具体街道地址，能定位吗？**
A：地图按港口/城市名匹配。`portTo` 填「美国纽约」这类城市/港口名才能上图；街道地址无法定位。

**Q：能推历史运单吗？**
A：可以，接口与 Excel 同一套校验；但轨迹以 17TRACK 当前能查到的节点为准。

**Q：能只推运单号吗？**
A：可以，只有 `trackingNo` 必填，其余字段后续可在 HRMS「编辑发货单」补全。
