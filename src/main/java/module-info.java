module io.github.eslam_allam.canvas {
    // Main entry point
    exports io.github.eslam_allam.canvas;

    // Public APIs used by CLI/GUI
    exports io.github.eslam_allam.canvas.cli;
    exports io.github.eslam_allam.canvas.gui;

    // JavaFX modules
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;

    // Third-party libraries
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.csv;
    requires org.apache.commons.text;

    // JDK modules
    requires java.net.http;
    requires java.prefs;
    requires com.fasterxml.jackson.annotation;

    // Allow JavaFX / reflection to access GUI internals
    opens io.github.eslam_allam.canvas.gui;
}
