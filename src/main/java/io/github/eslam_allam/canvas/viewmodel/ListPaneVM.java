package io.github.eslam_allam.canvas.viewmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;

public final class ListPaneVM<T> {
    private final ObservableValue<ObservableList<T>> items;
    private Optional<T> selected;
    private List<Consumer<T>> onSelectedChange;

    public ListPaneVM() {
        this.items = new SimpleListProperty<>(FXCollections.observableArrayList());
        this.selected = Optional.empty();
        this.onSelectedChange = new ArrayList<>();
    }

    public ObservableValue<ObservableList<T>> observableItems() {
        return this.items;
    }

    public ObservableList<T> items() {
        return this.items.getValue();
    }

    public Optional<T> selected() {
        return this.selected;
    }

    public void setSelected(T value) {
        this.selected = Optional.of(value);
        this.onSelectedChange.forEach(c -> c.accept(value));
    }

    public void onChange(Consumer<Change<? extends T>> callback) {
        this.items.getValue().addListener(callback::accept);
    }

    public void onSelectedChange(Consumer<T> callback) {
        this.onSelectedChange.add(callback);
    }
}
