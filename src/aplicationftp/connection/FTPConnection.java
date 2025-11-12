/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package aplicationftp.connection;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;

/**
 *
 * @author marccunillera
 */
public class FTPConnection {

    private final FTPSClient client;

    public FTPConnection() {
        this.client = new FTPSClient("TLS", false);

    }

    public FTPSClient connect(String host, int port, String user, String pass) throws Exception {
        client.connect(host, port);
        System.out.println("Connectat al host");

        client.execAUTH("TLS");
        client.login(user, pass);

        client.execPBSZ(0);
        client.execPROT("P");

        client.enterLocalPassiveMode();
        client.setFileType(FTP.BINARY_FILE_TYPE);
        System.out.println("Connexió segura FTPS establerta");

        return client;
    }

    public void disconnect() throws Exception {
        if (client.isConnected()) {
            client.logout();
            client.disconnect();
            System.out.println("Connexió FTPS tancada");
        }
    }
}
