package com.deadshotmdf.gLCoins_Server;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang3.time.StopWatch;

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
            config.setDriverClassName("org.postgresql.Driver");
            HikariDataSource dataSource = new HikariDataSource(config);
            connection = dataSource.getConnection();

            String createTableSQL = "CREATE TABLE IF NOT EXISTS currency (\n" +
                    "    uuid UUID PRIMARY KEY,\n" +
                    "    value DOUBLE PRECISION\n" +
                    ")";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(createTableSQL);
            }

            insertStmt = connection.prepareStatement("INSERT INTO currency (uuid, value) VALUES (?, ?)");
            selectStmt = connection.prepareStatement("SELECT value FROM currency WHERE uuid = ?");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addEntry(UUID uuid, double value, boolean log) {
        try {
            insertStmt.setObject(1, uuid);
            insertStmt.setDouble(2, value);
            long start = System.currentTimeMillis();
            insertStmt.executeUpdate();
            if (log)
                System.out.println("Insert execution time: " + (System.currentTimeMillis() - start) + " ms");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Double getEntry(UUID uuid) {
        StopWatch stopWatch = StopWatch.createStarted();
        try {
            selectStmt.setObject(1, uuid);
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    double value = rs.getDouble("value");
                    stopWatch.stop();
                    System.out.println("Query execution time: " + stopWatch.getNanoTime() + " ns");
                    return value;
                } else {
                    stopWatch.stop();
                    System.out.println("Query execution time: " + stopWatch.getNanoTime() + " ns (no result found)");
                    return null;
                }
            }
        } catch (SQLException e) {
            stopWatch.stop();
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
                insertStmt.setObject(1, uuid);
                insertStmt.setDouble(2, random.nextDouble());
                insertStmt.addBatch();
            }

            insertStmt.executeBatch();
            stopWatch.stop();
            System.out.println("Batch insert execution time: " + stopWatch.getNanoTime() + " ns");
        } catch (SQLException e) {
            stopWatch.stop();
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        if (connection == null)
            return;

        try {
            if (insertStmt != null) insertStmt.close();
            if (selectStmt != null) selectStmt.close();
            connection.close();
            System.out.println("Database connection closed.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
