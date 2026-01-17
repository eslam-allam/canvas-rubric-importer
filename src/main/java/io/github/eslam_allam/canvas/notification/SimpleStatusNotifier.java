package io.github.eslam_allam.canvas.notification;

import io.github.eslam_allam.canvas.viewmodel.StatusLabelVM;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public final class SimpleStatusNotifier implements StatusNotifier {
    private final StatusLabelVM vm;

    @Inject
    public SimpleStatusNotifier(StatusLabelVM vm) {
        this.vm = vm;
    }

    public void setStatus(String text) {
        this.vm.text().set(text);
    }
}
