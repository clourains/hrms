package com.hrms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.dto.ImportResult;
import com.hrms.entity.ImportLog;
import com.hrms.mapper.ImportLogMapper;
import com.hrms.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量导入审计：只记录「谁、从哪、导了多少」，不落具体业务字段值
 */
@Service
public class ImportAuditService {

    private static final int MAX_FILE_NAME_LENGTH = 200;

    @Autowired
    private ImportLogMapper importLogMapper;

    @Autowired
    private DataImportService dataImportService;

    /**
     * 记录一次导入。审计写失败不能影响业务导入，所以这里吞掉异常。
     */
    public void record(String module, String source, String fileName, ImportResult result) {
        try {
            ImportLog log = new ImportLog();
            log.setModule(module);
            log.setModuleLabel(dataImportService.moduleLabel(module));
            log.setSource(source);
            log.setOperator(UserContext.getOrDefault("unknown"));
            log.setIp(clientIp());
            if (StringUtils.hasText(fileName)) {
                log.setFileName(fileName.length() > MAX_FILE_NAME_LENGTH
                        ? fileName.substring(0, MAX_FILE_NAME_LENGTH) : fileName);
            }
            log.setTotal(result.getTotal());
            log.setSuccess(result.getSuccess());
            log.setSkipped(result.getSkipped());
            log.setFailed(result.getFailed());
            log.setCreateTime(LocalDateTime.now());
            importLogMapper.insert(log);
        } catch (Exception ignored) {
            // 审计失败不影响本次导入结果
        }
    }

    /**
     * 最近导入记录（按模块过滤，module 为空则查全部）
     */
    public List<ImportLog> recent(String module, int limit) {
        LambdaQueryWrapper<ImportLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(module)) {
            wrapper.eq(ImportLog::getModule, module);
        }
        wrapper.orderByDesc(ImportLog::getId).last("LIMIT " + Math.max(1, Math.min(limit, 50)));
        return importLogMapper.selectList(wrapper);
    }

    /**
     * 取调用方 IP：优先反向代理头，取第一个地址
     */
    private String clientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwarded)) {
                String first = forwarded.split(",")[0].trim();
                return first.length() > 64 ? first.substring(0, 64) : first;
            }
            String realIp = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(realIp)) {
                return realIp.length() > 64 ? realIp.substring(0, 64) : realIp;
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
