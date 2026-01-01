package io.github.eslam_allam.canvas.gui;

import io.github.eslam_allam.canvas.client.CanvasClient;
import io.github.eslam_allam.canvas.controller.MainController;
import io.github.eslam_allam.canvas.navigation.StageManager;
import io.github.eslam_allam.canvas.service.CanvasRubricService;
import io.github.eslam_allam.canvas.service.PreferencesService;
import java.net.URISyntaxException;
import javafx.application.Application;
import javafx.stage.Stage;

public class CanvasRubricGuiApp extends Application {

    @Override
    public void start(Stage primaryStage) throws URISyntaxException {
        StageManager stageManager = new StageManager(primaryStage);
        PreferencesService preferencesService = new PreferencesService(CanvasRubricGuiApp.class);

        CanvasClient client = new CanvasClient(preferencesService);
        CanvasRubricService rubricService = new CanvasRubricService(client);

        MainController controller = new MainController(preferencesService, rubricService, stageManager, client);
        controller.initAndShow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
