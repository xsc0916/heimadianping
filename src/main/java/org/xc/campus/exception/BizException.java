package org.xc.campus.exception;

import lombok.Getter;

/**
 * 业务异常：code 采用文档 5.1 错误码（2001/3001/...），非 200 一律按业务失败返回
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
