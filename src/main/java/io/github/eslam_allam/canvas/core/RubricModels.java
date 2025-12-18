package io.github.eslam_allam.canvas.core;

import java.util.ArrayList;
import java.util.List;

public final class RubricModels {

    private RubricModels() {}

    public static final class Rating {
        private final String description;
        private final double points;
        private final String longDescription;

        public Rating(String description, double points, String longDescription) {
            this.description = description;
            this.points = points;
            this.longDescription = longDescription;
        }

        public String getDescription() { return description; }
        public double getPoints() { return points; }
        public String getLongDescription() { return longDescription; }
    }

    public static final class Criterion {
        private final String name;
        private final String description;
        private final double points;
        private final List<Rating> ratings;

        public Criterion(String name, String description, double points, List<Rating> ratings) {
            this.name = name;
            this.description = description;
            this.points = points;
            this.ratings = new ArrayList<>(ratings);
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public double getPoints() { return points; }
        public List<Rating> getRatings() { return new ArrayList<>(ratings); }
    }

    public static final class Rubric {
        private final String title;
        private final List<Criterion> criteria;

        public Rubric(String title, List<Criterion> criteria) {
            this.title = title;
            this.criteria = new ArrayList<>(criteria);
        }

        public String getTitle() { return title; }
        public List<Criterion> getCriteria() { return new ArrayList<>(criteria); }

        public double getTotalPoints() {
            return criteria.stream().mapToDouble(Criterion::getPoints).sum();
        }
    }
}
