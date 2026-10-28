package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统公告实体（P6-1）
 */
@Data
@TableName("sys_notice")
public class Notice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /** 级别：normal普通 / important重要 */
    private String level;

    /** 状态：1发布 0下线 */
    private Integer status;

    /** 发布范围：0全员 1指定部门 */
    private Integer scopeType;

    /** 指定部门ID，逗号分隔（scopeType=1 时生效） */
    private String deptIds;

    /** 生效开始时间，空表示立即生效 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveStart;

    /** 生效结束时间，空表示长期有效 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime effectiveEnd;

    /** 1 置顶 */
    private Integer pinned;

    private String publisher;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
