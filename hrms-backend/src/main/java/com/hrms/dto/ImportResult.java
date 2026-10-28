package com.hrms.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入结果
 */
@Data
public class ImportResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 本次提交的总行数 */
    private int total;

    /** 成功写入条数 */
    private int success;

    /** 因重复跳过条数 */
    private int skipped;

    /** 校验失败条数 */
    private int failed;

    /** 逐行错误说明（最多保留前 50 条，避免响应过大） */
    private List<RowError> errors = new ArrayList<>();

    public void addError(int row, String message) {
        failed++;
        if (errors.size() < 50) {
            errors.add(new RowError(row, message));
        }
    }

    /**
     * 重复数据整组跳过：数量记在 skipped，错误列表里保留一条说明便于用户排查
     */
    public void addSkipped(int count, int row, String message) {
        skipped += Math.max(count, 1);
        if (errors.size() < 50) {
            errors.add(new RowError(row, message));
        }
    }

    @Data
    public static class RowError implements Serializable {

        private static final long serialVersionUID = 1L;

        /** Excel 行号（从 2 开始，第 1 行是表头） */
        private int row;

        private String message;

        public RowError() {
        }

        public RowError(int row, String message) {
            this.row = row;
            this.message = message;
        }
    }
}
