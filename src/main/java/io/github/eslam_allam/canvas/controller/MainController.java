package io.github.eslam_allam.canvas.controller;

import io.github.eslam_allam.canvas.domain.ResultStatus;
import io.github.eslam_allam.canvas.domain.RubricRow;
import io.github.eslam_allam.canvas.model.canvas.Assignment;
import io.github.eslam_allam.canvas.model.canvas.Course;
import io.github.eslam_allam.canvas.model.canvas.RubricModels;
import io.github.eslam_allam.canvas.navigation.StageManager;
import io.github.eslam_allam.canvas.notification.PopUp;
import io.github.eslam_allam.canvas.notification.StatusNotifier;
import io.github.eslam_allam.canvas.rubric.importing.csv.CsvRubricParser;
import io.github.eslam_allam.canvas.rubric.importing.csv.RatingHeaderDetector;
import io.github.eslam_allam.canvas.rubric.importing.csv.RatingHeaderDetector.RatingGroup;
import io.github.eslam_allam.canvas.service.CanvasRubricService;
import io.github.eslam_allam.canvas.view.component.ConnectionPanel;
import io.github.eslam_allam.canvas.view.component.ListPane;
import io.github.eslam_allam.canvas.view.component.StatusLabel;
import io.github.eslam_allam.canvas.view.section.CoursesAndAssignmentsSection;
import io.github.eslam_allam.canvas.viewmodel.ListPaneVM;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

public class MainController {
    private final ConnectionPanel connectionPanel;
    private final StatusLabel statusLabel;
    private final StatusNotifier statusNotifier;

    private final ListPane<Course> coursePane;
    private final ListPane<Assignment> assignmentPane;
    private final ListPaneVM<Assignment> assignmentPaneVM;
    private final CoursesAndAssignmentsSection coursesAndAssignmentsSection;

    private final CanvasRubricService rubricService;
    private final StageManager stageManager;

    // UI state and controls
    private SplitPane mainCenterPane;
    private BorderPane root;
    private TableView<RubricRow> rubricPreviewTable;

    public MainController(
            ConnectionPanel connectionPanel,
            StatusLabel statusLabel,
            CanvasRubricService rubricService,
            StageManager stageManager,
            StatusNotifier statusNotifier,
            ListPane<Course> coursePane,
            ListPaneVM<Course> coursePaneVM,
            ListPane<Assignment> assignmentPane,
            ListPaneVM<Assignment> assignmentPaneVM) {
        this.rubricService = rubricService;
        this.stageManager = stageManager;
        this.statusLabel = statusLabel;
        this.coursePane = coursePane;
        this.assignmentPane = assignmentPane;
        this.assignmentPaneVM = assignmentPaneVM;

        this.connectionPanel = connectionPanel;
        this.statusNotifier = statusNotifier;

        this.coursesAndAssignmentsSection = new CoursesAndAssignmentsSection(coursePane, assignmentPane);

        coursePaneVM.onSelectedChange(this::onCourseSelected);
        assignmentPaneVM.onSelectedChange(this::onAssignmentSelected);
    }

    public void initAndShow() {
        root = new BorderPane();
        root.getStyleClass().add("app-root");

        VBox topBox = new VBox(10);
        topBox.getStyleClass().add("top-bar");
        topBox.getChildren().add(wrapInCard("Canvas Connection", connectionPanel.getRoot()));
        root.setTop(topBox);

        root.setCenter(this.coursesAndAssignmentsSection.getRoot());
        BorderPane.setMargin(this.coursesAndAssignmentsSection.getRoot(), new Insets(5, 0, 5, 0));

        VBox bottomBox = new VBox(10);
        bottomBox.getChildren().add(wrapInCard("Rubric Configuration", buildRubricPane()));
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 1000, 650);
        scene.getStylesheets()
                .add(Objects.requireNonNull(getClass().getResource("/style.css"), "style.css not found on classpath")
                        .toExternalForm());

        stageManager.show(scene);
    }

    private VBox wrapInCard(String title, Node content) {
        VBox box = new VBox(6);
        box.getStyleClass().add("section-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");

        if (content instanceof TableView) {
            VBox.setVgrow(content, javafx.scene.layout.Priority.ALWAYS);
        }

        box.getChildren().addAll(titleLabel, content);
        return box;
    }

    private void onCourseSelected(Course course) {
        if (course == null) {
            return;
        }
        String id = course.id().toString();
        courseIdField.setText(id);
        this.assignmentPaneVM.items().clear();
        assignmentIdField.clear();
    }

    private void onAssignmentSelected(Assignment assignment) {
        if (assignment == null) {
            return;
        }
        String id = assignment.id().toString();
        assignmentIdField.setText(id);
        String name = assignment.name();
        titleField.setText("Rubric for " + name);
    }

    private void onDownloadTemplate() {
        String rubricTitle = titleField.getText().trim();
        if (rubricTitle.isEmpty()) {
            PopUp.showError("Error", "Rubric title is required before downloading a template.");
            return;
        }
        String safeName = rubricTitle.replaceAll("[^A-Za-z0-9_.-]+", "_");

        if (safeName.isBlank()) {
            safeName = "rubric";
        }

        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(safeName + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(stageManager.getPrimaryStage());
        if (file == null) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(file)) {
            writeCsvRow(writer, rubricService.defaultTemplateHeader());
            PopUp.showInfo("Template saved", "Saved rubric CSV template to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {

            PopUp.showError("Error", "Could not save template: " + ex.getMessage());
        }
    }

    private void onCopyTemplate() {
        String rubricTitle = titleField.getText().trim();
        if (rubricTitle.isEmpty()) {
            PopUp.showError("Error", "Rubric title is required before copying a template.");
            return;
        }

        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            writeCsvRow(pw, rubricService.defaultTemplateHeader());
        }
        String header = sw.toString();

        ClipboardContent content = new ClipboardContent();
        content.putString(header);

        Clipboard.getSystemClipboard().setContent(content);
        PopUp.showInfo("Template copied", "Rubric CSV header template copied to clipboard.");
    }

    private void onPasteCsvFromClipboard() {
        String csvText = Clipboard.getSystemClipboard().getString();
        if (csvText == null || csvText.trim().isEmpty()) {
            PopUp.showError("Error", "Clipboard does not contain any text.");
            return;
        }

        csvText = csvText.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = csvText.split("\n");
        if (lines.length < 2) {
            PopUp.showError("Error", "Clipboard CSV must include a header row and at least one data row.");
            return;
        }

        java.util.List<String> headerCells;
        try {
            java.io.StringReader sr = new java.io.StringReader(csvText);
            try (org.apache.commons.csv.CSVParser parser = org.apache.commons.csv.CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build()
                    .parse(sr)) {
                headerCells = parser.getHeaderNames();
            }
        } catch (IOException ex) {
            PopUp.showError("Error", "Failed to parse clipboard CSV header: " + ex.getMessage());
            return;
        }

        if (headerCells == null || headerCells.isEmpty()) {
            PopUp.showError("Error", "Clipboard CSV has an empty header row.");
            return;
        }

        String[] headerArray = headerCells.toArray(String[]::new);

        java.util.List<RatingGroup> ratingGroups = RatingHeaderDetector.detect(headerArray);

        if (ratingGroups.size() < 2) {
            PopUp.showError(
                    "Error",
                    "Clipboard CSV must have at least rating1/rating1_points/rating1_desc and"
                            + " rating2/rating2_points/rating2_desc columns.");
            return;
        }

        java.util.Set<String> allowed = new java.util.HashSet<>();
        allowed.add("criterion");
        allowed.add("criterion_desc");
        for (RatingGroup g : ratingGroups) {
            allowed.add(g.nameColumn());
            allowed.add(g.pointsColumn());
            allowed.add(g.descColumn());
        }

        java.util.List<String> extra = new java.util.ArrayList<>();
        for (String h : headerArray) {
            if (!allowed.contains(h)) {
                extra.add(h);
            }
        }
        if (!extra.isEmpty()) {
            PopUp.showError("Error", "Unexpected column(s) in clipboard CSV header: " + String.join(", ", extra));
            return;
        }

        try {
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("canvas_rubric_", ".csv");
            java.nio.file.Files.writeString(tempFile, csvText, java.nio.charset.StandardCharsets.UTF_8);
            csvPathField.setText(tempFile.toAbsolutePath().toString());
            PopUp.showInfo(
                    "Clipboard CSV saved",
                    "Saved clipboard rubric CSV to temporary file:\n" + tempFile.toAbsolutePath());
        } catch (IOException ex) {
            PopUp.showError("Error", "Failed to save clipboard CSV to temporary file: " + ex.getMessage());
        }
    }

    private void onCopyRubricCsv() {
        String courseId = courseIdField.getText().trim();
        if (courseId.isEmpty()) {
            PopUp.showError("Error", "Please select a course.");
            return;
        }
        String assignmentId = assignmentIdField.getText().trim();
        if (assignmentId.isEmpty()) {
            PopUp.showError("Error", "Please select an assignment.");
            return;
        }

        this.statusNotifier.setStatus("Downloading rubric...");
        rubricService.getCanvasRubricCSV(courseId, assignmentId, result -> {
            if (result.status() == ResultStatus.FAILURE) {
                Platform.runLater(() -> {
                    this.statusNotifier.setStatus("Error");
                    PopUp.showError("Failed to fetch rubric", result.data());
                });
                return;
            } else {

                Platform.runLater(() -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(result.data());

                    Clipboard.getSystemClipboard().setContent(content);
                    this.statusNotifier.setStatus("Copied to clipboard");
                    PopUp.showInfo("Done", "Rubric copied to clipboard successfully.");
                });
            }
        });
    }

    private void onDownloadRubricCsv() {
        String courseId = courseIdField.getText().trim();
        if (courseId.isEmpty()) {
            PopUp.showError("Error", "Please select a course.");
            return;
        }
        String assignmentId = assignmentIdField.getText().trim();
        if (assignmentId.isEmpty()) {
            PopUp.showError("Error", "Please select an assignment.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        chooser.setInitialFileName("rubric_assignment_" + assignmentId + ".csv");
        File file = chooser.showSaveDialog(stageManager.getPrimaryStage());
        if (file == null) {
            return;
        }

        this.statusNotifier.setStatus("Downloading rubric...");
        rubricService.getCanvasRubricCSV(courseId, assignmentId, result -> {
            if (result.status() == ResultStatus.FAILURE) {
                Platform.runLater(() -> {
                    this.statusNotifier.setStatus("Error");
                    PopUp.showError("Failed to Download Rubric", result.data());
                });
            } else {
                try {
                    Files.writeString(file.toPath(), result.data(), StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        this.statusNotifier.setStatus("Error");
                        PopUp.showError("Failed to Save Rubric", ex.getMessage());
                    });
                    return;
                }
                Platform.runLater(() -> {
                    this.statusNotifier.setStatus("Done");
                    PopUp.showInfo("Rubric Downloaded", "Saved rubric CSV to:\n" + file.getAbsolutePath());
                });
            }
        });
    }

    private void onCreate() {
        String courseId = courseIdField.getText().trim();
        if (courseId.isEmpty()) {
            PopUp.showError("Error", "Please select a course.");
            return;
        }
        String assignmentId = assignmentIdField.getText().trim();
        if (assignmentId.isEmpty()) {
            PopUp.showError("Error", "Please select an assignment.");
            return;
        }
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            PopUp.showError("Error", "Rubric title is required.");
            return;
        }
        String csvPath = csvPathField.getText().trim();
        if (csvPath.isEmpty()) {
            PopUp.showError("Error", "CSV file is required.");
            return;
        }

        boolean freeForm = freeFormCommentsCheck.isSelected();
        boolean useForGrading = useForGradingCheck.isSelected();
        boolean hideScoreTotal = hideScoreTotalCheck.isSelected();
        boolean syncPoints = syncPointsCheck.isSelected();

        this.statusNotifier.setStatus("Reading CSV...");
        new Thread(
                        () -> {
                            try {
                                CsvRubricParser parser = new CsvRubricParser(decodeHtmlCheck.isSelected());

                                CsvRubricParser.ParsedRubric parsed = parser.parse(Path.of(csvPath));
                                List<RubricModels.Criterion> criteria = parsed.criteria();
                                double total = parsed.totalPoints();

                                if (syncPoints) {
                                    Platform.runLater(
                                            () -> this.statusNotifier.setStatus("Updating assignment points..."));
                                    rubricService.updateAssignmentPoints(courseId, assignmentId, total);
                                }

                                Platform.runLater(() -> this.statusNotifier.setStatus("Creating rubric..."));
                                RubricModels.Created response = rubricService.createRubric(
                                        courseId,
                                        assignmentId,
                                        title,
                                        freeForm,
                                        useForGrading,
                                        hideScoreTotal,
                                        "grading",
                                        criteria);
                                String rubricTitle = response.rubric().title();
                                String assocId =
                                        response.rubricAssociation().id().toString();

                                Platform.runLater(() -> {
                                    this.statusNotifier.setStatus("Done");
                                    PopUp.showInfo(
                                            "Success",
                                            "Rubric created successfully!\nRubric: "
                                                    + rubricTitle
                                                    + "\nAssociation ID: "
                                                    + assocId);
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() -> {
                                    this.statusNotifier.setStatus("Error");
                                    PopUp.showError("Error", ex.getMessage());
                                });
                            }
                        },
                        "create-rubric")
                .start();
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
