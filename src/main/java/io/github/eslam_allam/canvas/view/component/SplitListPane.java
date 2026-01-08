package io.github.eslam_allam.canvas.view.component;

import io.github.eslam_allam.canvas.view.View;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;

public final class SplitListPane<T, K> implements View {

    private final SplitPane root;
    private final ListPane<T> leftPane;
    private final ListPane<K> rightPane;

    public SplitListPane(ListPane<T> leftPane, ListPane<K> rightPane) {
        this.leftPane = leftPane;
        this.rightPane = rightPane;
        this.root = new SplitPane();

        init();
    }

    private void init() {
        this.root.getStyleClass().add("section-card");
        this.root.getItems().addAll(leftPane.getRoot(), rightPane.getRoot());
        this.root.setDividerPositions(0.5);
    }

    @Override
    public Node getRoot() {
        return this.root;
    }
}
