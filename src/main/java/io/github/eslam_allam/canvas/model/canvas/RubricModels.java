package io.github.eslam_allam.canvas.model.canvas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class RubricModels {

    private RubricModels() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final record Rating(
            String id,
            String description,
            Double points,
            @JsonProperty("long_description") String longDescription) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final record Criterion(
            String name, String description, double points, List<Rating> ratings) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final record Criteria(
            Integer points,
            String id,
            @JsonProperty("learing_outcome_id") String learningOutcomeID,
            @JsonProperty("vendor_guid") String vendorGUID,
            String description,
            @JsonProperty("long_description") String longDescription,
            @JsonProperty("criterion_use_range") Boolean criterionUseRange,
            @JsonProperty("ignore_for_scoring") Boolean ignoreForScoring,
            List<Rating> ratings) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final record Settings(@JsonProperty("points_possible") String pointsPossible) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final record Created(
            Integer rubric, @JsonProperty("rubric_association") Integer rubricAssociation) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final record Rubric(String title, List<Criterion> criteria) {

        public double getTotalPoints() {
            return criteria.stream().mapToDouble(Criterion::points).sum();
        }
    }
}
