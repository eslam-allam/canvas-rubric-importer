package io.github.eslam_allam.canvas.factory;

import dagger.Module;
import dagger.Provides;
import io.github.eslam_allam.canvas.controller.MainController;
import io.github.eslam_allam.canvas.navigation.RestorableSceneSwitcher;
import io.github.eslam_allam.canvas.navigation.SimpleRestorableSceneSwitcher;
import io.github.eslam_allam.canvas.view.MainView;

@Module
public final class SceneSwitcherFactory {
    private SceneSwitcherFactory() {}

    @Provides
    public static RestorableSceneSwitcher mainViewResorter(MainView mainView, MainController mainController) {
        return new SimpleRestorableSceneSwitcher(mainView::setCenter, mainController::restoreCenter);
    }
}
