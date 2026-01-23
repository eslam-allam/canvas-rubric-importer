package io.github.eslam_allam.canvas.notification;

import io.github.eslam_allam.canvas.constant.StandardAlert;
import javafx.scene.control.Alert;
import javafx.stage.Modality;

public final class PopUp {

    private PopUp() {}

    public static void showError(String msg) {
        show(Alert.AlertType.ERROR, "Error", msg);
    }

    public static void showError(String title, String msg) {
        show(Alert.AlertType.ERROR, title, msg);
    }

    public static void showInfo(String msg) {
        show(Alert.AlertType.INFORMATION, "Info", msg);
    }

    public static void showInfo(String title, String msg) {
        show(Alert.AlertType.INFORMATION, title, msg);
    }

    private static void show(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public static void show(StandardAlert alert) {
        show(alert.type, alert.title, alert.message);
    }
}
