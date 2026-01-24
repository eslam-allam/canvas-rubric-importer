package io.github.eslam_allam.canvas.view.component;

import io.github.eslam_allam.canvas.domain.RubricRow;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.text.Text;

public final class WrappingTableCell extends TableCell<RubricRow, String> {
    private final Text text = new Text();

    public WrappingTableCell() {
        // CONTENT MOVED FROM INITIALIZER TO CONSTRUCTOR
        text.wrappingWidthProperty().bind(widthProperty().subtract(10));
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            text.setText(item);
            setGraphic(text);
        }
    }
}
