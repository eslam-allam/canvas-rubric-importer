plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

application {
    // Default entry point – CLI.
    mainClass.set("com.example.canvas.cli.CliApp")
}

// Convenience tasks to run CLI and GUI explicitly via the existing 'run' task

tasks.register("runCli") {
    group = "application"
    description = "Run the CLI application"
    doFirst {
        application {
            mainClass.set("com.example.canvas.cli.CliApp")
        }
    }
    finalizedBy(tasks.named("run"))
}

tasks.register("runGui") {
    group = "application"
    description = "Run the JavaFX GUI application"
    doFirst {
        application {
            mainClass.set("com.example.canvas.gui.CanvasRubricGuiApp")
        }
    }
    finalizedBy(tasks.named("run"))
}




javafx {
    version = "25"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.base")
}

