package io.github.eslam_allam.canvas.constant;

import javafx.scene.control.Alert.AlertType;

public enum StandardAlert {
    ASSIGNMENT_MISSING(AlertType.ERROR, "Error", "Please select an assignment."),
    COURSE_MISSING(AlertType.ERROR, "Error", "Please select a course.");

    public final AlertType type;
    public final String title;
    public final String message;

    private StandardAlert(AlertType type, String title, String message) {
        this.type = type;
        this.title = title;
        this.message = message;
    }
}
