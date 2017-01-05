package com.sungardas.enhancedsnapshots.exception;

public class SnsNotificationException extends NotificationException {
    public SnsNotificationException() {
    }

    public SnsNotificationException(String message) {
        super(message);
    }

    public SnsNotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SnsNotificationException(Throwable cause) {
        super(cause);
    }

    public SnsNotificationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
