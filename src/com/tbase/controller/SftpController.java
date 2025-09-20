package com.tbase.controller;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.tbase.config.AppConfig;
import com.tbase.service.SFTPConnection;
import com.tbase.service.SFTPConnectionManager;
import com.tbase.service.SFTPDirectoryService;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller class to handle SFTP operations.
 * It uses the AppConfig to get connection details and delegates
 * the actual SFTP tasks to the service layer.
 */
public class SftpController {

    private static final Logger LOGGER = Logger.getLogger(SftpController.class.getName());

    private final SFTPConnectionManager connectionManager;
    private final SFTPDirectoryService directoryService;

    public SftpController() {
        this.connectionManager = new SFTPConnectionManager();
        this.directoryService = new SFTPDirectoryService();
    }

    /**
     * Handles an SFTP operation by getting a connection,
     * navigating to a directory, and then closing the connection.
     *
     * This method demonstrates the complete flow: Config -> Service -> Controller.
     */
    public void processSftpConnection() {
        String accountId = AppConfig.getProperty("sftp.account.id");
        String host = AppConfig.getProperty("sftp.host");
        int port = Integer.parseInt(AppConfig.getProperty("sftp.port"));
        String username = AppConfig.getProperty("sftp.username");
        String password = AppConfig.getProperty("sftp.password");
        String directoryPath = AppConfig.getProperty("sftp.directory.path");

        try {
            LOGGER.log(Level.INFO, "--- Starting SFTP process from Controller for account: {0} ---", accountId);

            SFTPConnection connection = connectionManager.getConnection(accountId, host, port, username, password);

            directoryService.changeOrCreateDirectory(connection, directoryPath);

            LOGGER.log(Level.INFO, "SFTP operation completed successfully for account: {0}", accountId);

        } catch (JSchException | SftpException e) {
            // Log the error with a detailed stack trace
            LOGGER.log(Level.SEVERE, "An error occurred during the SFTP process.", e);
        } finally {
            // 3. Ensure the connection is closed, regardless of success or failure.
            connectionManager.closeAllConnections();
            LOGGER.info("--- All connections closed ---");
        }
    }
}
