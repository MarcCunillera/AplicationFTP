package aplicationftp.controller;

import aplicationftp.model.FTPFileItem;
import aplicationftp.service.FTPService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FtpController {

    @FXML
    private TextField hostField;
    @FXML
    private TextField userField;
    @FXML
    private TextField passField;
    @FXML
    private TreeView<String> remoteTree;
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
    @FXML
    private ProgressBar progressBar;

    private FTPService ftpService;

    @FXML
    private void initialize() {
        // Configuración de columnas
        colName.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        colSize.setCellValueFactory(cellData -> cellData.getValue().sizeProperty());
        colType.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        colDate.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
    }

    // ------------------ CONEXIÓN ------------------
    @FXML
    public void onConnectAction() {
        String host = hostField.getText();
        String user = userField.getText();
        String pass = passField.getText();

        ftpService = new FTPService(host, user, pass);
        progressBar.setVisible(true);
        statusLabel.setText("Conectando...");

        Task<Void> connectTask = new Task<>() {
            @Override
            protected Void call() {
                if (ftpService.connect()) {
                    updateMessage("Conectado correctamente");
                    Platform.runLater(FtpController.this::loadRemoteDirectories);
                } else {
                    updateMessage("Error de conexión");
                }
                return null;
            }
        };

        connectTask.messageProperty().addListener((obs, o, n)
                -> Platform.runLater(() -> statusLabel.setText(n)));

        connectTask.setOnSucceeded(e -> progressBar.setVisible(false));
        connectTask.setOnFailed(e -> progressBar.setVisible(false));

        new Thread(connectTask).start();
    }

    // ------------------ TREEVIEW RECURSIVO ------------------
    private void loadRemoteDirectories() {
        Task<TreeItem<String>> treeTask = new Task<>() {
            @Override
            protected TreeItem<String> call() throws Exception {
                TreeItem<String> rootItem = new TreeItem<>("Servidor FTP");
                rootItem.setExpanded(true);
                addChildrenRecursively(rootItem, "/");
                return rootItem;
            }
        };

        treeTask.setOnSucceeded(e -> {
            remoteTree.setRoot(treeTask.getValue());
            statusLabel.setText("Árbol de directorios cargado.");

            remoteTree.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                if (newSel != null) {
                    String path = getFullPath(newSel);
                    refreshFileList(path);
                }
            });
        });

        treeTask.setOnFailed(e -> statusLabel.setText("Error cargando estructura del servidor."));
        new Thread(treeTask).start();
    }

    private void addChildrenRecursively(TreeItem<String> parent, String path) throws IOException {
        FTPFile[] files = ftpService.getFtpClient().listFiles(path);
        if (files != null) {
            for (FTPFile file : files) {
                if (file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..")) {
                    TreeItem<String> child = new TreeItem<>(file.getName());
                    parent.getChildren().add(child);
                    addChildrenRecursively(child, path + "/" + file.getName());
                }
            }
        }
    }

    private String getFullPath(TreeItem<String> item) {
        StringBuilder path = new StringBuilder();
        TreeItem<String> current = item;
        while (current != null && current.getParent() != null) {
            path.insert(0, "/" + current.getValue());
            current = current.getParent();
        }
        return path.length() == 0 ? "/" : path.toString();
    }

    // ------------------ REFRESCAR TAULA ------------------
    private void refreshFileList(String path) {
        Task<List<FTPFileItem>> task = new Task<>() {
            @Override
            protected List<FTPFileItem> call() {
                try {
                    return ftpService.listFiles(path);
                } catch (Exception e) {
                    e.printStackTrace();
                    return List.of();
                }
            }
        };

        task.setOnSucceeded(e -> {
            filesTable.getItems().setAll(task.getValue());
            statusLabel.setText("Directorio: " + path);
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();
            statusLabel.setText("Error al cargar el directorio");
        });

        new Thread(task).start();
    }

    // ------------------ PUJAR ARXIU ------------------
    @FXML
    public void onUploadAction() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ftpService.uploadFile(file.getAbsolutePath(), file.getName());
                    Platform.runLater(() -> refreshFileList("/"));
                    return null;
                }
            };
            new Thread(task).start();
        }
    }

    // ------------------ DESCARREGAR ARXIU ------------------
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

    // ------------------ CREAR CARPETA ------------------
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
                        ftpService.createDirectory(folderName);
                        Platform.runLater(() -> {
                            statusLabel.setText("Carpeta creada: " + folderName);
                            loadRemoteDirectories();
                        });
                        return null;
                    }
                };
                new Thread(createDirTask).start();
            }
        });
    }

    // ------------------ ELIMINAR ARXIU ------------------
    @FXML
    public void onDeleteAction() {
        FTPFileItem selected = filesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ftpService.deleteFile(selected.getName());
                    Platform.runLater(() -> {
                        loadRemoteDirectories();
                        statusLabel.setText("Archivo eliminado: " + selected.getName());
                    });
                    return null;
                }
            };
            new Thread(task).start();
        }
    }
}
