package io.github.eslam_allam.canvas.factory;

import dagger.Module;
import dagger.Provides;
import io.github.eslam_allam.canvas.client.CanvasClient;
import io.github.eslam_allam.canvas.controller.ListPaneController;
import io.github.eslam_allam.canvas.model.canvas.Assignment;
import io.github.eslam_allam.canvas.model.canvas.Course;
import io.github.eslam_allam.canvas.notification.StatusNotifier;
import io.github.eslam_allam.canvas.view.component.ListPane;
import io.github.eslam_allam.canvas.view.component.SimpleListPane;
import io.github.eslam_allam.canvas.viewmodel.ListPaneVM;
import jakarta.inject.Singleton;

@Module
public final class ListPaneFactory {
    private ListPaneFactory() {}

    @Provides
    @Singleton
    public static ListPaneVM<Course> coursePaneVM() {
        return new ListPaneVM<>();
    }

    @Provides
    @Singleton
    public static ListPane<Course> coursePane() {
        return new SimpleListPane<>("Courses", course -> String.format("%s [%s]", course.name(), course.courseCode()));
    }

    @Provides
    @Singleton
    public static ListPaneController<Course> coursePaneController(
            ListPane<Course> coursePane,
            ListPaneVM<Course> coursePaneVM,
            CanvasClient canvasClient,
            StatusNotifier statusNotifier) {
        return new ListPaneController<>(coursePane, coursePaneVM, canvasClient::listCourses, statusNotifier);
    }

    @Provides
    @Singleton
    public static ListPaneVM<Assignment> assignmentPaneVM() {
        return new ListPaneVM<>();
    }

    @Provides
    @Singleton
    public static ListPane<Assignment> assignmentPane() {
        return new SimpleListPane<>("Assignments", Assignment::name);
    }

    @Provides
    @Singleton
    public static ListPaneController<Assignment> assignmentPaneController(
            ListPane<Assignment> assignmentPane,
            ListPaneVM<Assignment> assignmentPaneVM,
            ListPaneVM<Course> coursePaneVM,
            CanvasClient canvasClient,
            StatusNotifier statusNotifier) {
        return new ListPaneController<>(
                assignmentPane,
                assignmentPaneVM,
                () -> canvasClient.listAssignments(
                        coursePaneVM.selected().get().id().toString()),
                statusNotifier);
    }
}
