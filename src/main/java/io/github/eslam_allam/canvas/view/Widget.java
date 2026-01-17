package io.github.eslam_allam.canvas.view;

public interface Widget<T> extends View {
    void bind(T vm);
}
