package com.syty.common;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
/**
 * 全局异常处理
 * - 业务异常返回详细消息
 * - 系统异常隐藏 SQL 细节，只返回通用错误提示
 * - 权限异常统一格式返回
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /* ── 权限/认证异常（正常业务流，info 级别）── */
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleNotLogin(NotLoginException e) {
        return Result.unauthorized("未登录或登录已过期，请重新登录");
    }
    @ExceptionHandler(NotPermissionException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotPermission(NotPermissionException e) {
        log.warn("权限不足: {}", e.getPermission());
        return Result.error(403, "权限不足，无法执行此操作");
    }
    @ExceptionHandler(NotRoleException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleNotRole(NotRoleException e) {
        log.warn("角色不足: {}", e.getRole());
        return Result.error(403, "角色权限不足");
    }
    /* ── 客户端错误（400 级别）── */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(400, e.getMessage());
    }
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(400, "缺少必要参数: " + e.getParameterName());
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("RequestBody")) {
            return Result.error(400, "请求参数格式错误，请检查输入");
        }
        return Result.error(400, "请求体解析失败");
    }
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        return Result.error(400, "参数校验失败: " + e.getMessage());
    }
    /* ── 自定义业务异常── */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        return Result.error(404, "请求的资源不存在");
    }
    /* ── 数据库异常（隐藏 SQL 细节，只记日志） ── */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result<Void> handleDataIntegrity(DataIntegrityViolationException e, HttpServletRequest request) {
        log.error("数据完整性异常[{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(409, "数据冲突，操作失败");
    }
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        String msg = e.getMessage();
        // 检测是否为数据库异常（SQL/Constraint 相关）
        if (msg != null && (msg.contains("SQL") || msg.contains("PSQL") || msg.contains("constraint")
                || msg.contains("column") || msg.contains("relation") || msg.contains("table"))) {
            log.error("数据库异常[{}]: {}", request.getRequestURI(), msg, e);
            return Result.error(500, "数据库操作失败，请联系管理员");
        }
        // 兜底：所有未捕获的异常
        log.error("系统异常 [{}]: {}", request.getRequestURI(), msg, e);
        return Result.error(500, "系统内部错误，请稍后重试");
    }
}
