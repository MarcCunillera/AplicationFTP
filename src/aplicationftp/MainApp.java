package aplicationftp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/aplicationftp/ftp_view.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/aplicationftp/style.css").toExternalForm());
        stage.setTitle("AplicationFTP");
        // opcional: poner un icono si tienes uno
        // stage.getIcons().add(new Image(getClass().getResourceAsStream("/aplicationftp/icon.png")));
        stage.setScene(scene);
        stage.setWidth(900);
        stage.setHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


