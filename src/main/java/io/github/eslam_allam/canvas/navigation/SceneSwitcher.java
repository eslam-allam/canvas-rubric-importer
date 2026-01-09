package io.github.eslam_allam.canvas.navigation;

public final class SceneSwitcher {
    private final Runnable targetViewer;
    private final Runnable viewRestorer;

    public SceneSwitcher(Runnable targetViewer, Runnable viewRestorer) {
        this.targetViewer = targetViewer;
        this.viewRestorer = viewRestorer;
    }

    public void show() {
        this.targetViewer.run();
    }

    public void back() {
        this.viewRestorer.run();
    }
}
