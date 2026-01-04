package io.github.eslam_allam.canvas.controller;

import io.github.eslam_allam.canvas.client.CanvasClient;
import io.github.eslam_allam.canvas.domain.ResultStatus;
import io.github.eslam_allam.canvas.domain.RubricRow;
import io.github.eslam_allam.canvas.model.canvas.Assignment;
import io.github.eslam_allam.canvas.model.canvas.Course;
import io.github.eslam_allam.canvas.model.canvas.RubricModels;
import io.github.eslam_allam.canvas.navigation.StageManager;
import io.github.eslam_allam.canvas.rubric.importing.csv.CsvRubricParser;
import io.github.eslam_allam.canvas.rubric.importing.csv.RatingHeaderDetector;
import io.github.eslam_allam.canvas.rubric.importing.csv.RatingHeaderDetector.RatingGroup;
import io.github.eslam_allam.canvas.service.CanvasRubricService;
import io.github.eslam_allam.canvas.service.PreferencesService;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

public class MainController {

    private final PreferencesService preferencesService;
    private final CanvasRubricService rubricService;
    private final StageManager stageManager;
    private final CanvasClient canvasClient;

    // UI state and controls
    private BorderPane root;
    private SplitPane mainCenterPane;

    private TextField baseUrlField;
    private PasswordField tokenField;
    private ListView<Course> courseListView;
    private ListView<Assignment> assignmentListView;
    private TextField courseIdField;
    private TextField assignmentIdField;
    private TextField titleField;
    private TextField csvPathField;
    private CheckBox freeFormCommentsCheck;
    private CheckBox useForGradingCheck;
    private CheckBox hideScoreTotalCheck;
    private CheckBox syncPointsCheck;
    private CheckBox decodeHtmlCheck;
    private Button showPreviewBtn;
    private Button backBtn;
    private Label statusLabel;
    private TableView<RubricRow> rubricPreviewTable;

    private final ObservableList<Course> courses = FXCollections.observableArrayList();
    private final ObservableList<Assignment> assignments = FXCollections.observableArrayList();

    public MainController(
            PreferencesService preferencesService,
            CanvasRubricService rubricService,
            StageManager stageManager,
            CanvasClient canvasClient) {
        this.preferencesService = preferencesService;
        this.rubricService = rubricService;
        this.stageManager = stageManager;
        this.canvasClient = canvasClient;
    }

    public void initAndShow() {
        root = new BorderPane();
        root.getStyleClass().add("app-root");

        VBox topBox = new VBox(10);
        topBox.getStyleClass().add("top-bar");
        topBox.getChildren().add(wrapInCard("Canvas Connection", buildConnectionPane()));
        root.setTop(topBox);

        loadSettings();

        mainCenterPane = new SplitPane();
        mainCenterPane.getStyleClass().add("section-card");
        mainCenterPane.getItems().add(buildCoursesPane());
        mainCenterPane.getItems().add(buildAssignmentsPane());
        mainCenterPane.setDividerPositions(0.5);
        root.setCenter(mainCenterPane);
        BorderPane.setMargin(mainCenterPane, new Insets(5, 0, 5, 0));

        VBox bottomBox = new VBox(10);
        bottomBox.getChildren().add(wrapInCard("Rubric Configuration", buildRubricPane()));
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 1000, 650);
        scene.getStylesheets()
                .add(Objects.requireNonNull(getClass().getResource("/style.css"), "style.css not found on classpath")
                        .toExternalForm());

        stageManager.show(scene);
    }

    private GridPane buildConnectionPane() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);

        Label baseUrlLabel = new Label("Base URL:");
        baseUrlField = new TextField();

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
        VBox box = new VBox(10);
        box.setPadding(new Insets(5));
        Label label = new Label("Courses");

        courseListView = new ListView<>(courses);
        courseListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Course item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String name = item.name();
                    String code = item.courseCode();
                    if (!code.isEmpty()) {
                        setText(name + " [" + code + "]");
                    } else {
                        setText(name);
                    }
                }
            }
        });

        courseListView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldV, newV) -> onCourseSelected(newV));

        Button loadCoursesBtn = new Button("Load Courses");
        loadCoursesBtn.setOnAction(e -> onLoadCourses());

        box.getChildren().addAll(label, courseListView, loadCoursesBtn);
        return box;
    }

    private VBox buildAssignmentsPane() {
        VBox box = new VBox(10);
        box.setPadding(new Insets(5));
        Label label = new Label("Assignments");

        assignmentListView = new ListView<>(assignments);
        assignmentListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Assignment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String name = item.name();
                    setText(name);
                }
            }
        });

        assignmentListView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldV, newV) -> onAssignmentSelected(newV));

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
        courseIdField.getStyleClass().add("readonly");

        assignmentIdField = new TextField();
        assignmentIdField.setEditable(false);
        assignmentIdField.getStyleClass().add("readonly");

        titleField = new TextField();
        csvPathField = new TextField();

        freeFormCommentsCheck = new CheckBox("Free-form comments");
        freeFormCommentsCheck.setSelected(true);
        useForGradingCheck = new CheckBox("Use for grading");
        useForGradingCheck.setSelected(true);
        hideScoreTotalCheck = new CheckBox("Hide score total");
        syncPointsCheck = new CheckBox("Sync assignment points to rubric total");
        decodeHtmlCheck = new CheckBox("Decode HTML entities in CSV text");
        decodeHtmlCheck.setSelected(true);

        Button browseBtn = new Button("Browse...");
        browseBtn.setOnAction(e -> onBrowseCsv());

        Button pasteCsvFromClipboardBtn = new Button("Paste CSV from Clipboard");
        pasteCsvFromClipboardBtn.setOnAction(e -> onPasteCsvFromClipboard());

        showPreviewBtn = new Button("Show Preview");
        showPreviewBtn.setOnAction(e -> showPreviewFullHeight());
        showPreviewBtn.setVisible(false);
        showPreviewBtn.setManaged(false);

        backBtn = new Button("Back to Main View");
        backBtn.setOnAction(e -> showMainView());
        backBtn.setVisible(false);
        backBtn.setManaged(false);

        Button downloadTemplateBtn = new Button("Download CSV Template");
        downloadTemplateBtn.setOnAction(e -> onDownloadTemplate());

        Button copyTemplateBtn = new Button("Copy Template to Clipboard");
        copyTemplateBtn.setOnAction(e -> onCopyTemplate());

        Button downloadRubricBtn = new Button("Download Rubric as CSV");
        downloadRubricBtn.setOnAction(e -> onDownloadRubricCsv());

        Button copyRubricBtn = new Button("Copy Rubric as CSV");
        copyRubricBtn.setOnAction(e -> onCopyRubricCsv());

        HBox canvasRubricButtons = new HBox(5, downloadRubricBtn, copyRubricBtn);

        Button createBtn = new Button("Create Rubric");
        createBtn.setOnAction(e -> onCreate());

        Button quitBtn = new Button("Quit");
        quitBtn.setOnAction(e -> Platform.exit());

        statusLabel = new Label("Idle");
        statusLabel.getStyleClass().add("status-label");

        int row = 0;

        grid.add(new Label("Selected Course ID:"), 0, row);
        grid.add(courseIdField, 1, row++);

        grid.add(new Label("Selected Assignment ID:"), 0, row);
        grid.add(assignmentIdField, 1, row++);

        grid.add(new Label("Rubric Title:"), 0, row);
        grid.add(titleField, 1, row++, 2, 1);

        grid.add(new Label("CSV File:"), 0, row);
        grid.add(csvPathField, 1, row);

        HBox csvButtons = new HBox(5, browseBtn, pasteCsvFromClipboardBtn, showPreviewBtn, backBtn);
        grid.add(csvButtons, 2, row++);

        csvPathField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasCsv = newVal != null
                    && !newVal.trim().isEmpty()
                    && newVal.trim().toLowerCase().endsWith(".csv");
            showPreviewBtn.setVisible(hasCsv);
            showPreviewBtn.setManaged(hasCsv);
        });

        HBox templateButtons = new HBox(5, downloadTemplateBtn, copyTemplateBtn);
        grid.add(templateButtons, 2, row++);

        grid.add(canvasRubricButtons, 2, row++);

        VBox rubricOptions = new VBox(
                5,
                new HBox(10, freeFormCommentsCheck, useForGradingCheck, hideScoreTotalCheck),
                new HBox(10, syncPointsCheck, decodeHtmlCheck));
        grid.add(rubricOptions, 0, row, 6, 3);
        GridPane.setMargin(rubricOptions, new Insets(5));
        row += 3;

        grid.add(statusLabel, 0, row++, 3, 1);

        HBox buttons = new HBox(10, createBtn, quitBtn);
        buttons.getStyleClass().add("bottom-actions");
        grid.add(buttons, 0, row, 3, 1);

        return grid;
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

    private void onSaveSettings() {
        preferencesService.saveSettings(baseUrlField.getText(), tokenField.getText());
        showInfo("Saved", "Settings saved.");
    }

    private void loadSettings() {
        baseUrlField.setText(preferencesService.loadBaseUrl());
        tokenField.setText(preferencesService.loadToken());
    }

    private void onLoadCourses() {
        setStatus("Loading courses...");
        new Thread(
                        () -> {
                            try {
                                List<Course> list = this.canvasClient.listCourses();
                                Platform.runLater(() -> {
                                    courses.setAll(list);
                                    setStatus("Loaded " + list.size() + " courses");
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() -> showError("Error", ex.getMessage()));
                            }
                        },
                        "load-courses")
                .start();
    }

    private void onCourseSelected(Course course) {
        if (course == null) {
            return;
        }
        String id = course.id().toString();
        courseIdField.setText(id);
        assignments.clear();
        assignmentIdField.clear();
    }

    private void onLoadAssignments() {
        String courseId = courseIdField.getText().trim();
        if (courseId.isEmpty()) {
            showError("Error", "Please select a course first.");
            return;
        }
        setStatus("Loading assignments...");
        new Thread(
                        () -> {
                            try {
                                List<Assignment> list = this.canvasClient.listAssignments(courseId);
                                Platform.runLater(() -> {
                                    assignments.setAll(list);
                                    setStatus("Loaded " + list.size() + " assignments");
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() -> showError("Error", ex.getMessage()));
                            }
                        },
                        "load-assignments")
                .start();
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

    private void onBrowseCsv() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showOpenDialog(stageManager.getPrimaryStage());
        if (file != null) {
            csvPathField.setText(file.getAbsolutePath());
        }
    }

    private void showPreviewFullHeight() {
        String path = csvPathField.getText().trim();
        if (path.isEmpty()) {
            showError("Error", "Please select a CSV file first.");
            return;
        }
        loadRubricPreview(Path.of(path));
    }

    private void showMainView() {
        root.setCenter(mainCenterPane);
        if (showPreviewBtn != null) {
            showPreviewBtn.setVisible(true);
            showPreviewBtn.setManaged(true);
        }
        if (backBtn != null) {
            backBtn.setVisible(false);
            backBtn.setManaged(false);
        }
    }

    private TableView<RubricRow> buildRubricPreviewTable(int maxRatings) {

        TableView<RubricRow> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("rubric-preview-table");

        TableColumn<RubricRow, String> critCol = new TableColumn<>("Criterion");
        critCol.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getCriterion()));
        enableWrappingCellFactory(critCol);

        TableColumn<RubricRow, String> pointsCol = new TableColumn<>("Points");
        pointsCol.setCellValueFactory(
                cell -> new ReadOnlyStringWrapper(cell.getValue().getPoints()));
        enableWrappingCellFactory(pointsCol);

        java.util.List<TableColumn<RubricRow, RubricModels.Rating>> ratingCols = new java.util.ArrayList<>();
        for (int i = 0; i < maxRatings; i++) {
            final int idx = i;

            TableColumn<RubricRow, RubricModels.Rating> ratingCol = new TableColumn<>("");
            ratingCol.setCellValueFactory(cell -> {
                java.util.List<RubricModels.Rating> ratings = cell.getValue().getRatings();
                if (idx >= ratings.size()) {
                    return new javafx.beans.property.ReadOnlyObjectWrapper<>(null);
                }
                return new javafx.beans.property.ReadOnlyObjectWrapper<>(ratings.get(idx));
            });

            ratingCol.setCellFactory(col -> new TableCell<>() {
                private final Text text = new Text();

                {
                    text.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
                    setGraphic(text);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }

                @Override
                protected void updateItem(RubricModels.Rating item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        text.setText("");
                    } else {
                        String title = item.description() == null ? "" : item.description();
                        String pts = Double.toString(item.points());
                        String headerText = title.isEmpty() ? pts : (title + " (" + pts + ")");
                        String desc = item.longDescription() == null ? "" : item.longDescription();

                        if (desc.isEmpty()) {
                            text.setText(headerText);
                        } else {
                            text.setText(headerText + "\n\n" + desc);
                        }
                    }
                }
            });

            ratingCols.add(ratingCol);
        }

        TableColumn<RubricRow, RubricModels.Rating> ratingsParentCol = new TableColumn<>("Ratings");
        ratingsParentCol.getColumns().addAll(ratingCols);

        table.getColumns().addAll(critCol, ratingsParentCol, pointsCol);
        return table;
    }

    private void enableWrappingCellFactory(TableColumn<RubricRow, String> column) {
        column.setCellFactory(col -> new TableCell<>() {
            private final Text text = new Text();

            {
                text.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
                setGraphic(text);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    text.setText("");
                } else {
                    text.setText(item);
                }
            }
        });
    }

    private Pane buildFullHeightPreviewPane() {

        BorderPane pane = new BorderPane();

        pane.setCenter(wrapInCard("Rubric Preview", rubricPreviewTable));

        return pane;
    }

    private void loadRubricPreview(Path csvPath) {

        setStatus("Loading preview...");
        new Thread(
                        () -> {
                            try {
                                CsvRubricParser parser = new CsvRubricParser(decodeHtmlCheck.isSelected());

                                CsvRubricParser.ParsedRubric parsed = parser.parse(csvPath);

                                List<RubricModels.Criterion> criteria = parsed.criteria();

                                List<RubricRow> rows = new ArrayList<>();
                                int maxRatings = 0;
                                double totalPoints = 0;
                                for (RubricModels.Criterion c : criteria) {
                                    List<RubricModels.Rating> ratings = c.ratings();
                                    maxRatings = Math.max(maxRatings, ratings.size());

                                    double maxPoints = ratings.stream()
                                            .mapToDouble(RubricModels.Rating::points)
                                            .max()
                                            .orElse(0.0);

                                    totalPoints += maxPoints;
                                    rows.add(new RubricRow(
                                            c.name(), c.description(), Double.toString(maxPoints), ratings));
                                }

                                final int finalMaxRatings = maxRatings;
                                final double finalTotalPoints = totalPoints;

                                Platform.runLater(() -> {
                                    if (showPreviewBtn != null) {
                                        showPreviewBtn.setVisible(false);
                                        showPreviewBtn.setManaged(false);
                                    }
                                    if (backBtn != null) {
                                        backBtn.setVisible(true);
                                        backBtn.setManaged(true);
                                    }
                                    rubricPreviewTable = buildRubricPreviewTable(finalMaxRatings);
                                    rubricPreviewTable.getItems().setAll(rows);

                                    RubricRow totalRow = new RubricRow(
                                            "Total",
                                            "",
                                            Double.toString(finalTotalPoints),
                                            java.util.Collections.emptyList());
                                    rubricPreviewTable.getItems().add(totalRow);

                                    Pane previewPane = buildFullHeightPreviewPane();
                                    root.setCenter(previewPane);

                                    BorderPane.setMargin(previewPane, new Insets(5, 0, 5, 0));
                                    setStatus("Preview loaded");
                                });

                            } catch (Exception ex) {
                                Platform.runLater(() -> {
                                    if (rubricPreviewTable != null) {
                                        rubricPreviewTable.getItems().clear();
                                    }
                                    showError("Error", "Could not load preview: " + ex.getMessage());
                                    setStatus("Preview error");
                                });
                            }
                        },
                        "rubric-preview")
                .start();
    }

    private void onDownloadTemplate() {
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
        File file = chooser.showSaveDialog(stageManager.getPrimaryStage());
        if (file == null) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(file)) {
            writeCsvRow(writer, rubricService.defaultTemplateHeader());
            showInfo("Template saved", "Saved rubric CSV template to:\n" + file.getAbsolutePath());
        } catch (IOException ex) {

            showError("Error", "Could not save template: " + ex.getMessage());
        }
    }

    private void onCopyTemplate() {
        String rubricTitle = titleField.getText().trim();
        if (rubricTitle.isEmpty()) {
            showError("Error", "Rubric title is required before copying a template.");
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
        showInfo("Template copied", "Rubric CSV header template copied to clipboard.");
    }

    private void onPasteCsvFromClipboard() {
        String csvText = Clipboard.getSystemClipboard().getString();
        if (csvText == null || csvText.trim().isEmpty()) {
            showError("Error", "Clipboard does not contain any text.");
            return;
        }

        csvText = csvText.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = csvText.split("\n");
        if (lines.length < 2) {
            showError("Error", "Clipboard CSV must include a header row and at least one data row.");
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
            showError("Error", "Failed to parse clipboard CSV header: " + ex.getMessage());
            return;
        }

        if (headerCells == null || headerCells.isEmpty()) {
            showError("Error", "Clipboard CSV has an empty header row.");
            return;
        }

        String[] headerArray = headerCells.toArray(String[]::new);

        java.util.List<RatingGroup> ratingGroups = RatingHeaderDetector.detect(headerArray);

        if (ratingGroups.size() < 2) {
            showError(
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
            showError("Error", "Unexpected column(s) in clipboard CSV header: " + String.join(", ", extra));
            return;
        }

        try {
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("canvas_rubric_", ".csv");
            java.nio.file.Files.writeString(tempFile, csvText, java.nio.charset.StandardCharsets.UTF_8);
            csvPathField.setText(tempFile.toAbsolutePath().toString());
            showInfo(
                    "Clipboard CSV saved",
                    "Saved clipboard rubric CSV to temporary file:\n" + tempFile.toAbsolutePath());
        } catch (IOException ex) {
            showError("Error", "Failed to save clipboard CSV to temporary file: " + ex.getMessage());
        }
    }

    private void onCopyRubricCsv() {
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

        setStatus("Downloading rubric...");
        rubricService.getCanvasRubricCSV(courseId, assignmentId, result -> {
            if (result.status() == ResultStatus.FAILURE) {
                Platform.runLater(() -> {
                    setStatus("Error");
                    showError("Failed to fetch rubric", result.data());
                });
                return;
            } else {

                Platform.runLater(() -> {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(result.data());

                    Clipboard.getSystemClipboard().setContent(content);
                    setStatus("Copied to clipboard");
                    showInfo("Done", "Rubric copied to clipboard successfully.");
                });
            }
        });
    }

    private void onDownloadRubricCsv() {
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

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        chooser.setInitialFileName("rubric_assignment_" + assignmentId + ".csv");
        File file = chooser.showSaveDialog(stageManager.getPrimaryStage());
        if (file == null) {
            return;
        }

        setStatus("Downloading rubric...");
        rubricService.getCanvasRubricCSV(courseId, assignmentId, result -> {
            if (result.status() == ResultStatus.FAILURE) {
                Platform.runLater(() -> {
                    setStatus("Error");
                    showError("Failed to Download Rubric", result.data());
                });
            } else {
                try {
                    Files.writeString(file.toPath(), result.data(), StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    Platform.runLater(() -> {
                        setStatus("Error");
                        showError("Failed to Save Rubric", ex.getMessage());
                    });
                    return;
                }
                Platform.runLater(() -> {
                    setStatus("Done");
                    showInfo("Rubric Downloaded", "Saved rubric CSV to:\n" + file.getAbsolutePath());
                });
            }
        });
    }

    private void onCreate() {
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

        boolean freeForm = freeFormCommentsCheck.isSelected();
        boolean useForGrading = useForGradingCheck.isSelected();
        boolean hideScoreTotal = hideScoreTotalCheck.isSelected();
        boolean syncPoints = syncPointsCheck.isSelected();

        setStatus("Reading CSV...");
        new Thread(
                        () -> {
                            try {
                                CsvRubricParser parser = new CsvRubricParser(decodeHtmlCheck.isSelected());

                                CsvRubricParser.ParsedRubric parsed = parser.parse(Path.of(csvPath));
                                List<RubricModels.Criterion> criteria = parsed.criteria();
                                double total = parsed.totalPoints();

                                if (syncPoints) {
                                    Platform.runLater(() -> setStatus("Updating assignment points..."));
                                    rubricService.updateAssignmentPoints(courseId, assignmentId, total);
                                }

                                Platform.runLater(() -> setStatus("Creating rubric..."));
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
                                    setStatus("Done");
                                    showInfo(
                                            "Success",
                                            "Rubric created successfully!\nRubric: "
                                                    + rubricTitle
                                                    + "\nAssociation ID: "
                                                    + assocId);
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() -> {
                                    setStatus("Error");
                                    showError("Error", ex.getMessage());
                                });
                            }
                        },
                        "create-rubric")
                .start();
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
