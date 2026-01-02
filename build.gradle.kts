import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.internal.jvm.Jvm

buildscript {
    plugins {
        id("com.diffplug.spotless") version "8.1.0"
        id("org.beryx.jlink") version "3.2.0"
        id("org.openjfx.javafxplugin") version "0.1.0"
    }
}

plugins {
    id("application")
}

repositories {
    mavenCentral()
}

data class AppMeta(
    val name: String,
    val version: String,
    val vendor: String,
    val description: String,
    val id: String,
    val maintainerName: String,
    val maintainerEmail: String,
)

val appMeta =
    AppMeta(
        name = property("app.name").toString(),
        version = property("app.version").toString(),
        vendor = property("app.vendor").toString(),
        description = property("app.description").toString(),
        id = property("app.id").toString(),
        maintainerName = property("app.maintainer.name").toString(),
        maintainerEmail = property("app.maintainer.email").toString(),
    )

version = appMeta.version
group = appMeta.id

val mainClassName = "${appMeta.id}.MainApp"
val mainClassModule = "${appMeta.id}/$mainClassName"

val generatedSrcDir =
    layout.buildDirectory.dir("generated/sources/appinfo/java/main")
val generateAppInfo by tasks.registering {

    outputs.dir(generatedSrcDir)

    doLast {
        val baseDir = generatedSrcDir.get().asFile
        val packageDir =
            File(
                baseDir,
                appMeta.id.replace(".", "/"),
            )
        packageDir.mkdirs()
        file("$packageDir/AppInfo.java").writeText(
            """
            package ${appMeta.id};

            public final class AppInfo {
                public static final String NAME = "${appMeta.name}";
                public static final String VERSION = "${appMeta.version}";
                public static final String VENDOR = "${appMeta.vendor}";
                public static final String ID = "${appMeta.id}";
                public static final String MAINTAINER_NAME = "${appMeta.maintainerName}";
                public static final String MAINTAINER_EMAIL = "${appMeta.maintainerEmail}";
                private AppInfo() {}
            }
            """.trimIndent(),
        )
    }
}

sourceSets["main"].java.srcDir(
    generatedSrcDir,
)

tasks.named("compileJava") {
    dependsOn(generateAppInfo)
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
        palantirJavaFormat()
        formatAnnotations()
        targetExclude("build/generated/**")
    }

    kotlinGradle {
        // Apply the same formatting rules
        ktlint()
    }
}

javafx {

    version = "21"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.base")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
    implementation("org.apache.commons:commons-csv:1.11.0")
    implementation("org.apache.commons:commons-text:1.12.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.6")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainModule.set(appMeta.id)
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
            "Main-Class" to mainClassName,
        )
    }
}

val cleanedAppName = appMeta.name.replace(" ", "_").lowercase()

jlink {
    options = listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages")
    javaHome = Jvm.current().getJavaHome()

    launcher {
        name = appMeta.name
        jvmArgs = listOf("-m", mainClassModule, "--enable-native-access", "javafx.graphics")
    }

    jpackage {
        appVersion = appMeta.version
        vendor = appMeta.vendor
        installerOptions =
            mutableListOf(
                "--description",
                appMeta.description,
                "--copyright",
                "Copyright 2026 eslam-allam",
            )
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            project.logger.lifecycle("Using windows icon")
            imageOptions = mutableListOf("--icon", "icons/canvas_rubric_importer.ico")
            installerOptions.plus(
                listOf(
                    "--icon",
                    "icons/png/canvas_rubric_importer 128x128.png",
                    "--win-per-user-install",
                    "--win-shortcut", // Create a Desktop shortcut
                    "--win-menu", // Add to Start Menu
                    "--win-menu-group",
                    "Canvas Tools", // (Optional) Group in Start Menu
                    "--win-dir-chooser",
                ),
            )
        } else {
            project.logger.lifecycle("Using Linux Icon")
            imageOptions = mutableListOf("--icon", "icons/png/canvas_rubric_importer 128x128.png")
            installerOptions.plus(
                listOf(
                    "--icon",
                    "icons/png/canvas_rubric_importer 128x128.png",
                    "--linux-shortcut", // Creates a .desktop file
                    "--linux-menu-group",
                    "Canvas Tools", // (Optional) Menu category
                    "--linux-deb-maintainer",
                    appMeta.maintainerName + " " + appMeta.maintainerEmail,
                ),
            )
        }
    }
}
