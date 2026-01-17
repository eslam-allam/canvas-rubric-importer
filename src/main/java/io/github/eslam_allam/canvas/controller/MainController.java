package io.github.eslam_allam.canvas.controller;

import io.github.eslam_allam.canvas.navigation.StageManager;
import io.github.eslam_allam.canvas.view.MainView;
import io.github.eslam_allam.canvas.view.component.ConnectionPanel;
import io.github.eslam_allam.canvas.view.component.RubricConfiguration;
import io.github.eslam_allam.canvas.view.section.CoursesAndAssignmentsSection;
import io.github.eslam_allam.canvas.view.section.Section;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Objects;
import javafx.scene.Scene;

@Singleton
public class MainController {
    private final CoursesAndAssignmentsSection coursesAndAssignmentsSection;

    private final StageManager stageManager;

    private final MainView view;
    private final ConnectionPanel connectionPanel;
    private final RubricConfiguration rubricConfiguration;

    @Inject
    public MainController(
            StageManager stageManager,
            MainView view,
            ConnectionPanel connectionPanel,
            RubricConfiguration rubricConfiguration,
            CoursesAndAssignmentsSection coursesAndAssignmentsSection) {
        this.view = view;
        this.connectionPanel = connectionPanel;
        this.rubricConfiguration = rubricConfiguration;
        this.stageManager = stageManager;

        this.coursesAndAssignmentsSection = coursesAndAssignmentsSection;
    }

    public void initAndShow() {

        this.view.setTop(Section.oneTimeSection("Canvas Settings", this.connectionPanel.getRoot()));
        this.view.setCenter(this.coursesAndAssignmentsSection.getRoot());
        this.view.setBottom(Section.oneTimeSection("Rubric Configuration", this.rubricConfiguration.getRoot()));

        Scene scene = new Scene(this.view.getParent(), 1000, 650);
        scene.getStylesheets()
                .add(Objects.requireNonNull(getClass().getResource("/style.css"), "style.css not found on classpath")
                        .toExternalForm());

        stageManager.show(scene);
    }

    public void restoreCenter() {
        this.view.setCenter(this.coursesAndAssignmentsSection.getRoot());
    }
}
