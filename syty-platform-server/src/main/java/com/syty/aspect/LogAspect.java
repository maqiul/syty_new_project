package com.syty.aspect;
import com.syty.common.TenantContext;
import com.syty.entity.OperateLog;
import com.syty.mapper.OperateLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Arrays;
/**
 * 操作日志切面 - 异步记录
 * 不阻塞主业务线程
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {
    private final OperateLogMapper operateLogMapper;
    @Pointcut("execution(* com.syty.controller.*.*(..)) "
            + "&& !execution(* com.syty.controller.AuthController.*(..)) "
            + "&& !execution(* com.syty.controller.FileUploadController.*(..))")
    public void controllerPointcut() {}
    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Object result) {
        // 异步执行，不阻塞主线程
        recordLogAsync(joinPoint);
    }
    @Async
    public void recordLogAsync(JoinPoint joinPoint) {
        try {
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            String module = className.replace("Controller", "");
            String operation = methodName;
            // 跳过GET查询（不记录读操作）
            if (methodName.startsWith("get") || methodName.startsWith("page") || methodName.startsWith("list")
                    || methodName.startsWith("detail")) {
                return;
            }
            // 跳过导出
            if ("export".equals(methodName)) return;
            Long userId = TenantContext.getUserId();
            String username = TenantContext.getUsername();
            if (userId == null) return;
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return;
            HttpServletRequest request = attrs.getRequest();
            OperateLog logEntry = new OperateLog();
            logEntry.setTenantId(TenantContext.getTenantId());
            logEntry.setUserId(userId);
            logEntry.setUsername(username);
            logEntry.setModule(module);
            logEntry.setOperation(operation);
            logEntry.setTargetType(module.toLowerCase());
            logEntry.setIp(request.getRemoteAddr());
            // 从参数中提取目标ID
            Object[] args = joinPoint.getArgs();
            for (Object arg : args) {
                if (arg instanceof Long) {
                    logEntry.setTargetId((Long) arg);
                    break;
                }
                if (arg instanceof String && ((String) arg).matches("\\d+")) {
                    logEntry.setTargetId(Long.valueOf((String) arg));
                    break;
                }
            }
            logEntry.setDetail(module + "." + operation);
            operateLogMapper.insert(logEntry);
        } catch (Exception e) {
            // 日志记录失败不影响主业务
            log.error("操作日志记录失败", e);
        }
    }
}
