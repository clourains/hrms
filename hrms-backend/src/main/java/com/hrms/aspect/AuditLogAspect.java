package com.hrms.aspect;

import com.hrms.annotation.AuditLog;
import com.hrms.entity.AuditLogRecord;
import com.hrms.mapper.AuditLogRecordMapper;
import com.hrms.util.Result;
import com.hrms.util.UserContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 审计日志切面（P1-1）
 *
 * 环绕拦截标注 @AuditLog 的 Controller 方法：
 * - Controller 内部通常自行 catch 异常返回 Result.error，因此成功与否以返回的 Result.code 判断；
 * - 方法本身抛出异常（未被 catch）时同样记为失败并向上抛出；
 * - 审计落库失败绝不影响主业务流程。
 */
@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private static final int DETAIL_MAX_LENGTH = 900;
    private static final int ERROR_MAX_LENGTH = 490;

    @Autowired
    private AuditLogRecordMapper auditLogRecordMapper;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            boolean success = isSuccess(result);
            String errorMsg = success ? null : extractMessage(result);
            save(auditLog, joinPoint, success, errorMsg, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            save(auditLog, joinPoint, false, e.getMessage(), System.currentTimeMillis() - start);
            throw e;
        }
    }

    private boolean isSuccess(Object result) {
        if (result instanceof Result<?> r) {
            return r.getCode() != null && r.getCode() == 200;
        }
        return true;
    }

    private String extractMessage(Object result) {
        if (result instanceof Result<?> r && r.getMessage() != null) {
            String msg = r.getMessage();
            return msg.length() > ERROR_MAX_LENGTH ? msg.substring(0, ERROR_MAX_LENGTH) : msg;
        }
        return "操作失败";
    }

    private void save(AuditLog auditLog, ProceedingJoinPoint joinPoint,
                      boolean success, String errorMsg, long costMs) {
        try {
            AuditLogRecord record = new AuditLogRecord();
            record.setUsername(UserContext.getOrDefault("unknown"));
            record.setModule(auditLog.module());
            record.setAction(auditLog.action());
            record.setDetail(buildDetail(joinPoint.getArgs()));
            record.setSuccess(success ? 1 : 0);
            record.setErrorMsg(errorMsg);
            record.setCostMs(costMs);
            record.setIp(resolveIp());
            record.setCreateTime(LocalDateTime.now());
            auditLogRecordMapper.insert(record);
        } catch (Exception ex) {
            // 审计日志绝不能拖垮业务，只记录告警
            log.warn("审计日志写入失败: module={}, action={}", auditLog.module(), auditLog.action(), ex);
        }
    }

    private String buildDetail(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            String detail = Arrays.toString(args);
            return detail.length() > DETAIL_MAX_LENGTH
                    ? detail.substring(0, DETAIL_MAX_LENGTH) + "...(截断)"
                    : detail;
        } catch (Exception e) {
            return "(参数序列化失败)";
        }
    }

    private String resolveIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                int idx = ip.indexOf(',');
                return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
