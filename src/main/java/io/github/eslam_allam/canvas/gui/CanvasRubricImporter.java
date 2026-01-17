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

    default void init() {
        statusLabel().bind(statusLabelVM());
        connectionPanelController();
        coursesPaneController();
        assignmentsPaneController();
        rubricConfigurationController();
        mainController().initAndShow();
    }
}
