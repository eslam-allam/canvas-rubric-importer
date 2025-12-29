package io.github.eslam_allam.canvas.model.canvas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScoreStatistic(
        // Min score
        Integer min,
        // Max score
        Integer max,
        // Mean score
        Integer mean,
        // Upper quartile score
        @JsonProperty("upper_q")
        Integer upperQ,
        // Median score
        Integer median,
        // Lower quartile score
        @JsonProperty("lower_q")
        Integer lowerQ
) {
}
