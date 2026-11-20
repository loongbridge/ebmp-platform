package com.ebmp.common;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        this(message, 400);
    }

    public BizException(String message, int code) {
        super(message);
        this.code = code;
    }
}
