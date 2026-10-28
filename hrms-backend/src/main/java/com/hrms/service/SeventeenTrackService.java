package com.hrms.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 17TRACK 实时物流查询（API v2.2）。
 *
 * 流程：/register 登记单号（已登记会忽略） → /gettrackinfo 拉取轨迹。
 * Key 配置：app.seventeen-track.api-key 或环境变量 SEVENTEEN_TRACK_API_KEY。
 * 未配置 Key 或号码无效时抛业务异常，返回明确错误，不生成伪造轨迹。
 */
@Service
public class SeventeenTrackService {

    private static final String BASE = "https://api.17track.net/track/v2.2";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId CN = ZoneId.of("Asia/Shanghai");

    @Value("${app.seventeen-track.api-key:}")
    private String apiKey;

    /**
     * 查询运单实时轨迹，返回与本地轨迹同结构的节点列表（按时间升序）。
     */
    public List<Map<String, Object>> query(String trackingNo) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException(
                    "未配置 17TRACK API Key：请在本地配置 app.seventeen-track.api-key"
                            + " 或环境变量 SEVENTEEN_TRACK_API_KEY 中填写后重启后端");
        }
        if (trackingNo == null || trackingNo.isBlank()) {
            throw new RuntimeException("该发货单未填写运单号，无法查询");
        }
        String no = trackingNo.trim();
        JSONArray body = new JSONArray().put(new JSONObject().set("number", no));

        // 1. 登记单号：记录失败原因（例如承运商无法识别），供查询失败时给出准确提示
        String registerIssue = null;
        try {
            JSONObject reg = post("/register", body);
            JSONObject regData = reg.getJSONObject("data");
            JSONArray rejected = regData == null ? null : regData.getJSONArray("rejected");
            if (rejected != null && !rejected.isEmpty()) {
                JSONObject error = rejected.getJSONObject(0).getJSONObject("error");
                if (error != null) {
                    registerIssue = error.getStr("message");
                }
            }
        } catch (Exception ignore) {
            // 登记失败不阻断，交由查询步骤给出明确原因
        }

        // 2. 拉取轨迹
        JSONObject root = post("/gettrackinfo", body);
        Integer code = root.getInt("code");
        if (code == null || code != 0) {
            throw new RuntimeException("17TRACK 返回错误：" + root.getStr("message", "未知错误") + "（code=" + code + "）");
        }
        JSONObject data = root.getJSONObject("data");
        if (data == null) {
            throw new RuntimeException("17TRACK 未返回数据");
        }

        JSONArray accepted = data.getJSONArray("accepted");
        if (accepted == null || accepted.isEmpty()) {
            if (registerIssue != null && registerIssue.toLowerCase().contains("carrier cannot be detected")) {
                throw new RuntimeException("17TRACK " + translateMessage(registerIssue) + "（" + no + "）");
            }
            throw new RuntimeException("17TRACK 未接受该运单号：" + rejectReason(data.getJSONArray("rejected"), no));
        }

        JSONObject item = accepted.getJSONObject(0);
        JSONObject trackInfo = item.getJSONObject("track_info");
        JSONObject tracking = trackInfo == null ? null : trackInfo.getJSONObject("tracking");
        JSONArray providers = tracking == null ? null : tracking.getJSONArray("providers");

        // 多家承运商可能返回同一事件，按「时间+描述+地点」去重
        Map<String, JSONObject> merged = new LinkedHashMap<>();
        if (providers != null) {
            for (Object po : providers) {
                if (!(po instanceof JSONObject provider)) {
                    continue;
                }
                JSONArray events = provider.getJSONArray("events");
                if (events == null) {
                    continue;
                }
                for (Object eo : events) {
                    if (eo instanceof JSONObject ev) {
                        String k = ev.getStr("time_iso", ev.getStr("time_utc", ""))
                                + "|" + ev.getStr("description", "")
                                + "|" + ev.getStr("location", "");
                        merged.putIfAbsent(k, ev);
                    }
                }
            }
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        for (JSONObject ev : merged.values()) {
            Map<String, Object> node = new HashMap<>();
            node.put("node", resolveStage(ev));
            node.put("location", ev.getStr("location"));
            node.put("content", ev.getStr("description"));
            node.put("trackTime", resolveTime(ev));
            node.put("operator", "17TRACK");
            nodes.add(node);
        }
        nodes.sort(Comparator.comparing(n -> String.valueOf(n.getOrDefault("trackTime", ""))));
        if (nodes.isEmpty()) {
            throw new RuntimeException("17TRACK 暂未返回该运单的轨迹节点，可能号码刚登记还没物流信息");
        }
        return nodes;
    }

    private JSONObject post(String path, JSONArray body) {
        HttpResponse resp;
        try {
            resp = HttpRequest.post(BASE + path)
                    .header("17token", apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(15000)
                    .execute();
        } catch (Exception e) {
            throw new RuntimeException("调用 17TRACK 失败：" + e.getMessage());
        }
        try {
            return JSONUtil.parseObj(resp.body());
        } catch (Exception e) {
            throw new RuntimeException("17TRACK 返回内容无法解析：" + resp.body());
        }
    }

    private String rejectReason(JSONArray rejected, String no) {
        if (rejected == null || rejected.isEmpty()) {
            return "号码 " + no + " 无查询结果";
        }
        JSONObject first = rejected.getJSONObject(0);
        JSONObject error = first.getJSONObject("error");
        String msg = error == null ? "未知原因" : error.getStr("message", "未知原因");
        return no + "：" + translateMessage(msg);
    }

    /**
     * 17TRACK 的英文错误转中文，便于前端直接展示
     */
    private String translateMessage(String msg) {
        if (msg == null || msg.isBlank()) {
            return "未知原因";
        }
        String m = msg.toLowerCase();
        if (m.contains("carrier cannot be detected")) {
            return "承运商无法识别：演示数据里的运单号是虚构的，请改用真实快递单号查询";
        }
        if (m.contains("no tracking information")) {
            return "暂无物流信息：该单号可能还没有产生轨迹，或号码有误";
        }
        if (m.contains("does not register")) {
            return "该单号尚未登记成功，请稍后重试";
        }
        if (m.contains("temporarily not supported")) {
            return "该承运商暂不支持通过 API 登记（承运商政策限制），请改用其他承运商的单号";
        }
        if (m.contains("invalid")) {
            return "运单号格式无效";
        }
        return msg;
    }

    /**
     * 节点状态：stage 优先，缺失时退到 sub_status，再缺失按运输途中处理
     */
    private String resolveStage(JSONObject ev) {
        String stage = ev.getStr("stage");
        if (stage != null && !stage.isBlank()) {
            return stageText(stage);
        }
        String sub = ev.getStr("sub_status");
        if (sub != null && !sub.isBlank()) {
            return stageText(sub);
        }
        return "运输中";
    }

    /**
     * 节点时间：time_iso 优先，其次 time_utc，最后取 time_raw 的 date+time
     */
    private String resolveTime(JSONObject ev) {
        String iso = ev.getStr("time_iso");
        if (iso != null && !iso.isBlank()) {
            return formatTime(iso);
        }
        String utc = ev.getStr("time_utc");
        if (utc != null && !utc.isBlank()) {
            return formatTime(utc);
        }
        Object raw = ev.get("time_raw");
        if (raw instanceof JSONObject tr) {
            String date = tr.getStr("date");
            String time = tr.getStr("time");
            if (date != null && !date.isBlank()) {
                return formatTime(date + " " + (time == null || time.isBlank() ? "00:00:00" : time));
            }
        }
        return "";
    }

    /**
     * 17TRACK stage 转中文
     */
    private String stageText(String stage) {
        if (stage == null || stage.isBlank()) {
            return "轨迹更新";
        }
        String s = stage.toLowerCase();
        if (s.startsWith("inforeceived")) {
            return "信息已接收";
        }
        if (s.startsWith("intransit")) {
            return "运输中";
        }
        if (s.startsWith("outfordelivery")) {
            return "派送中";
        }
        if (s.startsWith("attemptfail")) {
            return "派送失败";
        }
        if (s.startsWith("delivered")) {
            return "已签收";
        }
        if (s.startsWith("availableforpickup")) {
            return "待取件";
        }
        if (s.startsWith("pickedup") || s.startsWith("pickup")) {
            return "已揽收";
        }
        if (s.startsWith("undelivered")) {
            return "未送达";
        }
        if (s.startsWith("exception")) {
            return "异常";
        }
        if (s.startsWith("expired")) {
            return "超期";
        }
        if (s.startsWith("customs")) {
            return "清关中";
        }
        if (s.startsWith("returning") || s.startsWith("returned")) {
            return "退回中";
        }
        return stage;
    }

    /**
     * 兼容 ISO8601（2023-05-10T09:08:00Z / +08:00）与常见格式，统一为北京时间 yyyy-MM-dd HH:mm:ss
     */
    private String formatTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.trim();
        try {
            return OffsetDateTime.parse(s).atZoneSameInstant(CN).toLocalDateTime().format(FMT);
        } catch (Exception ignore) {
            // 尝试其它格式
        }
        for (String p : new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"}) {
            try {
                if (p.contains("HH")) {
                    return LocalDateTime.parse(s, DateTimeFormatter.ofPattern(p)).format(FMT);
                }
                return LocalDateTime.parse(s + " 00:00:00", FMT).format(FMT);
            } catch (Exception ignore) {
                // 下一个格式
            }
        }
        return s;
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return "";
    }
}
