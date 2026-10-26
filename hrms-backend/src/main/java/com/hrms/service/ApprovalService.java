package com.hrms.service;

import com.hrms.util.Result;

import java.util.Map;

/**
 * 多级审批流服务（P3-1）
 */
public interface ApprovalService {

    /** 发起审批：approvers 为逐级审批人用户名（1-3 级） */
    Result<Long> apply(String me, Map<String, Object> body);

    /** 通过当前节点 */
    Result<Void> approve(String me, Long instanceId, String comment);

    /** 驳回（当前节点驳回即整单驳回） */
    Result<Void> reject(String me, Long instanceId, String comment);

    /** 申请人撤销（仅审批中） */
    Result<Void> cancel(String me, Long instanceId);

    /** 我发起的审批 */
    Result<java.util.List<Map<String, Object>>> myApply(String me);

    /** 待我审批 */
    Result<java.util.List<Map<String, Object>>> todo(String me);

    /** 我已处理的审批 */
    Result<java.util.List<Map<String, Object>>> done(String me);

    /** 审批详情（申请人/审批人可见） */
    Result<Map<String, Object>> detail(String me, Long id);
}
