package com.example.canvas.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CsvRubricParser {

    public record ParsedRubric(List<RubricModels.Criterion> criteria, double totalPoints) {}

    public ParsedRubric parse(Path csvPath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            var headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV has no header row.");
            }

            String[] headers = headerLine.split(",");
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim();
            }

            int criterionIdx = indexOf(headers, "criterion");
            int pointsIdx = indexOf(headers, "points");
            int criterionDescIdx = indexOf(headers, "criterion_desc");
            if (criterionIdx < 0 || pointsIdx < 0) {
                throw new IllegalArgumentException("CSV must contain 'criterion' and 'points' columns.");
            }

            var ratingGroups = RatingHeaderDetector.detect(headers);
            if (ratingGroups.size() < 2) {
                throw new IllegalArgumentException("CSV must have at least rating1/rating1_points/rating1_desc, etc.");
            }

            validateNoExtraColumns(headers, ratingGroups);

            List<RubricModels.Criterion> criteria = new ArrayList<>();
            double total = 0.0;
            String line;
            int rowNum = 1; // header is row 1
            while ((line = reader.readLine()) != null) {
                rowNum++;
                if (line.isBlank()) {
                    continue;
                }
                String[] cols = line.split(",");
                if (cols.length != headers.length) {
                    throw new IllegalArgumentException("Row " + rowNum + ": column count does not match header.");
                }

                String criterion = cols[criterionIdx].trim();
                if (criterion.isEmpty()) {
                    throw new IllegalArgumentException("Row " + rowNum + ": empty criterion.");
                }

                String pointsRaw = cols[pointsIdx].trim();
                double points = parseDouble(pointsRaw, "Row " + rowNum + ": points must be numeric.");
                total += points;

                String desc = criterionDescIdx >= 0 ? cols[criterionDescIdx].trim() : "";

                List<RubricModels.Rating> ratings = new ArrayList<>();
                for (RatingHeaderDetector.RatingGroup g : ratingGroups) {
                    String name = getCol(cols, headers, g.nameColumn());
                    String ptsRaw = getCol(cols, headers, g.pointsColumn());
                    String longDesc = getCol(cols, headers, g.descColumn());

                    if (name.isBlank() && ptsRaw.isBlank() && longDesc.isBlank()) {
                        continue;
                    }

                    if (name.isBlank()) {
                        throw new IllegalArgumentException("Row " + rowNum + ": " + g.nameColumn() + " empty.");
                    }

                    double ratingPts = parseDouble(ptsRaw, "Row " + rowNum + ": " + g.pointsColumn() + " must be numeric.");
                    ratings.add(new RubricModels.Rating(name, ratingPts, longDesc));
                }

                if (ratings.size() < 2) {
                    throw new IllegalArgumentException("Row " + rowNum + ": need >=2 ratings.");
                }

                validateRatingPoints(rowNum, points, ratings);

                criteria.add(new RubricModels.Criterion(criterion, desc, points, ratings));
            }

            return new ParsedRubric(criteria, total);
        }
    }

    private static void validateRatingPoints(int rowNum, double criterionPoints, List<RubricModels.Rating> ratings) {
        long zeros = ratings.stream().filter(r -> r.getPoints() == 0).count();
        long maxes = ratings.stream().filter(r -> r.getPoints() == criterionPoints).count();
        boolean anyAbove = ratings.stream().anyMatch(r -> r.getPoints() > criterionPoints);

        if (anyAbove) {
            throw new IllegalArgumentException("Row " + rowNum + ": rating points cannot exceed criterion points (" + criterionPoints + ").");
        }
        if (zeros != 1) {
            throw new IllegalArgumentException("Row " + rowNum + ": there must be exactly one rating with 0 points (found " + zeros + ").");
        }
        if (maxes != 1) {
            throw new IllegalArgumentException("Row " + rowNum + ": there must be exactly one rating with points equal to the criterion total (" + criterionPoints + "); found " + maxes + ".");
        }
    }

    private static void validateNoExtraColumns(String[] headers, List<RatingHeaderDetector.RatingGroup> ratingGroups) {
        var allowed = new java.util.HashSet<String>();
        allowed.add("criterion");
        allowed.add("criterion_desc");
        allowed.add("points");
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

    private static String getCol(String[] cols, String[] header, String columnName) {
        int idx = indexOf(header, columnName);
        if (idx < 0 || idx >= cols.length) {
            return "";
        }
        return cols[idx].trim();
    }

    private static double parseDouble(String value, String errorMessage) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage, ex);
        }
    }
}
