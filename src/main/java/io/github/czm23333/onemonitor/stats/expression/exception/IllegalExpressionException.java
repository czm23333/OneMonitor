package io.github.czm23333.onemonitor.stats.expression.exception;

public class IllegalExpressionException extends RuntimeException {
    public IllegalExpressionException() {}

    public IllegalExpressionException(String message) {
        super(message);
    }

    public IllegalExpressionException(String message, Exception cause) {
        super(message, cause);
    }
}