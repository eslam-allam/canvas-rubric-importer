package io.github.eslam_allam.canvas.rubric.importing.csv;

import io.github.eslam_allam.canvas.model.canvas.RubricModels;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.text.StringEscapeUtils;

public final class CsvRubricParser {

    public record ParsedRubric(List<RubricModels.Criterion> criteria, double totalPoints) {}

    private final boolean decodeHtmlEntities;

    public CsvRubricParser() {
        this(true);
    }

    public CsvRubricParser(boolean decodeHtmlEntities) {
        this.decodeHtmlEntities = decodeHtmlEntities;
    }

    public ParsedRubric parse(Path csvPath) throws IOException {

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8);
                CSVParser parser = CSVFormat.DEFAULT
                        .builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setTrim(true)
                        .build()
                        .parse(reader)) {

            List<String> headerList = parser.getHeaderNames();
            if (headerList == null || headerList.isEmpty()) {
                throw new IllegalArgumentException("CSV has no header row.");
            }
            String[] headers = headerList.toArray(String[]::new);

            int criterionIdx = indexOf(headers, "criterion");
            int criterionDescIdx = indexOf(headers, "criterion_desc");
            if (criterionIdx < 0) {
                throw new IllegalArgumentException("CSV must contain 'criterion' column.");
            }

            var ratingGroups = RatingHeaderDetector.detect(headers);
            if (ratingGroups.size() < 2) {
                throw new IllegalArgumentException("CSV must have at least rating1/rating1_points/rating1_desc, etc.");
            }

            validateNoExtraColumns(headers, ratingGroups);

            List<RubricModels.Criterion> criteria = new ArrayList<>();
            double total = 0.0;
            int rowNum = 1; // header is row 1
            for (CSVRecord record : parser) {
                rowNum++;
                if (record.size() == 0) {
                    continue;
                }

                String criterion = normalizeText(record.get(criterionIdx).trim());

                if (criterion.isEmpty()) {
                    throw new IllegalArgumentException("Row " + rowNum + ": empty criterion.");
                }

                String desc = criterionDescIdx >= 0
                        ? normalizeText(record.get(criterionDescIdx).trim())
                        : "";

                List<RubricModels.Rating> ratings = new ArrayList<>();
                boolean hasZeroRating = false;
                boolean hasPositiveRating = false;
                for (RatingHeaderDetector.RatingGroup g : ratingGroups) {
                    String name = normalizeText(getField(record, headers, g.nameColumn()));
                    String ptsRaw = getField(record, headers, g.pointsColumn());
                    String longDesc = normalizeText(getField(record, headers, g.descColumn()));

                    if (name.isBlank() && ptsRaw.isBlank() && longDesc.isBlank()) {
                        continue;
                    }

                    if (name.isBlank()) {
                        throw new IllegalArgumentException("Row " + rowNum + ": " + g.nameColumn() + " empty.");
                    }

                    if (longDesc.isBlank()) {
                        throw new IllegalArgumentException("Row " + rowNum + ": " + g.descColumn() + " empty.");
                    }

                    double ratingPts =
                            parseDouble(ptsRaw, "Row " + rowNum + ": " + g.pointsColumn() + " must be numeric.");

                    if (ratingPts == 0.0) {
                        hasZeroRating = true;
                    }
                    if (ratingPts > 0.0) {
                        hasPositiveRating = true;
                    }

                    ratings.add(new RubricModels.Rating("", name, ratingPts, longDesc));
                }

                if (ratings.isEmpty()) {
                    throw new IllegalArgumentException("Row " + rowNum + ": criterion must have at least one rating.");
                }

                if (!hasZeroRating) {
                    throw new IllegalArgumentException(
                            "Row " + rowNum + ": criterion must include at least one rating with 0" + " points.");
                }

                if (!hasPositiveRating) {
                    throw new IllegalArgumentException("Row "
                            + rowNum
                            + ": criterion must include at least one rating with"
                            + " positive points.");
                }

                double criterionPoints = ratings.stream()
                        .mapToDouble(RubricModels.Rating::points)
                        .max()
                        .orElse(0.0);

                criteria.add(new RubricModels.Criterion(criterion, desc, criterionPoints, ratings));
                total += criterionPoints;
            }

            return new ParsedRubric(criteria, total);
        }
    }

    private static void validateNoExtraColumns(String[] headers, List<RatingHeaderDetector.RatingGroup> ratingGroups) {
        var allowed = new java.util.HashSet<String>();
        allowed.add("criterion");
        allowed.add("criterion_desc");

        for (RatingHeaderDetector.RatingGroup g : ratingGroups) {
            allowed.add(g.nameColumn());
            allowed.add(g.pointsColumn());
            allowed.add(g.descColumn());
        }
        List<String> extra = new ArrayList<>();
        for (String h : headers) {
            if (!allowed.contains(h)) {
                extra.add(h);
            }
        }
        if (!extra.isEmpty()) {
            throw new IllegalArgumentException("Unexpected column(s) in header: " + String.join(", ", extra));
        }
    }

    private static int indexOf(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(value)) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeText(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        if (!decodeHtmlEntities) {
            return s;
        }
        return StringEscapeUtils.unescapeHtml4(s);
    }

    private static String getField(CSVRecord record, String[] header, String columnName) {

        int idx = indexOf(header, columnName);
        if (idx < 0 || idx >= record.size()) {
            return "";
        }
        return record.get(idx).trim();
    }

    private static double parseDouble(String value, String errorMessage) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage, ex);
        }
    }
}
