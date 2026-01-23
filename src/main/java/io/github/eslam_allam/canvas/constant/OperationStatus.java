package io.github.eslam_allam.canvas.constant;

public enum OperationStatus {
    DONE("Done"),
    ERROR("Error");

    public final String value;

    private OperationStatus(String value) {
        this.value = value;
    }
}
