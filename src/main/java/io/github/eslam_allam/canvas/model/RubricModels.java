package io.github.eslam_allam.canvas.model;

import java.util.List;

public final class RubricModels {

    private RubricModels() {}

    public static final record Rating(String description, double points, String longDescription) {}

    public static final record Criterion(
            String name, String description, double points, List<Rating> ratings) {}

    public static final record Rubric(String title, List<Criterion> criteria) {

        public double getTotalPoints() {
            return criteria.stream().mapToDouble(Criterion::points).sum();
        }
    }
}
