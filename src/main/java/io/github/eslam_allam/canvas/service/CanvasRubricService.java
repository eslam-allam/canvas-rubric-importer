package io.github.eslam_allam.canvas.service;

import io.github.eslam_allam.canvas.client.CanvasClient;
import io.github.eslam_allam.canvas.domain.Result;
import io.github.eslam_allam.canvas.domain.ResultStatus;
import io.github.eslam_allam.canvas.model.canvas.Assignment;
import io.github.eslam_allam.canvas.model.canvas.RubricModels;
import io.github.eslam_allam.canvas.rubric.importing.csv.CsvRubricParser;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public class CanvasRubricService {
    private final CanvasClient canvasClient;

    @Inject
    public CanvasRubricService(CanvasClient canvasClient) {
        this.canvasClient = canvasClient;
    }

    public void getCanvasRubricCSV(String courseID, String assignmentID, Consumer<Result<String>> callback) {

        new Thread(
                        () -> {
                            try {
                                Assignment assignment =
                                        this.canvasClient.getAssignmentWithRubric(courseID, assignmentID);
                                List<RubricModels.Criteria> rubric = assignment.rubric();
                                if (rubric == null || rubric.isEmpty()) {
                                    callback.accept(new Result<>(ResultStatus.FAILURE, "No rubric found."));
                                    return;
                                }

                                int maxRatings = 0;
                                for (RubricModels.Criteria crit : rubric) {
                                    List<RubricModels.Rating> ratings = crit.ratings();
                                    if (ratings != null && !ratings.isEmpty()) {
                                        maxRatings = Math.max(maxRatings, ratings.size());
                                    }
                                }
                                if (maxRatings == 0) {
                                    callback.accept(new Result<>(ResultStatus.FAILURE, "The rubric has no ratings."));
                                    return;
                                }

                                List<String> header = templateHeader(maxRatings);
                                List<List<String>> rows = new ArrayList<>();

                                for (RubricModels.Criteria crit : rubric) {
                                    String description = crit.description();
                                    String longDesc = crit.longDescription();

                                    List<RubricModels.Rating> ratings = crit.ratings();
                                    List<String> row = new ArrayList<>();
                                    row.add(description);
                                    row.add(longDesc);

                                    int count = ratings != null ? ratings.size() : 0;
                                    for (int i = 0; i < maxRatings; i++) {
                                        if (i < count) {
                                            RubricModels.Rating r = ratings.get(i);
                                            row.add(r.description());
                                            row.add(r.points().toString());
                                            row.add(r.longDescription());
                                        } else {
                                            row.add("");
                                            row.add("");
                                            row.add("");
                                        }
                                    }
                                    rows.add(row);
                                }

                                StringWriter sw = new StringWriter();
                                try (PrintWriter writer = new PrintWriter(sw)) {
                                    writeCsvRow(writer, header);
                                    for (List<String> row : rows) {
                                        writeCsvRow(writer, row);
                                    }
                                }

                                callback.accept(new Result<>(ResultStatus.SUCCESS, sw.toString()));

                            } catch (Exception ex) {
                                callback.accept(new Result<>(ResultStatus.FAILURE, ex.getMessage()));
                            }
                        },
                        "download-and-convert-rubric")
                .start();
    }

    public CsvRubricParser.ParsedRubric parseRubricCsv(Path csvPath, boolean decodeHtml) throws IOException {
        return CsvRubricParser.parse(csvPath, decodeHtml);
    }

    public void updateAssignmentPoints(String courseId, String assignmentId, double totalPoints)
            throws IOException, URISyntaxException {
        this.canvasClient.updateAssignmentPoints(courseId, assignmentId, totalPoints);
    }

    public RubricModels.Created createRubric(
            String courseId,
            String assignmentId,
            String title,
            boolean freeForm,
            boolean useForGrading,
            boolean hideScoreTotal,
            String purpose,
            List<RubricModels.Criterion> criteria)
            throws IOException, URISyntaxException {

        var formFields = this.canvasClient.buildFormFieldsForRubricCreate(
                title, freeForm, criteria, Integer.parseInt(assignmentId), useForGrading, hideScoreTotal, purpose);

        return this.canvasClient.createRubric(courseId, formFields);
    }

    public List<String> templateHeader(int maxRatings) {
        List<String> header = new ArrayList<>();
        header.add("criterion");
        header.add("criterion_desc");

        for (int i = 1; i <= maxRatings; i++) {
            header.add("rating" + i);
            header.add("rating" + i + "_points");
            header.add("rating" + i + "_desc");
        }
        return header;
    }

    public List<String> defaultTemplateHeader() {
        return templateHeader(2);
    }

    public void writeTemplateToFile(Path file, List<String> header) throws IOException {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
            writeCsvRow(writer, header);
        }
    }

    public String headerAsCsvLine(List<String> header) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            writeCsvRow(pw, header);
        }
        return sw.toString();
    }

    private void writeCsvRow(PrintWriter writer, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                writer.print(",");
            }
            writer.print(escapeCsv(cells.get(i)));
        }
        writer.print("\n");
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        boolean hasSpecial =
                value.contains(",") || value.contains("\n") || value.contains("\r") || value.contains("\"");
        String escaped = value.replace("\"", "\"\"");
        return hasSpecial ? "\"" + escaped + "\"" : escaped;
    }
}
