/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package aplicationftp.service;

import aplicationftp.connection.FTPConnection;
import aplicationftp.model.FTPFileItem;
import java.io.File;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;

public class FTPService {

    private final FTPConnection connection;
    private FTPSClient ftpClient;
    private final String host, user, pass;
    private final int port = 21;

    public FTPService(String host, String user, String pass) {
        this.host = host;
        this.user = user;
        this.pass = pass;
        this.connection = new FTPConnection();
    }

    /**
     * Conecta al servidor usando FTPConnection
     */
    public boolean connect() {
        boolean ok = connection.connect(host, port, user, pass);
        if (ok) {
            ftpClient = connection.getFTPClient();
        }
        return ok;
    }

    /**
     * Asegura que la conexión esté viva antes de cualquier operación
     */
    public boolean ensureConnected() {
        try {
            if (ftpClient == null || !ftpClient.isConnected() || !ftpClient.sendNoOp()) {
                System.out.println("Reconectando FTPS…");
                if (!connection.connect(host, port, user, pass)) {
                    System.err.println("No se pudo reconectar al servidor FTP");
                    return false;
                }
                ftpClient.enterLocalPassiveMode();    // PASV
                ftpClient.setUseEPSVwithIPv4(true);   // EPSV sobre IPv4
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized List<FTPFileItem> listFiles(String path) {
        try {
            if (!ensureConnected()) {
                return List.of();
            }

            FTPFile[] files = ftpClient.listFiles(path);
            if (files == null) {
                return List.of();
            }

            return Arrays.stream(files)
                    .filter(FTPFile::isFile)
                    .map(f -> new FTPFileItem(
                    f.getName(),
                    String.valueOf(f.getSize()),
                    "File",
                    f.getTimestamp() != null ? f.getTimestamp().getTime().toString() : ""
            ))
                    .collect(Collectors.toList());

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("No es poden llistar els fitxers de: " + path + " -> " + e.getMessage());
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

    public boolean deleteDirectory(String dirPath) throws IOException {
        return ftpClient.removeDirectory(dirPath);
    }

    public void downloadDirectoryRecursively(String remotePath, String localPath) throws IOException {
        FTPFile[] files = ftpClient.listFiles(remotePath);
        if (files != null) {
            File localDir = new File(localPath);
            if (!localDir.exists()) {
                localDir.mkdirs();
            }

            for (FTPFile file : files) {
                String remoteFilePath = remotePath + "/" + file.getName();
                String localFilePath = localPath + "/" + file.getName();
                if (file.isDirectory() && !file.getName().equals(".") && !file.getName().equals("..")) {
                    downloadDirectoryRecursively(remoteFilePath, localFilePath);
                } else if (file.isFile()) {
                    downloadFile(remoteFilePath, localFilePath);
                }
            }
        }
    }

    public void disconnect() throws Exception {
        connection.disconnect();
    }

    public FTPClient getFtpClient() {
        return ftpClient;
    }

}
