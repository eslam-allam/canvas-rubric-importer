plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.diffplug.spotless") version "8.1.0"
}

group = "io.github.eslam_allam.canvas"
version = "1.0.7"

val appName = "CanvasRubricImporter"
val appVersion = (project.version.takeIf { it != "unspecified" } ?: "1.0.0").toString()
val mainClassName = "io.github.eslam_allam.canvas.MainApp"
val mainModule = "io.github.eslam_allam.canvas"
val mainClassModule = mainModule + "/" + mainClassName

repositories {
    mavenCentral()
}

spotless {

    format("misc") {
        // define the files to apply `misc` to
        target("*.gradle.*", ".gitattributes", ".gitignore")

        // define the steps to apply to those files
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }

    java {
        // apply a specific flavor of google-java-format
        googleJavaFormat().aosp().reflowLongStrings().skipJavadocFormatting()
        // fix formatting of type annotations
        formatAnnotations()
    }
}

javafx {

    version = "25"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.base")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
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
    mainModule.set(mainClassModule)
}


tasks.register<JavaExec>("runCli") {
    group = "application"
    description = "Run the CLI application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(mainClassName)
    args("--cli")
}

tasks.register<JavaExec>("runGui") {
    group = "application"
    description = "Run the JavaFX GUI application"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set(mainClassName)
}



tasks.jar {
    manifest {
        attributes(
            "Main-Class" to mainClassName
        )
    }
}


val jpackageOutputDir = layout.buildDirectory.dir("jpackage")
val jlinkImageDir = layout.buildDirectory.dir("image")

// Choose the platform-specific javafx-jmods directory
fun javafxJmodsDirForCurrentOs(): String {
    val os = org.gradle.internal.os.OperatingSystem.current()
    val base = "javafx-jmods"
    return when {
        os.isWindows -> "$base/windows"
        os.isLinux -> "$base/linux"
        os.isMacOsX -> "$base/macos"
        else -> base
    }
}

// Build a custom runtime image using jlink and local jmods (javafx-jmods)
tasks.register<org.gradle.api.tasks.Exec>("jlinkImage") {
    group = "distribution"
    description = "Create custom runtime image with jlink using local, platform-specific javafx-jmods directory"

    dependsOn("jar")

    doFirst {
        // jlink requires that the output directory must NOT exist before running
        val imageDirFile = jlinkImageDir.get().asFile
        if (imageDirFile.exists()) {
            imageDirFile.deleteRecursively()
        }
    }

    // Resolve all runtime dependencies (3rd-party libraries, not including the app JAR)
    val runtimeClasspath = configurations["runtimeClasspath"].resolve()
    val dependencyJars = runtimeClasspath.filter { !it.name.startsWith("${project.name}-") }
    val depsOnPath = dependencyJars.joinToString(File.pathSeparator) { it.absolutePath }

    // The application jar, which contains the named module io.github.eslam_allam.canvas
    val libsDir = layout.buildDirectory.dir("libs").get().asFile
    val appJar = File(libsDir, "${project.name}-${project.version}.jar")

    // Compose full module-path: platform-specific javafx-jmods + all dependency jars + the app jar
    val jmodsDir = javafxJmodsDirForCurrentOs()
    val fullModulePath = listOf(jmodsDir, depsOnPath, appJar.absolutePath)
        .filter { it.isNotBlank() }
        .joinToString(File.pathSeparator)

    // Use local jmods directory plus all runtime jars and the app jar on the module path
    commandLine(
        "jlink",
        "--module-path", fullModulePath,
        "--add-modules", mainModule,
        "--output", jlinkImageDir.get().asFile.absolutePath,
        "--strip-debug",
        "--compress", "2",
        "--no-header-files",
        "--no-man-pages"
    )
}



// Build DEB (Linux) using jpackage and a custom runtime image from jlink
val debOutputDir = jpackageOutputDir.map { it.dir("deb") }

tasks.register<org.gradle.api.tasks.Exec>("packageDeb") {
    dependsOn("jlinkImage")

    group = "distribution"
    description = "Build DEB installer using jpackage (run on Linux)."

    dependsOn("jar")

    doFirst {
        val outDir = debOutputDir.get().asFile
        if (outDir.exists()) {
            outDir.deleteRecursively()
        }
        outDir.mkdirs()
    }

    val imageDir = jlinkImageDir.get().asFile

    commandLine(
        "jpackage",
        "--type", "deb",
        "--name", appName,
        "--app-version", appVersion,
        "--runtime-image", imageDir.absolutePath,
        "--module", mainClassModule,
        "--dest", debOutputDir.get().asFile.absolutePath,
        "--icon", "icons/png/canvas_rubric_importer 128x128.png",
        "--vendor", "Canvas Rubric Importer",

        "--linux-shortcut",
        "--linux-menu-group", "Canvas Tools",
        "--linux-deb-maintainer", "eslam allam <elamallam73@gmail.com>"

    )
}



// Build RPM (Linux) using jpackage and a custom runtime image from jlink
val rpmOutputDir = jpackageOutputDir.map { it.dir("rpm") }

tasks.register<org.gradle.api.tasks.Exec>("packageRpm") {
    dependsOn("jlinkImage")

    group = "distribution"
    description = "Build RPM installer using jpackage (run on Linux)."

    dependsOn("jar")

    doFirst {
        val outDir = rpmOutputDir.get().asFile
        if (outDir.exists()) {
            outDir.deleteRecursively()
        }
        outDir.mkdirs()
    }

    val imageDir = jlinkImageDir.get().asFile

    commandLine(
        "jpackage",
        "--type", "rpm",
        "--name", appName,
        "--app-version", appVersion,
        "--runtime-image", imageDir.absolutePath,
        "--module", mainClassModule,
        "--dest", rpmOutputDir.get().asFile.absolutePath,
        "--icon", "icons/png/canvas_rubric_importer 128x128.png",
        "--vendor", "Canvas Rubric Importer",

        "--linux-shortcut",
        "--linux-menu-group", "Canvas Tools"

    )
}

// Build MSI (Windows) using jpackage and a custom runtime image from jlink
val msiOutputDir = jpackageOutputDir.map { it.dir("msi") }

tasks.register<org.gradle.api.tasks.Exec>("packageMsi") {
    dependsOn("jlinkImage")

    group = "distribution"
    description = "Build MSI installer using jpackage (run on Windows)."

    dependsOn("jar")

    doFirst {
        val outDir = msiOutputDir.get().asFile
        if (outDir.exists()) {
            outDir.deleteRecursively()
        }
        outDir.mkdirs()
    }

    val imageDir = jlinkImageDir.get().asFile

    commandLine(
        "jpackage",
        "--type", "msi",
        "--name", appName,
        "--app-version", appVersion,
        "--runtime-image", imageDir.absolutePath,
        "--module", mainClassModule,
        "--dest", msiOutputDir.get().asFile.absolutePath,
        "--icon", "icons/canvas_rubric_importer.ico",
        "--vendor", "Canvas Rubric Importer",
        "--win-shortcut",
        "--win-menu",
        "--win-menu-group", "Canvas Tools"
    )
}
