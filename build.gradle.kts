plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    implementation("org.apache.commons:commons-csv:1.11.0")
    implementation("org.apache.commons:commons-text:1.12.0")

    // JavaFX dependencies for your platform
    val javafxVersion = "25"
    implementation("org.openjfx:javafx-base:$javafxVersion")
    implementation("org.openjfx:javafx-controls:$javafxVersion")
    implementation("org.openjfx:javafx-graphics:$javafxVersion")
}



java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

application {
    // Default entry point – CLI.
    mainClass.set("io.github.eslam_allam.canvas.cli.CliApp")
}


// Convenience tasks to run CLI and GUI explicitly via the existing 'run' task

tasks.register("runCli") {
    group = "application"
    description = "Run the CLI application"
    doFirst {
        application {
            mainClass.set("io.github.eslam_allam.canvas.cli.CliApp")
        }
    }
    finalizedBy(tasks.named("run"))
}

tasks.register("runGui") {
    group = "application"
    description = "Run the JavaFX GUI application"
    doFirst {
        application {
            mainClass.set("io.github.eslam_allam.canvas.gui.CanvasRubricGuiApp")
        }
    }
    finalizedBy(tasks.named("run"))
}





javafx {
    version = "25"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.base")
}

// ---------------- jpackage packaging tasks ----------------

val appName = "CanvasRubricImporter"
val appVersion = (project.version.takeIf { it != "unspecified" } ?: "1.0.0").toString()
val mainClassName = "io.github.eslam_allam.canvas.gui.CanvasRubricGuiApp"


val jpackageOutputDir = layout.buildDirectory.dir("jpackage")

// Build DEB (Linux)
tasks.register<org.gradle.api.tasks.Exec>("packageDeb") {
    group = "distribution"
    description = "Build DEB installer using jpackage (run on Linux)."

    dependsOn("jar")

    doFirst {
        jpackageOutputDir.get().asFile.mkdirs()
    }

    val libsDir = layout.buildDirectory.dir("libs").get().asFile
    commandLine(
        "jpackage",
        "--type", "deb",
        "--name", appName,
        "--app-version", appVersion,
        "--input", libsDir.absolutePath,
        "--main-jar", "${project.name}-${project.version}.jar",
        "--main-class", mainClassName,
        "--dest", jpackageOutputDir.get().asFile.absolutePath,
        "--icon", "icons/canvas-rubric-gui.png",
        "--vendor", "Canvas Rubric Importer"
    )
}

// Build RPM (Linux)
tasks.register<org.gradle.api.tasks.Exec>("packageRpm") {
    group = "distribution"
    description = "Build RPM installer using jpackage (run on Linux)."

    dependsOn("jar")

    doFirst {
        jpackageOutputDir.get().asFile.mkdirs()
    }

    val libsDir = layout.buildDirectory.dir("libs").get().asFile
    commandLine(
        "jpackage",
        "--type", "rpm",
        "--name", appName,
        "--app-version", appVersion,
        "--input", libsDir.absolutePath,
        "--main-jar", "${project.name}-${project.version}.jar",
        "--main-class", mainClassName,
        "--dest", jpackageOutputDir.get().asFile.absolutePath,
        "--icon", "icons/canvas-rubric-gui.png",
        "--vendor", "Canvas Rubric Importer"
    )
}

// Build MSI (Windows)
tasks.register<org.gradle.api.tasks.Exec>("packageMsi") {
    group = "distribution"
    description = "Build MSI installer using jpackage (run on Windows)."

    dependsOn("jar")

    doFirst {
        jpackageOutputDir.get().asFile.mkdirs()
    }

    val libsDir = layout.buildDirectory.dir("libs").get().asFile
    commandLine(
        "jpackage",
        "--type", "msi",
        "--name", appName,
        "--app-version", appVersion,
        "--input", libsDir.absolutePath,
        "--main-jar", "${project.name}-${project.version}.jar",
        "--main-class", mainClassName,
        "--dest", jpackageOutputDir.get().asFile.absolutePath,
        // Use a .ico on Windows if you have one; fall back to PNG otherwise
        // "--icon", "icons/canvas-rubric-gui.ico",
        "--vendor", "Canvas Rubric Importer"
    )
}



