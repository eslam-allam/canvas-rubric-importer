package io.github.eslam_allam.canvas.gui;

import dagger.BindsInstance;
import dagger.Component;
import io.github.eslam_allam.canvas.client.CanvasCredentialProvider;
import io.github.eslam_allam.canvas.client.CanvasCredentialProviderModule;
import io.github.eslam_allam.canvas.controller.ConnectionPanelController;
import io.github.eslam_allam.canvas.controller.ControllerModule;
import io.github.eslam_allam.canvas.controller.ListPaneController;
import io.github.eslam_allam.canvas.controller.MainController;
import io.github.eslam_allam.canvas.controller.RubricConfigurationController;
import io.github.eslam_allam.canvas.factory.ListPaneFactory;
import io.github.eslam_allam.canvas.factory.SceneSwitcherFactory;
import io.github.eslam_allam.canvas.model.canvas.Assignment;
import io.github.eslam_allam.canvas.model.canvas.Course;
import io.github.eslam_allam.canvas.notification.NotificationModule;
import io.github.eslam_allam.canvas.service.PreferencesService;
import io.github.eslam_allam.canvas.view.component.ConnectionPanel;
import io.github.eslam_allam.canvas.view.component.ListPane;
import io.github.eslam_allam.canvas.view.component.StatusLabel;
import io.github.eslam_allam.canvas.viewmodel.ConnectionPanelVM;
import io.github.eslam_allam.canvas.viewmodel.ListPaneVM;
import io.github.eslam_allam.canvas.viewmodel.StatusLabelVM;
import jakarta.inject.Singleton;
import java.util.Objects;
import javafx.scene.text.Font;
import javafx.stage.Stage;

@Singleton
@Component(
        modules = {
            ListPaneFactory.class,
            SceneSwitcherFactory.class,
            NotificationModule.class,
            CanvasCredentialProviderModule.class,
            ControllerModule.class
        })
public interface CanvasRubricImporter {
    CanvasCredentialProvider canvasCredentialProvider();

    StatusLabelVM statusLabelVM();

    StatusLabel statusLabel();

    ConnectionPanelVM connectionPanelVM();

    ConnectionPanel connectionPanel();

    ConnectionPanelController connectionPanelController();

    ListPaneVM<Course> courseListPaneVM();

    ListPane<Course> courseListPane();

    ListPaneController<Course> coursesPaneController();

    ListPaneController<Assignment> assignmentsPaneController();

    RubricConfigurationController rubricConfigurationController();

    MainController mainController();

    @Component.Factory
    interface Factory {
        CanvasRubricImporter create(
                @BindsInstance Stage primaryStage, @BindsInstance PreferencesService preferencesService);
    }

    default void init() throws IllegalStateException {
        loadResources();

        statusLabel().bind(statusLabelVM());
        connectionPanelController();
        coursesPaneController();
        assignmentsPaneController();
        rubricConfigurationController();
        mainController().initAndShow();
    }

    private void loadResources() throws IllegalStateException {
        String[] fontFiles = {
            "Inter_28pt-Black.ttf",
            "Inter_28pt-BlackItalic.ttf",
            "Inter_28pt-Bold.ttf",
            "Inter_28pt-BoldItalic.ttf",
            "Inter_28pt-ExtraBold.ttf",
            "Inter_28pt-ExtraBoldItalic.ttf",
            "Inter_28pt-ExtraLight.ttf",
            "Inter_28pt-ExtraLightItalic.ttf",
            "Inter_28pt-Italic.ttf",
            "Inter_28pt-Light.ttf",
            "Inter_28pt-LightItalic.ttf",
            "Inter_28pt-Medium.ttf",
            "Inter_28pt-MediumItalic.ttf",
            "Inter_28pt-Regular.ttf",
            "Inter_28pt-SemiBold.ttf",
            "Inter_28pt-SemiBoldItalic.ttf",
            "Inter_28pt-Thin.ttf",
            "Inter_28pt-ThinItalic.ttf",
        };

        for (String file : fontFiles) {
            Objects.requireNonNull(
                    Font.loadFont(
                            Objects.requireNonNull(
                                    getClass().getResourceAsStream("/fonts/Inter/" + file),
                                    file + " font not found on class path."),
                            12),
                    "Failed to load " + file + " font");
        }
    }
}
