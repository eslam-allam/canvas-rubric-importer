plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "io.github.eslam_allam.canvas"
version = "1.0.0"

repositories {
    mavenCentral()
}

javafx {
    version = "25"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.base")
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

// ---------------- jlink + jpackage packaging tasks ----------------

val appName = "CanvasRubricImporter"
val appVersion = (project.version.takeIf { it != "unspecified" } ?: "1.0.0").toString()
val mainClassName = "io.github.eslam_allam.canvas.gui.CanvasRubricGuiApp"

val jlinkImageDir = layout.buildDirectory.dir("jlink-image")
val jpackageOutputDir = layout.buildDirectory.dir("jpackage")

// Create custom runtime image using jlink
tasks.register<org.gradle.api.tasks.Exec>("jlinkImage") {
    group = "distribution"
    description = "Create custom runtime image using jlink."

    dependsOn("jar")

    val imageDir = jlinkImageDir.get().asFile

    doFirst {
        if (imageDir.exists()) {
            imageDir.deleteRecursively()
        }
    }

    // Base modules you need; add/remove as required by your app
    val modules = listOf(
        "java.base",
        "java.logging",
        "java.xml",
        "jdk.crypto.ec",
        "jdk.unsupported",
        "java.desktop" // often needed by JavaFX host environment
    )

    val javaHome = System.getProperty("java.home")
    val jmodsDir = file("$javaHome/jmods")

    commandLine(
        "jlink",
        "--module-path", jmodsDir.absolutePath,
        "--no-header-files",
        "--no-man-pages",
        "--strip-debug",
        "--compress", "2",
        "--add-modules", modules.joinToString(","),
        "--output", imageDir.absolutePath
    )
}

// Build DEB (Linux) using the custom runtime image
tasks.register<org.gradle.api.tasks.Exec>("packageDeb") {
    group = "distribution"
    description = "Build DEB installer using jpackage with jlink image (run on Linux)."

    dependsOn("jlinkImage")

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
        "--runtime-image", jlinkImageDir.get().asFile.absolutePath,
        "--dest", jpackageOutputDir.get().asFile.absolutePath,
        "--icon", "icons/canvas-rubric-gui.png",
        "--vendor", "Canvas Rubric Importer"
    )
}

// Build RPM (Linux) using the custom runtime image
tasks.register<org.gradle.api.tasks.Exec>("packageRpm") {
    group = "distribution"
    description = "Build RPM installer using jpackage with jlink image (run on Linux)."

    dependsOn("jlinkImage")

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
        "--runtime-image", jlinkImageDir.get().asFile.absolutePath,
        "--dest", jpackageOutputDir.get().asFile.absolutePath,
        "--icon", "icons/canvas-rubric-gui.png",
        "--vendor", "Canvas Rubric Importer"
    )
}

// Build MSI (Windows) using the custom runtime image
tasks.register<org.gradle.api.tasks.Exec>("packageMsi") {
    group = "distribution"
    description = "Build MSI installer using jpackage with jlink image (run on Windows)."

    dependsOn("jlinkImage")

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
        "--runtime-image", jlinkImageDir.get().asFile.absolutePath,
        "--dest", jpackageOutputDir.get().asFile.absolutePath,
        "--vendor", "Canvas Rubric Importer"
    )
}

