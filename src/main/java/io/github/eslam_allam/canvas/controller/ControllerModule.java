package io.github.eslam_allam.canvas.controller;

import dagger.Binds;
import dagger.Module;
import io.github.eslam_allam.canvas.service.ConnectionStore;
import io.github.eslam_allam.canvas.service.PreferencesService;
import io.github.eslam_allam.canvas.view.component.ConnectionPanel;
import io.github.eslam_allam.canvas.view.component.SimpleConnectionPanel;
import jakarta.inject.Singleton;

@Module
public interface ControllerModule {

    @Binds
    @Singleton
    ConnectionStore bindConnectionStore(PreferencesService impl);

    @Binds
    @Singleton
    ConnectionPanel bindConnectionPanel(SimpleConnectionPanel impl);
}
