package io.github.eslam_allam.canvas.view.component;

import io.github.eslam_allam.canvas.view.Widget;
import io.github.eslam_allam.canvas.viewmodel.StatusLabelVM;
import javafx.scene.Node;
import javafx.scene.control.Label;

public final class StatusLabel implements Widget<StatusLabelVM> {
    private final Label status;

    public StatusLabel() {
        this.status = new Label();
        this.status.getStyleClass().add("status-label");
    }

    public void bind(StatusLabelVM vm) {
        this.status.textProperty().bind(vm.text());
    }

    public Node getRoot() {
        return this.status;
    }
}
