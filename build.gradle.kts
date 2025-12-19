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
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

application {
    // Modular application: specify module/name instead of only the main class
    mainModule.set("io.github.eslam_allam.canvas/io.github.eslam_allam.canvas.MainApp")
}



tasks.register<JavaExec>("runCli") {
    group = "application"
    description = "Run the CLI application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.eslam_allam.canvas.MainApp")
    args("--cli")
}

tasks.register<JavaExec>("runGui") {
    group = "application"
    description = "Run the JavaFX GUI application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.eslam_allam.canvas.MainApp")
    args("--gui")
}



tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "io.github.eslam_allam.canvas.MainApp"
        )
    }
}

val appName = "CanvasRubricImporter"
val appVersion = (project.version.takeIf { it != "unspecified" } ?: "1.0.0").toString()
val mainClassName = "io.github.eslam_allam.canvas.MainApp"

val jpackageOutputDir = layout.buildDirectory.dir("jpackage")
val jlinkImageDir = layout.buildDirectory.dir("image")

// Build a custom runtime image using jlink and local jmods (javafx-jmods)
tasks.register<org.gradle.api.tasks.Exec>("jlinkImage") {
    group = "distribution"
    description = "Create custom runtime image with jlink using local javafx-jmods directory"

    dependsOn("jar")

    doFirst {
        // jlink requires that the output directory must NOT exist before running
        val imageDirFile = jlinkImageDir.get().asFile
        if (imageDirFile.exists()) {
            imageDirFile.deleteRecursively()
        }
        // Do not create the directory here; let jlink create it.
    }


    // Resolve all runtime dependencies (3rd-party libraries, not including the app JAR)
    val runtimeClasspath = configurations["runtimeClasspath"].resolve()
    val dependencyJars = runtimeClasspath.filter { !it.name.startsWith("${project.name}-") }
    val depsOnPath = dependencyJars.joinToString(File.pathSeparator) { it.absolutePath }

    // The application jar, which contains the named module io.github.eslam_allam.canvas
    val libsDir = layout.buildDirectory.dir("libs").get().asFile
    val appJar = File(libsDir, "${project.name}-${project.version}.jar")

    // Compose full module-path: local jmods + all dependency jars + the app jar
    val fullModulePath = listOf("javafx-jmods", depsOnPath, appJar.absolutePath)
        .filter { it.isNotBlank() }
        .joinToString(File.pathSeparator)

    // Use local jmods directory plus all runtime jars and the app jar on the module path
    commandLine(
        "jlink",
        "--module-path", fullModulePath,
        "--add-modules", "io.github.eslam_allam.canvas",
        "--output", jlinkImageDir.get().asFile.absolutePath,
        "--strip-debug",
        "--compress", "2",
        "--no-header-files",
        "--no-man-pages"
    )
}


// Build DEB (Linux) using jpackage and a custom runtime image from jlink
tasks.register<org.gradle.api.tasks.Exec>("packageDeb") {
    dependsOn("jlinkImage")

    group = "distribution"
    description = "Build DEB installer using jpackage (run on Linux)."

    dependsOn("jar")

    doFirst {
        jpackageOutputDir.get().asFile.mkdirs()
    }

    val imageDir = jlinkImageDir.get().asFile
    commandLine(
        "jpackage",
        "--type", "deb",
        "--name", appName,
        "--app-version", appVersion,
        "--runtime-image", imageDir.absolutePath,
        "--module", "io.github.eslam_allam.canvas/io.github.eslam_allam.canvas.MainApp",
        "--dest", jpackageOutputDir.get().asFile.absolutePath,
        "--icon", "icons/canvas-rubric-gui.png",
        "--vendor", "Canvas Rubric Importer"
    )
}

// Build RPM (Linux) using jpackage and the default runtime
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

// Build MSI (Windows) using jpackage and the default runtime
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
        "--vendor", "Canvas Rubric Importer"
    )
}


