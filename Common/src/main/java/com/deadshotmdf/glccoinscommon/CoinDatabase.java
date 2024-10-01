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

public class CoinDatabase {

    private final Executor mainThreadExecutor;
    private final Logger logger;
    private final Random random;
    private final HashMap<UUID, Long> interacted;
    private final HikariDataSource dataSource;

    private static final CompletableFuture<Double> NULL_VALUE = CompletableFuture.completedFuture(null);

    public CoinDatabase(Executor mainThreadExecutor, Logger logger) {
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

    /**
     *
     * Runs everything on the main thread. Worst performance variant.
     * <p/>
     * For whatever the fuck reason we might need this idk.
     * <p/>
     * @param initiator The player who requested the entry, this applies a 1.1 seconds cooldown for the initiator, use null if the initiator is non-player which won't check for a cooldown and won't apply one
     * @param uuid The UUID used for the query to retrieve the current GLCoins amount
     * @param connection A connection if you have access to one that can be reused. If the provided connection is null, or closed then this method will retrieve a new connection.
     * @return The value of the UUID, or if it doesn't exist returns 0.0, while also adding it to the database
     */
    public Double getEntry(UUID initiator, UUID uuid, Connection connection) {
        return !hasInteractWithDatabaseTooSoon(initiator) ? getEntryInternal(uuid, connection) : null;
    }
    /**
     * This method will fully run async, the best performance of the three variants.
     * <p/>
     * Since this runs async, that means we are limited in what we can do with the value while using miencraft's api.
     * <p/>
     * @param initiator The player who requested the entry, this applies a 1.1 seconds cooldown for the initiator, use null if the initiator is non-player which won't check for a cooldown and won't apply one
     * @param uuid The UUID used for the query to retrieve the current GLCoins amount
     * @param connection A connection if you have access to one that can be reused. If the provided connection is null, or closed then this method will retrieve a new connection.
     * @return The value of the UUID, or if it doesn't exist returns 0.0, while also adding it to the database
     */
    public CompletableFuture<Double> getEntryAsync(UUID initiator, UUID uuid, Connection connection) {
        return hasInteractWithDatabaseTooSoon(initiator) ? NULL_VALUE : CompletableFuture.supplyAsync(() -> getEntryInternal(uuid, connection))
                .exceptionally(ex -> {
                    logger.warning(ex.getMessage());
                    return null;
                });
    }

    /**
     * This method will extract the value from the database async, and will run the rest of the code back on minecraft's main thread.
     * We can fully and safely use minecraft stuff without fucking shit up.
     * <p/>
     * There is a one tick (50ms) delay at the very least until this method is fully ran after the value has been extract from the database. This is due to the fact that we need to schedule this code to run on the main thread again, and that only takes places on the next tick of the server. Nothing we can do around that.
     * <p/>
     * @param initiator The player who requested the entry, this applies a 1.1 seconds cooldown for the initiator, use null if the initiator is non-player which won't check for a cooldown and won't apply one
     * @param uuid The UUID used for the query to retrieve the current GLCoins amount
     * @param connection A connection if you have access to one that can be reused. If the provided connection is null, or closed then this method will retrieve a new connection.
     * @return The value of the UUID, or if it doesn't exist returns 0.0, while also adding it to the database
     */
    public CompletableFuture<Double> getEntryPartialAsync(UUID initiator, UUID uuid, Connection connection) {
        return getEntryAsync(initiator, uuid, connection).thenApplyAsync(value -> value, mainThreadExecutor)
                .exceptionally(ex -> {
                    logger.warning(ex.getMessage());
                    return null;
                });
    }

    /**
     * This method executes fully on the main thread = worst performance.
     * <p/>
     * Modifies the value for an entry. You can add, remove to the current value of the UUID, or set it to something else.
     * <p/>
     * If UUID is not present, it'll set the initial balance to 0.0, modify that with the provided @value and @modifyType and insert it into the database as well
     *
     * @param uuid The uuid that is going to be modified, or added if it is not already present
     * @param value The value that will modify the uuid's balance
     * @param modifyType ADD, REMOVE, SET
     * @param connection A connection if you have access to one that can be reused. If the provided connection is null, or closed then this method will retrieve a new connection.
     */
    public double modifyEntry(UUID uuid, double value, ModifyType modifyType, Connection connection) {
        return modifyEntryInternal(uuid, value, modifyType, connection);
    }

    /**
     * This method executes fully async = best performance, limited minecraft API usage.
     * <p/>
     * Modifies the value for an entry. You can add, remove to the current value of the UUID, or set it to something else.
     * <p/>
     * If UUID is not present, it'll set the initial balance to 0.0, modify that with the provided @value and @modifyType and insert it into the database as well
     *
     * @param uuid The uuid that is going to be modified, or added if it is not already present
     * @param value The value that will modify the uuid's balance
     * @param modifyType ADD, REMOVE, SET
     * @param connection A connection if you have access to one that can be reused. If the provided connection is null, or closed then this method will retrieve a new connection.
     */
    public CompletableFuture<Double> modifyEntryAsync(UUID uuid, double value, ModifyType modifyType, Connection connection) {
        return CompletableFuture.supplyAsync(() -> modifyEntryInternal(uuid, value, modifyType, connection))
                .exceptionally(ex -> {
                    logger.warning(ex.getMessage());
                    return null;
                });
    }

    /**
     * This method will modify, and extract the new value from the database async, and will run the rest of the code back on minecraft's main thread.
     * <p/>
     * Modifies the value for an entry. You can add, remove to the current value of the UUID, or set it to something else.
     * <p/>
     * If UUID is not present, it'll set the initial balance to 0.0, modify that with the provided @value and @modifyType and insert it into the database as well
     *
     * @param uuid The uuid that is going to be modified, or added if it is not already present
     * @param value The value that will modify the uuid's balance
     * @param modifyType ADD, REMOVE, SET
     * @param connection A connection if you have access to one that can be reused. If the provided connection is null, or closed then this method will retrieve a new connection.
     */
    public CompletableFuture<Double> modifyEntryPartialAsync(UUID uuid, double value, ModifyType modifyType, Connection connection) {
        return modifyEntryAsync(uuid, value, modifyType, connection).thenApplyAsync(val -> val, mainThreadExecutor)
                .exceptionally(ex -> {
                    logger.warning(ex.getMessage());
                    return null;
                });
    }

    /**
     * Used by the main plugin to close out any connections when the server stops
     * Not meant for API usage
     */
    public void close() {
        if (this.dataSource != null && !this.dataSource.isClosed())
            this.dataSource.close();
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

    private Double getEntryInternal(UUID uuid, Connection connection) {
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

    private Double modifyEntryInternal(UUID uuid, double value, ModifyType modifyType, Connection connection) {
        String sql = switch (modifyType) {
            case ADD -> "INSERT INTO currency (uuid, value) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE value = value + VALUES(value) " +
                    "RETURNING value";

            case REMOVE -> "INSERT INTO currency (uuid, value) VALUES (?, -?) " +
                    "ON DUPLICATE KEY UPDATE value = value + VALUES(value) " +
                    "RETURNING value";

            case SET -> "INSERT INTO currency (uuid, value) VALUES (?, ?) " +
                    "ON DUPLICATE KEY UPDATE value = VALUES(value) " +
                    "RETURNING value";
        };

        try (Connection conn = connection != null && !connection.isClosed() ? connection : dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBytes(1, uuidToBytes(uuid));
            stmt.setDouble(2, value);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("value") : 0.0;
            }
        } catch (SQLException e) {
            this.logger.warning(e.getMessage());
            return 0.0;
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

    /**
     * This method will run ASYNC.
     * <p/>
     * Easy way to fill the database in case we don't already have a script that does that.
     * <p/>
     * Prints at the end the amount of time it took to complete.
     * <p/>
     * Adding 10k entries takes about 80 seconds to complete.
     * </p>
     * @param count The amount of random entries that will be added to the database.
     * @return The amount in nanoseconds it took this operation to complete
     */
    public CompletableFuture<Long> fillRandomEntriesAsync(int count){
        return CompletableFuture.supplyAsync(() -> fillRandomEntries(count));
    }

    /**
     * This method will run on the MAIN thread. Adding a large volume will crash the server.
     * <p/>
     * Easy way to fill the database in case we don't already have a script that does that.
     * <p/>
     * Prints at the end the amount of time it took to complete.
     * <p/>
     * Adding 10k entries takes about 80 seconds to complete.
     * <p/>
     * @param count The amount of random entries that will be added to the database.
     * @return The amount in nanoseconds it took this operation to complete
     */
    public long fillRandomEntries(int count) {
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
            return stopWatch.getNanoTime();
        }
        catch (Throwable e) {
            this.logger.warning(e.getMessage());
            return 0L;
        }
    }

}