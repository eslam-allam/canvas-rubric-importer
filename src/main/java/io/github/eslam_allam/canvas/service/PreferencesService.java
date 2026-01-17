package io.github.eslam_allam.canvas.service;

import io.github.eslam_allam.canvas.client.CanvasCredentialProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.prefs.Preferences;

@Singleton
public class PreferencesService implements CanvasCredentialProvider, ConnectionStore {

    private final Preferences prefs;

    @Inject
    public PreferencesService(Class<?> prefsNodeForClass) {
        this.prefs = Preferences.userNodeForPackage(prefsNodeForClass);
    }

    public String loadBaseUrl() {
        return prefs.get("baseUrl", "").trim();
    }

    public String loadToken() {
        return prefs.get("token", "").trim();
    }

    public void saveSettings(String baseUrl, String token) {
        prefs.put("baseUrl", baseUrl == null ? "" : baseUrl.trim());
        prefs.put("token", token == null ? "" : token.trim());
    }
}
