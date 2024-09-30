package com.deadshotmdf.gLCoins_Server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DatabaseTest {

    private final Logger logger;
    private final HikariDataSource dataSource;

    public DatabaseTest(Logger logger, String url, String user, String password) {
        this.logger = logger;
        try {
            this.dataSource = new HikariDataSource(getHikariConfig(url, user, password));

            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {

                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS currency (\n" +
                        "    uuid BINARY(16) PRIMARY KEY,\n" +
                        "    value DOUBLE\n)ENGINE=InnoDB");
            }

        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Double getEntry(UUID uuid) {
        return getEntryInternal(uuid);
    }

    public CompletableFuture<Double> getEntryAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> getEntryInternal(uuid));
    }

    private Double getEntryInternal(UUID uuid) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement selectStmt = conn.prepareStatement("SELECT value FROM currency WHERE uuid = ?")) {
                selectStmt.setBytes(1, uuidToBytes(uuid));
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) 
                        return rs.getDouble("value");
                    
                    addEntry(uuid, 0.0, conn);
                    return 0.0;
                }
            }
        }
        catch (SQLException e) {
            this.logger.warning(e.getMessage());
            return null;
        }
    }

    public void addEntry(UUID uuid, double value, Connection connection) {
        try (Connection conn = connection != null && !connection.isClosed() ? connection : dataSource.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO currency (uuid, value) VALUES (?, ?) ON DUPLICATE KEY UPDATE value = value")) {

            insertStmt.setBytes(1, uuidToBytes(uuid));
            insertStmt.setDouble(2, value);
            insertStmt.executeUpdate();
        } 
        catch (SQLException e) {
            this.logger.warning(e.getMessage());
        }
    }

    public void fillRandomEntries(int count) {
        StopWatch stopWatch = StopWatch.createStarted();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO currency (uuid, value) VALUES (?, ?)")) {

            Random random = new Random();

            for (int i = 0; i < count; i++) {
                UUID uuid = UUID.randomUUID();
                insertStmt.setBytes(1, uuidToBytes(uuid));
                insertStmt.setDouble(2, random.nextDouble());
                insertStmt.addBatch();
            }

            insertStmt.executeBatch();
            stopWatch.stop();
            System.out.println("Batch insert took: " + stopWatch.getNanoTime() + " ns");
        }
        catch (SQLException e) {
            this.logger.warning(e.getMessage());
        }
    }

    private static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private static HikariConfig getHikariConfig(String url, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "500");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(20);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(2000);
        return config;
    }

}
