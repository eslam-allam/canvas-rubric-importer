package io.github.eslam_allam.canvas.controller;

import io.github.eslam_allam.canvas.notification.PopUp;
import io.github.eslam_allam.canvas.service.ConnectionStore;
import io.github.eslam_allam.canvas.view.component.ConnectionPanel;
import io.github.eslam_allam.canvas.viewmodel.ConnectionPanelVM;
import javafx.event.ActionEvent;

public class ConnectionPanelController {

    private final ConnectionPanelVM vm;
    private final ConnectionStore store;

    public ConnectionPanelController(ConnectionPanel view, ConnectionPanelVM vm, ConnectionStore store) {
        this.vm = vm;
        this.store = store;

        view.bind(vm);
        view.onSave(this::onSaveSettings);
        loadSettings();
    }

    private void onSaveSettings(ActionEvent event) {
        this.store.saveSettings(this.vm.baseUrl().get(), this.vm.token().get());
        PopUp.showInfo("Saved", "Settings saved.");
    }

    private void loadSettings() {
        this.vm.baseUrl().set(this.store.loadBaseUrl());
        this.vm.token().set(this.store.loadToken());
    }
}
