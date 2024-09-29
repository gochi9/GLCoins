package com.deadshotmdf.gLCoins_Server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.Random;
import java.util.UUID;

public class DatabaseTest {

    private Connection connection;
    private PreparedStatement insertStmt;
    private PreparedStatement selectStmt;

    public DatabaseTest(String url, String user, String password) {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            HikariDataSource  dataSource = new HikariDataSource(config);
            connection = dataSource.getConnection();

//            String dropTableSQL = "DROP TABLE IF EXISTS currency";
//            try (Statement stmt = connection.createStatement()) {
//                stmt.executeUpdate(dropTableSQL);
//            }

            String createTableSQL = "CREATE TABLE IF NOT EXISTS currency (\n" +
                    "    uuid BINARY(16) PRIMARY KEY,\n" +
                    "    value DOUBLE\n" +
                    ")";
            Statement stmt = connection.createStatement();
            stmt.executeUpdate(createTableSQL);

            insertStmt = connection.prepareStatement("INSERT INTO currency (uuid, value) VALUES (?, ?)");
            selectStmt = connection.prepareStatement("SELECT value FROM currency WHERE uuid = ?");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addEntry(UUID uuid, double value, boolean log) {
        try {
            insertStmt.setBytes(1, uuidToBytes(uuid));
            insertStmt.setDouble(2, value);
            long start = System.currentTimeMillis();
            insertStmt.executeUpdate();
            if (log)
                System.out.println(System.currentTimeMillis() - start);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Double getEntry(UUID uuid) {
        StopWatch stopWatch = StopWatch.createStarted();
        try {
            selectStmt.setBytes(1, uuidToBytes(uuid));
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    stopWatch.stop();
                    System.out.println(stopWatch.getNanoTime());
                    return rs.getDouble("value");
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void fillRandomEntries(int count) {
        StopWatch stopWatch = StopWatch.createStarted();
        try {
            Random random = new Random();
            insertStmt.clearBatch();

            for (int i = 0; i < count; i++) {
                UUID uuid = UUID.randomUUID();
                insertStmt.setBytes(1, uuidToBytes(uuid));
                insertStmt.setDouble(2, random.nextDouble());
                insertStmt.addBatch();
            }

            insertStmt.executeBatch();
            stopWatch.stop();
            System.out.println(stopWatch.getNanoTime());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        if (connection == null)
            return;

        try {
            connection.close();
            System.out.println("Database connection closed.");
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static byte[] uuidToBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

}
