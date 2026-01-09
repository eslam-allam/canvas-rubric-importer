package io.github.eslam_allam.canvas.view.section;

import io.github.eslam_allam.canvas.model.canvas.Assignment;
import io.github.eslam_allam.canvas.model.canvas.Course;
import io.github.eslam_allam.canvas.view.component.ListPane;
import io.github.eslam_allam.canvas.view.component.SplitListPane;
import javafx.geometry.Insets;

public final class CoursesAndAssignmentsSection extends Section<SplitListPane<Course, Assignment>> {

    public CoursesAndAssignmentsSection(ListPane<Course> coursesPane, ListPane<Assignment> assignmentsPane) {
        super(new SplitListPane<>(coursesPane, assignmentsPane), new Insets(5, 0, 5, 0));
    }
}
