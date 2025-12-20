package io.github.eslam_allam.canvas.cli;

import io.github.eslam_allam.canvas.core.CanvasClient;
import io.github.eslam_allam.canvas.core.CsvRubricParser;
import io.github.eslam_allam.canvas.core.RubricModels;
import java.nio.file.Path;
import java.util.List;

public final class CliApp {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String baseUrl = "https://canvas.aubh.edu.bh";
        String courseId = null;
        String assignmentId = null;
        String title = null;
        Path csvPath = null;
        String token = System.getenv("CANVAS_TOKEN");
        boolean freeFormComments = true;
        boolean useForGrading = true;
        boolean hideScoreTotal = false;
        String purpose = "grading";
        boolean syncAssignmentPoints = false;
        boolean dryRun = false;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--base-url" -> baseUrl = args[++i];
                case "--course-id" -> courseId = args[++i];
                case "--assignment-id" -> assignmentId = args[++i];
                case "--title" -> title = args[++i];
                case "--csv" -> csvPath = Path.of(args[++i]);
                case "--token" -> token = args[++i];
                case "--free-form-comments" -> freeFormComments = Boolean.parseBoolean(args[++i]);
                case "--use-for-grading" -> useForGrading = Boolean.parseBoolean(args[++i]);
                case "--hide-score-total" -> hideScoreTotal = Boolean.parseBoolean(args[++i]);
                case "--purpose" -> purpose = args[++i];
                case "--sync-assignment-points" ->
                        syncAssignmentPoints = Boolean.parseBoolean(args[++i]);
                case "--dry-run" -> dryRun = true;
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    printUsage();
                    System.exit(1);
                }
            }
        }

        if (token == null || token.isBlank()) {
            System.err.println("Missing token (set CANVAS_TOKEN or pass --token).");
            System.exit(2);
        }
        if (courseId == null || assignmentId == null || title == null || csvPath == null) {
            System.err.println("Missing required arguments.");
            printUsage();
            System.exit(1);
        }

        boolean decodeHtml = true;
        for (String arg : args) {
            if ("--no-html-decode".equals(arg)) {
                decodeHtml = false;
            }
        }

        CsvRubricParser parser = new CsvRubricParser(decodeHtml);

        CsvRubricParser.ParsedRubric parsed = parser.parse(csvPath);
        List<RubricModels.Criterion> criteria = parsed.criteria();
        double totalPoints = parsed.totalPoints();

        CanvasClient client = new CanvasClient(baseUrl, token);
        var formFields =
                client.buildFormFieldsForRubricCreate(
                        title,
                        freeFormComments,
                        criteria,
                        Integer.parseInt(assignmentId),
                        useForGrading,
                        hideScoreTotal,
                        purpose);

        if (dryRun) {
            System.out.println("# Rubric total (sum of criterion points): " + totalPoints);
            formFields.keySet().stream()
                    .filter(k -> k.startsWith("rubric[criteria][0]"))
                    .sorted()
                    .limit(20)
                    .forEach(System.out::println);
            return;
        }

        if (syncAssignmentPoints) {
            client.updateAssignmentPoints(courseId, assignmentId, totalPoints);
            System.out.println("Updated assignment points_possible to " + totalPoints);
        }

        var response = client.createRubric(courseId, formFields);
        var rubricId = response.path("rubric").path("id").asText();
        var assocId = response.path("rubric_association").path("id").asText();
        System.out.println("Created rubric + association.");
        System.out.println("Rubric ID: " + rubricId);
        System.out.println("Association ID: " + assocId);
    }

    private static void printUsage() {
        System.out.println(
                "Usage: java -jar app.jar --course-id <id> --assignment-id <id> --title <title>"
                        + " --csv <file> [options]");
        System.out.println("Options:");
        System.out.println(
                "  --no-html-decode   Do not decode HTML entities in text fields (e.g., &amp;lt;,"
                        + " -&gt;).");
    }
}
