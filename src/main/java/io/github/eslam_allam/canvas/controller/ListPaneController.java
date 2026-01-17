package io.github.eslam_allam.canvas.controller;

import io.github.eslam_allam.canvas.notification.PopUp;
import io.github.eslam_allam.canvas.notification.StatusNotifier;
import io.github.eslam_allam.canvas.view.component.ListPane;
import io.github.eslam_allam.canvas.viewmodel.ListPaneVM;
import java.util.List;
import java.util.concurrent.Callable;
import javafx.application.Platform;
import javafx.event.ActionEvent;

public final class ListPaneController<T> {

    private final ListPaneVM<T> vm;
    private final ListPane<T> view;
    private final StatusNotifier statusNotifier;
    private final Callable<List<T>> itemGenerator;

    public ListPaneController(
            ListPane<T> view, ListPaneVM<T> vm, Callable<List<T>> itemGenerator, StatusNotifier statusNotifier) {
        this.view = view;
        this.vm = vm;
        this.statusNotifier = statusNotifier;
        this.itemGenerator = itemGenerator;

        view.bind(vm);
        view.onLoad(this::onLoadItems);
    }

    private void onLoadItems(ActionEvent event) {
        this.statusNotifier.setStatus(String.format("Loading %s...", this.view.getTitle()));
        new Thread(
                        () -> {
                            try {
                                List<T> list = this.itemGenerator.call();
                                Platform.runLater(() -> {
                                    this.vm.items().setAll(list);
                                    this.statusNotifier.setStatus(String.format(
                                            "Loaded %d %s", this.vm.items().size(), this.view.getTitle()));
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() -> PopUp.showError("Error", ex.getMessage()));
                            }
                        },
                        String.format(
                                "load-%s",
                                this.view.getTitle().replace(" ", "-").toLowerCase()))
                .start();
    }
}
