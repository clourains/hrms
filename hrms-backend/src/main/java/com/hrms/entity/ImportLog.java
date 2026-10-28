package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 批量导入审计日志实体
 */
@Data
@TableName("sys_import_log")
public class ImportLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String module;

    private String moduleLabel;

    /** EXCEL / API */
    private String source;

    private String operator;

    private String ip;

    private String fileName;

    private Integer total;

    private Integer success;

    private Integer skipped;

    private Integer failed;

    private LocalDateTime createTime;
}
