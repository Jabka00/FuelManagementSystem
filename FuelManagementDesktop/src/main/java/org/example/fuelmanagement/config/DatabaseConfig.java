package org.example.fuelmanagement.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;
    private static Dotenv dotenv;

    static {
        dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
    }

    private static String getEnvOrProperty(Properties config, String envKey, String propertyKey) {
        String envValue = dotenv.get(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }
        return config.getProperty(propertyKey);
    }

    public static void initialize() throws IOException, SQLException {
        Properties config = new Properties();

        try (InputStream inputStream = DatabaseConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (inputStream == null) {
                throw new IOException("Файл application.properties не знайдено в classpath");
            }
            config.load(inputStream);
        }

        String jdbcUrl = getEnvOrProperty(config, "DATABASE_URL", "database.url");
        String username = getEnvOrProperty(config, "DATABASE_USERNAME", "database.username");
        String password = getEnvOrProperty(config, "DATABASE_PASSWORD", "database.password");

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName(config.getProperty("database.driver", "com.mysql.cj.jdbc.Driver"));

        hikariConfig.setMaximumPoolSize(Integer.parseInt(config.getProperty("database.pool.maximum", "10")));
        hikariConfig.setMinimumIdle(Integer.parseInt(config.getProperty("database.pool.minimum", "2")));
        hikariConfig.setConnectionTimeout(Long.parseLong(config.getProperty("database.pool.timeout", "30000")));
        hikariConfig.setIdleTimeout(300000);
        hikariConfig.setMaxLifetime(1200000);

        hikariConfig.setConnectionTestQuery("SELECT 1");

        try {
            dataSource = new HikariDataSource(hikariConfig);

            try (Connection testConnection = dataSource.getConnection()) {
                if (testConnection != null && !testConnection.isClosed()) {
                    logger.info(" База даних успішно підключена");
                    logger.info("URL: {}", jdbcUrl);
                    logger.info("Користувач: {}", username);
                }
            }
        } catch (Exception e) {
            logger.error(" Помилка підключення до БД: {}", e.getMessage());
            logger.error("URL: {}", jdbcUrl);
            logger.error("Користувач: {}", username);
            throw e;
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DatabaseConfig не ініціалізовано");
        }
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            logger.info("З'єднання з базою даних закрито");
        }
    }
}