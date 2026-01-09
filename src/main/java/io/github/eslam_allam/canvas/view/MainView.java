package io.github.eslam_allam.canvas.view;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public final class MainView implements View {

    private final BorderPane root;
    private final VBox top;
    private final VBox bottom;

    public MainView() {
        this.root = new BorderPane();
        this.root.getStyleClass().add("app-root");

        this.top = new VBox(10);
        this.top.getStyleClass().add("top-bar");
        this.root.setTop(this.top);

        this.bottom = new VBox(10);
        root.setBottom(this.bottom);
    }

    public void setTop(Node... node) {
        this.top.getChildren().setAll(node);
    }

    public void setCenter(Node node) {
        this.root.setCenter(node);
    }

    public void setBottom(Node... node) {
        this.bottom.getChildren().setAll(node);
    }

    @Override
    public Node getRoot() {
        return this.root;
    }

    public Parent getParent() {
        return this.root;
    }
}
