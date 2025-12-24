package io.github.eslam_allam.canvas.gui;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.eslam_allam.canvas.AppInfo;
import io.github.eslam_allam.canvas.core.CanvasClient;
import io.github.eslam_allam.canvas.core.CsvRubricParser;
import io.github.eslam_allam.canvas.core.RubricModels;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.prefs.Preferences;
import javafx.application.Application;
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
import javafx.stage.Stage;

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
    private CheckBox decodeHtmlCheck;
    private Button showPreviewBtn;
    private Button backBtn;

    private Label statusLabel;

    private TableView<RubricRow> rubricPreviewTable;
    private BorderPane root;
    private SplitPane mainCenterPane;

    private ObservableList<JsonNode> courses = FXCollections.observableArrayList();

    private ObservableList<JsonNode> assignments = FXCollections.observableArrayList();

    private final Preferences prefs = Preferences.userNodeForPackage(CanvasRubricGuiApp.class);

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(AppInfo.NAME);

        root = new BorderPane();
        root.getStyleClass().add("app-root");

        VBox topBox = new VBox(10);
        topBox.getStyleClass().add("top-bar");
        topBox.getChildren().add(wrapInCard("Canvas Connection", buildConnectionPane()));
        root.setTop(topBox);

        loadSettings();

        mainCenterPane = new SplitPane();
        mainCenterPane.getItems().add(buildCoursesPane());
        mainCenterPane.getItems().add(buildAssignmentsPane());

        mainCenterPane.setDividerPositions(0.5);
        root.setCenter(mainCenterPane);

        VBox bottomBox = new VBox(10);
        bottomBox.getChildren().add(wrapInCard("Rubric Configuration", buildRubricPane()));
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 1000, 650);
        scene.getStylesheets()
                .add(
                        Objects.requireNonNull(
                                        getClass().getResource("/style.css"),
                                        "style.css not found on classpath")
                                .toExternalForm());
        primaryStage.setScene(scene);

        primaryStage.show();
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
        VBox box = new VBox(5);
        box.setPadding(new Insets(5));
        Label label = new Label("Courses");

        courseListView = new ListView<>(courses);
        courseListView.setCellFactory(
                list ->
                        new ListCell<>() {
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
        VBox box = new VBox(5);
        box.setPadding(new Insets(5));
        Label label = new Label("Assignments");

        assignmentListView = new ListView<>(assignments);
        assignmentListView.setCellFactory(
                list ->
                        new ListCell<>() {
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

        csvPathField
                .textProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            boolean hasCsv =
                                    newVal != null
                                            && !newVal.trim().isEmpty()
                                            && newVal.trim().toLowerCase().endsWith(".csv");
                            showPreviewBtn.setVisible(hasCsv);
                            showPreviewBtn.setManaged(hasCsv);
                        });

        grid.add(downloadTemplateBtn, 2, row++);

        grid.add(copyTemplateBtn, 2, row++);
        grid.add(downloadRubricBtn, 2, row++);

        grid.add(freeFormCommentsCheck, 0, row++, 2, 1);
        grid.add(useForGradingCheck, 0, row++, 2, 1);
        grid.add(hideScoreTotalCheck, 0, row++, 2, 1);
        grid.add(syncPointsCheck, 0, row++, 3, 1);
        grid.add(decodeHtmlCheck, 0, row++, 3, 1);

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

        box.getChildren().addAll(titleLabel, content);
        return box;
    }

    private void onSaveSettings() {

        prefs.put("baseUrl", baseUrlField.getText().trim());
        prefs.put("token", tokenField.getText());
        showInfo("Saved", "Settings saved.");
    }

    private void loadSettings() {
        String defaultBase = "";
        baseUrlField.setText(prefs.get("baseUrl", defaultBase));
        tokenField.setText(prefs.get("token", ""));
    }

    private void onLoadCourses() {
        String token = tokenField.getText().trim();
        if (token.isEmpty()) {
            showError("Error", "You must enter an access token first.");
            return;
        }
        String baseUrl = baseUrlField.getText().trim();
        setStatus("Loading courses...");
        new Thread(
                        () -> {
                            try {
                                CanvasClient client = new CanvasClient(baseUrl, token);
                                List<JsonNode> list = client.listCourses();
                                Platform.runLater(
                                        () -> {
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
        new Thread(
                        () -> {
                            try {
                                CanvasClient client = new CanvasClient(baseUrl, token);
                                List<JsonNode> list = client.listAssignments(courseId);
                                Platform.runLater(
                                        () -> {
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
            // Preview is only shown when the user clicks the Show Preview button.
        }
    }

    private void showPreviewFullHeight() {
        String path = csvPathField.getText().trim();
        if (path.isEmpty()) {
            showError("Error", "Please select a CSV file first.");
            return;
        }
        // Do not hide the preview button here; only hide it after preview loads successfully.
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

        java.util.List<TableColumn<RubricRow, RubricModels.Rating>> ratingCols =
                new java.util.ArrayList<>();
        for (int i = 0; i < maxRatings; i++) {
            final int idx = i;

            TableColumn<RubricRow, RubricModels.Rating> ratingCol = new TableColumn<>("");
            ratingCol.setCellValueFactory(
                    cell -> {
                        java.util.List<RubricModels.Rating> ratings = cell.getValue().getRatings();
                        if (idx >= ratings.size()) {
                            return new javafx.beans.property.ReadOnlyObjectWrapper<>(null);
                        }
                        return new javafx.beans.property.ReadOnlyObjectWrapper<>(ratings.get(idx));
                    });

            ratingCol.setCellFactory(
                    col ->
                            new TableCell<>() {
                                private final Text text = new Text();

                                {
                                    text.wrappingWidthProperty()
                                            .bind(col.widthProperty().subtract(10));
                                    setGraphic(text);
                                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                                }

                                @Override
                                protected void updateItem(RubricModels.Rating item, boolean empty) {
                                    super.updateItem(item, empty);
                                    if (empty || item == null) {
                                        text.setText("");
                                    } else {
                                        String title =
                                                item.getDescription() == null
                                                        ? ""
                                                        : item.getDescription();
                                        String pts = Double.toString(item.getPoints());
                                        String headerText =
                                                title.isEmpty() ? pts : (title + " (" + pts + ")");
                                        String desc =
                                                item.getLongDescription() == null
                                                        ? ""
                                                        : item.getLongDescription();

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
        column.setCellFactory(
                col ->
                        new TableCell<>() {
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
                                CsvRubricParser parser =
                                        new CsvRubricParser(decodeHtmlCheck.isSelected());

                                CsvRubricParser.ParsedRubric parsed = parser.parse(csvPath);

                                List<RubricModels.Criterion> criteria = parsed.criteria();

                                List<RubricRow> rows = new ArrayList<>();
                                int maxRatings = 0;
                                List<String> ratingHeaders = new ArrayList<>();
                                boolean headerInitialized = false;

                                double totalPoints = 0;
                                for (RubricModels.Criterion c : criteria) {
                                    List<RubricModels.Rating> ratings = c.getRatings();
                                    maxRatings = Math.max(maxRatings, ratings.size());

                                    if (!headerInitialized && !ratings.isEmpty()) {
                                        for (RubricModels.Rating r : ratings) {
                                            ratingHeaders.add(
                                                    r.getDescription()
                                                            + " ("
                                                            + r.getPoints()
                                                            + ")");
                                        }
                                        headerInitialized = true;
                                    }

                                    double maxPoints =
                                            ratings.stream()
                                                    .mapToDouble(RubricModels.Rating::getPoints)
                                                    .max()
                                                    .orElse(0.0);

                                    totalPoints += maxPoints;
                                    rows.add(
                                            new RubricRow(
                                                    c.getName(),
                                                    c.getDescription(),
                                                    Double.toString(maxPoints),
                                                    ratings));
                                }

                                final int finalMaxRatings = maxRatings;
                                final double finalTotalPoints = totalPoints;

                                Platform.runLater(
                                        () -> {
                                            if (showPreviewBtn != null) {
                                                showPreviewBtn.setVisible(false);
                                                showPreviewBtn.setManaged(false);
                                            }
                                            if (backBtn != null) {

                                                backBtn.setVisible(true);
                                                backBtn.setManaged(true);
                                            }
                                            rubricPreviewTable =
                                                    buildRubricPreviewTable(finalMaxRatings);
                                            rubricPreviewTable.getItems().setAll(rows);

                                            // Add total row at the end
                                            RubricRow totalRow =
                                                    new RubricRow(
                                                            "Total",
                                                            "",
                                                            Double.toString(finalTotalPoints),
                                                            java.util.Collections.emptyList());
                                            rubricPreviewTable.getItems().add(totalRow);

                                            root.setCenter(buildFullHeightPreviewPane());
                                            setStatus("Preview loaded");
                                        });

                            } catch (Exception ex) {
                                Platform.runLater(
                                        () -> {
                                            if (rubricPreviewTable != null) {
                                                rubricPreviewTable.getItems().clear();
                                            }
                                            showError(
                                                    "Error",
                                                    "Could not load preview: " + ex.getMessage());
                                            setStatus("Preview error");
                                        });
                            }
                        },
                        "rubric-preview")
                .start();
    }

    private List<String> templateHeader(int maxRatings) {
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

    private List<String> templateHeader() {
        // Backwards-compatible default template with two ratings
        return templateHeader(2);
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
        File file = chooser.showSaveDialog(null);
        if (file == null) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writeCsvRow(writer, templateHeader());
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
            writeCsvRow(pw, templateHeader());
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

        // Normalize newlines
        csvText = csvText.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = csvText.split("\n");
        if (lines.length < 2) {
            showError(
                    "Error", "Clipboard CSV must include a header row and at least one data row.");
            return;
        }

        java.util.List<String> headerCells;
        try {
            java.io.StringReader sr = new java.io.StringReader(csvText);
            try (org.apache.commons.csv.CSVParser parser =
                    org.apache.commons.csv.CSVFormat.DEFAULT
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

        // Reuse RatingHeaderDetector to validate rating groups
        java.util.List<io.github.eslam_allam.canvas.core.RatingHeaderDetector.RatingGroup>
                ratingGroups =
                        io.github.eslam_allam.canvas.core.RatingHeaderDetector.detect(headerArray);

        if (ratingGroups.size() < 2) {
            showError(
                    "Error",
                    "Clipboard CSV must have at least rating1/rating1_points/rating1_desc and"
                            + " rating2/rating2_points/rating2_desc columns.");
            return;
        }

        // Ensure no unexpected extra columns, mirroring CsvRubricParser.validateNoExtraColumns
        // behavior
        java.util.Set<String> allowed = new java.util.HashSet<>();
        allowed.add("criterion");
        allowed.add("criterion_desc");
        for (io.github.eslam_allam.canvas.core.RatingHeaderDetector.RatingGroup g : ratingGroups) {
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
            showError(
                    "Error",
                    "Unexpected column(s) in clipboard CSV header: " + String.join(", ", extra));
            return;
        }

        try {
            java.nio.file.Path tempFile =
                    java.nio.file.Files.createTempFile("canvas_rubric_", ".csv");
            java.nio.file.Files.writeString(
                    tempFile, csvText, java.nio.charset.StandardCharsets.UTF_8);
            csvPathField.setText(tempFile.toAbsolutePath().toString());
            showInfo(
                    "Clipboard CSV saved",
                    "Saved clipboard rubric CSV to temporary file:\n" + tempFile.toAbsolutePath());
        } catch (IOException ex) {
            showError(
                    "Error", "Failed to save clipboard CSV to temporary file: " + ex.getMessage());
        }
    }

    private void onDownloadRubricCsv() {

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

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        chooser.setInitialFileName("rubric_assignment_" + assignmentId + ".csv");
        File file = chooser.showSaveDialog(null);
        if (file == null) {
            return;
        }

        setStatus("Downloading rubric...");
        new Thread(
                        () -> {
                            try {
                                CanvasClient client =
                                        new CanvasClient(baseUrlField.getText().trim(), token);
                                JsonNode assignment =
                                        client.getAssignmentWithRubric(courseId, assignmentId);
                                JsonNode rubric = assignment.path("rubric");
                                if (rubric.isMissingNode()
                                        || !rubric.isArray()
                                        || rubric.isEmpty()) {

                                    Platform.runLater(
                                            () -> {
                                                setStatus("No rubric");
                                                showError(
                                                        "Error", "This assignment has no rubric.");
                                            });
                                    return;
                                }

                                int maxRatings = 0;
                                for (JsonNode crit : rubric) {
                                    JsonNode ratings = crit.path("ratings");
                                    if (ratings.isArray()) {
                                        maxRatings = Math.max(maxRatings, ratings.size());
                                    }
                                }
                                if (maxRatings == 0) {
                                    Platform.runLater(
                                            () -> {
                                                setStatus("No ratings");
                                                showError("Error", "The rubric has no ratings.");
                                            });
                                    return;
                                }

                                List<String> header = templateHeader(maxRatings);
                                List<List<String>> rows = new ArrayList<>();

                                for (JsonNode crit : rubric) {
                                    String description = crit.path("description").asText("");
                                    String longDesc = crit.path("long_description").asText("");

                                    JsonNode ratings = crit.path("ratings");
                                    List<String> row = new ArrayList<>();
                                    row.add(description);
                                    row.add(longDesc);

                                    int count = ratings.isArray() ? ratings.size() : 0;
                                    for (int i = 0; i < maxRatings; i++) {
                                        if (i < count) {
                                            JsonNode r = ratings.get(i);
                                            row.add(r.path("description").asText(""));
                                            row.add(r.path("points").asText(""));
                                            row.add(r.path("long_description").asText(""));
                                        } else {
                                            row.add("");
                                            row.add("");
                                            row.add("");
                                        }
                                    }
                                    rows.add(row);
                                }

                                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                                    writeCsvRow(writer, header);
                                    for (List<String> row : rows) {
                                        writeCsvRow(writer, row);
                                    }
                                }

                                Platform.runLater(
                                        () -> {
                                            setStatus("Done");
                                            showInfo(
                                                    "Rubric downloaded",
                                                    "Saved rubric CSV to:\n"
                                                            + file.getAbsolutePath());
                                        });

                            } catch (Exception ex) {
                                Platform.runLater(
                                        () -> {
                                            setStatus("Error");
                                            showError("Error", ex.getMessage());
                                        });
                            }
                        },
                        "download-rubric")
                .start();
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
        new Thread(
                        () -> {
                            try {
                                CsvRubricParser parser =
                                        new CsvRubricParser(decodeHtmlCheck.isSelected());

                                CsvRubricParser.ParsedRubric parsed =
                                        parser.parse(Path.of(csvPath));
                                List<RubricModels.Criterion> criteria = parsed.criteria();
                                double total = parsed.totalPoints();

                                CanvasClient client = new CanvasClient(baseUrl, token);
                                var formFields =
                                        client.buildFormFieldsForRubricCreate(
                                                title,
                                                freeForm,
                                                criteria,
                                                Integer.parseInt(assignmentId),
                                                useForGrading,
                                                hideScoreTotal,
                                                "grading");

                                if (syncPoints) {
                                    Platform.runLater(
                                            () -> setStatus("Updating assignment points..."));
                                    client.updateAssignmentPoints(courseId, assignmentId, total);
                                }

                                Platform.runLater(() -> setStatus("Creating rubric..."));
                                JsonNode response = client.createRubric(courseId, formFields);
                                String rubricId = response.path("rubric").path("id").asText("");
                                String assocId =
                                        response.path("rubric_association").path("id").asText("");

                                Platform.runLater(
                                        () -> {
                                            setStatus("Done");
                                            showInfo(
                                                    "Success",
                                                    "Rubric created successfully!\nRubric ID: "
                                                            + rubricId
                                                            + "\nAssociation ID: "
                                                            + assocId);
                                        });
                            } catch (Exception ex) {
                                Platform.runLater(
                                        () -> {
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
                value.contains(",")
                        || value.contains("\n")
                        || value.contains("\r")
                        || value.contains("\"");
        String escaped = value.replace("\"", "\"\"");
        return hasSpecial ? "\"" + escaped + "\"" : escaped;
    }

    public static class RubricRow {
        private final String criterion;
        private final String description;
        private final String points;
        private final List<RubricModels.Rating> ratings;

        public RubricRow(
                String criterion,
                String description,
                String points,
                List<RubricModels.Rating> ratings) {
            this.criterion = criterion;
            this.description = description;
            this.points = points;
            this.ratings = ratings;
        }

        public String getCriterion() {
            return criterion;
        }

        public String getDescription() {
            return description;
        }

        public String getPoints() {
            return points;
        }

        public List<RubricModels.Rating> getRatings() {
            return ratings;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
