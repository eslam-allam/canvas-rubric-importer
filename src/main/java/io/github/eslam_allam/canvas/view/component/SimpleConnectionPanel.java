package io.github.eslam_allam.canvas.view.component;

import io.github.eslam_allam.canvas.viewmodel.ConnectionPanelVM;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

public class SimpleConnectionPanel implements ConnectionPanel {

    private final GridPane root;

    private final Label baseUrlLabel;
    private final TextField baseUrlField;

    private final Label tokenLabel;
    private final PasswordField tokenField;

    private final Button saveButton;

    public SimpleConnectionPanel() {
        this.root = new GridPane();
        root.setHgap(10);
        root.setVgap(5);

        this.baseUrlLabel = new Label("Base URL:");
        baseUrlField = new TextField();

        this.tokenLabel = new Label("Access token:");

        tokenField = new PasswordField();

        this.saveButton = new Button("Save Settings");

        root.add(baseUrlLabel, 0, 0);
        root.add(baseUrlField, 1, 0);
        root.add(tokenLabel, 0, 1);
        root.add(tokenField, 1, 1);
        root.add(saveButton, 2, 0, 1, 2);
    }

    public void onSave(EventHandler<ActionEvent> callback) {
        this.saveButton.setOnAction(callback);
    }

    public void bind(ConnectionPanelVM vm) {
        this.baseUrlField.textProperty().bindBidirectional(vm.baseUrl());
        this.tokenField.textProperty().bindBidirectional(vm.token());
    }

    public Node getRoot() {
        return this.root;
    }
}
