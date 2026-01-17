package io.github.eslam_allam.canvas.viewmodel;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

@Singleton
public final class ConnectionPanelVM {

    private final StringProperty baseUrl;
    private final StringProperty token;

    @Inject
    public ConnectionPanelVM() {
        this.baseUrl = new SimpleStringProperty();
        this.token = new SimpleStringProperty();
    }

    public StringProperty baseUrl() {
        return this.baseUrl;
    }

    public StringProperty token() {
        return this.token;
    }
}
