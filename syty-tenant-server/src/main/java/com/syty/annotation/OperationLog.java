package com.syty.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 用于标记需要记录操作日志的方法。
 * 即使不添加此注解，LogAspect 也会自动拦截 Controller 方法并记录日志。
 * 此注解用于提供更详细的日志信息。
 * </p>
 * 
 * <p>使用示例：</p>
 * <pre>
 * {@code @OperationLog(module = "订单管理", operation = "创建订单")}
 * public Result<Order> createOrder(@RequestBody Order order) {
 *     // ...
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {
    
    /**
     * 模块名称
     * <p>例如：订单管理、库存管理、用户管理</p>
     */
    String module() default "";
    
    /**
     * 操作类型
     * <p>例如：创建、更新、删除、导出</p>
     */
    String operation() default "";
    
    /**
     * 是否记录请求参数
     * <p>默认 true，对于敏感操作可以设置为 false</p>
     */
    boolean recordParams() default true;
    
    /**
     * 是否记录响应结果
     * <p>默认 false，响应结果通常较大</p>
     */
    boolean recordResult() default false;
    
    /**
     * 操作描述
     * <p>可选，用于提供更详细的操作说明</p>
     */
    String description() default "";
}
