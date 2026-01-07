package io.github.eslam_allam.canvas.view.component;

import io.github.eslam_allam.canvas.view.Widget;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public interface LoadingWidget<T> extends Widget<T> {
    void onLoad(EventHandler<ActionEvent> callback);
}
