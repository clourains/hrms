package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内信实体（P2-1）
 */
@Data
@TableName("sys_message")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收人用户名 */
    private String receiver;

    private String title;

    private String content;

    /** 类型：系统/审批/提醒 */
    private String msgType;

    /** 发送人用户名，system 表示系统消息 */
    private String sender;

    /** 0未读 1已读 */
    private Integer isRead;

    private LocalDateTime readTime;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
