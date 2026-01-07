package io.github.eslam_allam.canvas.viewmodel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class StatusLabelVM {
    private final StringProperty text;

    public StatusLabelVM() {
        this.text = new SimpleStringProperty();
    }

    public StringProperty text() {
        return text;
    }
}
