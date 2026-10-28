package erp.integration;

import lombok.Builder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 推送给 HRMS 的运单数据，字段与开放接口一一对应，只有 trackingNo 必填。
 * <p>
 * ERP 出库确认后可这样构造：
 * <pre>
 * ShipmentPushItem item = ShipmentPushItem.builder()
 *         .trackingNo(waybill.getNo())
 *         .carrier(waybill.getCarrier())          // UPS / DHL / COSCO ...
 *         .transportType(outbound.getTransportType()) // 海运/空运/铁路/快递/卡车
 *         .portFrom(outbound.getWarehousePort())  // 如「上海港」
 *         .portTo(outbound.getDestCity())         // 如「美国纽约」，建议用 HRMS 坐标字典里的城市/港口名
 *         .shipDate(outbound.getShipDate().toString())
 *         .orderNo(outbound.getSalesOrderNo())    // 可空；填了必须是 HRMS 已存在的订单号
 *         .eta(outbound.getEta() == null ? null : outbound.getEta().toString())
 *         .remark("ERP出库单:" + outbound.getOutboundNo())
 *         .build();
 * </pre>
 */
@Data
@Builder
public class ShipmentPushItem {

    private String trackingNo;
    private String carrier;
    private String transportType;
    private String portFrom;
    private String portTo;
    private String shipDate;
    private String orderNo;
    private String eta;
    private String freightCostUsd;
    private String remark;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("trackingNo", trackingNo);
        if (carrier != null) putIfNotBlank(map, "carrier", carrier);
        if (transportType != null) putIfNotBlank(map, "transportType", transportType);
        if (portFrom != null) putIfNotBlank(map, "portFrom", portFrom);
        if (portTo != null) putIfNotBlank(map, "portTo", portTo);
        if (shipDate != null) putIfNotBlank(map, "shipDate", shipDate);
        if (orderNo != null) putIfNotBlank(map, "orderNo", orderNo);
        if (eta != null) putIfNotBlank(map, "eta", eta);
        if (freightCostUsd != null) putIfNotBlank(map, "freightCostUsd", freightCostUsd);
        if (remark != null) putIfNotBlank(map, "remark", remark);
        return map;
    }

    private void putIfNotBlank(Map<String, Object> map, String key, String value) {
        if (!value.isBlank()) {
            map.put(key, value);
        }
    }
}
