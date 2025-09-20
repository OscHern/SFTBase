package com.tbase.controller;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.tbase.config.AppConfig;
import com.tbase.service.SFTPConnection;
import com.tbase.service.SFTPConnectionManager;
import com.tbase.service.SFTPDirectoryService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Controller class to handle SFTP operations. It acts as the entry point
 * for business logic, coordinating calls to service classes.
 */
public class SftpController2 {

    private static final Logger LOGGER = Logger.getLogger(SftpController.class.getName());
    
    // Check if metrics are enabled from configuration
    private static final boolean METRICS_ENABLED = Boolean.parseBoolean(AppConfig.getProperty("app.metrics.enabled"));

    private final SFTPConnectionManager connectionManager;
    private final SFTPDirectoryService directoryService;

    public SftpController2() {
        this.connectionManager = new SFTPConnectionManager();
        this.directoryService = new SFTPDirectoryService();
    }

    /**
     * Orchestrates the SFTP connection and file transfer process.
     */
    public void processSftpConnection() {
        // Start main process timer
        long startTime = 0;
        if (METRICS_ENABLED) {
            startTime = System.nanoTime();
        }

        String env = getRequiredProperty("app.env", null, "The 'app.env' property must be defined.");
        
        String accountId = getRequiredProperty("sftp.account.id", env, "Missing SFTP account ID configuration.");
        String host = getRequiredProperty("sftp.host", env, "Missing SFTP host configuration.");
        int port = Integer.parseInt(getRequiredProperty("sftp.port", env, "Missing SFTP port configuration."));
        String username = getRequiredProperty("sftp.username", env, "Missing SFTP username configuration.");
        String password = getRequiredProperty("sftp.password", env, "Missing SFTP password configuration.");
        String baseRemotePath = getRequiredProperty("sftp.base.path", env, "Missing SFTP base path configuration.");
        String localDestinationPath = getRequiredProperty("local.destination.path", env, "Missing local destination path configuration.");

        // Define the subdirectories to be processed
        List<String> subdirectories = List.of("HR", "ADM", "Des");
        
        // Define the files to be downloaded for HR and Des with dynamic dates
        List<String> mainFiles = List.of(
            getDatedFileName("reporte_1"), 
            getDatedFileName("reporte_2"), 
            getDatedFileName("reporte_3"));
        
        List<String> admFiles = List.of(
            getDatedFileName("reembolso1"), 
            getDatedFileName("reembolso2"));

        SFTPConnection connection = null;

        try {
            LOGGER.log(Level.INFO, "Starting SFTP process for account: {0}", accountId);
            
            // Step 1: Connect to SFTP
            connection = connectionManager.getConnection(accountId, host, port, username, password);

            for (String subDir : subdirectories) {
                String remotePath = baseRemotePath + "/" + subDir;
                String currentLocalPath = localDestinationPath + "/" + subDir;
                LOGGER.log(Level.INFO, "Processing directory: {0}", remotePath);

                // Change to the specified directory, creating it if it doesn't exist.
                directoryService.changeOrCreateDirectory(connection, remotePath);

                if ("ADM".equals(subDir)) {
                    processAdmDirectoryLogic(connection, currentLocalPath, mainFiles, admFiles);
                } else {
                    processDirectoryLogic(connection, currentLocalPath, mainFiles);
                }
            }
            
            LOGGER.log(Level.INFO, "SFTP process completed successfully.");

        } catch (JSchException e) {
            LOGGER.log(Level.SEVERE, "SFTP connection error: {0}", e.getMessage());
        } catch (SftpException e) {
            LOGGER.log(Level.SEVERE, "SFTP file operation error: {0}", e.getMessage());
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "Configuration error: {0}", e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Service initialization error: {0}", e.getMessage());
        } finally {
            if (connection != null) {
                connectionManager.closeConnection(accountId);
            }
        }
        if (METRICS_ENABLED) {
            long endTime = System.nanoTime();
            LOGGER.log(Level.INFO, "Total service execution time: {0} ms", (endTime - startTime) / 1_000_000.0);
        }
    }
    
    /**
     * Processes a standard directory by downloading and analyzing a list of files.
     * @param connection The active SFTP connection.
     * @param currentLocalPath The local path for files.
     * @param filesToProcess The list of files to download and analyze.
     */
    private void processDirectoryLogic(SFTPConnection connection, String currentLocalPath, List<String> filesToProcess) {
        // Define critical files
        List<String> criticalFiles = List.of(
            getDatedFileName("reporte_1"), 
            getDatedFileName("reporte_2"));

        // Validate and download critical files first.
        boolean criticalFilesDownloaded = downloadAndValidateFiles(connection, currentLocalPath, criticalFiles);
        if (!criticalFilesDownloaded) {
            throw new RuntimeException("Critical files " + criticalFiles + " for directory not downloaded. Stopping service.");
        }
        
        // Continue with the rest of the files if critical ones were downloaded.
        boolean allFilesDownloaded = downloadAndValidateFiles(connection, currentLocalPath, filesToProcess);
        if (!allFilesDownloaded) {
            throw new RuntimeException("One or more main files could not be downloaded. Stopping service.");
        }

        analyzeLocalFiles(currentLocalPath, filesToProcess);
    }
    
    /**
     * Processes the ADM subdirectory, handling the conditional file downloads.
     * @param connection The active SFTP connection.
     * @param currentLocalPath The local path for ADM files.
     * @param mainFiles The list of main files.
     * @param admFiles The list of ADM specific files.
     */
    private void processAdmDirectoryLogic(SFTPConnection connection, String currentLocalPath, List<String> mainFiles, List<String> admFiles) {
        LOGGER.log(Level.INFO, "Processing special logic for ADM directory.");
        
        // Process main files for ADM first. This also validates the critical files.
        processDirectoryLogic(connection, currentLocalPath, mainFiles);
        
        String mainReportPath = currentLocalPath + File.separator + mainFiles.get(0);

        try {
            // Read the first line of the main report to check the condition
            String firstLine = Files.lines(Paths.get(mainReportPath)).findFirst().orElse("");
            String column1Value = firstLine.split(",")[0]; // Assuming comma-separated values

            if ("true".equalsIgnoreCase(column1Value)) {
                LOGGER.log(Level.INFO, "Condition met in ADM report. Starting download of refund files.");
                
                // Process refund files
                downloadAndValidateFiles(connection, currentLocalPath, admFiles);
                analyzeLocalFiles(currentLocalPath, admFiles);
                
            } else {
                LOGGER.log(Level.INFO, "Condition not met in ADM report. Skipping download of refund files.");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading file during ADM processing: {0}", e.getMessage());
            throw new RuntimeException("Error processing ADM report", e);
        }
    }

    /**
     * Downloads and validates a list of files.
     * @param connection The active SFTP connection.
     * @param localPath The local path to save files.
     * @param filesToDownload The list of file names to download.
     * @return true if all files were downloaded successfully, false otherwise.
     */
    private boolean downloadAndValidateFiles(SFTPConnection connection, String localPath, List<String> filesToDownload) {
        boolean allDownloaded = true;
        for (String fileName : filesToDownload) {
            long downloadStart = 0;
            if (METRICS_ENABLED) {
                downloadStart = System.nanoTime();
            }
            boolean downloaded = directoryService.copyFileFromSftpToLocal(connection, fileName, localPath);
            if (METRICS_ENABLED) {
                long downloadEnd = System.nanoTime();
                LOGGER.log(Level.INFO, "Download time for {0}: {1} ms", new Object[]{fileName, (downloadEnd - downloadStart) / 1_000_000.0});
            }
            if (!downloaded) {
                allDownloaded = false;
            }
        }
        return allDownloaded;
    }
    
    /**
     * Analyzes a list of files by reading each one line by line.
     * @param localPath The local path where files are located.
     * @param filesToAnalyze The list of file names to analyze.
     */
    private void analyzeLocalFiles(String localPath, List<String> filesToAnalyze) {
        for (String fileName : filesToAnalyze) {
            long analysisStart = 0;
            if (METRICS_ENABLED) {
                analysisStart = System.nanoTime();
            }
            analyzeFile(localPath + File.separator + fileName);
            if (METRICS_ENABLED) {
                long analysisEnd = System.nanoTime();
                LOGGER.log(Level.INFO, "Analysis time for file {0}: {1} ms", new Object[]{fileName, (analysisEnd - analysisStart) / 1_000_000.0});
            }
        }
    }

    /**
     * Helper method to get a required property from AppConfig and validate it.
     *
     * @param key The property key.
     * @param env The environment prefix (e.g., "development" or "production"). Null for global properties.
     * @return The property value.
     * @throws IllegalStateException if the property is null or empty.
     */
    private String getRequiredProperty(String key, String env, String errorMessage) {
        String fullKey = (env != null) ? env + "." + key : key;
        String value = AppConfig.getProperty(fullKey);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(errorMessage);
        }
        return value;
    }
    
    /**
     * Generates a file name with the current date in YYYY-MM-DD format.
     * @param baseName The base name of the file without extension.
     * @return The complete file name with date and extension.
     */
    private String getDatedFileName(String baseName) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("YYYY-MM-dd"));
        return baseName + "_" + date + ".txt";
    }

    /**
     * Analyzes a file by reading it line by line.
     * @param filePath The full path to the file.
     */
    private void analyzeFile(String filePath) {
        LOGGER.log(Level.INFO, "Analyzing file: {0}", filePath);
        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            lines.forEach(line -> {
                // Here you would add your file analysis logic.
                LOGGER.log(Level.INFO, "Reading line: {0}", line);
            });
            LOGGER.log(Level.INFO, "File analysis complete for: {0}", filePath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error reading file during analysis: {0}", e.getMessage());
        }
    }
}
