package io.github.eslam_allam.canvas.controller;

import io.github.eslam_allam.canvas.view.component.ListPane;
import io.github.eslam_allam.canvas.viewmodel.ListPaneVM;

public final class ListPaneController<T> {

    private final ListPaneVM<T> vm;

    public ListPaneController(ListPane<T> view, ListPaneVM<T> vm) {
        this.vm = vm;
        view.bind(vm);
    }

    private void onLoadItems() {
        setStatus("Loading courses...");
        new Thread(
                        () -> {
                            try {
                                List<Course> list = this.canvasClient.listCourses();
                                Platform.runLater(() -> {
                                    courses.setAll(list);
                                    setStatus("Loaded " + list.size() + " courses");
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() -> PopUp.showError("Error", ex.getMessage()));
                            }
                        },
                        "load-courses")
                .start();
    }
}
