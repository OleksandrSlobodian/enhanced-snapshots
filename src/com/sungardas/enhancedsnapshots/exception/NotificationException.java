package com.sungardas.enhancedsnapshots.exception;

public class NotificationException extends EnhancedSnapshotsException {
    public NotificationException() {
    }

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotificationException(Throwable cause) {
        super(cause);
    }

    public NotificationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
