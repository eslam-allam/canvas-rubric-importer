module io.github.eslam_allam.canvas {
    // Main entry point
    exports io.github.eslam_allam.canvas;

    // Public APIs used by CLI/GUI
    exports io.github.eslam_allam.canvas.cli;
    exports io.github.eslam_allam.canvas.gui;
    exports io.github.eslam_allam.canvas.client;
    exports io.github.eslam_allam.canvas.domain;
    exports io.github.eslam_allam.canvas.model.canvas;

    // JavaFX modules
    requires javafx.base;
    requires javafx.controls;
    requires transitive javafx.graphics;

    // Third-party libraries
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.csv;
    requires org.apache.commons.text;
    requires transitive org.apache.httpcomponents.core5.httpcore5;
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.commons.lang3;

    // JDK modules
    requires java.net.http;
    requires java.prefs;
    requires com.fasterxml.jackson.annotation;

    // Allow JavaFX / reflection to access GUI internals
    opens io.github.eslam_allam.canvas.gui;
}
