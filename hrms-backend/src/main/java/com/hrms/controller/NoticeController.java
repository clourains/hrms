package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.Notice;
import com.hrms.entity.NoticeRead;
import com.hrms.entity.User;
import com.hrms.mapper.NoticeMapper;
import com.hrms.mapper.NoticeReadMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.util.ExcelExportUtil;
import com.hrms.util.Result;
import com.hrms.util.UserContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
            LocalDateTime now = LocalDateTime.now();
            LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<Notice>()
                    .eq(Notice::getStatus, 1)
                    .and(w -> w.isNull(Notice::getEffectiveStart).or().le(Notice::getEffectiveStart, now))
                    .and(w -> w.isNull(Notice::getEffectiveEnd).or().ge(Notice::getEffectiveEnd, now));
            // 发布范围：管理员看全部；普通用户只看「全员」或包含自己部门的公告
            if (!"admin".equals(UserContext.getRole())) {
                Long deptId = UserContext.getDeptId();
                if (deptId == null) {
                    wrapper.and(w -> w.isNull(Notice::getScopeType).or().eq(Notice::getScopeType, 0));
                } else {
                    wrapper.and(w -> w.isNull(Notice::getScopeType)
                            .or().eq(Notice::getScopeType, 0)
                            .or(y -> y.eq(Notice::getScopeType, 1)
                                    .apply("FIND_IN_SET({0}, dept_ids)", deptId)));
                }
            }
            List<Notice> list = noticeMapper.selectList(wrapper.orderByDesc(Notice::getCreateTime));
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
                row.put("scopeType", n.getScopeType());
                row.put("deptIds", n.getDeptIds());
                row.put("effectiveStart", n.getEffectiveStart());
                row.put("effectiveEnd", n.getEffectiveEnd());
                row.put("pinned", n.getPinned() == null ? 0 : n.getPinned());
                row.put("publisher", n.getPublisher());
                row.put("createTime", n.getCreateTime());
                row.put("isRead", readIds.contains(n.getId()) ? 1 : 0);
                rows.add(row);
            }
            // 置顶优先，其次重要公告，其余按发布时间（查询已按时间倒序）
            rows.sort((a, b) -> {
                int ap = ((Number) a.get("pinned")).intValue();
                int bp = ((Number) b.get("pinned")).intValue();
                if (ap != bp) return bp - ap;
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
            String scopeError = normalizeScope(notice);
            if (scopeError != null) {
                return Result.error(scopeError);
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
            // 上下线切换只传 id+status：不动正文/范围，避免清空字段
            if (notice.getTitle() == null) {
                noticeMapper.update(null, new LambdaUpdateWrapper<Notice>()
                        .eq(Notice::getId, existing.getId())
                        .set(Notice::getStatus, notice.getStatus())
                        .set(Notice::getUpdateTime, LocalDateTime.now()));
                return Result.success("更新成功", null);
            }
            String scopeError = normalizeScope(notice);
            if (scopeError != null) {
                return Result.error(scopeError);
            }
            // 用 UpdateWrapper 显式 set，允许把有效期/部门范围清空
            LambdaUpdateWrapper<Notice> uw = new LambdaUpdateWrapper<Notice>()
                    .eq(Notice::getId, existing.getId())
                    .set(Notice::getTitle, notice.getTitle())
                    .set(Notice::getContent, notice.getContent())
                    .set(Notice::getLevel, notice.getLevel())
                    .set(Notice::getStatus, notice.getStatus())
                    .set(Notice::getScopeType, notice.getScopeType())
                    .set(Notice::getDeptIds, notice.getDeptIds())
                    .set(Notice::getEffectiveStart, notice.getEffectiveStart())
                    .set(Notice::getEffectiveEnd, notice.getEffectiveEnd())
                    .set(Notice::getPinned, notice.getPinned() == null ? 0 : notice.getPinned())
                    .set(Notice::getUpdateTime, LocalDateTime.now());
            noticeMapper.update(null, uw);
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

    /**
     * 统一处理发布范围与时效：
     * scopeType=1 必须至少指定一个部门；全员发布时清空部门，避免残留脏数据；
     * 结束时间不能早于开始时间。
     */
    private String normalizeScope(Notice notice) {
        if (notice.getScopeType() == null) {
            notice.setScopeType(0);
        }
        if (notice.getPinned() == null) {
            notice.setPinned(0);
        }
        if (notice.getScopeType() == 1) {
            String ids = notice.getDeptIds() == null ? "" : notice.getDeptIds().replaceAll("\\s+", "");
            notice.setDeptIds(ids);
            if (ids.isEmpty()) {
                return "指定部门发布时请至少选择一个部门";
            }
        } else {
            notice.setDeptIds(null);
        }
        if (notice.getEffectiveStart() != null && notice.getEffectiveEnd() != null
                && notice.getEffectiveEnd().isBefore(notice.getEffectiveStart())) {
            return "生效结束时间不能早于开始时间";
        }
        return null;
    }

    /**
     * 公告列表导出（P8-A）
     */
    @RequireRole("admin")
    @GetMapping("/export")
    public void export(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       HttpServletResponse response) throws Exception {
        List<Notice> list = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Notice::getTitle, keyword)
                        .or().like(Notice::getContent, keyword))
                .eq(status != null, Notice::getStatus, status)
                .orderByDesc(Notice::getCreateTime));
        List<List<Object>> rows = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Notice n : list) {
            rows.add(List.of(
                    n.getId(),
                    n.getTitle() == null ? "" : n.getTitle(),
                    n.getLevel() == null ? "normal" : n.getLevel(),
                    n.getStatus() != null && n.getStatus() == 1 ? "已发布" : "已下线",
                    n.getPublisher() == null ? "" : n.getPublisher(),
                    n.getCreateTime() == null ? "" : n.getCreateTime().format(fmt)
            ));
        }
        ExcelExportUtil.write(response, "公告列表.xlsx",
                List.of("ID", "标题", "级别", "状态", "发布人", "发布时间"),
                rows);
    }
}
