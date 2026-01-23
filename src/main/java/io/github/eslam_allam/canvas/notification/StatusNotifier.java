package io.github.eslam_allam.canvas.notification;

import io.github.eslam_allam.canvas.constant.OperationStatus;

public interface StatusNotifier {
    void setStatus(String text);

    default void setStatus(OperationStatus status) {
        setStatus(status.value);
    }
}
