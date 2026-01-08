package io.github.eslam_allam.canvas.view.component;

import io.github.eslam_allam.canvas.viewmodel.ListPaneVM;
import java.util.function.Function;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

public final class SimpleListPane<T> implements ListPane<T> {
    private final ListView<T> listView;

    private final VBox root;
    private final Button loadAssignmentsBtn;
    private final String title;

    public String getTitle() {
        return title;
    }

    public SimpleListPane(String title, Function<T, String> itemFormatter) {
        this.title = title;
        this.listView = new ListView<>();
        this.listView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(itemFormatter.apply(item));
                }
            }
        });

        this.root = new VBox(10);
        this.root.setPadding(new Insets(5));

        Label label = new Label(title);

        loadAssignmentsBtn = new Button("Load " + title);

        this.root.getChildren().addAll(label, listView, loadAssignmentsBtn);
    }

    public void onLoad(EventHandler<ActionEvent> callback) {
        this.loadAssignmentsBtn.setOnAction(callback);
    }

    public void bind(ListPaneVM<T> vm) {
        this.listView.itemsProperty().bind(vm.observableItems());
        this.listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> vm.setSelected(newV));
    }

    public Node getRoot() {
        return this.root;
    }
}
