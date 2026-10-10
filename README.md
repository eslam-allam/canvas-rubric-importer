# Canvas Rubric Importer

Canvas Rubric Importer is a cross-platform desktop and CLI tool for importing rubrics into Instructure Canvas from CSV files. It helps you:

- Generate Canvas-compatible rubrics from a simple CSV template
- Upload rubrics to specific courses and assignments
- Download existing rubrics from Canvas as CSV for editing or backup
- Run either as a GUI application (JavaFX) or as a CLI tool

The project is written in Java, uses JavaFX for the GUI, and supports modular builds with custom runtimes via `jlink` and installers via `jpackage`.

---

## Features

- **GUI mode (JavaFX)**
  - Configure Canvas base URL and access token
  - Browse courses and assignments
  - Preview rubrics before upload
  - Create or update rubrics from CSV
  - Download existing rubrics as CSV

- **CLI mode**
  - Run rubric imports in scripts or CI
  - Uses the same core logic as the GUI

- **Installers and runtimes**
  - Linux: `.deb` and `.rpm` packages
  - Linux: Flatpak bundle (`.flatpak`)
  - Windows: `.msi` installer with Start Menu and desktop shortcut
  - Each installer includes a custom runtime image (no external JRE required)

---

## Getting the application

### From GitHub Releases (recommended)

1. Go to the **Releases** page of this repository.
2. Download the installer for your platform:
   - **Linux (Debian/Ubuntu)**: `CanvasRubricImporter-<version>.deb`
   - **Linux (Fedora/openSUSE/RHEL)**: `CanvasRubricImporter-<version>.rpm`
   - **Linux (any distribution with Flatpak)**: `CanvasRubricImporter-<version>.flatpak`
   - **Windows**: `CanvasRubricImporter-<version>.msi`
3. Install it as you would any other package/installer for your OS.

After installation:

- **Windows**
  - Use the Start Menu shortcut under the *Canvas Tools* group or the desktop shortcut.
  - The application starts directly in GUI mode; no command-line flags are needed.

- **Linux (.deb/.rpm)**
  - The installer places the application in your system applications menu (under a name similar to *Canvas Rubric Importer*), depending on your desktop environment.
  - You can also run it from the terminal (see below for CLI usage).

- **Linux (Flatpak)**

  ```bash
  flatpak install --user CanvasRubricImporter-<version>.flatpak
  ```

  After installation the app appears in your applications menu, or you can launch it from the terminal:

  ```bash
  flatpak run io.github.eslam_allam.canvas          # GUI
  flatpak run io.github.eslam_allam.canvas --cli    # CLI
  ```

> Note: Replace `<version>` with the actual version string shown on the Releases page.

---

## Running from source

You need:

- Java Development Kit (JDK) 25 (the project is configured to use a toolchain for Java 25)
- Gradle (optional; the project includes Gradle wrapper scripts)

### Clone

```bash
git clone https://github.com/<your-username>/canvas-rubric-importer.git
cd canvas-rubric-importer
```

### Run the GUI from source

```bash
./gradlew runGui
```

This uses the `runGui` task, which runs `MainApp` (GUI is the default mode when no flags are passed) and starts the JavaFX UI.

### Run the CLI from source

```bash
./gradlew runCli
```

This uses the `runCli` task, which runs `MainApp` with the `--cli` flag. If you run `MainApp` without any flags, it starts in GUI mode (pass `--cli` to run the CLI).

### Run the installed app in CLI mode (Linux)

After installing the `.deb` or `.rpm`, you can usually run the CLI directly from a terminal with:

```bash
CanvasRubricImporter --cli [options]
```

With the Flatpak:

```bash
flatpak run io.github.eslam_allam.canvas --cli [options]
```

Passing `--cli` selects CLI mode; without any flags the application starts in GUI mode.



### Build platform installers from source

From the project root:

- **Linux DEB**

  ```bash
  ./gradlew packageDeb
  ```

  Output: `build/jpackage/deb/CanvasRubricImporter-<version>.deb`

- **Linux RPM**

  ```bash
  ./gradlew packageRpm
  ```

  Output: `build/jpackage/rpm/CanvasRubricImporter-<version>.rpm`

- **Windows MSI** (run on Windows)

  ```powershell
  .\gradlew packageMsi
  ```

  Output: `build\jpackage\msi\CanvasRubricImporter-<version>.msi`

Each of these tasks:

- Uses `jlink` to build a minimal Java runtime image tailored to this app and JavaFX.
- Uses `jpackage` to create the OS-specific installer.

### Build the Flatpak from source

You need `flatpak` and `flatpak-builder` (on Arch: `sudo pacman -S flatpak flatpak-builder`), plus the Flathub remote:

```bash
flatpak remote-add --user --if-not-exists flathub https://dl.flathub.org/repo/flathub.flatpakrepo
```

Build and install directly into your user installation:

```bash
flatpak-builder --force-clean --user --install build-dir flatpak/io.github.eslam_allam.canvas.yml
```

The first build downloads the freedesktop runtime and the JDK 25 SDK extension (several hundred MB, cached afterwards); Gradle and all dependencies are fetched inside the build sandbox. Later builds reuse the Gradle stage from the build cache unless the sources change.

To produce a redistributable `.flatpak` bundle instead (the same file attached to releases):

```bash
flatpak-builder --force-clean --user --repo=flatpak-repo build-dir flatpak/io.github.eslam_allam.canvas.yml
flatpak build-bundle flatpak-repo CanvasRubricImporter-<version>.flatpak io.github.eslam_allam.canvas
```

The Flatpak packaging lives in the `flatpak/` directory:

- `io.github.eslam_allam.canvas.yml` — the flatpak-builder manifest
- `io.github.eslam_allam.canvas.flathub.yml` — the Flathub manifest (offline build)
- `flatpak-sources.json` — generated dependency sources for the offline Flathub build
- `flatpak-sources-aarch64.json` — JavaFX aarch64 natives (hand-maintained)
- `launcher.sh` — the `/app/bin/CanvasRubricImporter` entry point
- `io.github.eslam_allam.canvas.desktop` — desktop menu entry
- `io.github.eslam_allam.canvas.metainfo.xml` — AppStream metadata

Packaging notes:

- The app is built from source inside the Flatpak sandbox with the `jlink` task, so the bundle contains its own Java runtime and JavaFX — no system JRE is required.
- Both manifests grant `--share=network` to the app (Canvas API access) and `--filesystem=home` (JavaFX file dialogs do not use the xdg-desktop-portal). The local manifest additionally lets the *build* download Gradle dependencies; the Flathub build is fully offline (see below).
- JavaFX is X11-only, so the app is granted `--socket=x11` and runs via XWayland on Wayland sessions.

### Publishing to Flathub

Publishing is a one-time manual submission; updates are then automatic:

1. **Submit the app** to Flathub following the
   [submission guide](https://docs.flathub.org/docs/for-app-authors/submission).
   Use `flatpak/io.github.eslam_allam.canvas.flathub.yml` as the manifest in the
   generated `flathub/io.github.eslam_allam.canvas` repository, together with
   `flatpak/flatpak-sources.json` and `flatpak/flatpak-sources-aarch64.json`
   (the manifest references them, so they must sit next to it there). The
   manifest's git tag must contain the offline build changes in
   `build.gradle.kts`/`settings.gradle.kts` (v1.2.5 predates them), and the
   metainfo needs screenshots (required by Flathub).

2. **Updates happen automatically**: the manifest's git source carries
   `x-checker-data`, so Flathub's global External Data Checker opens an update PR
   in `flathub/io.github.eslam_allam.canvas` within ~2 hours of every `vX.Y.Z` tag
   pushed to this repository. A test build runs on the PR; once a maintainer
   merges it, an official build is published to Flathub (usually within 1–2 hours).

3. Keep the `<releases>` entry in
   `flatpak/io.github.eslam_allam.canvas.metainfo.xml` current when tagging, since
   the app's AppStream metadata is built from this repository.

The Flathub manifest builds **fully offline** — Flathub's sandboxed builds
reject `build-args`, so the build cannot download anything at build time.
Instead:

- `flatpak/flatpak-sources.json` pins every direct and transitive dependency
  (Maven artifacts and Gradle plugins, generated by the
  [flatpak-gradle-generator](https://github.com/jwharm/flatpak-gradle-generator)
  plugin) in a Maven repository layout; flatpak-builder downloads all of them
  before the build starts.
- `flatpak/flatpak-sources-aarch64.json` adds the JavaFX aarch64 natives (the
  generator only captures artifacts for the host architecture).
- The Gradle distribution is pinned as a manifest source, and the build runs
  the same `jlink` Gradle task as local builds, with `--offline`.

After changing any dependency — or the Gradle version, which also needs its
archive source in `flatpak/io.github.eslam_allam.canvas.flathub.yml` updated —
regenerate and commit the sources file:

```sh
./gradlew flatpakGradleGenerator --no-configuration-cache
```

---

## Usage overview

### Configuring Canvas access

On first run in GUI mode:

1. Enter your **Canvas base URL** (e.g. `https://yourinstitution.instructure.com`).
2. Enter your **Canvas access token** (generated from your Canvas user settings).
3. Click **Save Settings**.

These settings are stored locally using the Java `Preferences` API and reused in subsequent sessions.

### Importing a rubric from CSV (GUI)

1. Choose a course and assignment from the lists (use **Load Courses** and **Load Assignments** buttons).
2. Select or type the rubric title.
3. Choose the CSV file using **Browse...**.
4. Adjust options as needed:
   - Free-form comments
   - Use for grading
   - Hide score total
   - Sync assignment points to rubric total
   - Decode HTML entities
5. Click **Show Preview** to inspect the rubric.
6. Click **Create Rubric** to upload it to Canvas.

### CLI basics

The CLI mode uses `MainApp` with the `--cli` flag and then delegates to the CLI implementation in `io.github.eslam_allam.canvas.cli`.

In practice you will usually:

- Use `./gradlew runCli` while developing
- Or run the installed binary (on Linux) as:
  ```bash
  CanvasRubricImporter --cli [options]
  ```

Refer to the CLI help (`--help`) for supported options once you have the binary or are running via `runCli`.


---

## Development notes

- **Modules**
  - The app is modular, with `module-info.java` defining:
    - `io.github.eslam_allam.canvas` as the main module
    - package exports for core, CLI, and GUI packages
    - `requires` entries for JavaFX, Jackson, Commons CSV/Text, `java.net.http`, `java.prefs`, etc.
  - JavaFX GUI classes live under `io.github.eslam_allam.canvas.gui`.

- **JavaFX**
  - The project uses the `org.openjfx.javafxplugin` for development.
  - For runtime images, platform-specific JavaFX JMODs are expected under:
    - `javafx-jmods/linux` for Linux builds
    - `javafx-jmods/windows` for Windows builds

- **jlink / jpackage**
  - `jlink` task builds a minimal runtime image in `build/image` using:
    - Platform-specific `javafx-jmods`
    - All resolved runtime dependencies (including your app JAR)
  - `packageDeb`, `packageRpm`, and `packageMsi` use that image via `--runtime-image`.

---

## License

This project is licensed under the **MIT License**.

```
MIT License

Copyright (c) 2024 Eslam Allam


Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

