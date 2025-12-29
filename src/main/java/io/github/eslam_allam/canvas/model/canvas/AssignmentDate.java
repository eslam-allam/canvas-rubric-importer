package io.github.eslam_allam.canvas.model.canvas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record AssignmentDate(
        // (Optional) id of the assignment override this date represents
        Long id,
        // (Optional) whether this date represents the default due date
        Boolean base,
        String title,
        @JsonProperty("due_at")
        String dueAt,
        @JsonProperty("unlock_at")
        String unlockAt,
        @JsonProperty("lock_at")
        String lockAt
) {
}
