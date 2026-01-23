package io.github.eslam_allam.canvas.constant;

import java.util.stream.Stream;

public enum FileType {
    CSV("CSV Files", "csv"),
    HTML("HTML Files", "html"),
    JSON("JSON Files", "json"),
    PDF("PDF Files", "pdf"),
    PNG("PNG Files", "png"),
    WORD("Office Word Files", "doc", "docx"),
    EXCEL("Office Excel Files", "xls", "xlsx"),
    ZIP("ZIP Files", "zip");

    private final String description;
    private final String[] extensions;

    private FileType(String description, String... extensions) {
        this.description = description;
        this.extensions = extensions;
    }

    public String getDescription() {
        return description;
    }

    public String[] getExtensions() {
        return extensions;
    }

    public String[] getGlobs() {
        return Stream.of(extensions).map(ext -> "*." + ext).toArray(String[]::new);
    }
}
