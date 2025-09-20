package com.tbase.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an active SFTP connection containing both the JSch session and channel.
 */
public class SFTPConnection {

    private static final Logger LOGGER = Logger.getLogger(SFTPConnection.class.getName());

    private final Session session;
    private final ChannelSftp channel;

    public SFTPConnection(Session session, ChannelSftp channel) {
        this.session = session;
        this.channel = channel;
    }

    /**
     * Checks if both the session and channel are connected.
     *
     * @return true if both are connected, false otherwise.
     */
    public boolean isConnected() {
        return session.isConnected() && channel.isConnected();
    }

    /**
     * Disconnects the SFTP channel and session.
     */
    public void disconnect() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
            LOGGER.log(Level.INFO, "SFTP channel disconnected.");
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
            LOGGER.log(Level.INFO, "JSch session disconnected.");
        }
        LOGGER.log(Level.INFO, "SFTP connection fully disconnected.");
    }

    /**
     * Gets the SFTP channel for file operations.
     *
     * @return The ChannelSftp instance.
     */
    public ChannelSftp getChannel() {
        return channel;
    }
}
