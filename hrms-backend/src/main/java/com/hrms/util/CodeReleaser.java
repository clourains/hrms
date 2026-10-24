package com.hrms.util;

import org.springframework.util.StringUtils;

/**
 * 逻辑删除前释放业务编码（P043）
 *
 * 客户、供应商、仓库、产品四张表的编码列都有唯一索引（uk_customer_code / uk_supplier_code /
 * uk_warehouse_code / uk_sku），而实体是 @TableLogic 逻辑删除——记录删掉后行还在，编码仍被占着。
 * 结果就是：删掉「TEST-DEL」之后再用同一编码新建，接口报「该编码已存在」，可列表里又看不到这条
 * 已删记录，用户完全无从判断。
 *
 * 处理办法：删除时把编码改名成「原编码#DEL{id}」，让出原编码；被改名的那行早已不在任何列表里，
 * 改名对用户不可见，同时仍保留历史数据的可追溯性（能看出它原来是哪个编码、哪条记录）。
 * 编码列都是 varchar(50)，前缀部分按剩余长度截断，保证不超长。
 */
public final class CodeReleaser {

    private static final String MARK = "#DEL";

    private CodeReleaser() {
    }

    /**
     * @param code   原编码
     * @param id     记录主键（拼进后缀，保证同一编码被反复删除也不冲突）
     * @param maxLen 编码列长度（如 varchar(50) 传 50）
     * @return 释放后的编码，可直接 update 进待删除记录
     */
    public static String release(String code, Long id, int maxLen) {
        if (!StringUtils.hasText(code) || id == null) {
            return code;
        }
        // 已经是释放过的编码（重复删除/脏数据），不再二次加后缀
        if (code.contains(MARK)) {
            return code;
        }
        String suffix = MARK + id;
        int keep = Math.max(0, maxLen - suffix.length());
        String base = code.length() > keep ? code.substring(0, keep) : code;
        return base + suffix;
    }
}
