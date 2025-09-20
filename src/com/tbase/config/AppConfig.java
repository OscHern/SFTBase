package com.tbase.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to manage application configuration.
 * It loads the base properties and then the environment-specific properties.
 * The loading process is performed statically only once when the class is initialized.
 */
public final class AppConfig {

    private static final Logger LOGGER = Logger.getLogger(AppConfig.class.getName());
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    private AppConfig() {}

    private static void loadProperties() {
        try {
            try (InputStream input = AppConfig.class.getClassLoader().getResourceAsStream("app.properties")) {
                if (input == null) {
                    LOGGER.log(Level.SEVERE, "Could not find the main properties file: app.properties");
                    throw new RuntimeException("Could not find the main properties file: app.properties");
                }
                properties.load(input);
            }

            String env = properties.getProperty("app.env");
            if (env == null || env.trim().isEmpty()) {
                LOGGER.log(Level.SEVERE, "The 'app.env' property is not defined in app.properties. Please specify a value like 'development' or 'production'.");
                throw new RuntimeException("The 'app.env' property must be defined in app.properties");
            }
            String envFile = "app." + env + ".properties";

            try (InputStream envInput = AppConfig.class.getClassLoader().getResourceAsStream(envFile)) {
                if (envInput != null) {
                    properties.load(envInput);
                    LOGGER.log(Level.INFO, "Properties loaded successfully for the environment: " + env);
                } else {
                    LOGGER.log(Level.WARNING, "Could not find the properties file for the environment: " + envFile);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while loading application properties", e);
            throw new RuntimeException("Error while loading application properties", e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}