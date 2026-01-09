package io.github.eslam_allam.canvas.controller;

import io.github.eslam_allam.canvas.navigation.SceneSwitcher;
import io.github.eslam_allam.canvas.view.component.RubricConfiguration;
import io.github.eslam_allam.canvas.viewmodel.RubricConfigurationVM;
import java.io.File;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public final class RubricConfigurationController {

    private final RubricConfigurationVM vm;
    private final RubricConfiguration view;

    private final SceneSwitcher sceneSwitcher;
    private final Window ownerWindow;

    public RubricConfigurationController(
            RubricConfiguration view, RubricConfigurationVM vm, Window ownerWindow, SceneSwitcher sceneSwitcher) {
        this.ownerWindow = ownerWindow;
        this.sceneSwitcher = sceneSwitcher;
        this.view = view;
        this.vm = vm;

        this.view.bind(vm);
    }

    private void onBrowseCsv() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showOpenDialog(this.ownerWindow);
        if (file != null) {
            this.vm.csvPath().set(file.getAbsolutePath());
        }
    }

    private void showMainView() {
        this.sceneSwitcher.back();
        this.vm.previewButtonVisible().set(true);
        this.vm.backBtnVisible().set(false);
    }

    private void showPreviewFullHeight() {
        String path = csvPathField.getText().trim();
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

    private Pane buildFullHeightPreviewPane() {

        BorderPane pane = new BorderPane();

        pane.setCenter(wrapInCard("Rubric Preview", rubricPreviewTable));

        return pane;
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

    private void wire() {}
}
