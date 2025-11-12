/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package aplicationftp.controller;

import aplicationftp.model.FTPFileItem;
import aplicationftp.service.FTPService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

public class FtpController {

    @FXML
    private TextField hostField;
    @FXML
    private TextField userField;
    @FXML
    private TextField passField;
    @FXML
    private TableView<FTPFileItem> filesTable;
    @FXML
    private TableColumn<FTPFileItem, String> colName;
    @FXML
    private TableColumn<FTPFileItem, String> colSize;
    @FXML
    private TableColumn<FTPFileItem, String> colType;
    @FXML
    private TableColumn<FTPFileItem, String> colDate;
    @FXML
    private Label statusLabel;

    private FTPService ftpService;

    @FXML
    public void onConnectAction() {
        String host = hostField.getText();
        String user = userField.getText();
        String pass = passField.getText();

        ftpService = new FTPService(host, user, pass);

        Task<Void> connectTask = new Task<>() {
            @Override
            protected Void call() {
                if (ftpService.connect()) {
                    updateMessage("Connectat correctament!");
                    refreshFileList();
                } else {
                    updateMessage("Error de connexió!");
                }
                return null;
            }
        };

        connectTask.messageProperty().addListener((obs, oldMsg, newMsg) -> {
            Platform.runLater(() -> statusLabel.setText(newMsg));
        });

        new Thread(connectTask).start();
    }

    private void refreshFileList() {
        Platform.runLater(() -> {
            filesTable.getItems().clear();
            List<FTPFileItem> items = ftpService.listFiles();
            filesTable.getItems().addAll(items);
        });

        colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        colSize.setCellValueFactory(cell -> cell.getValue().sizeProperty());
        colType.setCellValueFactory(cell -> cell.getValue().typeProperty());
        colDate.setCellValueFactory(cell -> cell.getValue().dateProperty());
    }

    @FXML
    public void onUploadAction() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ftpService.uploadFile(file.getAbsolutePath(), file.getName());
                    refreshFileList();
                    return null;
                }
            };
            new Thread(task).start();
        }
    }

    @FXML
    public void onDownloadAction() {
        FTPFileItem selected = filesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName(selected.getName());
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        ftpService.downloadFile(selected.getName(), file.getAbsolutePath());
                        return null;
                    }
                };
                new Thread(task).start();
            }
        }
    }

    @FXML
    public void onDeleteAction() {
        FTPFileItem selected = filesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ftpService.deleteFile(selected.getName());
                    refreshFileList();
                    return null;
                }
            };
            new Thread(task).start();
        }
    }

    @FXML
    public void onCreateDirAction() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Crear Carpeta");
        dialog.setHeaderText("Introduce el nombre de la nueva carpeta:");
        dialog.setContentText("Nombre:");

        dialog.showAndWait().ifPresent(folderName -> {
            if (!folderName.trim().isEmpty()) {
                Task<Void> createDirTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        try {
                            ftpService.createDirectory(folderName);
                            refreshFileList();
                            Platform.runLater(() -> statusLabel.setText("Carpeta creada: " + folderName));
                        } catch (Exception e) {
                            Platform.runLater(() -> statusLabel.setText("Error creando carpeta"));
                            e.printStackTrace();
                        }
                        return null;
                    }
                };
                new Thread(createDirTask).start();
            }
        });
    }

}
