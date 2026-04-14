package com.paiagent.common.exception;

public class AuthException extends BizException {
    public AuthException(String message) {
        super(401, message);
    }
}
