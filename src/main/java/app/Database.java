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
            statement.execute("PRAGMA foreign_keys = ON");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS scans (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "created_at TEXT NOT NULL, " +
                    "raw TEXT NOT NULL, " +
                    "normalized TEXT, " +
                    "source TEXT, " +
                    "port TEXT, " +
                    "symbology_id TEXT, " +
                    "parse_ok INTEGER NOT NULL DEFAULT 0, " +
                    "error TEXT" +
                    ")");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS scan_elements (" +
                    "scan_id INTEGER NOT NULL, " +
                    "ai TEXT NOT NULL, " +
                    "value TEXT NOT NULL, " +
                    "pos_start INTEGER, " +
                    "pos_end INTEGER, " +
                    "FOREIGN KEY(scan_id) REFERENCES scans(id) ON DELETE CASCADE" +
                    ")");
            ensureColumn(statement, "normalized", "TEXT");
            ensureColumn(statement, "source", "TEXT");
            ensureColumn(statement, "port", "TEXT");
            ensureColumn(statement, "symbology_id", "TEXT");
            ensureColumn(statement, "parse_ok", "INTEGER NOT NULL DEFAULT 0");
            ensureColumn(statement, "error", "TEXT");
        }
    }

    private void ensureColumn(Statement statement, String name, String definition) {
        try {
            statement.executeUpdate("ALTER TABLE scans ADD COLUMN " + name + " " + definition);
        } catch (SQLException ignored) {
        }
    }

    public long insertScan(String raw,
                           String normalized,
                           String source,
                           String port,
                           String symbologyId,
                           boolean parseOk,
                           String error,
                           List<ParsedElement> elements) throws SQLException {
        String now = Instant.now().toString();
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO scans(created_at, raw, normalized, source, port, symbology_id, parse_ok, error) VALUES(?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, now);
                statement.setString(2, raw);
                statement.setString(3, normalized);
                statement.setString(4, source);
                statement.setString(5, port);
                statement.setString(6, symbologyId);
                statement.setInt(7, parseOk ? 1 : 0);
                statement.setString(8, error);
                statement.executeUpdate();
                long id;
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        id = keys.getLong(1);
                    } else {
                        connection.rollback();
                        return -1;
                    }
                }
                try (PreparedStatement insertElement = connection.prepareStatement(
                        "INSERT INTO scan_elements(scan_id, ai, value, pos_start, pos_end) VALUES(?, ?, ?, ?, ?)")) {
                    for (ParsedElement element : elements) {
                        insertElement.setLong(1, id);
                        insertElement.setString(2, element.element().ai());
                        insertElement.setString(3, element.element().value());
                        insertElement.setInt(4, element.valueStart());
                        insertElement.setInt(5, element.valueEnd());
                        insertElement.addBatch();
                    }
                    insertElement.executeBatch();
                }
                connection.commit();
                return id;
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Scan> listScans(int limit) throws SQLException {
        List<Scan> scans = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, created_at, raw, normalized, source, port, symbology_id, parse_ok, error FROM scans ORDER BY id DESC LIMIT ?")) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    String createdAt = resultSet.getString("created_at");
                    String raw = resultSet.getString("raw");
                    String normalized = resultSet.getString("normalized");
                    String source = resultSet.getString("source");
                    String port = resultSet.getString("port");
                    String symbologyId = resultSet.getString("symbology_id");
                    boolean parseOk = resultSet.getInt("parse_ok") == 1;
                    String error = resultSet.getString("error");
                    List<ScanElement> elements = loadElements(connection, id);
                    scans.add(new Scan(id, createdAt, raw, normalized, source, port, symbologyId, parseOk, error, elements));
                }
            }
        }
        return scans;
    }

    public List<Scan> listAllScans() throws SQLException {
        return listScans(Integer.MAX_VALUE);
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private List<ScanElement> loadElements(Connection connection, long id) throws SQLException {
        List<ScanElement> elements = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT ai, value, pos_start, pos_end FROM scan_elements WHERE scan_id = ? ORDER BY rowid")) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String ai = rs.getString("ai");
                    String value = rs.getString("value");
                    int start = rs.getInt("pos_start");
                    int end = rs.getInt("pos_end");
                    elements.add(new ScanElement(id, ai, value, start, end));
                }
            }
        }
        return elements;
    }
}
