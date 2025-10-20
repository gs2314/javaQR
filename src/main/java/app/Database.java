package app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private final String url;

    public Database(String filePath) {
        this.url = "jdbc:sqlite:" + filePath;
    }

    public void initialize() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS scans (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "created_at TEXT NOT NULL, " +
                    "raw TEXT NOT NULL"
                    + ")");
        }
    }

    public long insertScan(String raw) throws SQLException {
        String now = Instant.now().toString();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO scans(created_at, raw) VALUES(?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, now);
            statement.setString(2, raw);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        return -1;
    }

    public List<Scan> listScans(int limit) throws SQLException {
        List<Scan> scans = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, created_at, raw FROM scans ORDER BY id DESC LIMIT ?")) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    String createdAt = resultSet.getString("created_at");
                    String raw = resultSet.getString("raw");
                    scans.add(new Scan(id, createdAt, raw));
                }
            }
        }
        return scans;
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }
}
