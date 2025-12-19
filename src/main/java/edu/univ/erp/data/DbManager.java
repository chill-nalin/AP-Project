package edu.univ.erp.data;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class DbManager {

    private static final Logger log = LoggerFactory.getLogger(DbManager.class);

    private static DbManager instance;
    private final HikariDataSource authDataSource;
    private final HikariDataSource erpDataSource;

    private DbManager() {

        // 1. Get the loaded configuration
        ConfigManager configManager = ConfigManager.getInstance();
        String dbHost = configManager.getDbHost();
        String dbPort = configManager.getDbPort();
        String dbUser = configManager.getDbUser();
        String dbPassword = configManager.getDbPassword();

        String jdbcUrlBase = "jdbc:mysql://" + dbHost + ":" + dbPort + "/";

        log.info("Configuring Auth database connection pool (auth_db)...");
        HikariConfig authConfig = new HikariConfig();
        authConfig.setJdbcUrl(jdbcUrlBase + "auth_db");
        authConfig.setUsername(dbUser);
        authConfig.setPassword(dbPassword);
        authConfig.setPoolName("AuthPool");
        authConfig.setMaximumPoolSize(10);
        authConfig.setMinimumIdle(2);
        authConfig.addDataSourceProperty("cachePrepStmts", "true");

        this.authDataSource = new HikariDataSource(authConfig);
        log.info("AuthPool successfully initialized.");

        log.info("Configuring ERP database connection pool (erp_db)...");
        HikariConfig erpConfig = new HikariConfig();
        erpConfig.setJdbcUrl(jdbcUrlBase + "erp_db");
        erpConfig.setUsername(dbUser);
        erpConfig.setPassword(dbPassword);
        erpConfig.setPoolName("ErpPool");
        erpConfig.setMaximumPoolSize(20);
        erpConfig.setMinimumIdle(5);
        erpConfig.addDataSourceProperty("cachePrepStmts", "true");

        this.erpDataSource = new HikariDataSource(erpConfig);
        log.info("ErpPool successfully initialized.");
    }
    public static synchronized DbManager getInstance() {
        if (instance == null) {
            instance = new DbManager();
        }
        return instance;
    }

    public Connection getAuthConnection() throws SQLException {
        return authDataSource.getConnection();
    }

    public Connection getErpConnection() throws SQLException {
        return erpDataSource.getConnection();
    }

    public void close() {
        log.info("Closing database connection pools...");
        if (authDataSource != null) {
            authDataSource.close();
            log.info("AuthPool closed.");
        }
        if (erpDataSource != null) {
            erpDataSource.close();
            log.info("ErpPool closed.");
        }
    }
}