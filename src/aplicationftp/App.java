package aplicationftp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/aplicationftp/view/ftp_view.fxml"));
        stage.setScene(new Scene(loader.load()));
        stage.setTitle("FTP Client");
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}

