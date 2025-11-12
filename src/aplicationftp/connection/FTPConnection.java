package aplicationftp.connection;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;

public class FTPConnection {

    private final FTPSClient client;

    public FTPConnection() {
        this.client = new FTPSClient("TLS", false);
    }

    public boolean connect(String host, int port, String user, String pass) {
        try {
            client.connect(host, port);
            client.execAUTH("TLS");
            client.login(user, pass);
            client.execPBSZ(0);
            client.execPROT("P");
            client.enterLocalPassiveMode();
            client.setFileType(FTP.BINARY_FILE_TYPE);
            System.out.println("Connexió FTPS establerta amb èxit");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public FTPSClient getFTPClient() {
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
