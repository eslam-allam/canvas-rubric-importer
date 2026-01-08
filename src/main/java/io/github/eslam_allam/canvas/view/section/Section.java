package io.github.eslam_allam.canvas.view.section;

import io.github.eslam_allam.canvas.view.View;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;

public abstract class Section<T extends View> implements View {
    protected final Node root;

    protected Section(String title, T view) {
        if (title == null) {
            this.root = view.getRoot();
            return;
        }
        this.root = wrapInCard(title, view.getRoot());
    }

    protected Section(T view) {
        this(null, view);
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

    public Node getRoot() {
        return this.root;
    }
}
