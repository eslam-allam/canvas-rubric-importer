package io.github.eslam_allam.canvas.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RatingHeaderDetectorService {

    public record RatingGroup(String nameColumn, String pointsColumn, String descColumn) {}

    private static final Pattern RATING_NAME_PATTERN = Pattern.compile("rating(\\d+)");

    private RatingHeaderDetectorService() {}

    public static List<RatingGroup> detect(String[] header) {
        List<int[]> ratingNameCols = new ArrayList<>();
        for (int i = 0; i < header.length; i++) {
            String col = header[i].trim();
            Matcher m = RATING_NAME_PATTERN.matcher(col);
            if (m.matches()) {
                int index = Integer.parseInt(m.group(1));
                ratingNameCols.add(new int[] {index, i});
            }
        }
        ratingNameCols.sort(Comparator.comparingInt(a -> a[0]));

        List<RatingGroup> groups = new ArrayList<>();
        for (int[] entry : ratingNameCols) {
            int n = entry[0];
            String nameCol = "rating" + n;
            String ptsCol = "rating" + n + "_points";
            String descCol = "rating" + n + "_desc";

            boolean hasPoints = contains(header, ptsCol);
            boolean hasDesc = contains(header, descCol);
            if (hasPoints && hasDesc) {
                groups.add(new RatingGroup(nameCol, ptsCol, descCol));
            }
        }
        return groups;
    }

    private static boolean contains(String[] arr, String value) {
        for (String s : arr) {
            if (s.equals(value)) {
                return true;
            }
        }
        return false;
    }
}
