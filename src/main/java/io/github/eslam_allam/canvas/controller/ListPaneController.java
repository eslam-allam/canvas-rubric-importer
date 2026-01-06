package io.github.eslam_allam.canvas.controller;

import io.github.eslam_allam.canvas.view.component.ListPane;
import io.github.eslam_allam.canvas.viewmodel.ListPaneVM;

public final class ListPaneController<T> {

    public ListPaneController(ListPane<T> view, ListPaneVM<T> vm) {
        view.bind(vm);
    }
}
