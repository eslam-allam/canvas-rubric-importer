package io.github.eslam_allam.canvas.domain;

public record Result<T>(ResultStatus status, T data) {}
