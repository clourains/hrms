package com.hrms.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.annotation.AuditLog;
import com.hrms.annotation.RequireRole;
import com.hrms.entity.Message;
import com.hrms.entity.User;
import com.hrms.mapper.MessageMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.util.Result;
import com.hrms.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 站内信 Controller（P2-1）
 *
 * receiver 存 sys_user.username；收件箱/未读数/已读均以当前登录人为准。
 */
@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 收件箱：当前用户的消息，可按类型/关键字过滤
     */
    @GetMapping("/inbox")
    public Result<List<Map<String, Object>>> inbox(
            @RequestParam(required = false) String msgType,
            @RequestParam(required = false) String keyword) {
        try {
            String me = UserContext.get();
            List<Message> list = messageMapper.selectList(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getReceiver, me)
                            .eq(msgType != null && !msgType.isEmpty(), Message::getMsgType, msgType)
                            .and(keyword != null && !keyword.isEmpty(),
                                    w -> w.like(Message::getTitle, keyword)
                                            .or().like(Message::getContent, keyword))
                            .orderByDesc(Message::getCreateTime));
            return Result.success(fillSender(list));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 未读数（顶部铃铛角标）
     */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        try {
            String me = UserContext.get();
            Long count = messageMapper.selectCount(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getReceiver, me)
                            .eq(Message::getIsRead, 0));
            return Result.success(count);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 标记单条已读（只能读自己的）
     */
    @PutMapping("/read/{id}")
    public Result<Void> markRead(@PathVariable Long id) {
        try {
            String me = UserContext.get();
            Message msg = messageMapper.selectById(id);
            if (msg == null || !me.equals(msg.getReceiver())) {
                return Result.error("消息不存在");
            }
            if (msg.getIsRead() == null || msg.getIsRead() == 0) {
                msg.setIsRead(1);
                msg.setReadTime(LocalDateTime.now());
                messageMapper.updateById(msg);
            }
            return Result.success("已读", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 全部已读
     */
    @PutMapping("/read-all")
    public Result<Void> readAll() {
        try {
            String me = UserContext.get();
            List<Message> unread = messageMapper.selectList(
                    new LambdaQueryWrapper<Message>()
                            .eq(Message::getReceiver, me)
                            .eq(Message::getIsRead, 0));
            LocalDateTime now = LocalDateTime.now();
            for (Message msg : unread) {
                msg.setIsRead(1);
                msg.setReadTime(now);
                messageMapper.updateById(msg);
            }
            return Result.success("已全部标记为已读", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除自己收件箱里的消息
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Long id) {
        try {
            String me = UserContext.get();
            Message msg = messageMapper.selectById(id);
            if (msg == null || !me.equals(msg.getReceiver())) {
                return Result.error("消息不存在");
            }
            messageMapper.deleteById(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 可选收件人列表（管理员发消息用）
     */
    @RequireRole("admin")
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> users() {
        try {
            List<User> users = userMapper.selectList(
                    new LambdaQueryWrapper<User>().orderByAsc(User::getUsername));
            List<Map<String, Object>> rows = users.stream().map(u -> {
                Map<String, Object> row = new HashMap<>();
                row.put("username", u.getUsername());
                row.put("nickname", u.getNickname());
                row.put("role", u.getRole());
                return row;
            }).collect(Collectors.toList());
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 发送站内信（管理员）：receivers 为用户名数组，["all"] 表示全员
     */
    @RequireRole("admin")
    @AuditLog(module = "站内信", action = "发送")
    @PostMapping("/send")
    public Result<Map<String, Object>> send(@RequestBody Map<String, Object> body) {
        try {
            String title = Objects.toString(body.get("title"), "").trim();
            String content = Objects.toString(body.get("content"), "").trim();
            String msgType = Objects.toString(body.get("msgType"), "系统").trim();
            Object receiversObj = body.get("receivers");

            if (title.isEmpty()) {
                return Result.error("标题不能为空");
            }
            if (content.isEmpty()) {
                return Result.error("内容不能为空");
            }
            if (!(receiversObj instanceof List<?> receivers) || receivers.isEmpty()) {
                return Result.error("请选择接收人");
            }

            String me = UserContext.getOrDefault("system");
            List<String> targets;
            if (receivers.contains("all")) {
                // 全员发送：只发给状态正常的用户
                targets = userMapper.selectList(
                                new LambdaQueryWrapper<User>()
                                        .and(w -> w.isNull(User::getStatus).or().eq(User::getStatus, 1)))
                        .stream().map(User::getUsername).distinct().collect(Collectors.toList());
            } else {
                targets = receivers.stream().map(String::valueOf).distinct().collect(Collectors.toList());
            }

            LocalDateTime now = LocalDateTime.now();
            for (String receiver : targets) {
                Message msg = new Message();
                msg.setReceiver(receiver);
                msg.setTitle(title);
                msg.setContent(content);
                msg.setMsgType(msgType);
                msg.setSender(me);
                msg.setIsRead(0);
                msg.setCreateTime(now);
                msg.setUpdateTime(now);
                messageMapper.insert(msg);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("count", targets.size());
            return Result.success("发送成功", data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 列表附带发送人昵称
     */
    private List<Map<String, Object>> fillSender(List<Message> messages) {
        List<String> senders = messages.stream().map(Message::getSender)
                .filter(Objects::nonNull).distinct().toList();
        Map<String, User> userMap = senders.isEmpty() ? Collections.emptyMap()
                : userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getUsername, senders))
                        .stream().collect(Collectors.toMap(User::getUsername, u -> u, (a, b) -> a));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Message m : messages) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", m.getId());
            row.put("title", m.getTitle());
            row.put("content", m.getContent());
            row.put("msgType", m.getMsgType());
            row.put("sender", m.getSender());
            User senderUser = m.getSender() == null ? null : userMap.get(m.getSender());
            row.put("senderName", senderUser != null && senderUser.getNickname() != null
                    ? senderUser.getNickname() : m.getSender());
            row.put("isRead", m.getIsRead());
            row.put("readTime", m.getReadTime());
            row.put("createTime", m.getCreateTime());
            rows.add(row);
        }
        return rows;
    }
}
