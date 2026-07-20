package com.syty.common;
import lombok.Getter;
/**
 * 业务异常 - 替代 RuntimeException 用于业务校验失败
 * 由全局异常处理器捕获，返回 400 + 清晰提示
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;
    public BizException(String message) {
        super(message);
        this.code = 400;
    }
    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
    public static BizException of(String msg) {
        return new BizException(msg);
    }
    public static BizException forbidden(String msg) {
        return new BizException(403, msg);
    }
    public static BizException notFound(String msg) {
        return new BizException(404, msg);
    }
}
