package io.github.eslam_allam.canvas.controller;

import io.github.eslam_allam.canvas.domain.ResultStatus;
import io.github.eslam_allam.canvas.domain.RubricRow;
import io.github.eslam_allam.canvas.model.canvas.Assignment;
import io.github.eslam_allam.canvas.model.canvas.Course;
import io.github.eslam_allam.canvas.model.canvas.RubricModels;
import io.github.eslam_allam.canvas.navigation.RestorableSceneSwitcher;
import io.github.eslam_allam.canvas.navigation.StageManager;
import io.github.eslam_allam.canvas.notification.PopUp;
import io.github.eslam_allam.canvas.notification.StatusNotifier;
import io.github.eslam_allam.canvas.rubric.importing.csv.CsvRubricParser;
import io.github.eslam_allam.canvas.rubric.importing.csv.RatingHeaderDetector;
import io.github.eslam_allam.canvas.rubric.importing.csv.RatingHeaderDetector.RatingGroup;
import io.github.eslam_allam.canvas.service.CanvasRubricService;
import io.github.eslam_allam.canvas.view.component.RubricConfiguration;
import io.github.eslam_allam.canvas.view.section.Section;
import io.github.eslam_allam.canvas.viewmodel.ListPaneVM;
import io.github.eslam_allam.canvas.viewmodel.RubricConfigurationVM;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

@Singleton
public final class RubricConfigurationController {

    private final CanvasRubricService rubricService;

    private final RubricConfigurationVM vm;
    private final RubricConfiguration view;

    private final ListPaneVM<Course> coursePaneVM;
    private final ListPaneVM<Assignment> assignmentPaneVM;

    private final RestorableSceneSwitcher sceneSwitcher;
    private final StageManager stageManager;

    private final StatusNotifier statusNotifier;

    private TableView<RubricRow> rubricPreviewTable;

    @Inject
    public RubricConfigurationController(
            RubricConfiguration view,
            RubricConfigurationVM vm,
            StageManager stageManager,
            RestorableSceneSwitcher sceneSwitcher,
            StatusNotifier statusNotifier,
            CanvasRubricService rubricService,
            ListPaneVM<Course> coursePaneVM,
            ListPaneVM<Assignment> assignmentPaneVM) {
        this.stageManager = stageManager;
        this.sceneSwitcher = sceneSwitcher;
        this.view = view;
        this.vm = vm;

        this.coursePaneVM = coursePaneVM;
        this.assignmentPaneVM = assignmentPaneVM;

        this.statusNotifier = statusNotifier;
        this.rubricService = rubricService;

        this.view.bind(vm);
        this.vm.useForGrading().set(true);
        this.vm.decodeHtml().set(true);
        wire();
    }

    private void onBrowseCsv(ActionEvent e) {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showOpenDialog(this.stageManager.getPrimaryStage());
        if (file != null) {
            this.vm.csvPath().set(file.getAbsolutePath());
        }
    }

    private void showMainView(ActionEvent e) {
        this.sceneSwitcher.restore();
        this.vm.backBtnVisible().set(false);
        onCsvPathChange(this.vm.csvPath().get());
    }

    private void showPreviewFullHeight(ActionEvent e) {
        String path = this.vm.csvPath().get().trim();
        if (path.isEmpty()) {
            PopUp.showError("Error", "Please select a CSV file first.");
            return;
        }
        loadRubricPreview(Path.of(path));
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

    private void loadRubricPreview(Path csvPath) {

        this.statusNotifier.setStatus("Loading preview...");
        new Thread(
                        () -> {
                            try {
                                CsvRubricParser.ParsedRubric parsed = CsvRubricParser.parse(
                                        csvPath, this.vm.decodeHtml().get());

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
                                    this.vm.previewButtonVisible().set(false);
                                    this.vm.backBtnVisible().set(true);
                                    rubricPreviewTable = buildRubricPreviewTable(finalMaxRatings);
                                    rubricPreviewTable.getItems().setAll(rows);

                                    RubricRow totalRow = new RubricRow(
                                            "Total",
                                            "",
                                            Double.toString(finalTotalPoints),
                                            java.util.Collections.emptyList());
                                    rubricPreviewTable.getItems().add(totalRow);

                                    this.sceneSwitcher.show(Section.oneTimeSection(
                                            "Rubric Preview", rubricPreviewTable, new Insets(5, 0, 5, 0)));
                                    this.statusNotifier.setStatus("Preview loaded");
                                });

                            } catch (Exception ex) {
                                Platform.runLater(() -> {
                                    if (rubricPreviewTable != null) {
                                        rubricPreviewTable.getItems().clear();
                                    }
                                    PopUp.showError("Error", "Could not load preview: " + ex.getMessage());
                                    this.statusNotifier.setStatus("Preview error");
                                });
                            }
                        },
                        "rubric-preview")
                .start();
    }

    private void onCreate(ActionEvent e) {
        String courseId = this.vm.courseId().get().trim();
        if (courseId.isEmpty()) {
            PopUp.showError("Error", "Please select a course.");
            return;
        }
        String assignmentId = this.vm.assignmentId().get().trim();
        if (assignmentId.isEmpty()) {
            PopUp.showError("Error", "Please select an assignment.");
            return;
        }
        String title = this.vm.title().get().trim();
        if (title.isEmpty()) {
            PopUp.showError("Error", "Rubric title is required.");
            return;
        }
        String csvPath = this.vm.csvPath().get().trim();
        if (csvPath.isEmpty()) {
            PopUp.showError("Error", "CSV file is required.");
            return;
        }

        boolean freeForm = this.vm.freeFormComments().get();
        boolean useForGrading = this.vm.useForGrading().get();
        boolean hideScoreTotal = this.vm.hideTotalScore().get();
        boolean syncPoints = this.vm.syncPoints().get();

        this.statusNotifier.setStatus("Reading CSV...");
        new Thread(
                        () -> {
                            try {

                                CsvRubricParser.ParsedRubric parsed = CsvRubricParser.parse(
                                        Path.of(csvPath), this.vm.decodeHtml().get());
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

    private void onDownloadTemplate(ActionEvent e) {
        String rubricTitle = this.vm.title().get().trim();
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
        File file = chooser.showSaveDialog(this.stageManager.getPrimaryStage());
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

    private void onCopyTemplate(ActionEvent e) {
        String rubricTitle = this.vm.title().get().trim();
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

    private void onPasteCsvFromClipboard(ActionEvent e) {
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
            this.vm.csvPath().set(tempFile.toAbsolutePath().toString());
            PopUp.showInfo(
                    "Clipboard CSV saved",
                    "Saved clipboard rubric CSV to temporary file:\n" + tempFile.toAbsolutePath());
        } catch (IOException ex) {
            PopUp.showError("Error", "Failed to save clipboard CSV to temporary file: " + ex.getMessage());
        }
    }

    private void onCopyRubricCsv(ActionEvent e) {
        String courseId = this.vm.courseId().get().trim();
        if (courseId.isEmpty()) {
            PopUp.showError("Error", "Please select a course.");
            return;
        }
        String assignmentId = this.vm.assignmentId().get().trim();
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

    private void onDownloadRubricCsv(ActionEvent e) {
        String courseId = this.vm.courseId().get().trim();
        if (courseId.isEmpty()) {
            PopUp.showError("Error", "Please select a course.");
            return;
        }
        String assignmentId = this.vm.assignmentId().get().trim();
        if (assignmentId.isEmpty()) {
            PopUp.showError("Error", "Please select an assignment.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        chooser.setInitialFileName("rubric_assignment_" + assignmentId + ".csv");
        File file = chooser.showSaveDialog(this.stageManager.getPrimaryStage());
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

    private void onCsvPathChange(String newPath) {
        if (newPath.trim().isEmpty()) {
            return;
        }
        try {
            Path path = Paths.get(newPath);
            this.vm.previewButtonVisible().set(path.getFileName().toString().endsWith(".csv"));
        } catch (InvalidPathException e) {
            this.vm.previewButtonVisible().set(false);
        }
    }

    private void onCourseSelected(Course course) {
        if (course == null) {
            return;
        }
        String id = course.id().toString();
        this.vm.courseId().set(id);
        this.assignmentPaneVM.items().clear();
        this.vm.assignmentId().set("");
    }

    private void onAssignmentSelected(Assignment assignment) {
        if (assignment == null) {
            return;
        }
        String id = assignment.id().toString();
        this.vm.assignmentId().set(id);
        String name = assignment.name();
        this.vm.title().set("Rubric for " + name);
    }

    private void wire() {
        this.view.onBrowseClick(this::onBrowseCsv);
        this.view.onPasteCsvFromClipboardClick(this::onPasteCsvFromClipboard);
        this.view.onShowPreviewClick(this::showPreviewFullHeight);
        this.view.onBackClick(this::showMainView);

        this.view.onDownloadTemplateClick(this::onDownloadTemplate);
        this.view.onCopyTemplateClick(this::onCopyTemplate);

        this.view.onDownloadRubricClick(this::onDownloadRubricCsv);
        this.view.onCopyRubricClick(this::onCopyRubricCsv);

        this.view.onCreateClick(this::onCreate);
        this.view.onCsvPathChange(this::onCsvPathChange);

        this.coursePaneVM.onSelectedChange(this::onCourseSelected);
        this.assignmentPaneVM.onSelectedChange(this::onAssignmentSelected);
    }
}
