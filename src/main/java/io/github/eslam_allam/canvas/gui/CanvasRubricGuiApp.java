package io.github.eslam_allam.canvas.gui;

import io.github.eslam_allam.canvas.client.CanvasClient;
import io.github.eslam_allam.canvas.controller.ConnectionPanelController;
import io.github.eslam_allam.canvas.controller.ListPaneController;
import io.github.eslam_allam.canvas.controller.MainController;
import io.github.eslam_allam.canvas.model.canvas.Assignment;
import io.github.eslam_allam.canvas.model.canvas.Course;
import io.github.eslam_allam.canvas.navigation.StageManager;
import io.github.eslam_allam.canvas.notification.SimpleStatusNotifier;
import io.github.eslam_allam.canvas.notification.StatusNotifier;
import io.github.eslam_allam.canvas.service.CanvasRubricService;
import io.github.eslam_allam.canvas.service.PreferencesService;
import io.github.eslam_allam.canvas.view.component.ConnectionPanel;
import io.github.eslam_allam.canvas.view.component.ListPane;
import io.github.eslam_allam.canvas.view.component.SimpleConnectionPanel;
import io.github.eslam_allam.canvas.view.component.SimpleListPane;
import io.github.eslam_allam.canvas.view.component.StatusLabel;
import io.github.eslam_allam.canvas.viewmodel.ConnectionPanelVM;
import io.github.eslam_allam.canvas.viewmodel.ListPaneVM;
import io.github.eslam_allam.canvas.viewmodel.StatusLabelVM;
import java.net.URISyntaxException;
import javafx.application.Application;
import javafx.stage.Stage;

public class CanvasRubricGuiApp extends Application {

    @Override
    public void start(Stage primaryStage) throws URISyntaxException {
        StageManager stageManager = new StageManager(primaryStage);
        PreferencesService preferencesService = new PreferencesService(CanvasRubricGuiApp.class);

        StatusLabelVM statusLabelVM = new StatusLabelVM();
        StatusLabel statusLabel = new StatusLabel();
        statusLabel.bind(statusLabelVM);
        StatusNotifier statusNotifier = new SimpleStatusNotifier(statusLabelVM);

        CanvasClient client = new CanvasClient(preferencesService);
        CanvasRubricService rubricService = new CanvasRubricService(client);

        ConnectionPanelVM connectionPanelVM = new ConnectionPanelVM();
        ConnectionPanel connectionPanel = new SimpleConnectionPanel();
        ConnectionPanelController connectionPanelController =
                new ConnectionPanelController(connectionPanel, connectionPanelVM, preferencesService);

        ListPaneVM<Course> coursesPaneVM = new ListPaneVM<>();
        ListPane<Course> coursesPane =
                new SimpleListPane<>("Courses", course -> String.format("%s [%s]", course.name(), course.courseCode()));
        ListPaneController<Course> coursesPaneController =
                new ListPaneController<>(coursesPane, coursesPaneVM, client::listCourses, statusNotifier);

        ListPaneVM<Assignment> assignmentsPaneVM = new ListPaneVM<>();
        ListPane<Assignment> assignmentsPane = new SimpleListPane<>("Assignments", Assignment::name);
        ListPaneController<Assignment> assignmentsPaneController = new ListPaneController<>(
                assignmentsPane,
                assignmentsPaneVM,
                () -> client.listAssignments(coursesPaneVM.selected().get().id().toString()),
                statusNotifier);

        MainController controller = new MainController(
                connectionPanel,
                statusLabel,
                rubricService,
                stageManager,
                statusNotifier,
                coursesPane,
                coursesPaneVM,
                assignmentsPane,
                assignmentsPaneVM);
        controller.initAndShow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
