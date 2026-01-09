package io.github.eslam_allam.canvas.viewmodel;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class RubricConfigurationVM {

    private final StringProperty courseId;
    private final StringProperty assignmentId;
    private final StringProperty title;
    private final StringProperty csvPath;

    private final BooleanProperty freeFormComments;
    private final BooleanProperty useForGrading;
    private final BooleanProperty hideScoreTotal;
    private final BooleanProperty syncPoints;
    private final BooleanProperty decodeHtml;

    private final BooleanProperty previewButtonVisible;
    private final BooleanProperty backBtnVisible;

    public StringProperty courseId() {
        return courseId;
    }

    public StringProperty assignmentId() {
        return assignmentId;
    }

    public StringProperty title() {
        return title;
    }

    public StringProperty csvPath() {
        return csvPath;
    }

    public BooleanProperty freeFormComments() {
        return freeFormComments;
    }

    public BooleanProperty useForGrading() {
        return useForGrading;
    }

    public BooleanProperty hideTotalScore() {
        return hideScoreTotal;
    }

    public BooleanProperty syncPoints() {
        return syncPoints;
    }

    public BooleanProperty decodeHtml() {
        return decodeHtml;
    }

    public BooleanProperty previewButtonVisible() {
        return previewButtonVisible;
    }

    public BooleanProperty backBtnVisible() {
        return backBtnVisible;
    }

    public RubricConfigurationVM() {
        this.courseId = new SimpleStringProperty();
        this.assignmentId = new SimpleStringProperty();
        this.title = new SimpleStringProperty();
        this.csvPath = new SimpleStringProperty();

        this.freeFormComments = new SimpleBooleanProperty();
        this.useForGrading = new SimpleBooleanProperty();
        this.hideScoreTotal = new SimpleBooleanProperty();
        this.syncPoints = new SimpleBooleanProperty();
        this.decodeHtml = new SimpleBooleanProperty();

        this.previewButtonVisible = new SimpleBooleanProperty();
        this.backBtnVisible = new SimpleBooleanProperty();
    }
}
