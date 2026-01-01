package io.github.eslam_allam.canvas.domain;

import io.github.eslam_allam.canvas.model.canvas.RubricModels;
import java.util.List;

public class RubricRow {
    private final String criterion;
    private final String description;
    private final String points;
    private final List<RubricModels.Rating> ratings;

    public RubricRow(String criterion, String description, String points, List<RubricModels.Rating> ratings) {
        this.criterion = criterion;
        this.description = description;
        this.points = points;
        this.ratings = ratings;
    }

    public String getCriterion() {
        return criterion;
    }

    public String getDescription() {
        return description;
    }

    public String getPoints() {
        return points;
    }

    public List<RubricModels.Rating> getRatings() {
        return ratings;
    }
}
