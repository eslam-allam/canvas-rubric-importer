package io.github.eslam_allam.canvas.view.component;

import io.github.eslam_allam.canvas.domain.RubricCriterion;
import io.github.eslam_allam.canvas.domain.RubricRow;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public final class RubricCriterionWrappingTableCell extends TableCell<RubricRow, RubricCriterion> {
    private final Text header = new Text();
    private final Text text = new Text();
    private final VBox content = new VBox();

    public RubricCriterionWrappingTableCell() {
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        header.wrappingWidthProperty().bind(widthProperty().subtract(10));
        header.getStyleClass().add("rubric-rating-header");

        text.wrappingWidthProperty().bind(widthProperty().subtract(10));
        text.getStyleClass().add("rubric-rating-text");

        this.content.setSpacing(10);
        this.content.getChildren().addAll(header, text);
    }

    @Override
    protected void updateItem(RubricCriterion item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            String title = item.criterion() == null ? "" : item.criterion();
            String desc = item.description() == null ? "" : item.description();

            header.setText(title);
            text.setText(desc);

            setGraphic(content);
        }
    }
}
