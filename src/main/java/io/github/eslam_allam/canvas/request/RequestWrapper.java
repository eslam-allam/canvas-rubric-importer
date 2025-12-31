package io.github.eslam_allam.canvas.request;

import java.util.Map;

public final class RequestWrapper {

    private RequestWrapper() {}

    public static <T> Map<String, T> wrap(String key, T value) {
        return Map.of(key, value);
    }
}
