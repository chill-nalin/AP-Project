package edu.univ.erp.data;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    private static ConfigManager instance;
    private final Config config;

    private ConfigManager() {
        // 1. Load the application.conf file
        config = ConfigFactory.load("application.conf");
        log.info("Configuration file 'application.conf' loaded.");

        // 2. Validate that our required keys exist
        try {
            config.getString("db.user");
            config.getString("db.password");
            log.info("Database configuration found.");
        } catch (ConfigException.Missing e) {
            log.error("FATAL: Database configuration is missing from application.conf.", e);
            log.error("Please check 'db.user' and 'db.password' are set.");
            throw new RuntimeException("Missing DB config", e);
        }
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public Config getConfig() {
        return config;
    }

    public String getDbHost() {
        return config.getString("db.host");
    }

    public String getDbPort() {
        return config.getString("db.port");
    }

    public String getDbUser() {
        return config.getString("db.user");
    }

    public String getDbPassword() {
        return config.getString("db.password");
    }

    public String getMySqlBinDirectory() {
        // Return path
        String path = config.getString("mysql_paths.bin_directory");
        if (!path.endsWith(File.separator)) {
            path += File.separator;
        }
        return path;
    }
}