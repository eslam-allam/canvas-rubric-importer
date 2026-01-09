package io.github.eslam_allam.canvas.view.component;

import io.github.eslam_allam.canvas.view.Widget;
import io.github.eslam_allam.canvas.viewmodel.RubricConfigurationVM;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class RubricConfiguration implements Widget<RubricConfigurationVM> {

    private final GridPane root;

    private final StatusLabel statusLabel;

    private final TextField courseIdField;
    private final TextField assignmentIdField;
    private final TextField titleField;
    private final TextField csvPathField;

    private final Button browseBtn;
    private final Button pasteCsvFromClipboardBtn;
    private final Button downloadTemplateBtn;
    private final Button copyTemplateBtn;
    private final Button downloadRubricBtn;
    private final Button copyRubricBtn;

    private final CheckBox freeFormCommentsCheck;
    private final CheckBox useForGradingCheck;
    private final CheckBox hideScoreTotalCheck;
    private final CheckBox syncPointsCheck;
    private final CheckBox decodeHtmlCheck;
    private final Button showPreviewBtn;
    private final Button backBtn;
    private final Button createBtn;
    private final Button quitBtn;

    public RubricConfiguration(StatusLabel statusLabel) {
        this.statusLabel = statusLabel;

        this.root = new GridPane();
        this.root.setHgap(10);
        this.root.setVgap(5);

        this.courseIdField = new TextField();
        this.courseIdField.setEditable(false);
        this.courseIdField.getStyleClass().add("readonly");

        this.assignmentIdField = new TextField();
        this.assignmentIdField.setEditable(false);
        this.assignmentIdField.getStyleClass().add("readonly");

        this.titleField = new TextField();
        this.csvPathField = new TextField();

        this.freeFormCommentsCheck = new CheckBox("Free-form comments");
        this.freeFormCommentsCheck.setSelected(true);
        this.useForGradingCheck = new CheckBox("Use for grading");
        this.useForGradingCheck.setSelected(true);
        this.hideScoreTotalCheck = new CheckBox("Hide score total");
        this.syncPointsCheck = new CheckBox("Sync assignment points to rubric total");
        this.decodeHtmlCheck = new CheckBox("Decode HTML entities in CSV text");
        this.decodeHtmlCheck.setSelected(true);

        this.browseBtn = new Button("Browse...");

        this.pasteCsvFromClipboardBtn = new Button("Paste CSV from Clipboard");

        this.showPreviewBtn = new Button("Show Preview");
        this.showPreviewBtn.setVisible(false);
        this.showPreviewBtn.setManaged(false);

        this.backBtn = new Button("Back to Main View");
        this.backBtn.setVisible(false);
        this.backBtn.setManaged(false);

        this.downloadTemplateBtn = new Button("Download CSV Template");

        this.copyTemplateBtn = new Button("Copy Template to Clipboard");

        this.downloadRubricBtn = new Button("Download Rubric as CSV");

        this.copyRubricBtn = new Button("Copy Rubric as CSV");

        this.createBtn = new Button("Create Rubric");

        this.quitBtn = new Button("Quit");
        this.quitBtn.setOnAction(e -> Platform.exit());

        initRubricPane();
    }

    private void initRubricPane() {
        HBox canvasRubricButtons = new HBox(5, downloadRubricBtn, copyRubricBtn);

        int row = 0;

        this.root.add(new Label("Selected Course ID:"), 0, row);
        this.root.add(courseIdField, 1, row++);

        this.root.add(new Label("Selected Assignment ID:"), 0, row);
        this.root.add(assignmentIdField, 1, row++);

        this.root.add(new Label("Rubric Title:"), 0, row);
        this.root.add(titleField, 1, row++, 2, 1);

        this.root.add(new Label("CSV File:"), 0, row);
        this.root.add(csvPathField, 1, row);

        HBox csvButtons = new HBox(5, browseBtn, pasteCsvFromClipboardBtn, showPreviewBtn, backBtn);
        this.root.add(csvButtons, 2, row++);

        csvPathField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasCsv = newVal != null
                    && !newVal.trim().isEmpty()
                    && newVal.trim().toLowerCase().endsWith(".csv");
            showPreviewBtn.setVisible(hasCsv);
            showPreviewBtn.setManaged(hasCsv);
        });

        HBox templateButtons = new HBox(5, downloadTemplateBtn, copyTemplateBtn);
        this.root.add(templateButtons, 2, row++);

        this.root.add(canvasRubricButtons, 2, row++);

        VBox rubricOptions = new VBox(
                5,
                new HBox(10, freeFormCommentsCheck, useForGradingCheck, hideScoreTotalCheck),
                new HBox(10, syncPointsCheck, decodeHtmlCheck));
        this.root.add(rubricOptions, 0, row, 6, 3);
        GridPane.setMargin(rubricOptions, new Insets(5));
        row += 3;

        this.root.add(statusLabel.getRoot(), 0, row++, 3, 1);

        HBox buttons = new HBox(10, createBtn, quitBtn);
        buttons.getStyleClass().add("bottom-actions");
        this.root.add(buttons, 0, row, 3, 1);
    }

    @Override
    public Node getRoot() {
        return this.root;
    }

    @Override
    public void bind(RubricConfigurationVM vm) {
        this.courseIdField.textProperty().bind(vm.courseId());
        this.assignmentIdField.textProperty().bind(vm.assignmentId());
        this.titleField.textProperty().bindBidirectional(vm.title());
        this.csvPathField.textProperty().bindBidirectional(vm.csvPath());
        this.freeFormCommentsCheck.selectedProperty().bindBidirectional(vm.freeFormComments());
        this.useForGradingCheck.selectedProperty().bindBidirectional(vm.useForGrading());
        this.hideScoreTotalCheck.selectedProperty().bindBidirectional(vm.hideTotalScore());
        this.syncPointsCheck.selectedProperty().bindBidirectional(vm.syncPoints());
        this.decodeHtmlCheck.selectedProperty().bindBidirectional(vm.decodeHtml());

        this.showPreviewBtn.visibleProperty().bindBidirectional(vm.previewButtonVisible());
        this.showPreviewBtn.managedProperty().bindBidirectional(vm.previewButtonVisible());

        this.backBtn.visibleProperty().bindBidirectional(vm.backBtnVisible());
        this.backBtn.managedProperty().bindBidirectional(vm.backBtnVisible());
    }

    public void onBrowseClick(EventHandler<ActionEvent> callback) {
        this.browseBtn.setOnAction(callback);
    }

    public void onPasteCsvFromClipboardClick(EventHandler<ActionEvent> callback) {
        this.pasteCsvFromClipboardBtn.setOnAction(callback);
    }

    public void onDownloadTemplateClick(EventHandler<ActionEvent> callback) {
        this.downloadTemplateBtn.setOnAction(callback);
    }

    public void onCopyTemplateClick(EventHandler<ActionEvent> callback) {
        this.copyTemplateBtn.setOnAction(callback);
    }

    public void onDownloadRubricClick(EventHandler<ActionEvent> callback) {
        this.downloadRubricBtn.setOnAction(callback);
    }

    public void onCopyRubricClick(EventHandler<ActionEvent> callback) {
        this.copyRubricBtn.setOnAction(callback);
    }

    public void onShowPreviewClick(EventHandler<ActionEvent> callback) {
        this.showPreviewBtn.setOnAction(callback);
    }

    public void onBackClick(EventHandler<ActionEvent> callback) {
        this.backBtn.setOnAction(callback);
    }

    public void onCsvPathChange(Consumer<String> callback) {
        this.csvPathField.textProperty().addListener((obs, oldVal, newVal) -> callback.accept(newVal));
    }

    public void onCreateClick(EventHandler<ActionEvent> callback) {
        this.createBtn.setOnAction(callback);
    }
}
