package com.tbase;

import com.tbase.config.AppConfig;

/**
 * Clase principal para probar la carga de configuraciones.
 */
public class TestApp {
    public static void main(String[] args) {
        System.out.println("Cargando la configuración de la aplicación...");

        // Usamos la clase AppConfig para obtener las propiedades.
        // Las propiedades ya han sido cargadas gracias al bloque estático.
        String appName = AppConfig.getProperty("app.name");
        String sftpHost = AppConfig.getProperty("sftp.host");
        String sftpPort = AppConfig.getProperty("sftp.port");
        String sftpUser = AppConfig.getProperty("sftp.user");
        String sftpRootDir = AppConfig.getProperty("sftp.root_dir");

        System.out.println("--- Propiedades de SFTP ---");
        System.out.println("Nombre de la aplicación: " + appName);
        System.out.println("SFTP Host: " + sftpHost);
        System.out.println("SFTP Puerto: " + sftpPort);
        System.out.println("SFTP Usuario: " + sftpUser);
        System.out.println("SFTP Directorio Raíz: " + sftpRootDir);
        System.out.println("-------------------------");
    }
    
    
    /*
    public static void main(String[] args) {
        SFTPConnectionManager manager = new SFTPConnectionManager();

        try {
            String account1 = "cuenta1";
            String host1 = "sftp.example.com";
            int port1 = 22;
            String user1 = "user1";
            String pass1 = "pass1";

            String account2 = "cuenta2";
            String host2 = "sftp.another-server.org";
            int port2 = 22;
            String user2 = "user2";
            String pass2 = "pass2";

            SFTPConnection conn1 = manager.getConnection(account1, host1, port1, user1, pass1);
            conn1.getChannel().cd("/directorio/remoto1");
            System.out.println("Conectado a la cuenta 1.");

            SFTPConnection conn2 = manager.getConnection(account2, host2, port2, user2, pass2);
            conn2.getChannel().cd("/directorio/remoto2");
            System.out.println("Conectado a la cuenta 2.");

            manager.closeConnection(account1);
            manager.closeAllConnections();

        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        }
    }
    */
    
    
    
    
}
