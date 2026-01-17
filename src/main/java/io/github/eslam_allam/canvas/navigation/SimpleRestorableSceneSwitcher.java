package io.github.eslam_allam.canvas.navigation;

import java.util.function.Consumer;
import javafx.scene.Node;

public final class SimpleRestorableSceneSwitcher implements RestorableSceneSwitcher {
    private final Consumer<Node> targetViewer;
    private final Runnable viewRestorer;

    public SimpleRestorableSceneSwitcher(Consumer<Node> targetViewer, Runnable viewRestorer) {
        this.targetViewer = targetViewer;
        this.viewRestorer = viewRestorer;
    }

    public void show(Node node) {
        this.targetViewer.accept(node);
    }

    public void restore() {
        this.viewRestorer.run();
    }
}
