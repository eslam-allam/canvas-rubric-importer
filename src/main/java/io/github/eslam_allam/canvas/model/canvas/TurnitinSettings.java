package io.github.eslam_allam.canvas.model.canvas;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record TurnitinSettings(
        @JsonProperty("originality_report_visibility") String originalityReportVisibility,
        @JsonProperty("s_paper_check") Boolean sPaperCheck,
        @JsonProperty("internet_check") Boolean internetCheck,
        @JsonProperty("journal_check") Boolean journalCheck,
        @JsonProperty("exclude_biblio") Boolean excludeBiblio,
        @JsonProperty("exclude_quoted") Boolean excludeQuoted,
        @JsonProperty("exclude_small_matches_type") String excludeSmallMatchesType,
        @JsonProperty("exclude_small_matches_value") Integer excludeSmallMatchesValue) {}
