package io.github.eslam_allam.canvas.viewmodel;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@Singleton
public final class StatusLabelVM {
    private final StringProperty text;

    @Inject
    public StatusLabelVM() {
        this.text = new SimpleStringProperty();
    }

    public StringProperty text() {
        return text;
    }
}
