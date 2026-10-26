package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.Notice;
import com.hrms.entity.NoticeRead;
import com.hrms.entity.User;
import com.hrms.mapper.NoticeMapper;
import com.hrms.mapper.NoticeReadMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.util.Result;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统公告 Controller（P6-1）
 *
 * /active、/read 面向所有登录用户；其余管理接口仅 admin。
 * 已读统计基于 sys_notice_read，分母为状态正常的用户总数。
 */
@RestController
@RequestMapping("/notice")
public class NoticeController {

    @Autowired
    private NoticeMapper noticeMapper;

    @Autowired
    private NoticeReadMapper noticeReadMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 生效中的公告（首页置顶展示）：重要置顶，其余按发布时间倒序；
     * 附带当前用户是否已读
     */
    @GetMapping("/active")
    public Result<List<Map<String, Object>>> active() {
        try {
            String me = UserContext.get();
            List<Notice> list = noticeMapper.selectList(
                    new LambdaQueryWrapper<Notice>()
                            .eq(Notice::getStatus, 1)
                            .orderByDesc(Notice::getCreateTime));
            if (list.isEmpty()) {
                return Result.success(new ArrayList<>());
            }
            // 我已读的公告 id 集合
            List<Long> ids = list.stream().map(Notice::getId).toList();
            Set<Long> readIds = noticeReadMapper.selectList(
                            new LambdaQueryWrapper<NoticeRead>()
                                    .eq(NoticeRead::getUsername, me)
                                    .in(NoticeRead::getNoticeId, ids))
                    .stream().map(NoticeRead::getNoticeId).collect(Collectors.toSet());

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Notice n : list) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", n.getId());
                row.put("title", n.getTitle());
                row.put("content", n.getContent());
                row.put("level", n.getLevel());
                row.put("publisher", n.getPublisher());
                row.put("createTime", n.getCreateTime());
                row.put("isRead", readIds.contains(n.getId()) ? 1 : 0);
                rows.add(row);
            }
            // 重要公告排前面
            rows.sort((a, b) -> {
                boolean ai = "important".equals(a.get("level"));
                boolean bi = "important".equals(b.get("level"));
                if (ai != bi) return ai ? -1 : 1;
                return 0;
            });
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 标记已读（唯一键兜底：并发重复点不产生重复记录）
     */
    @PostMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id) {
        try {
            String me = UserContext.get();
            Notice notice = noticeMapper.selectById(id);
            if (notice == null) {
                return Result.error("公告不存在");
            }
            Long count = noticeReadMapper.selectCount(
                    new LambdaQueryWrapper<NoticeRead>()
                            .eq(NoticeRead::getNoticeId, id)
                            .eq(NoticeRead::getUsername, me));
            if (count == null || count == 0) {
                try {
                    NoticeRead read = new NoticeRead();
                    read.setNoticeId(id);
                    read.setUsername(me);
                    read.setReadTime(LocalDateTime.now());
                    noticeReadMapper.insert(read);
                } catch (Exception ignored) {
                    // 唯一键冲突说明已读，忽略
                }
            }
            return Result.success("已读", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 管理端分页列表（admin）
     */
    @RequireRole("admin")
    @GetMapping("/list")
    public Result<Page<Notice>> list(@RequestParam(defaultValue = "1") long current,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Integer status) {
        try {
            Page<Notice> page = noticeMapper.selectPage(new Page<>(current, size),
                    new LambdaQueryWrapper<Notice>()
                            .and(StringUtils.hasText(keyword), w -> w
                                    .like(Notice::getTitle, keyword)
                                    .or().like(Notice::getContent, keyword))
                            .eq(status != null, Notice::getStatus, status)
                            .orderByDesc(Notice::getCreateTime));
            return Result.success(page);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发布公告（admin）
     */
    @RequireRole("admin")
    @AuditLog(module = "公告管理", action = "发布")
    @PostMapping
    public Result<Void> create(@RequestBody Notice notice) {
        try {
            if (!StringUtils.hasText(notice.getTitle())) {
                return Result.error("标题不能为空");
            }
            notice.setId(null);
            if (!StringUtils.hasText(notice.getLevel())) {
                notice.setLevel("normal");
            }
            if (notice.getStatus() == null) {
                notice.setStatus(1);
            }
            notice.setPublisher(UserContext.getOrDefault("admin"));
            notice.setCreateTime(LocalDateTime.now());
            notice.setUpdateTime(LocalDateTime.now());
            noticeMapper.insert(notice);
            return Result.success("发布成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 编辑/上线下线（admin）
     */
    @RequireRole("admin")
    @AuditLog(module = "公告管理", action = "修改")
    @PutMapping
    public Result<Void> update(@RequestBody Notice notice) {
        try {
            Notice existing = noticeMapper.selectById(notice.getId());
            if (existing == null) {
                return Result.error("公告不存在");
            }
            Notice update = new Notice();
            update.setId(existing.getId());
            update.setTitle(notice.getTitle());
            update.setContent(notice.getContent());
            update.setLevel(notice.getLevel());
            update.setStatus(notice.getStatus());
            update.setUpdateTime(LocalDateTime.now());
            noticeMapper.updateById(update);
            return Result.success("更新成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除（admin）：连带清除已读记录
     */
    @RequireRole("admin")
    @AuditLog(module = "公告管理", action = "删除")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            noticeMapper.deleteById(id);
            noticeReadMapper.delete(
                    new LambdaQueryWrapper<NoticeRead>().eq(NoticeRead::getNoticeId, id));
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 已读统计（admin）：已读名单 + 总数/已读数
     */
    @RequireRole("admin")
    @GetMapping("/readStats/{id}")
    public Result<Map<String, Object>> readStats(@PathVariable Long id) {
        try {
            Long totalUsers = userMapper.selectCount(
                    new LambdaQueryWrapper<User>()
                            .and(w -> w.isNull(User::getStatus).or().eq(User::getStatus, 1)));
            List<NoticeRead> reads = noticeReadMapper.selectList(
                    new LambdaQueryWrapper<NoticeRead>()
                            .eq(NoticeRead::getNoticeId, id)
                            .orderByAsc(NoticeRead::getReadTime));
            List<Map<String, Object>> readers = reads.stream().map(r -> {
                Map<String, Object> row = new HashMap<>();
                row.put("username", r.getUsername());
                row.put("readTime", r.getReadTime());
                return row;
            }).collect(Collectors.toList());
            Map<String, Object> data = new HashMap<>();
            data.put("totalUsers", totalUsers);
            data.put("readCount", readers.size());
            data.put("readers", readers);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
