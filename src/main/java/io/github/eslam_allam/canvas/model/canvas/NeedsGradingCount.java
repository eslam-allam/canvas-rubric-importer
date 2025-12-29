package io.github.eslam_allam.canvas.model.canvas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record NeedsGradingCount(
        // The section ID
        @JsonProperty("section_id")
        String sectionId,
        // Number of submissions that need grading
        @JsonProperty("needs_grading_count")
        Integer needsGradingCount
) {
}
