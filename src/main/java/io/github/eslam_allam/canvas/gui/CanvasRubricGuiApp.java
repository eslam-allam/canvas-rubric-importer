package io.github.eslam_allam.canvas.gui;

import io.github.eslam_allam.canvas.service.PreferencesService;
import java.net.URISyntaxException;
import javafx.application.Application;
import javafx.stage.Stage;

public class CanvasRubricGuiApp extends Application {

    @Override
    public void start(Stage primaryStage) throws URISyntaxException {

        CanvasRubricImporter canvasRubricImporter = DaggerCanvasRubricImporter.factory()
                .create(primaryStage, new PreferencesService(CanvasRubricGuiApp.class));

        canvasRubricImporter.init();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
