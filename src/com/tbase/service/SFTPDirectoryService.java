package com.tbase.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SFTPDirectoryService {

    private static final Logger LOGGER = Logger.getLogger(SFTPDirectoryService.class.getName());

    /**
     * Changes the current directory to a specified path. If the directory
     * does not exist, it creates it and then changes to it.
     *
     * @param connection    The active SFTP connection.
     * @param directoryPath The path to the directory to change to or create.
     * @throws SftpException If there is an issue with the SFTP operation
     * (e.g., permissions, invalid path).
     */
    public void changeOrCreateDirectory(SFTPConnection connection, String directoryPath) throws SftpException {
        ChannelSftp channel = connection.getChannel();
        try {
            // First, try to change the directory directly.
            channel.cd(directoryPath);
            LOGGER.log(Level.INFO, "Changed current directory to: {0}", directoryPath);
        } catch (SftpException e) {
            // If the directory does not exist, the error code will be ChannelSftp.SSH_FX_NO_SUCH_FILE.
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                LOGGER.log(Level.INFO, "Directory does not exist. Creating folder: {0}", directoryPath);
                // Create the directory.
                channel.mkdir(directoryPath);
                // Then, change to the newly created directory.
                channel.cd(directoryPath);
                LOGGER.log(Level.INFO, "Successfully created and changed to directory: {0}", directoryPath);
            } else {
                // If the exception is for another reason, rethrow it.
                LOGGER.log(Level.SEVERE, "Failed to change or create directory: {0}", e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Copies a file from the SFTP server to a local path.
     *
     * @param connection The active SFTP connection.
     * @param remoteFileName The name of the file to download.
     * @param localPath The local directory where the file will be saved.
     * @return {@code true} if the file was downloaded successfully, {@code false} otherwise.
     */
    public boolean copyFileFromSftpToLocal(SFTPConnection connection, String remoteFileName, String localPath) {
        ChannelSftp channel = connection.getChannel();
        try {
            // Check if the local directory exists, create it if not
            File localDirectory = new File(localPath);
            if (!localDirectory.exists()) {
                if (localDirectory.mkdirs()) {
                    LOGGER.log(Level.INFO, "Created local directory: {0}", localPath);
                } else {
                    LOGGER.log(Level.WARNING, "Failed to create local directory: {0}", localPath);
                    return false;
                }
            }

            String fullLocalPath = localPath + File.separator + remoteFileName;
            LOGGER.log(Level.INFO, "Attempting to download file: {0} to {1}", new Object[]{remoteFileName, fullLocalPath});

            // Use the get method to download the file
            channel.get(remoteFileName, fullLocalPath);

            LOGGER.log(Level.INFO, "File downloaded successfully: {0}", fullLocalPath);
            return true;
        } catch (SftpException e) {
            LOGGER.log(Level.SEVERE, "SFTP error during file download: {0}", e.getMessage());
            return false;
        }
    }

    /**
     * Uploads a file from a local path to the SFTP server.
     *
     * @param connection The active SFTP connection.
     * @param localFilePath The local path of the file to upload.
     * @param remoteFileName The desired name of the file on the SFTP server.
     * @return {@code true} if the file was uploaded successfully, {@code false} otherwise.
     * @throws RuntimeException if the local file does not exist.
     */
    public boolean uploadFileToSftp(SFTPConnection connection, String localFilePath, String remoteFileName) {
        // Step 1: Check if the local file exists.
        File localFile = new File(localFilePath);
        if (!localFile.exists()) {
            LOGGER.log(Level.SEVERE, "Local file not found. Service cannot proceed: {0}", localFilePath);
            throw new RuntimeException("Local file not found, service cannot proceed.");
        }

        // Step 2: Proceed with the upload if the file exists.
        ChannelSftp channel = connection.getChannel();
        try {
            // Use the put method to upload the file
            channel.put(localFilePath, remoteFileName);

            LOGGER.log(Level.INFO, "File uploaded successfully: {0}", localFilePath);
            return true;
        } catch (SftpException e) {
            LOGGER.log(Level.SEVERE, "SFTP error during file upload: {0}", e.getMessage());
            return false;
        }
    }
}
