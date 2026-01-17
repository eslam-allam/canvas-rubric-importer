package io.github.eslam_allam.canvas.client;

import dagger.Binds;
import dagger.Module;
import io.github.eslam_allam.canvas.service.PreferencesService;
import jakarta.inject.Singleton;

@Module
public interface CanvasCredentialProviderModule {

    @Binds
    @Singleton
    CanvasCredentialProvider bindCanvasCredentialProvider(PreferencesService impl);
}
