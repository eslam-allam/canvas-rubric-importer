package io.github.eslam_allam.canvas.view.component;

import io.github.eslam_allam.canvas.domain.RubricRow;
import io.github.eslam_allam.canvas.model.canvas.RubricModels;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public final class RubricRatingWrappingTableCell extends TableCell<RubricRow, RubricModels.Rating> {
    private final Text header = new Text();
    private final Text text = new Text();
    private final VBox content = new VBox();

    public RubricRatingWrappingTableCell() {
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        header.wrappingWidthProperty().bind(widthProperty().subtract(10));
        header.getStyleClass().add("rubric-rating-header");

        text.wrappingWidthProperty().bind(widthProperty().subtract(10));
        text.getStyleClass().add("rubric-rating-text");

        this.content.setSpacing(10);
        this.content.getChildren().addAll(header, text);
    }

    @Override
    protected void updateItem(RubricModels.Rating item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            String title = item.description() == null ? "" : item.description();
            String pts = Double.toString(item.points());
            String headerText = title.isEmpty() ? pts : (title + " (" + pts + ")");
            String desc = item.longDescription() == null ? "" : item.longDescription();

            header.setText(headerText);
            text.setText(desc);

            setGraphic(content);
        }
    }
}
