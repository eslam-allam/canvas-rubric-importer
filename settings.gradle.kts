pluginManagement {
    repositories {
        // Offline Maven repository populated by the flatpak-gradle-generator
        // plugin; used by the Flathub build, harmless when absent.
        maven { url = file("offline-repository").toURI() }
        gradlePluginPortal()
    }
}
