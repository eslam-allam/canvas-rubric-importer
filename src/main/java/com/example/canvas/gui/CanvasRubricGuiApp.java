package com.example.canvas.gui;

import com.example.canvas.core.CanvasClient;
import com.example.canvas.core.CsvRubricParser;
import com.example.canvas.core.RubricModels;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class CanvasRubricGuiApp extends Application {

    private TextField baseUrlField;
    private PasswordField tokenField;
    private ListView<JsonNode> courseListView;
    private ListView<JsonNode> assignmentListView;
    private TextField courseIdField;
    private TextField assignmentIdField;
    private TextField titleField;
    private TextField csvPathField;
    private CheckBox freeFormCommentsCheck;
    private CheckBox useForGradingCheck;
    private CheckBox hideScoreTotalCheck;
    private CheckBox syncPointsCheck;
    private Label statusLabel;

    private ObservableList<JsonNode> courses = FXCollections.observableArrayList();
    private ObservableList<JsonNode> assignments = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Canvas Rubric Uploader");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        VBox topBox = new VBox(10);
        topBox.getChildren().add(buildConnectionPane());
        root.setTop(topBox);

        SplitPane centerPane = new SplitPane();
        centerPane.getItems().add(buildCoursesPane());
        centerPane.getItems().add(buildAssignmentsPane());
        centerPane.setDividerPositions(0.5);
        root.setCenter(centerPane);

        VBox bottomBox = new VBox(10);
        bottomBox.getChildren().add(buildRubricPane());
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private GridPane buildConnectionPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        Label baseUrlLabel = new Label("Base URL:");
        baseUrlField = new TextField("https://canvas.aubh.edu.bh");

        Label tokenLabel = new Label("Access token:");
        tokenField = new PasswordField();

        Button saveButton = new Button("Save Settings");
        saveButton.setOnAction(e -> onSaveSettings());

        grid.add(baseUrlLabel, 0, 0);
        grid.add(baseUrlField, 1, 0);
        grid.add(tokenLabel, 0, 1);
        grid.add(tokenField, 1, 1);
        grid.add(saveButton, 2, 0, 1, 2);

        return grid;
    }

    private VBox buildCoursesPane() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(5));
        Label label = new Label("Courses");

        courseListView = new ListView<>(courses);
        courseListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(JsonNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String name = item.path("name").asText("");
                    String code = item.path("course_code").asText("");
                    if (!code.isEmpty()) {
                        setText(name + " [" + code + "]");
                    } else {
                        setText(name);
                    }
                }
            }
        });

        courseListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> onCourseSelected(newV));

        Button loadCoursesBtn = new Button("Load Courses");
        loadCoursesBtn.setOnAction(e -> onLoadCourses());

        box.getChildren().addAll(label, courseListView, loadCoursesBtn);
        return box;
    }

    private VBox buildAssignmentsPane() {
        VBox box = new VBox(5);
        box.setPadding(new Insets(5));
        Label label = new Label("Assignments");

        assignmentListView = new ListView<>(assignments);
        assignmentListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(JsonNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String name = item.path("name").asText("");
                    setText(name);
                }
            }
        });

        assignmentListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> onAssignmentSelected(newV));

        Button loadAssignmentsBtn = new Button("Load Assignments");
        loadAssignmentsBtn.setOnAction(e -> onLoadAssignments());

        box.getChildren().addAll(label, assignmentListView, loadAssignmentsBtn);
        return box;
    }

    private GridPane buildRubricPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        courseIdField = new TextField();
        courseIdField.setEditable(false);
        assignmentIdField = new TextField();
        assignmentIdField.setEditable(false);
        titleField = new TextField();
        csvPathField = new TextField();

        freeFormCommentsCheck = new CheckBox("Free-form comments");
        freeFormCommentsCheck.setSelected(true);
        useForGradingCheck = new CheckBox("Use for grading");
        useForGradingCheck.setSelected(true);
        hideScoreTotalCheck = new CheckBox("Hide score total");
        syncPointsCheck = new CheckBox("Sync assignment points to rubric total");

        Button browseBtn = new Button("Browse...");
        browseBtn.setOnAction(e -> onBrowseCsv());

        Button downloadTemplateBtn = new Button("Download CSV Template");
        downloadTemplateBtn.setOnAction(e -> onDownloadTemplate());

        Button copyTemplateBtn = new Button("Copy Template to Clipboard");
        copyTemplateBtn.setOnAction(e -> onCopyTemplate());

        Button createBtn = new Button("Create Rubric");
        createBtn.setOnAction(e -> onCreate());

        Button quitBtn = new Button("Quit");
        quitBtn.setOnAction(e -> Platform.exit());

        statusLabel = new Label("Idle");

        int row = 0;
        grid.add(new Label("Selected Course ID:"), 0, row);
        grid.add(courseIdField, 1, row++);

        grid.add(new Label("Selected Assignment ID:"), 0, row);
        grid.add(assignmentIdField, 1, row++);

        grid.add(new Label("Rubric Title:"), 0, row);
        grid.add(titleField, 1, row++, 2, 1);

        grid.add(new Label("CSV File:"), 0, row);
        grid.add(csvPathField, 1, row);
        grid.add(browseBtn, 2, row++);

        grid.add(downloadTemplateBtn, 2, row++);
        grid.add(copyTemplateBtn, 2, row++);

        grid.add(freeFormCommentsCheck, 0, row++, 2, 1);
        grid.add(useForGradingCheck, 0, row++, 2, 1);
        grid.add(hideScoreTotalCheck, 0, row++, 2, 1);
        grid.add(syncPointsCheck, 0, row++, 3, 1);

        grid.add(statusLabel, 0, row++, 3, 1);

        HBox buttons = new HBox(10, createBtn, quitBtn);
        grid.add(buttons, 1, row, 2, 1);

        return grid;
    }

    private void onSaveSettings() {
        // simple in-memory only for now; could be extended to persist in a file
        showInfo("Saved", "Settings saved for this session.");
    }

    private void onLoadCourses() {
        String token = tokenField.getText().trim();
        if (token.isEmpty()) {
            showError("Error", "You must enter an access token first.");
            return;
        }
        String baseUrl = baseUrlField.getText().trim();
        setStatus("Loading courses...");
        new Thread(() -> {
            try {
                CanvasClient client = new CanvasClient(baseUrl, token);
                List<JsonNode> list = client.listCourses();
                Platform.runLater(() -> {
                    courses.setAll(list);
                    setStatus("Loaded " + list.size() + " courses");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Error", ex.getMessage()));
            }
        }, "load-courses").start();
    }

    private void onCourseSelected(JsonNode course) {
        if (course == null) {
            return;
        }
        String id = course.path("id").asText("");
        courseIdField.setText(id);
        assignments.clear();
        assignmentIdField.clear();
    }

    private void onLoadAssignments() {
        String token = tokenField.getText().trim();
        if (token.isEmpty()) {
            showError("Error", "You must enter an access token first.");
            return;
        }
        String baseUrl = baseUrlField.getText().trim();
        String courseId = courseIdField.getText().trim();
        if (courseId.isEmpty()) {
            showError("Error", "Please select a course first.");
            return;
        }
        setStatus("Loading assignments...");
        new Thread(() -> {
            try {
                CanvasClient client = new CanvasClient(baseUrl, token);
                List<JsonNode> list = client.listAssignments(courseId);
                Platform.runLater(() -> {
                    assignments.setAll(list);
                    setStatus("Loaded " + list.size() + " assignments");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> showError("Error", ex.getMessage()));
            }
        }, "load-assignments").start();
    }

    private void onAssignmentSelected(JsonNode assignment) {
        if (assignment == null) {
            return;
        }
        String id = assignment.path("id").asText("");
        assignmentIdField.setText(id);
        String name = assignment.path("name").asText(id);
        titleField.setText("Rubric for " + name);
    }

    private void onBrowseCsv() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            csvPathField.setText(file.getAbsolutePath());
        }
    }

    private List<String> templateHeader() {
        List<String> header = new ArrayList<>();
        header.add("criterion");
        header.add("criterion_desc");
        header.add("points");
        header.add("rating1");
        header.add("rating1_points");
        header.add("rating1_desc");
        header.add("rating2");
        header.add("rating2_points");
        header.add("rating2_desc");
        return header;
    }

    private void onDownloadTemplate() {
        if (assignmentIdField.getText().trim().isEmpty()) {
            showError("Error", "Please select an assignment first.");
            return;
        }
        String rubricTitle = titleField.getText().trim();
        if (rubricTitle.isEmpty()) {
            showError("Error", "Rubric title is required before downloading a template.");
            return;
        }
        String safeName = rubricTitle.replaceAll("[^A-Za-z0-9_.-]+", "_");
        if (safeName.isBlank()) {
            safeName = "rubric";
        }

        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(safeName + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(null);
        if (file == null) {
            return;
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(String.join(",", templateHeader()));
            writer.write("\n");
            showInfo("Template saved", "Saved rubric CSV template to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            showError("Error", "Could not save template: " + ex.getMessage());
        }
    }

    private void onCopyTemplate() {
        if (assignmentIdField.getText().trim().isEmpty()) {
            showError("Error", "Please select an assignment first.");
            return;
        }
        String rubricTitle = titleField.getText().trim();
        if (rubricTitle.isEmpty()) {
            showError("Error", "Rubric title is required before copying a template.");
            return;
        }

        String header = String.join(",", templateHeader());
        ClipboardContent content = new ClipboardContent();
        content.putString(header + "\n");
        Clipboard.getSystemClipboard().setContent(content);
        showInfo("Template copied", "Rubric CSV header template copied to clipboard.");
    }

    private void onCreate() {
        String token = tokenField.getText().trim();
        if (token.isEmpty()) {
            showError("Error", "You must enter an access token first.");
            return;
        }
        String courseId = courseIdField.getText().trim();
        if (courseId.isEmpty()) {
            showError("Error", "Please select a course.");
            return;
        }
        String assignmentId = assignmentIdField.getText().trim();
        if (assignmentId.isEmpty()) {
            showError("Error", "Please select an assignment.");
            return;
        }
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            showError("Error", "Rubric title is required.");
            return;
        }
        String csvPath = csvPathField.getText().trim();
        if (csvPath.isEmpty()) {
            showError("Error", "CSV file is required.");
            return;
        }

        String baseUrl = baseUrlField.getText().trim();
        boolean freeForm = freeFormCommentsCheck.isSelected();
        boolean useForGrading = useForGradingCheck.isSelected();
        boolean hideScoreTotal = hideScoreTotalCheck.isSelected();
        boolean syncPoints = syncPointsCheck.isSelected();

        setStatus("Reading CSV...");
        new Thread(() -> {
            try {
                CsvRubricParser parser = new CsvRubricParser();
                CsvRubricParser.ParsedRubric parsed = parser.parse(Path.of(csvPath));
                List<RubricModels.Criterion> criteria = parsed.criteria();
                double total = parsed.totalPoints();

                CanvasClient client = new CanvasClient(baseUrl, token);
                var formFields = client.buildFormFieldsForRubricCreate(
                    title,
                    freeForm,
                    criteria,
                    Integer.parseInt(assignmentId),
                    useForGrading,
                    hideScoreTotal,
                    "grading"
                );

                if (syncPoints) {
                    Platform.runLater(() -> setStatus("Updating assignment points..."));
                    client.updateAssignmentPoints(courseId, assignmentId, total);
                }

                Platform.runLater(() -> setStatus("Creating rubric..."));
                JsonNode response = client.createRubric(courseId, formFields);
                String rubricId = response.path("rubric").path("id").asText("");
                String assocId = response.path("rubric_association").path("id").asText("");

                Platform.runLater(() -> {
                    setStatus("Done");
                    showInfo("Success", "Rubric created successfully!\nRubric ID: " + rubricId + "\nAssociation ID: " + assocId);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setStatus("Error");
                    showError("Error", ex.getMessage());
                });
            }
        }, "create-rubric").start();
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
