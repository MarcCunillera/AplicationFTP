/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package aplicationftp.service;

import aplicationftp.connection.FTPConnection;
import aplicationftp.model.FTPFileItem;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FTPService {

    private final FTPConnection connection;
    private FTPClient ftpClient;
    private final String host, user, pass;
    private final int port = 21;

    public FTPService(String host, String user, String pass) {
        this.host = host;
        this.user = user;
        this.pass = pass;
        this.connection = new FTPConnection();
    }

    public boolean connect() {
        boolean ok = connection.connect(host, port, user, pass);
        if (ok) {
            ftpClient = connection.getFTPClient();
        }
        return ok;
    }

    public List<FTPFileItem> listFiles(String path) {
        try {
            FTPFile[] files = ftpClient.listFiles(path);
            return Arrays.stream(files)
                    .filter(f -> f.isFile()) // <-- només fitxers
                    .map(f -> new FTPFileItem(
                    f.getName(),
                    String.valueOf(f.getSize()),
                    "File", // ja sabem que és un fitxer
                    f.getTimestamp() != null ? f.getTimestamp().getTime().toString() : ""
            ))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public void uploadFile(String localPath, String remoteName) throws IOException {
        try (FileInputStream fis = new FileInputStream(localPath)) {
            ftpClient.storeFile(remoteName, fis);
        }
    }

    public void downloadFile(String remoteName, String localPath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(localPath)) {
            ftpClient.retrieveFile(remoteName, fos);
        }
    }

    public void createDirectory(String folderName) throws IOException {
        ftpClient.makeDirectory(folderName);
    }

    public void deleteFile(String remoteName) throws IOException {
        ftpClient.deleteFile(remoteName);
    }

    public boolean deleteDirectory(String remotePath) {
        try {
            // Intentar eliminar todos los archivos y subdirectorios primero
            FTPFile[] files = ftpClient.listFiles(remotePath);
            if (files != null) {
                for (FTPFile file : files) {
                    String childPath = remotePath + "/" + file.getName();
                    if (file.isDirectory()) {
                        deleteDirectory(childPath); // recursivo
                    } else {
                        ftpClient.deleteFile(childPath);
                    }
                }
            }
            // Finalmente eliminar la carpeta vacía
            return ftpClient.removeDirectory(remotePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() throws Exception {
        connection.disconnect();
    }

    public FTPClient getFtpClient() {
        return ftpClient;
    }

}
