package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.entity.ApprovalInstance;
import com.hrms.entity.ApprovalRecord;
import com.hrms.entity.Message;
import com.hrms.entity.User;
import com.hrms.mapper.ApprovalInstanceMapper;
import com.hrms.mapper.ApprovalRecordMapper;
import com.hrms.mapper.MessageMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.service.ApprovalService;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 多级审批流实现（P3-1）
 *
 * 状态机：实例 status 0审批中/1通过/2驳回/3撤销；节点 record status 0待审/1通过/2驳回。
 * 当前节点定义：instance.currentNode 指向的 record 才可操作；
 * 每个关键动作通过站内信（msg_type=审批）触达相关人。
 */
@Service
public class ApprovalServiceImpl implements ApprovalService {

    private static final int MAX_NODES = 3;

    @Autowired
    private ApprovalInstanceMapper instanceMapper;

    @Autowired
    private ApprovalRecordMapper recordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> apply(String me, Map<String, Object> body) {
        try {
            String bizType = Objects.toString(body.get("bizType"), "通用").trim();
            String title = Objects.toString(body.get("title"), "").trim();
            Object detailObj = body.get("detail");
            String detail = detailObj == null ? "" : (detailObj instanceof Map
                    ? com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(detailObj)
                    : Objects.toString(detailObj, ""));
            Object approversObj = body.get("approvers");

            if (title.isEmpty()) {
                return Result.error("标题不能为空");
            }
            if (!(approversObj instanceof List<?> rawApprovers) || rawApprovers.isEmpty()) {
                return Result.error("请选择审批人");
            }
            List<String> approvers = rawApprovers.stream()
                    .map(String::valueOf).distinct().collect(Collectors.toList());
            if (approvers.size() > MAX_NODES) {
                return Result.error("最多支持 " + MAX_NODES + " 级审批");
            }
            if (approvers.contains(me)) {
                return Result.error("审批人不能包含申请人本人");
            }
            // 校验审批人账号存在
            List<User> approverUsers = userMapper.selectList(
                    new LambdaQueryWrapper<User>().in(User::getUsername, approvers));
            if (approverUsers.size() != approvers.size()) {
                return Result.error("存在无效的审批人账号");
            }
            Map<String, User> userMap = approverUsers.stream()
                    .collect(Collectors.toMap(User::getUsername, u -> u, (a, b) -> a));
            User meUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, me));

            LocalDateTime now = LocalDateTime.now();
            ApprovalInstance inst = new ApprovalInstance();
            inst.setBizType(bizType);
            inst.setTitle(title);
            inst.setDetail(detail);
            inst.setApplicant(me);
            inst.setApplicantName(meUser != null && meUser.getNickname() != null ? meUser.getNickname() : me);
            inst.setStatus(0);
            inst.setCurrentNode(1);
            inst.setNodeCount(approvers.size());
            inst.setCreateTime(now);
            inst.setUpdateTime(now);
            instanceMapper.insert(inst);

            for (int i = 0; i < approvers.size(); i++) {
                ApprovalRecord rec = new ApprovalRecord();
                rec.setInstanceId(inst.getId());
                rec.setNodeOrder(i + 1);
                rec.setApprover(approvers.get(i));
                User au = userMap.get(approvers.get(i));
                rec.setApproverName(au != null && au.getNickname() != null ? au.getNickname() : approvers.get(i));
                rec.setStatus(0);
                rec.setCreateTime(now);
                recordMapper.insert(rec);
            }

            // 通知第一级审批人
            sendMsg(approvers.get(0), "您有新的审批待处理",
                    meUser != null && meUser.getNickname() != null ? meUser.getNickname() : me
                            + " 提交了「" + bizType + "」申请：" + title, title);

            return Result.success("提交成功", inst.getId());
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> approve(String me, Long instanceId, String comment) {
        try {
            ApprovalInstance inst = instanceMapper.selectById(instanceId);
            if (inst == null || inst.getStatus() != 0) {
                return Result.error("审批单不存在或已结束");
            }
            ApprovalRecord current = currentRecord(inst);
            if (current == null) {
                return Result.error("审批数据异常");
            }
            if (!me.equals(current.getApprover())) {
                return Result.error("当前节点由 " + current.getApproverName() + " 审批");
            }
            current.setStatus(1);
            current.setComment(comment);
            current.setApproveTime(LocalDateTime.now());
            recordMapper.updateById(current);

            if (inst.getCurrentNode() >= inst.getNodeCount()) {
                inst.setStatus(1);
                inst.setUpdateTime(LocalDateTime.now());
                instanceMapper.updateById(inst);
                sendMsg(inst.getApplicant(), "您的审批已通过",
                        "「" + inst.getTitle() + "」全部 " + inst.getNodeCount() + " 级审批已通过。", inst.getTitle());
            } else {
                inst.setCurrentNode(inst.getCurrentNode() + 1);
                inst.setUpdateTime(LocalDateTime.now());
                instanceMapper.updateById(inst);
                ApprovalRecord next = currentRecord(inst);
                if (next != null) {
                    sendMsg(next.getApprover(), "您有新的审批待处理",
                            "「" + inst.getTitle() + "」已通过第 " + current.getNodeOrder() + " 级审批，等待您处理。",
                            inst.getTitle());
                }
                sendMsg(inst.getApplicant(), "审批进度更新",
                        "「" + inst.getTitle() + "」第 " + current.getNodeOrder() + " 级已通过，当前第 "
                                + inst.getCurrentNode() + " 级审批中。", inst.getTitle());
            }
            return Result.success("已通过", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> reject(String me, Long instanceId, String comment) {
        try {
            ApprovalInstance inst = instanceMapper.selectById(instanceId);
            if (inst == null || inst.getStatus() != 0) {
                return Result.error("审批单不存在或已结束");
            }
            ApprovalRecord current = currentRecord(inst);
            if (current == null) {
                return Result.error("审批数据异常");
            }
            if (!me.equals(current.getApprover())) {
                return Result.error("当前节点由 " + current.getApproverName() + " 审批");
            }
            current.setStatus(2);
            current.setComment(comment);
            current.setApproveTime(LocalDateTime.now());
            recordMapper.updateById(current);

            inst.setStatus(2);
            inst.setUpdateTime(LocalDateTime.now());
            instanceMapper.updateById(inst);

            sendMsg(inst.getApplicant(), "您的审批被驳回",
                    "「" + inst.getTitle() + "」在第 " + current.getNodeOrder() + " 级被驳回"
                            + (comment == null || comment.isEmpty() ? "。" : "，意见：" + comment), inst.getTitle());
            return Result.success("已驳回", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> cancel(String me, Long instanceId) {
        try {
            ApprovalInstance inst = instanceMapper.selectById(instanceId);
            if (inst == null) {
                return Result.error("审批单不存在");
            }
            if (!me.equals(inst.getApplicant())) {
                return Result.error("只有申请人可以撤销");
            }
            if (inst.getStatus() != 0) {
                return Result.error("仅审批中的申请可撤销");
            }
            ApprovalRecord current = currentRecord(inst);
            inst.setStatus(3);
            inst.setUpdateTime(LocalDateTime.now());
            instanceMapper.updateById(inst);
            if (current != null) {
                sendMsg(current.getApprover(), "审批申请已撤销",
                        inst.getApplicantName() + " 撤销了「" + inst.getTitle() + "」，无需再处理。", inst.getTitle());
            }
            return Result.success("已撤销", null);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public Result<List<Map<String, Object>>> myApply(String me) {
        try {
            List<ApprovalInstance> list = instanceMapper.selectList(
                    new LambdaQueryWrapper<ApprovalInstance>()
                            .eq(ApprovalInstance::getApplicant, me)
                            .orderByDesc(ApprovalInstance::getCreateTime));
            return Result.success(fill(list, me));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public Result<List<Map<String, Object>>> todo(String me) {
        try {
            // 当前轮到我的节点
            List<ApprovalRecord> myPending = recordMapper.selectList(
                    new LambdaQueryWrapper<ApprovalRecord>()
                            .eq(ApprovalRecord::getApprover, me)
                            .eq(ApprovalRecord::getStatus, 0)
                            .orderByAsc(ApprovalRecord::getId));
            List<Long> instIds = myPending.stream()
                    .filter(r -> r.getNodeOrder() != null)
                    .map(ApprovalRecord::getInstanceId).distinct().collect(Collectors.toList());
            List<Map<String, Object>> rows = new ArrayList<>();
            if (!instIds.isEmpty()) {
                List<ApprovalInstance> insts = instanceMapper.selectList(
                        new LambdaQueryWrapper<ApprovalInstance>()
                                .in(ApprovalInstance::getId, instIds)
                                .eq(ApprovalInstance::getStatus, 0)
                                .orderByDesc(ApprovalInstance::getCreateTime));
                for (ApprovalInstance inst : insts) {
                    boolean isCurrent = myPending.stream().anyMatch(r ->
                            r.getInstanceId().equals(inst.getId())
                                    && r.getNodeOrder().equals(inst.getCurrentNode()));
                    if (isCurrent) {
                        rows.addAll(fill(List.of(inst), me));
                    }
                }
            }
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public Result<List<Map<String, Object>>> done(String me) {
        try {
            List<ApprovalRecord> handled = recordMapper.selectList(
                    new LambdaQueryWrapper<ApprovalRecord>()
                            .eq(ApprovalRecord::getApprover, me)
                            .ne(ApprovalRecord::getStatus, 0)
                            .orderByDesc(ApprovalRecord::getApproveTime));
            List<Long> instIds = handled.stream().map(ApprovalRecord::getInstanceId)
                    .distinct().collect(Collectors.toList());
            List<Map<String, Object>> rows = new ArrayList<>();
            if (!instIds.isEmpty()) {
                List<ApprovalInstance> insts = instanceMapper.selectList(
                        new LambdaQueryWrapper<ApprovalInstance>()
                                .in(ApprovalInstance::getId, instIds)
                                .orderByDesc(ApprovalInstance::getCreateTime));
                rows.addAll(fill(insts, me));
            }
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> detail(String me, Long id) {
        try {
            ApprovalInstance inst = instanceMapper.selectById(id);
            if (inst == null) {
                return Result.error("审批单不存在");
            }
            boolean isApplicant = me.equals(inst.getApplicant());
            List<ApprovalRecord> records = recordMapper.selectList(
                    new LambdaQueryWrapper<ApprovalRecord>()
                            .eq(ApprovalRecord::getInstanceId, id)
                            .orderByAsc(ApprovalRecord::getNodeOrder));
            boolean isApprover = records.stream().anyMatch(r -> me.equals(r.getApprover()));
            if (!isApplicant && !isApprover) {
                return Result.error("无权查看该审批单");
            }
            Map<String, Object> data = new HashMap<>();
            data.put("instance", toRow(inst, records));
            data.put("records", records.stream().map(this::toRecordRow).collect(Collectors.toList()));
            data.put("canApprove", isApplicant ? false : records.stream().anyMatch(r ->
                    me.equals(r.getApprover()) && r.getStatus() == 0
                            && r.getNodeOrder().equals(inst.getCurrentNode()) && inst.getStatus() == 0));
            data.put("canCancel", isApplicant && inst.getStatus() == 0);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 取当前节点的待审记录
     */
    private ApprovalRecord currentRecord(ApprovalInstance inst) {
        return recordMapper.selectOne(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getInstanceId, inst.getId())
                .eq(ApprovalRecord::getNodeOrder, inst.getCurrentNode())
                .eq(ApprovalRecord::getStatus, 0)
                .last("LIMIT 1"));
    }

    /**
     * 列表行：实例信息 + 节点进度
     */
    private List<Map<String, Object>> fill(List<ApprovalInstance> insts, String me) {
        if (insts.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = insts.stream().map(ApprovalInstance::getId).collect(Collectors.toList());
        Map<Long, List<ApprovalRecord>> recMap = recordMapper.selectList(
                        new LambdaQueryWrapper<ApprovalRecord>()
                                .in(ApprovalRecord::getInstanceId, ids)
                                .orderByAsc(ApprovalRecord::getNodeOrder))
                .stream().collect(Collectors.groupingBy(ApprovalRecord::getInstanceId));
        return insts.stream().map(i -> toRow(i, recMap.getOrDefault(i.getId(), List.of()))).collect(Collectors.toList());
    }

    private Map<String, Object> toRow(ApprovalInstance inst, List<ApprovalRecord> records) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", inst.getId());
        row.put("bizType", inst.getBizType());
        row.put("title", inst.getTitle());
        row.put("detail", inst.getDetail());
        row.put("applicant", inst.getApplicant());
        row.put("applicantName", inst.getApplicantName());
        row.put("status", inst.getStatus());
        row.put("currentNode", inst.getCurrentNode());
        row.put("nodeCount", inst.getNodeCount());
        row.put("createTime", inst.getCreateTime());
        row.put("updateTime", inst.getUpdateTime());
        row.put("records", records.stream().map(this::toRecordRow).collect(Collectors.toList()));
        return row;
    }

    private Map<String, Object> toRecordRow(ApprovalRecord rec) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", rec.getId());
        row.put("nodeOrder", rec.getNodeOrder());
        row.put("approver", rec.getApprover());
        row.put("approverName", rec.getApproverName());
        row.put("status", rec.getStatus());
        row.put("comment", rec.getComment());
        row.put("approveTime", rec.getApproveTime());
        return row;
    }

    /**
     * 审批联动站内信（P2-1）
     */
    private void sendMsg(String receiver, String title, String content, String refTitle) {
        try {
            Message msg = new Message();
            msg.setReceiver(receiver);
            msg.setTitle(title);
            msg.setContent(content + (refTitle == null || refTitle.isEmpty() ? "" : "（" + refTitle + "）"));
            msg.setMsgType("审批");
            msg.setSender("system");
            msg.setIsRead(0);
            msg.setCreateTime(LocalDateTime.now());
            msg.setUpdateTime(LocalDateTime.now());
            messageMapper.insert(msg);
        } catch (Exception ignored) {
            // 站内信失败不影响审批主流程
        }
    }
}
