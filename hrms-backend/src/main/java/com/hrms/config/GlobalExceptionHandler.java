package com.hrms.config;

import com.hrms.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全局异常处理（P024）
 *
 * 以前每个 Controller 各自 catch 后把 e.getMessage() 原样抛给前端，
 * 用户会看到 "Duplicate entry 'WH-01' for key 'uk_warehouse_code'" 这种带表名的英文报错。
 * 这里统一翻译成人话，并且不再把 SQL 细节暴露给页面。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 从 "Duplicate entry 'XXX' for key 'uk_xxx'" 里提取重复值和约束名 */
    private static final Pattern DUPLICATE_PATTERN =
            Pattern.compile("Duplicate entry '(.+?)' for key '(.+?)'");

    @ExceptionHandler(DuplicateKeyException.class)
    public Result<Void> handleDuplicateKey(DuplicateKeyException e) {
        String raw = e.getMessage() == null ? "" : e.getMessage();
        Matcher m = DUPLICATE_PATTERN.matcher(raw);
        if (m.find()) {
            String value = m.group(1);
            String key = m.group(2).toLowerCase();
            String field = key.contains("code") ? "编码"
                    : key.contains("sku") ? "SKU"
                    : key.contains("order_no") ? "订单号"
                    : key.contains("shipment_no") ? "发货单号"
                    : key.contains("payment_no") ? "收付款单号"
                    : "名称";
            return Result.error("该" + field + "「" + value + "」已存在，请换一个再试");
        }
        return Result.error("数据重复，请检查编码是否已被占用");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        String raw = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (raw.contains("data too long") || raw.contains("out of range")) {
            return Result.error("输入内容超出字段长度限制，请缩短后重试");
        }
        if (raw.contains("cannot be null")) {
            return Result.error("必填项不能为空");
        }
        log.warn("数据完整性异常", e);
        return Result.error("数据不符合约束要求，请检查后重试");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        return Result.error(msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError == null ? "参数校验失败" : fieldError.getDefaultMessage();
        return Result.error(msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error("缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.error("提交的数据格式不正确，请刷新页面后重试");
    }

    /**
     * 业务异常：Service 里主动抛的 RuntimeException 大多是给用户看的提示，原样返回
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntime(RuntimeException e) {
        String msg = e.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            log.error("运行时异常", e);
            return Result.error("操作失败，请稍后重试");
        }
        // SQL 异常混进业务提示时，统一换成友好文案
        String lower = msg.toLowerCase();
        if (lower.contains("sql") || lower.contains("jdbc") || lower.contains("duplicate entry")) {
            log.warn("业务异常包含 SQL 细节，已转换：{}", msg);
            return Result.error("数据保存失败，请检查输入内容是否重复或过长");
        }
        return Result.error(msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未处理的异常", e);
        return Result.error("服务器处理失败，请稍后重试");
    }
}
