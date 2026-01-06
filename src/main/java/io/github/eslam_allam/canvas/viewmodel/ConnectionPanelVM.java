package io.github.eslam_allam.canvas.viewmodel;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class ConnectionPanelVM {

    private final StringProperty baseUrl;
    private final StringProperty token;

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
