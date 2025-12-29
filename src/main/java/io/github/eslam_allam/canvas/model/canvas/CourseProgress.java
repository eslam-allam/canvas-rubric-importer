package io.github.eslam_allam.canvas.model.canvas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CourseProgress(
        // total number of requirements from all modules
        @JsonProperty("requirement_count") Integer requirementCount,

        // total number of requirements the user has completed from all modules
        @JsonProperty("requirement_completed_count") Integer requirementCompletedCount,

        // url to next module item that has an unmet requirement
        @JsonProperty("next_requirement_url") String nextRequirementUrl,

        // date the course was completed. null if the course has not been completed by this user
        @JsonProperty("completed_at") String completedAt) {}
