package io.github.eslam_allam.canvas.navigation;

import io.github.eslam_allam.canvas.AppInfo;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.scene.Scene;
import javafx.stage.Stage;

@Singleton
public class StageManager {

    private final Stage primaryStage;

    @Inject
    public StageManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(AppInfo.NAME);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void show(Scene scene) {
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
