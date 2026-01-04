package io.github.eslam_allam.canvas.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.eslam_allam.canvas.model.canvas.Assignment.GradingType;

public record AssignmentPointsUpdateRequest(
        @JsonProperty("points_possible") Double pointsPossible,
        @JsonProperty("grading_type") GradingType gradingType) {}
