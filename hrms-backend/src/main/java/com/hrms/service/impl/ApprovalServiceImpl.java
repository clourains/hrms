package com.hrms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.hrms.entity.ApprovalInstance;
import com.hrms.entity.ApprovalRecord;
import com.hrms.entity.Employee;
import com.hrms.entity.Message;
import com.hrms.entity.Overtime;
import com.hrms.entity.User;
import com.hrms.mapper.ApprovalInstanceMapper;
import com.hrms.mapper.ApprovalRecordMapper;
import com.hrms.mapper.AttendanceMapper;
import com.hrms.mapper.EmployeeMapper;
import com.hrms.mapper.LeaveQuotaMapper;
import com.hrms.mapper.MessageMapper;
import com.hrms.mapper.OvertimeMapper;
import com.hrms.mapper.UserMapper;
import com.hrms.service.ApprovalService;
import com.hrms.service.LeaveQuotaService;
import com.hrms.util.AttendanceStatus;
import com.hrms.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
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

    private static final Logger log = LoggerFactory.getLogger(ApprovalServiceImpl.class);

    @Autowired
    private ApprovalInstanceMapper instanceMapper;

    @Autowired
    private AttendanceMapper attendanceMapper;

    @Autowired
    private EmployeeMapper employeeMapper;

    @Autowired
    private ApprovalRecordMapper recordMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private OvertimeMapper overtimeMapper;

    @Autowired
    private LeaveQuotaService leaveQuotaService;

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
                // P4-1/P4-2 业务联动：加班单生效、请假扣减额度
                onApproved(inst);
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
            // P4-1 联动：加班单同步为已驳回
            syncOvertimeStatus(inst.getId(), 2);

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
            // P4-1 联动：加班单同步为已撤销
            syncOvertimeStatus(inst.getId(), 3);
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
     * P4-1/P4-2 审批全部通过后的业务联动：
     * 加班单置为已生效（薪资计算取数）；请假按 detail.days 扣减年假额度。
     */
    private void onApproved(ApprovalInstance inst) {
        try {
            if ("加班".equals(inst.getBizType())) {
                syncOvertimeStatus(inst.getId(), 1);
            } else if ("请假".equals(inst.getBizType())) {
                handleLeaveApproved(inst);
            } else if ("调薪".equals(inst.getBizType())) {
                applySalaryAdjustment(inst);
            }
        } catch (Exception e) {
            // 业务联动失败不阻断审批主流程，但不能再静默吞掉：
            // 否则会出现「审批显示已通过、实际什么都没发生」且无人知晓的情况
            log.error("审批通过后的业务联动失败，instanceId={}, bizType={}",
                    inst.getId(), inst.getBizType(), e);
            notifyLinkageFailure(inst, e.getMessage());
        }
    }

    /**
     * 调薪审批通过 → 回写 sys_employee.salary。
     *
     * 原先这一步完全缺失：调薪单走完所有审批节点、界面显示「已通过」、站内信也发了，
     * 但员工薪资纹丝不动，下个月一键核算还是按老底薪算，用户会认为系统在骗人。
     */
    private void applySalaryAdjustment(ApprovalInstance inst) {
        JsonNode detail = parseJson(inst.getDetail());
        if (detail == null) {
            throw new RuntimeException("调薪单缺少明细（需包含员工与调整后月薪）");
        }
        BigDecimal newSalary = detail.path("newSalary").decimalValue();
        if (newSalary == null || newSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("调薪单未填写有效的调整后月薪");
        }
        long empId = detail.path("empId").asLong(0L);
        if (empId <= 0) {
            // 没指定员工时按申请人本人处理
            empId = resolveEmpId(inst.getApplicant());
        }
        Employee emp = employeeMapper.selectById(empId);
        if (emp == null) {
            throw new RuntimeException("调薪员工不存在，id=" + empId);
        }
        BigDecimal oldSalary = emp.getSalary();
        emp.setSalary(newSalary);
        emp.setUpdateTime(LocalDateTime.now());
        employeeMapper.updateById(emp);

        sendMsg(inst.getApplicant(), "调薪已生效",
                "调薪审批已通过：" + emp.getName() + " 的月薪由 "
                        + (oldSalary == null ? "未设置" : oldSalary + " 元") + " 调整为 "
                        + newSalary + " 元，后续薪资核算按新基数执行。",
                inst.getTitle());
    }

    /** 业务联动失败时给申请人发站内信，让失败可见 */
    private void notifyLinkageFailure(ApprovalInstance inst, String reason) {
        sendMsg(inst.getApplicant(), "审批业务联动失败",
                "审批单已通过，但后续数据处理失败：" + (reason == null ? "未知原因" : reason)
                        + "。请联系管理员手工处理，否则本次审批不会体现在考勤/薪资中。",
                inst.getTitle());
    }

    /**
     * 按审批实例ID同步加班单状态：1已通过 2已驳回 3已撤销
     */
    private void syncOvertimeStatus(Long instanceId, int status) {
        Overtime ot = overtimeMapper.selectOne(new LambdaQueryWrapper<Overtime>()
                .eq(Overtime::getInstanceId, instanceId)
                .last("LIMIT 1"));
        if (ot != null && ot.getStatus() != 1) {
            ot.setStatus(status);
            ot.setUpdateTime(LocalDateTime.now());
            overtimeMapper.updateById(ot);
        }
    }

    /**
     * 请假审批通过后的业务联动：
     * 1) 按起止日期逐日写考勤记录（status=5 请假）—— 这一步原先缺失，
     *    导致请假期间没有考勤记录，薪资计算把它当成缺勤再扣一次工资；
     * 2) 年假类型额外扣减年假额度（事假/病假等不占年假池）。
     *
     * 这里失败会抛异常，由 onApproved 统一捕获并留痕，不再静默返回。
     */
    private void handleLeaveApproved(ApprovalInstance inst) {
        JsonNode detail = parseJson(inst.getDetail());
        String leaveType = detail == null ? "年假" : detail.path("leaveType").asText("年假");
        if (leaveType.isEmpty()) {
            leaveType = "年假";
        }
        String reason = detail == null ? "" : detail.path("reason").asText("");
        LocalDate start = parseDate(detail == null ? "" : detail.path("startDate").asText(""));
        LocalDate end = parseDate(detail == null ? "" : detail.path("endDate").asText(""));

        Long empId = resolveEmpId(inst.getApplicant());
        if (start == null && end == null) {
            throw new RuntimeException("请假单缺少起止日期，无法写入考勤");
        }
        if (start == null) {
            start = end;
        }
        if (end == null) {
            end = start;
        }
        if (end.isBefore(start)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }

        // 1) 写考勤：只写工作日，与薪资的 countWorkDays 口径一致
        writeLeaveAttendance(empId, start, end, leaveType, reason);

        // 2) 年假扣池（带余额校验，不足直接失败，避免超额扣减无人知晓）
        if (LeaveQuotaService.TYPE_ANNUAL.equals(leaveType)) {
            BigDecimal days = detail == null ? null : detail.path("days").decimalValue();
            if (days == null || days.compareTo(BigDecimal.ZERO) <= 0) {
                days = BigDecimal.valueOf(countWorkDays(start, end));
            }
            int year = start.getYear();
            BigDecimal remain = leaveQuotaService.remainDays(empId, year, LeaveQuotaService.TYPE_ANNUAL);
            if (remain != null && days.compareTo(remain) > 0) {
                throw new RuntimeException("年假余额不足：剩余 " + remain + " 天，本次申请 " + days + " 天");
            }
            leaveQuotaService.deduct(empId, year, LeaveQuotaService.TYPE_ANNUAL, days);
        }
    }

    /** 按申请人账号反查员工ID；找不到直接抛异常，避免「审批通过但额度/考勤都没动」 */
    private Long resolveEmpId(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || user.getEmpId() == null) {
            throw new RuntimeException("账号 " + username + " 未绑定员工档案，无法完成业务联动");
        }
        return user.getEmpId();
    }

    /** 逐日写入请假考勤记录（跳过周末） */
    private void writeLeaveAttendance(Long empId, LocalDate start, LocalDate end, String leaveType, String reason) {
        String remark = leaveType + (reason == null || reason.isEmpty() ? "" : "：" + reason);
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek w = d.getDayOfWeek();
            if (w == DayOfWeek.SATURDAY || w == DayOfWeek.SUNDAY) {
                continue;
            }
            attendanceMapper.upsertNonCheckIn(empId, d, AttendanceStatus.LEAVE, remark);
        }
    }

    private long countWorkDays(LocalDate start, LocalDate end) {
        long count = 0;
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayOfWeek w = d.getDayOfWeek();
            if (w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY) {
                count++;
            }
        }
        return count;
    }

    private LocalDate parseDate(String text) {
        if (text == null || text.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(text.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode parseJson(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return JsonMapper.builder().build().readTree(text);
        } catch (Exception e) {
            return null;
        }
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
