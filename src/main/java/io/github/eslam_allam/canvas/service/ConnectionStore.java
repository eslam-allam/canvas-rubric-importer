package io.github.eslam_allam.canvas.service;

public interface ConnectionStore {
    void saveSettings(String baseUrl, String token);

    String loadBaseUrl();

    String loadToken();
}
