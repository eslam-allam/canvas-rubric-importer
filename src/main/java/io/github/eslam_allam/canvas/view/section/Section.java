package io.github.eslam_allam.canvas.view.section;

import io.github.eslam_allam.canvas.view.View;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public abstract class Section<T extends View> implements View {
    protected final Node root;

    protected Section(String title, T view, Insets margins) {
        if (title == null) {
            if (margins != null) {
                this.root = addMargin(view.getRoot(), margins);
            } else {
                this.root = view.getRoot();
            }
            return;
        }
        this.root = wrapInCard(title, view.getRoot(), margins);
    }

    protected Section(String title, T view) {
        this(title, view, null);
    }

    protected Section(T view) {
        this(null, view, null);
    }

    protected Section(T view, Insets margins) {
        this(null, view, margins);
    }

    private static Node addMargin(Node node, Insets insets) {
        StackPane wrapper = new StackPane(node);
        wrapper.setPadding(insets);
        return wrapper;
    }

    private static Node wrapInCard(String title, Node content, Insets margins) {
        VBox box = new VBox(6);
        box.getStyleClass().add("section-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");

        if (content instanceof TableView) {
            VBox.setVgrow(content, javafx.scene.layout.Priority.ALWAYS);
        }

        box.getChildren().addAll(titleLabel, content);

        if (margins != null) {
            return addMargin(box, margins);
        }
        return box;
    }

    public static Node oneTimeSection(String title, Node content, Insets margins) {
        return wrapInCard(title, content, margins);
    }

    public static Node oneTimeSection(String title, Node content) {
        return oneTimeSection(title, content, null);
    }

    public Node getRoot() {
        return this.root;
    }
}
