package io.github.eslam_allam.canvas.notification;

import io.github.eslam_allam.canvas.viewmodel.StatusLabelVM;

public final class SimpleStatusNotifier implements StatusNotifier {
    private final StatusLabelVM vm;

    public SimpleStatusNotifier(StatusLabelVM vm) {
        this.vm = vm;
    }

    public void setStatus(String text) {
        this.vm.text().set(text);
    }
}
