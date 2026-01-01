package io.github.eslam_allam.canvas.model.canvas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record Term(
        Long id,
        String name,
        @JsonProperty("start_at") String startAt,
        @JsonProperty("end_at") String endAt) {}
