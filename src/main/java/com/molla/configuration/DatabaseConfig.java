package com.molla.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Value("${MYSQLDATABASE_URL:}")
    private String mysqlDatabaseUrl;

    @Value("${MYSQLHOST:}")
    private String mysqlHost;

    @Value("${MYSQLPORT:3306}")
    private String mysqlPort;

    @Value("${MYSQLDATABASE:}")
    private String mysqlDatabase;

    @Value("${MYSQLUSER:}")
    private String mysqlUser;

    @Value("${MYSQLPASSWORD:}")
    private String mysqlPassword;

    @Bean
    @Primary
    public DataSourceProperties dataSourceProperties() {
        // Create fresh properties object - don't bind from application.properties
        // to avoid picking up the bad mysql.railway.internal URL
        DataSourceProperties properties = new DataSourceProperties();
        
        logger.info("=== Database Configuration Debug ===");
        logger.info("MYSQLDATABASE_URL: {}", mysqlDatabaseUrl != null && !mysqlDatabaseUrl.isEmpty() ? "SET" : "NOT SET");
        logger.info("MYSQLHOST: {}", mysqlHost != null && !mysqlHost.isEmpty() ? mysqlHost : "NOT SET");
        logger.info("MYSQLDATABASE: {}", mysqlDatabase != null && !mysqlDatabase.isEmpty() ? mysqlDatabase : "NOT SET");
        logger.info("MYSQLUSER: {}", mysqlUser != null && !mysqlUser.isEmpty() ? "SET" : "NOT SET");
        
        // Priority 1: If Railway's MYSQLDATABASE_URL is provided, parse it
        if (mysqlDatabaseUrl != null && !mysqlDatabaseUrl.isEmpty()) {
            try {
                logger.info("Parsing MYSQLDATABASE_URL...");
                // Railway format: mysql://user:password@host:port/database
                URI dbUri = new URI(mysqlDatabaseUrl.replace("mysql://", "http://"));
                String username = dbUri.getUserInfo().split(":")[0];
                String password = dbUri.getUserInfo().split(":")[1];
                String host = dbUri.getHost();
                int port = dbUri.getPort() == -1 ? 3306 : dbUri.getPort();
                String database = dbUri.getPath().replaceFirst("/", "");
                
                String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database;
                properties.setUrl(jdbcUrl);
                properties.setUsername(username);
                properties.setPassword(password);
                logger.info("Using MYSQLDATABASE_URL - Host: {}, Database: {}", host, database);
                return properties;
            } catch (Exception e) {
                logger.warn("Failed to parse MYSQLDATABASE_URL: {}", e.getMessage());
                // If parsing fails, continue to check individual variables
            }
        }
        
        // Priority 2: Use Railway's individual MySQL variables
        if (mysqlHost != null && !mysqlHost.isEmpty() && 
            mysqlDatabase != null && !mysqlDatabase.isEmpty()) {
            String jdbcUrl = "jdbc:mysql://" + mysqlHost + ":" + mysqlPort + "/" + mysqlDatabase;
            properties.setUrl(jdbcUrl);
            if (mysqlUser != null && !mysqlUser.isEmpty()) {
                properties.setUsername(mysqlUser);
            }
            if (mysqlPassword != null && !mysqlPassword.isEmpty()) {
                properties.setPassword(mysqlPassword);
            }
            logger.info("Using Railway MySQL variables - Host: {}, Database: {}", mysqlHost, mysqlDatabase);
            return properties;
        }
        
        // Priority 3: Check for SPRING_DATASOURCE_URL from environment
        // BUT: Skip if it contains mysql.railway.internal (internal hostname that doesn't work)
        String springUrl = System.getenv("SPRING_DATASOURCE_URL");
        String springUser = System.getenv("SPRING_DATASOURCE_USERNAME");
        String springPass = System.getenv("SPRING_DATASOURCE_PASSWORD");
        
        if (springUrl != null && !springUrl.isEmpty() && !springUrl.contains("mysql.railway.internal")) {
            properties.setUrl(springUrl);
            if (springUser != null && !springUser.isEmpty()) {
                properties.setUsername(springUser);
            }
            if (springPass != null && !springPass.isEmpty()) {
                properties.setPassword(springPass);
            }
            logger.info("Using SPRING_DATASOURCE_URL from environment");
            return properties;
        } else if (springUrl != null && springUrl.contains("mysql.railway.internal")) {
            logger.warn("SPRING_DATASOURCE_URL contains mysql.railway.internal (internal hostname). " +
                      "Ignoring and trying Railway MySQL variables instead.");
        }
        
        // Priority 4: Fall back to application.properties values
        String appPropsUrl = System.getProperty("spring.datasource.url");
        if (appPropsUrl != null && !appPropsUrl.isEmpty()) {
            properties.setUrl(appPropsUrl);
            String appPropsUser = System.getProperty("spring.datasource.username");
            String appPropsPass = System.getProperty("spring.datasource.password");
            if (appPropsUser != null && !appPropsUser.isEmpty()) {
                properties.setUsername(appPropsUser);
            }
            if (appPropsPass != null && !appPropsPass.isEmpty()) {
                properties.setPassword(appPropsPass);
            }
            logger.info("Using values from application.properties");
            return properties;
        }
        
        // If we get here, no database configuration was found
        logger.error("================================================");
        logger.error("DATABASE CONFIGURATION ERROR:");
        logger.error("No database connection details found!");
        logger.error("Please ensure one of the following is set:");
        logger.error("1. Railway MySQL service is linked to your app service");
        logger.error("2. Or set SPRING_DATASOURCE_URL environment variable (NOT mysql.railway.internal)");
        logger.error("3. Or set MYSQLHOST, MYSQLDATABASE, MYSQLUSER, MYSQLPASSWORD");
        logger.error("================================================");
        
        // Don't return properties with null/empty URL - this will cause Spring Boot to fail
        // which is better than trying to connect to a non-existent host
        throw new IllegalStateException(
            "Database configuration not found. Please link Railway MySQL service or set valid database connection variables. " +
            "Current SPRING_DATASOURCE_URL contains 'mysql.railway.internal' which is not accessible. " +
            "Please remove SPRING_DATASOURCE_URL from Railway variables or update it to use the public MySQL hostname."
        );
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        logger.info("Creating DataSource with URL: {}", 
                   properties.getUrl() != null ? properties.getUrl().replaceAll(":[^:@]+@", ":****@") : "null");
        return properties.initializeDataSourceBuilder().build();
    }
}
