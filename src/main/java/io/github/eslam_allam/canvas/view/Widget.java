package io.github.eslam_allam.canvas.view;

import javafx.scene.Node;

public interface Widget<T> {
    void bind(T vm);

    Node getRoot();
}
