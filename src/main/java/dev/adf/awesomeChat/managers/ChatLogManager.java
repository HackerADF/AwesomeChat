package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatLogManager {

    private final AwesomeChat plugin;
    private Connection connection;
    private final String storageType;
    private final Map<UUID, SearchState> searchStates = new ConcurrentHashMap<>();

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM d");
    private static final DateTimeFormatter SHORT_DATETIME = DateTimeFormatter.ofPattern("MMM d HH:mm");

    public ChatLogManager(AwesomeChat plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getPluginConfig();
        this.storageType = config.getString("chat-logging.storage-type", "sqlite").toLowerCase();
        initDatabase();
    }

    private void initDatabase() {
        try {
            if ("mysql".equals(storageType)) {
                initMySQL();
            } else {
                initSQLite();
            }
            createTable();
        } catch (Exception e) {
            plugin.getLogger().warning("ChatLogManager failed to connect to database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initSQLite() throws SQLException {
        String fileName = plugin.getPluginConfig().getString("chat-logging.sqlite.file", "chat-logs.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);
        plugin.getDataFolder().mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        connection.createStatement().execute("PRAGMA journal_mode=WAL");
    }

    private void initMySQL() throws SQLException {
        FileConfiguration config = plugin.getPluginConfig();
        String host = config.getString("chat-logging.mysql.host", "localhost");
        int port = config.getInt("chat-logging.mysql.port", 3306);
        String database = config.getString("chat-logging.mysql.database", "awesomechat");
        String username = config.getString("chat-logging.mysql.username", "root");
        String password = config.getString("chat-logging.mysql.password", "");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
        connection = DriverManager.getConnection(url, username, password);
    }

    private void createTable() throws SQLException {
        String sql;
        if ("mysql".equals(storageType)) {
            sql = "CREATE TABLE IF NOT EXISTS chat_logs ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "uuid VARCHAR(36) NOT NULL, "
                    + "username VARCHAR(16) NOT NULL, "
                    + "message TEXT NOT NULL, "
                    + "channel VARCHAR(64) DEFAULT NULL, "
                    + "filtered BOOLEAN DEFAULT FALSE, "
                    + "timestamp BIGINT NOT NULL, "
                    + "INDEX idx_uuid (uuid), "
                    + "INDEX idx_timestamp (timestamp)"
                    + ")";
        } else {
            sql = "CREATE TABLE IF NOT EXISTS chat_logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "uuid TEXT NOT NULL, "
                    + "username TEXT NOT NULL, "
                    + "message TEXT NOT NULL, "
                    + "channel TEXT DEFAULT NULL, "
                    + "filtered INTEGER DEFAULT 0, "
                    + "timestamp INTEGER NOT NULL"
                    + ")";
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(sql);
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_uuid ON chat_logs(uuid)");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_timestamp ON chat_logs(timestamp)");
            }
            return;
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    public void logMessage(UUID uuid, String username, String message, String channel, boolean filtered) {
        if (!plugin.getPluginConfig().getBoolean("chat-logging.enabled", false)) return;
        if (filtered && !plugin.getPluginConfig().getBoolean("chat-logging.log-filtered", true)) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    ensureConnection();
                    String sql = "INSERT INTO chat_logs (uuid, username, message, channel, filtered, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setString(1, uuid.toString());
                        ps.setString(2, username);
                        ps.setString(3, message);
                        ps.setString(4, channel);
                        ps.setBoolean(5, filtered);
                        ps.setLong(6, System.currentTimeMillis());
                        ps.executeUpdate();
                    }
                } catch (SQLException e) {
                    plugin.getLogger().warning("Failed to log chat message: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void searchAsync(UUID searcher, UUID targetUuid, Long afterMs, Long beforeMs, int page, SearchCallback callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    ensureConnection();
                    int pageSize = plugin.getPluginConfig().getInt("chat-logging.page-size", 10);
                    int offset = (page - 1) * pageSize;

                    StringBuilder where = new StringBuilder("WHERE uuid = ?");
                    List<Object> params = new ArrayList<>();
                    params.add(targetUuid.toString());

                    if (afterMs != null) {
                        where.append(" AND timestamp >= ?");
                        params.add(afterMs);
                    }
                    if (beforeMs != null) {
                        where.append(" AND timestamp <= ?");
                        params.add(beforeMs);
                    }

                    // Count total
                    int total;
                    try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM chat_logs " + where)) {
                        for (int i = 0; i < params.size(); i++) {
                            setParam(ps, i + 1, params.get(i));
                        }
                        ResultSet rs = ps.executeQuery();
                        rs.next();
                        total = rs.getInt(1);
                    }

                    // Fetch page
                    List<ChatLogEntry> entries = new ArrayList<>();
                    String sql = "SELECT id, uuid, username, message, channel, filtered, timestamp FROM chat_logs "
                            + where + " ORDER BY timestamp DESC LIMIT ? OFFSET ?";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        for (int i = 0; i < params.size(); i++) {
                            setParam(ps, i + 1, params.get(i));
                        }
                        ps.setInt(params.size() + 1, pageSize);
                        ps.setInt(params.size() + 2, offset);
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) {
                            entries.add(new ChatLogEntry(
                                    rs.getLong("id"),
                                    rs.getString("uuid"),
                                    rs.getString("username"),
                                    rs.getString("message"),
                                    rs.getString("channel"),
                                    rs.getBoolean("filtered"),
                                    rs.getLong("timestamp")
                            ));
                        }
                    }

                    int totalPages = (int) Math.ceil((double) total / pageSize);

                    // Store search state for pagination
                    SearchState state = new SearchState(targetUuid, afterMs, beforeMs, page, totalPages, total);
                    searchStates.put(searcher, state);

                    // Callback on main thread
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            callback.onResult(entries, page, totalPages, total);
                        }
                    }.runTask(plugin);

                } catch (SQLException e) {
                    plugin.getLogger().warning("Failed to search chat logs: " + e.getMessage());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            callback.onError(e.getMessage());
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void setParam(PreparedStatement ps, int index, Object value) throws SQLException {
        if (value instanceof String) {
            ps.setString(index, (String) value);
        } else if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else if (value instanceof Boolean) {
            ps.setBoolean(index, (Boolean) value);
        }
    }

    private void ensureConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            if ("mysql".equals(storageType)) {
                initMySQL();
            } else {
                initSQLite();
            }
        }
    }

    public SearchState getSearchState(UUID player) {
        return searchStates.get(player);
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }

    // --- Time Parsing ---

    public static Long parseDuration(String input) {
        if (input == null || input.isEmpty()) return null;
        input = input.trim().toLowerCase();
        try {
            long number = Long.parseLong(input.substring(0, input.length() - 1));
            char unit = input.charAt(input.length() - 1);
            long ms = switch (unit) {
                case 's' -> number * 1000L;
                case 'm' -> number * 60_000L;
                case 'h' -> number * 3_600_000L;
                case 'd' -> number * 86_400_000L;
                case 'w' -> number * 604_800_000L;
                default -> -1L;
            };
            return ms > 0 ? ms : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static String formatShortTimestamp(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        LocalDateTime nowDateTime = LocalDateTime.now();

        if (diff < 60_000L) return "just now";
        if (diff < 3_600_000L) return (diff / 60_000L) + "m ago";
        if (diff < 86_400_000L) return (diff / 3_600_000L) + "h ago";
        if (diff < 604_800_000L) return (diff / 86_400_000L) + "d ago";

        if (dateTime.getYear() == nowDateTime.getYear()) {
            return SHORT_DATE.format(dateTime);
        }
        return SHORT_DATETIME.format(dateTime);
    }

    // --- Data Classes ---

    public static class ChatLogEntry {
        public final long id;
        public final String uuid;
        public final String username;
        public final String message;
        public final String channel;
        public final boolean filtered;
        public final long timestamp;

        public ChatLogEntry(long id, String uuid, String username, String message, String channel, boolean filtered, long timestamp) {
            this.id = id;
            this.uuid = uuid;
            this.username = username;
            this.message = message;
            this.channel = channel;
            this.filtered = filtered;
            this.timestamp = timestamp;
        }
    }

    public static class SearchState {
        public final UUID targetUuid;
        public final Long afterMs;
        public final Long beforeMs;
        public int currentPage;
        public int totalPages;
        public int totalResults;

        public SearchState(UUID targetUuid, Long afterMs, Long beforeMs, int currentPage, int totalPages, int totalResults) {
            this.targetUuid = targetUuid;
            this.afterMs = afterMs;
            this.beforeMs = beforeMs;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.totalResults = totalResults;
        }
    }

    @FunctionalInterface
    public interface SearchCallback {
        void onResult(List<ChatLogEntry> entries, int page, int totalPages, int total);

        default void onError(String message) {}
    }
}
