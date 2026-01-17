package io.github.eslam_allam.canvas.view.component;

import io.github.eslam_allam.canvas.view.Widget;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public interface SavingWidget<T> extends Widget<T> {
    void onSave(EventHandler<ActionEvent> callback);
}
