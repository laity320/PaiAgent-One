package com.paiagent.common.exception;

public class WorkflowExecutionException extends BizException {
    public WorkflowExecutionException(String message) {
        super(500, message);
    }

    public WorkflowExecutionException(String message, Throwable cause) {
        super(500, message);
        initCause(cause);
    }
}
