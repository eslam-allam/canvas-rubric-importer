package io.github.eslam_allam.canvas.view.component;

import io.github.eslam_allam.canvas.viewmodel.ListPaneVM;

public interface ListPane<T> extends LoadingWidget<ListPaneVM<T>> {
    String getTitle();
}
