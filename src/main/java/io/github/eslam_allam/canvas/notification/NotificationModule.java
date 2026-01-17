package io.github.eslam_allam.canvas.notification;

import dagger.Binds;
import dagger.Module;
import jakarta.inject.Singleton;

@Module
public interface NotificationModule {

    @Binds
    @Singleton
    StatusNotifier bindStatusNotifier(SimpleStatusNotifier impl);
}
