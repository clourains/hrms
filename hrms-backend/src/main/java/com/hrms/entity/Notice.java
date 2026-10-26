package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

    private String publisher;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
