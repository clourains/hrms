package com.hrms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 登录日志实体（P5-2）
 *
 * 对应表 sys_login_log，只写不删，无逻辑删除列。
 */
@Data
@TableName("sys_login_log")
public class LoginLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 0失败 1成功 */
    private Integer success;

    private String msg;

    private String ip;

    private String userAgent;

    private LocalDateTime createTime;
}
