package io.github.eslam_allam.canvas.service;

import io.github.eslam_allam.canvas.client.CanvasCredentialProvider;
import java.util.prefs.Preferences;

public class PreferencesService implements CanvasCredentialProvider {

    private final Preferences prefs;

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
