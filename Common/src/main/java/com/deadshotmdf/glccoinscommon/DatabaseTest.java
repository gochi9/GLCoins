package com.deadshotmdf.glccoinscommon;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class DatabaseTest {

    private final Executor mainThreadExecutor;
    private final Logger logger;
    private final Random random;
    private final HashMap<UUID, Long> interacted;
    private final HikariDataSource dataSource;

    private static final CompletableFuture<Double> NULL_VALUE = CompletableFuture.completedFuture(null);

    public DatabaseTest(Executor mainThreadExecutor, Logger logger) {
        this.mainThreadExecutor = mainThreadExecutor;
        this.logger = logger;
        this.random = new Random();
        this.interacted = new HashMap<>();

        try {
            this.dataSource = new HikariDataSource(getHikariConfig("jdbc:mysql://localhost:3306/testing", "root", "glcpasswordfortesting"));

            try (Connection connection = dataSource.getConnection();
                 Statement stmt = connection.createStatement()) {

                stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS currency (
                            uuid BINARY(16) PRIMARY KEY,
                            value DOUBLE
                        )ENGINE=InnoDB""");
            }

        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    //Runs everything on the main thread
    //For whatever the fuck reason we might need this idk
    public Double getEntry(UUID initiator, UUID uuid) {
        return !hasInteractWithDatabaseTooSoon(initiator) ? getEntryInternal(uuid) : null;
    }

    //This method will fully run async, meaning that we are limited in what we can do with the value while using miencraft's api
    public CompletableFuture<Double> getEntryAsync(UUID initiator, UUID uuid) {
        return hasInteractWithDatabaseTooSoon(initiator) ? NULL_VALUE : CompletableFuture.supplyAsync(() -> getEntryInternal(uuid))
                .exceptionally(ex -> {
                    logger.warning(ex.getMessage());
                    return null;
                });
    }

    //This method will extract the value from the database async, and will run the rest of the code back on minecraft's main thread
    //We can fully and safely use minecraft stuff without fucking shit up
    //There is a one tick (50ms) delay at the very least until this method is fully ran after the value has been extract from the database
    public CompletableFuture<Double> getEntryPartialAsync(UUID initiator, UUID uuid) {
        return getEntryAsync(initiator, uuid).thenApplyAsync(value -> value, mainThreadExecutor)
                .exceptionally(ex -> {
                    logger.warning(ex.getMessage());
                    return null;
                });
    }

    public void modifyEntry(UUID uuid, double value, ModifyType modifyType, Connection connection) {
        String sql = switch (modifyType) {
            case ADD -> "INSERT INTO currency (uuid, value) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE value = value + VALUES(value)";

            case REMOVE -> "INSERT INTO currency (uuid, value) VALUES (?, -?) " +
                    "ON DUPLICATE KEY UPDATE value = value + VALUES(value)";

            case SET -> "INSERT INTO currency (uuid, value) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE value = VALUES(value)";
        };

        try (Connection conn = connection != null && !connection.isClosed() ? connection : dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBytes(1, uuidToBytes(uuid));
            stmt.setDouble(2, value);
            stmt.executeUpdate();

        } catch (SQLException e) {
            this.logger.warning(e.getMessage());
        }
    }

    public void fillRandomEntries(int count) {
        StopWatch stopWatch = StopWatch.createStarted();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO currency (uuid, value) VALUES (?, ?)")) {

            for (int i = 0; i < count; i++) {
                insertStmt.setBytes(1, uuidToBytes(UUID.randomUUID()));
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

    public void close() {
        if (this.dataSource != null && !this.dataSource.isClosed())
            this.dataSource.close();
    }

    private Double getEntryInternal(UUID uuid) {
        try (Connection conn = dataSource.getConnection()) {
            try (PreparedStatement selectStmt = conn.prepareStatement("SELECT value FROM currency WHERE uuid = ?")) {
                selectStmt.setBytes(1, uuidToBytes(uuid));
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next())
                        return rs.getDouble("value");

                    modifyEntry(uuid, 0.0, ModifyType.SET, conn);
                    return 0.0;
                }
            }
        }
        catch (SQLException e) {
            this.logger.warning(e.getMessage());
            return null;
        }
    }

    private boolean hasInteractWithDatabaseTooSoon(UUID uuid){
        if(uuid == null)
            return false;

        long cooldown = interacted.computeIfAbsent(uuid, _ -> 0L);

        if(cooldown == 0 || cooldown - System.currentTimeMillis() < 0){
            interacted.put(uuid, System.currentTimeMillis() + 1150);
            return false;
        }

        return true;
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