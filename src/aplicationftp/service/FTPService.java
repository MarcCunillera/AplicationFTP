/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package aplicationftp.service;

import java.io.File;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTP;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FTPService {

    private final FTPSClient client;

    public FTPService(FTPSClient client) {
        this.client = client;
    }

    // LLISTAR DIRECTORIS
    public void listDirectory(String path) throws IOException {
        FTPFile[] files = client.listFiles(path);
        for (FTPFile file : files) {
            System.out.println((file.isDirectory() ? "[DIR] " : "[FILE] ") + file.getName());
        }
    }

    // CREAR DIRECTORI (SEGUR)
    public boolean createDirectory(String path) throws IOException {
        return client.makeDirectory(path);
    }

    // PUJAR FITXER amb detecció automàtica del nom
    public boolean uploadFile(String localFilePath, String remoteDirPath) throws IOException {
        client.enterLocalPassiveMode();
        client.setFileType(FTP.BINARY_FILE_TYPE);

        File localFile = new File(localFilePath);
        String remoteFilePath = remoteDirPath.endsWith("/")
                ? remoteDirPath + localFile.getName()
                : remoteDirPath + "/" + localFile.getName();

        try (InputStream input = new FileInputStream(localFile)) {
            return client.storeFile(remoteFilePath, input);
        }
    }

    // PUJAR CARPETA MANTENINT ESTRUCTURA
    public void uploadDirectory(String localDir, String remoteParentDir) throws IOException {
        File localFolder = new File(localDir);
        String remoteFolder = remoteParentDir + "/" + localFolder.getName();

        createDirectory(remoteFolder);

        for (File file : localFolder.listFiles()) {

            if (file.isDirectory()) {
                uploadDirectory(file.getAbsolutePath(), remoteFolder);
            } else {
                uploadFile(file.getAbsolutePath(), remoteFolder);
                System.out.println("Fitxer pujat: " + remoteFolder + "/" + file.getName());
            }
        }
    }

    // DESCARREGAR FITXER (accepta directori o camí complet de fitxer)
    public boolean downloadFile(String remoteFilePath, String localTarget) throws IOException {
        // Assegura mode passiu i tipus
        client.enterLocalPassiveMode();
        client.setFileType(FTP.BINARY_FILE_TYPE);

        File target = new File(localTarget);
        String localFilePath;

        if (target.exists() && target.isDirectory()) {
            String fileName = remoteFilePath.substring(remoteFilePath.lastIndexOf('/') + 1);
            localFilePath = new File(target, fileName).getAbsolutePath();
        } else if (localTarget.endsWith(File.separator) || localTarget.endsWith("/")) {
            File dir = new File(localTarget);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileName = remoteFilePath.substring(remoteFilePath.lastIndexOf('/') + 1);
            localFilePath = new File(dir, fileName).getAbsolutePath();
        } else {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            localFilePath = target.getAbsolutePath();
        }

        try (FileOutputStream fos = new FileOutputStream(localFilePath)) {
            return client.retrieveFile(remoteFilePath, fos);
        }
    }

    // DESCARREGAR CARPETA MANTENINT ESTRUCTURA (millorat)
    public void downloadDirectory(String remoteDir, String localDir) throws IOException {

        client.enterLocalPassiveMode();
        client.setFileType(FTP.BINARY_FILE_TYPE);

        String folderName = remoteDir.substring(remoteDir.lastIndexOf('/') + 1);
        File localFolder = new File(localDir, folderName);
        if (!localFolder.exists()) {
            localFolder.mkdirs();
        }

        FTPFile[] files = client.listFiles(remoteDir);
        if (files == null) {
            return;
        }

        for (FTPFile file : files) {

            if (file.getName().equals(".") || file.getName().equals("..")) {
                continue;
            }

            String remoteFilePath = remoteDir + "/" + file.getName();
            String localFilePath = localFolder.getAbsolutePath() + File.separator + file.getName();

            if (file.isDirectory()) {
                downloadDirectory(remoteFilePath, localFolder.getAbsolutePath());
            } else {
                File parent = new File(localFilePath).getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                downloadFile(remoteFilePath, localFilePath);
                System.out.println("Fitxer descarregat: " + localFilePath);
            }
        }
    }

    // ESBORRAR FITXER
    public boolean deleteFile(String remoteFilePath) throws IOException {
        return client.deleteFile(remoteFilePath);
    }

    // ESBORRAR CARPETA RECUSIVA CORRECTE
    public void deleteDirectoryRecursive(String remoteDir) throws IOException {
        FTPFile[] files = client.listFiles(remoteDir);

        for (FTPFile file : files) {
            String fullPath = remoteDir + "/" + file.getName();

            if (file.isDirectory()) {
                deleteDirectoryRecursive(fullPath);
            } else {
                client.deleteFile(fullPath);
            }
        }
        client.removeDirectory(remoteDir);
    }
}
