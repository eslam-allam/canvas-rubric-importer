import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.diffplug.spotless") version "8.1.0"
    id("org.beryx.jlink") version "3.1.3"
}

repositories {
    mavenCentral()
}

data class AppMeta(
    val name: String,
    val version: String,
    val vendor: String,
    val id: String,
    val maintainerName: String,
    val maintainerEmail: String,
)

val appMeta =
    AppMeta(
        name = property("app.name").toString(),
        version = property("app.version").toString(),
        vendor = property("app.vendor").toString(),
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

    version = "25"
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
        languageVersion.set(JavaLanguageVersion.of(25))
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

jlink {
    imageDir.set(layout.buildDirectory.dir("image"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))

    launcher {
        name = appMeta.name
        jvmArgs = listOf("-m", mainClassModule)
    }

    jpackage {
        // Use plugin defaults for output dirs

        installerName = appMeta.name
        appVersion = appMeta.version
        vendor = appMeta.vendor
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            project.logger.lifecycle("Using windows icon")
            icon = "icons/canvas_rubric_importer.ico"
            installerOptions =
                listOf(
                    "--win-shortcut", // Create a Desktop shortcut
                    "--win-menu", // Add to Start Menu
                    "--win-menu-group",
                    "Canvas Tools", // (Optional) Group in Start Menu
                    "--win-dir-chooser",
                )
        } else {
            project.logger.lifecycle("Using Linux Icon")
            icon = "icons/png/canvas_rubric_importer 128x128.png"
            installerOptions =
                listOf(
                    "--linux-shortcut", // Creates a .desktop file
                    "--linux-menu-group",
                    "Canvas Tools", // (Optional) Menu category
                    "--linux-deb-maintainer",
                    appMeta.maintainerName + " " + appMeta.maintainerEmail,
                )
        }
    }
}
