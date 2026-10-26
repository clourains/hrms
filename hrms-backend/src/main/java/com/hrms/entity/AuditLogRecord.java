package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 敏感操作审计日志实体（P1-1）
 *
 * 对应表 sys_audit_log，只写不删，无逻辑删除列。
 */
@Data
@TableName("sys_audit_log")
public class AuditLogRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String module;

    private String action;

    private String detail;

    private Integer success;

    private String errorMsg;

    private Long costMs;

    private String ip;

    private LocalDateTime createTime;
}
