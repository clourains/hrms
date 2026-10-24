package com.hrms.util;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 通用必填/取值校验。
 *
 * 前端 el-form 的 required 只能拦住正常点击保存的操作，直接调接口照样能提交空值；
 * 而库里的 NOT NULL 只会抛 SQL 错（500 + 一长串异常），提示对用户没有意义。
 * 所以每个模块的 save/update 都在服务端再校验一次，口径与前端表单一致。
 */
public final class Validators {

    private Validators() {
    }

    /** 字符串必填（null、空串、纯空格都算空） */
    public static void text(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new RuntimeException(label + "不能为空");
        }
    }

    /** 对象必填（下拉选中的 id、日期等） */
    public static void notNull(Object value, String label) {
        if (value == null) {
            throw new RuntimeException(label + "不能为空");
        }
    }

    /** 数值不能为负 */
    public static void nonNegative(BigDecimal value, String label) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(label + "不能为负数");
        }
    }

    /** 整数不能为负 */
    public static void nonNegative(Integer value, String label) {
        if (value != null && value < 0) {
            throw new RuntimeException(label + "不能为负数");
        }
    }

    /** 数值必须大于 0 */
    public static void positive(BigDecimal value, String label) {
        if (value != null && value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(label + "必须大于 0");
        }
    }

    /**
     * 编辑时不允许把已有值清空。
     * 否则「新增必填 → 编辑清空」就能绕过新增校验；传 null 视为未改动，不拦。
     */
    public static void notCleared(String originValue, String newValue, String label) {
        if (StringUtils.hasText(originValue) && newValue != null && !StringUtils.hasText(newValue)) {
            throw new RuntimeException(label + "不能清空，如需变更请填写新值");
        }
    }

    /** 同 {@link #notCleared}，用于 id、日期等非字符串字段 */
    public static void notCleared(Object originValue, Object newValue, String label) {
        if (originValue != null && newValue == null) {
            throw new RuntimeException(label + "不能清空，如需变更请填写新值");
        }
    }
}
