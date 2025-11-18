package aplicationftp.controller;

import aplicationftp.model.FTPFileItem;
import aplicationftp.service.FTPService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.scene.input.ContextMenuEvent;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FtpController {

    // Campos de conexión
    @FXML
    private TextField hostField;
    @FXML
    private TextField userField;
    @FXML
    private TextField passField;

    // Árbol de directorios remoto y tabla de archivos
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

    // Label de estado
    @FXML
    private Label statusLabel;

    // Servicio FTP
    private FTPService ftpService;

    // Menús contextuales
    private ContextMenu remoteTreeMenu;
    private ContextMenu filesTableMenu;

    // ------------------ INICIALIZACIÓN ------------------
    @FXML
    public void initialize() {
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
        statusLabel.setText("Conectando...");

        // Ejecutar conexión en hilo separado para no bloquear la interfaz
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

        connectTask.messageProperty().addListener((obs, o, n) ->
                Platform.runLater(() -> statusLabel.setText(n)));

        new Thread(connectTask).start();
    }

    // ------------------ TREEVIEW ------------------
    /** Carga el árbol de directorios remoto a partir de la raíz "/" */
    private void loadRemoteDirectories() {
        Task<TreeItem<String>> treeTask = new Task<>() {
            @Override
            protected TreeItem<String> call() {
                TreeItem<String> rootItem = new TreeItem<>("Servidor FTP");
                rootItem.setExpanded(true);
                addChildrenLazy(rootItem, "/"); // Carga subdirectorios perezosamente
                return rootItem;
            }
        };

        // Cuando el árbol se carga, asignamos listener para actualizar la tabla al seleccionar un directorio
        treeTask.setOnSucceeded(e -> {
            remoteTree.setRoot(treeTask.getValue());
            statusLabel.setText("Árbol de directorios cargado.");
            remoteTree.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
                if (newSel != null) {
                    refreshFileList(getFullPath(newSel));
                }
            });
        });

        treeTask.setOnFailed(e -> {
            Throwable ex = treeTask.getException();
            if (ex != null) ex.printStackTrace();
            statusLabel.setText("Error cargando estructura del servidor.");
        });

        new Thread(treeTask).start();
    }

    /** Carga subdirectorios de manera perezosa (solo al expandir el nodo) */
    private void addChildrenLazy(TreeItem<String> parent, String path) {
        TreeItem<String> dummy = new TreeItem<>("Cargando...");
        parent.getChildren().add(dummy);

        parent.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
            if (isNowExpanded && parent.getChildren().contains(dummy)) {
                parent.getChildren().remove(dummy);

                Task<Void> loadTask = new Task<>() {
                    @Override
                    protected Void call() {
                        try {
                            if (!ftpService.ensureConnected()) return null;
                            FTPFile[] files = ftpService.getFtpClient().listFiles(path);
                            if (files != null) {
                                for (FTPFile file : files) {
                                    if (file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..")) {
                                        TreeItem<String> child = new TreeItem<>(file.getName());
                                        Platform.runLater(() -> {
                                            parent.getChildren().add(child);
                                            addChildrenLazy(child, path + "/" + file.getName());
                                        });
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        return null;
                    }
                };
                new Thread(loadTask).start();
            }
        });
    }

    /** Devuelve la ruta completa del TreeItem seleccionado */
    private String getFullPath(TreeItem<String> item) {
        StringBuilder path = new StringBuilder();
        TreeItem<String> current = item;
        while (current != null && current.getParent() != null) {
            path.insert(0, "/" + current.getValue());
            current = current.getParent();
        }
        return path.length() == 0 ? "/" : path.toString();
    }

    // ------------------ REFRESCAR TABLA ------------------
    /** Actualiza la tabla de archivos de un directorio remoto */
    private synchronized void refreshFileList(String path) {
        Task<List<FTPFileItem>> task = new Task<>() {
            @Override
            protected List<FTPFileItem> call() {
                try {
                    if (!ftpService.ensureConnected()) return List.of();
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
            Throwable ex = task.getException();
            if (ex != null) ex.printStackTrace();
            statusLabel.setText("Error al cargar el directorio: " + path);
        });

        new Thread(task).start();
    }

    // ------------------ SUBIR ARCHIVO ------------------
    /** Sube un archivo local al directorio remoto seleccionado */
    @FXML
    public void onUploadAction() {
        TreeItem<String> selectedDir = remoteTree.getSelectionModel().getSelectedItem();
        if (selectedDir == null) {
            statusLabel.setText("Selecciona una carpeta primero");
            return;
        }
        String remotePath = getFullPath(selectedDir);

        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ftpService.uploadFile(file.getAbsolutePath(), remotePath + "/" + file.getName());
                    Platform.runLater(() -> refreshFileList(remotePath));
                    return null;
                }
            };
            new Thread(task).start();
        }
    }

    // ------------------ DESCARGAR ------------------
    /** Descarga un archivo o carpeta seleccionada */
    @FXML
    public void onDownloadAction() {
        TreeItem<String> selectedDir = remoteTree.getSelectionModel().getSelectedItem();
        FTPFileItem selectedFile = filesTable.getSelectionModel().getSelectedItem();

        if (selectedFile != null) {
            // Descargar archivo individual
            FileChooser fileChooser = new FileChooser();
            fileChooser.setInitialFileName(selectedFile.getName());
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        String fullPath = getFullPath(selectedDir) + "/" + selectedFile.getName();
                        ftpService.downloadFile(fullPath, file.getAbsolutePath());
                        return null;
                    }
                };
                new Thread(task).start();
            }
        } else if (selectedDir != null && selectedDir.getParent() != null) {
            // Descargar carpeta completa
            DirectoryChooser dirChooser = new DirectoryChooser();
            File localDir = dirChooser.showDialog(null);
            if (localDir != null) {
                String remotePath = getFullPath(selectedDir);
                Task<Void> task = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        ftpService.downloadDirectoryRecursively(remotePath, localDir.getAbsolutePath());
                        return null;
                    }
                };
                new Thread(task).start();
            }
        } else {
            statusLabel.setText("Selecciona un archivo o carpeta para descargar");
        }
    }

    // ------------------ CREAR CARPETA ------------------
    /** Crea una nueva carpeta en el directorio remoto seleccionado */
    @FXML
    public void onCreateDirAction() {
        TreeItem<String> selectedDir = remoteTree.getSelectionModel().getSelectedItem();
        if (selectedDir == null) {
            statusLabel.setText("Selecciona la carpeta donde crear la nueva carpeta");
            return;
        }
        String parentPath = getFullPath(selectedDir);

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Crear Carpeta");
        dialog.setHeaderText("Introduce el nombre de la nueva carpeta:");
        dialog.setContentText("Nombre:");

        dialog.showAndWait().ifPresent(folderName -> {
            if (!folderName.trim().isEmpty()) {
                Task<Void> createDirTask = new Task<>() {
                    @Override
                    protected Void call() throws Exception {
                        ftpService.createDirectory(parentPath + "/" + folderName);
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

    // ------------------ ELIMINAR ------------------
    /** Elimina un archivo o carpeta seleccionada */
    @FXML
    public void onDeleteAction() {
        FTPFileItem selectedFile = filesTable.getSelectionModel().getSelectedItem();
        TreeItem<String> selectedDir = remoteTree.getSelectionModel().getSelectedItem();

        if (selectedFile != null) {
            // Eliminar archivo
            String fullPath = getFullPath(selectedDir) + "/" + selectedFile.getName();
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    ftpService.deleteFile(fullPath);
                    Platform.runLater(() -> refreshFileList(getFullPath(selectedDir)));
                    return null;
                }
            };
            new Thread(task).start();
        } else if (selectedDir != null && selectedDir.getParent() != null) {
            // Eliminar carpeta recursivamente
            String dirPath = getFullPath(selectedDir);
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    deleteDirectoryRecursively(dirPath);
                    Platform.runLater(FtpController.this::loadRemoteDirectories);
                    return null;
                }
            };
            new Thread(task).start();
        } else {
            statusLabel.setText("Selecciona un archivo o carpeta para eliminar");
        }
    }

    /** Método auxiliar para eliminar carpetas recursivamente */
    private void deleteDirectoryRecursively(String path) throws IOException {
        FTPFile[] files = ftpService.getFtpClient().listFiles(path);
        if (files != null) {
            for (FTPFile file : files) {
                String fullPath = path + "/" + file.getName();
                if (file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..")) {
                    deleteDirectoryRecursively(fullPath);
                } else {
                    ftpService.deleteFile(fullPath);
                }
            }
        }
        ftpService.deleteDirectory(path);
    }

    // ------------------ MENÚS CONTEXTUALES ------------------
    /** Muestra menú contextual al hacer clic derecho sobre el árbol de directorios */
    @FXML
    public void onRemoteTreeClicked(ContextMenuEvent event) {
        TreeItem<String> selected = remoteTree.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (remoteTreeMenu == null) {
            remoteTreeMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Eliminar carpeta");
            deleteItem.setOnAction(e -> onDeleteAction());
            MenuItem createDirItem = new MenuItem("Crear carpeta");
            createDirItem.setOnAction(e -> onCreateDirAction());
            MenuItem uploadFileItem = new MenuItem("Subir archivo");
            uploadFileItem.setOnAction(e -> onUploadAction());
            remoteTreeMenu.getItems().addAll(deleteItem, createDirItem, uploadFileItem);
        }

        if (remoteTreeMenu.isShowing()) remoteTreeMenu.hide();
        remoteTreeMenu.show(remoteTree, event.getScreenX(), event.getScreenY());
        event.consume();
    }

    /** Muestra menú contextual al hacer clic derecho sobre la tabla de archivos */
    @FXML
    public void onFilesTableClicked(ContextMenuEvent event) {
        FTPFileItem selected = filesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (filesTableMenu == null) {
            filesTableMenu = new ContextMenu();
            MenuItem deleteItem = new MenuItem("Eliminar archivo");
            deleteItem.setOnAction(e -> onDeleteAction());
            MenuItem uploadFileItem = new MenuItem("Subir archivo");
            uploadFileItem.setOnAction(e -> onUploadAction());
            filesTableMenu.getItems().addAll(deleteItem, uploadFileItem);
        }

        if (filesTableMenu.isShowing()) filesTableMenu.hide();
        filesTableMenu.show(filesTable, event.getScreenX(), event.getScreenY());
        event.consume();
    }
}
