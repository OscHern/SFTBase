package com.tbase.service;

import com.jcraft.jsch.*;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages multiple SFTP connections in a thread-safe manner.
 * Connections are stored and retrieved based on a unique account ID.
 */
public class SFTPConnectionManager {

    private static final Logger LOGGER = Logger.getLogger(SFTPConnectionManager.class.getName());

    private final ConcurrentHashMap<String, SFTPConnection> activeConnections = new ConcurrentHashMap<>();
    private final Properties config;

    public SFTPConnectionManager() {
        this.config = new Properties();
        config.put("StrictHostKeyChecking", "no");
    }

    /**
     * Gets an SFTP connection for a specific account.
     * If an active and valid connection already exists, it is reused.
     * Otherwise, a new one is created.
     *
     * @param accountId The unique identifier for the account.
     * @param host      The SFTP host.
     * @param port      The SFTP port.
     * @param username  The username for authentication.
     * @param password  The password for authentication.
     * @return An active SFTPConnection instance.
     * @throws JSchException If there is an error during the connection process.
     */
    public SFTPConnection getConnection(String accountId, String host, int port, String username, String password) throws JSchException {
        // Lock at the account level to allow concurrent connections to different accounts.
        synchronized (accountId.intern()) { 
            SFTPConnection connection = activeConnections.get(accountId);

            // Reuse the connection if it is active.
            if (connection != null && connection.isConnected()) {
                LOGGER.log(Level.INFO, "Reusing connection for account: {0}", accountId);
                return connection;
            }

            LOGGER.log(Level.INFO, "Creating new connection for account: {0}", accountId);
            Session session = null;
            ChannelSftp channel = null;

            try {
                JSch jsch = new JSch();
                session = jsch.getSession(username, host, port);
                session.setPassword(password);
                session.setConfig(config);
                session.connect();

                channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();

                SFTPConnection newConnection = new SFTPConnection(session, channel);
                activeConnections.put(accountId, newConnection);
                return newConnection;

            } catch (JSchException e) {
                // Ensure resources are closed on failure
                if (channel != null) {
                    channel.disconnect();
                }
                if (session != null) {
                    session.disconnect();
                }
                LOGGER.log(Level.SEVERE, "Error creating connection for account: {0}", accountId);
                throw e;
            }
        }
    }

    /**
     * Closes a specific connection.
     *
     * @param accountId The unique identifier for the account to close.
     */
    public void closeConnection(String accountId) {
        SFTPConnection connection = activeConnections.remove(accountId);
        if (connection != null) {
            connection.disconnect();
            LOGGER.log(Level.INFO, "Connection for account {0} closed.", accountId);
        }
    }

    /**
     * Closes all active connections.
     */
    public void closeAllConnections() {
        activeConnections.forEach((accountId, connection) -> {
            connection.disconnect();
            LOGGER.log(Level.INFO, "Connection for account {0} closed.", accountId);
        });
        activeConnections.clear();
    }
}
