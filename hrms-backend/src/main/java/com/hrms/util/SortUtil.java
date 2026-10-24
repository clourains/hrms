package com.hrms.util;

import java.util.Map;

/**
 * 列表排序辅助
 *
 * 前端只传字段名，后端通过白名单换算成真实列名，避免 SQL 注入。
 * 对编码/单号这类"字母+数字"的字段使用自然排序：把数字部分按数值大小比较，
 * 这样 WH-CN-2 会排在 WH-CN-10 前面，而不是字典序反过来（P004）。
 */
public class SortUtil {

    /**
     * 一个可排序字段：真实列名 + 是否按自然序（提取数字部分比较）
     */
    public static class OrderCol {
        private final String column;
        private final boolean natural;

        private OrderCol(String column, boolean natural) {
            this.column = column;
            this.natural = natural;
        }

        /** 普通字段：时间、金额、数量等 */
        public static OrderCol plain(String column) {
            return new OrderCol(column, false);
        }

        /** 带数字的文本字段：编码、单号、SKU 等 */
        public static OrderCol natural(String column) {
            return new OrderCol(column, true);
        }
    }

    private SortUtil() {
    }

    /**
     * 生成 ORDER BY 子句，字段不在白名单或参数为空时返回 null（由调用方走默认排序）
     */
    public static String build(String sortField, String sortOrder, Map<String, OrderCol> whitelist) {
        if (sortField == null || whitelist == null || !whitelist.containsKey(sortField)) {
            return null;
        }
        boolean asc = !"desc".equalsIgnoreCase(sortOrder);
        String dir = asc ? "ASC" : "DESC";

        OrderCol col = whitelist.get(sortField);
        String column = col.column;

        if (!col.natural) {
            return "ORDER BY " + column + " " + dir;
        }

        // 自然排序：先按"去掉数字后的前缀"分组，再按"提取出来的数字"比数值，最后按原值兜底
        return "ORDER BY REGEXP_REPLACE(" + column + ", '[0-9]', '') " + dir
                + ", CAST(NULLIF(REGEXP_REPLACE(" + column + ", '[^0-9]', ''), '') AS UNSIGNED) " + dir
                + ", " + column + " " + dir;
    }
}
