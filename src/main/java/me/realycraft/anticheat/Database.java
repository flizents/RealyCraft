package me.realycraft.anticheat;

import org.bukkit.configuration.ConfigurationSection;
import java.io.File;
import java.sql.*;
import java.util.UUID;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Database implements AutoCloseable {
    private final RealCraftAntiCheat plugin; private final ExecutorService executor = Executors.newSingleThreadExecutor(); private Connection connection; private boolean mysql;
    public Database(RealCraftAntiCheat plugin) { this.plugin = plugin; }
    public void open() {
        try { mysql = plugin.getConfig().getString("database.type", "SQLITE").equalsIgnoreCase("MYSQL"); connection = DriverManager.getConnection(url()); try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS players(uuid VARCHAR(36) PRIMARY KEY,name VARCHAR(16),first_join VARCHAR(40),last_seen VARCHAR(40),checks BIGINT,vl DOUBLE,rare_blocks BIGINT,mined_blocks BIGINT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS violations(id INTEGER PRIMARY KEY AUTOINCREMENT,uuid VARCHAR(36),check_name VARCHAR(32),amount DOUBLE,detail TEXT,created_at VARCHAR(40))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS actions(id INTEGER PRIMARY KEY AUTOINCREMENT,uuid VARCHAR(36),action VARCHAR(32),detail TEXT,created_at VARCHAR(40))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS bans(id INTEGER PRIMARY KEY " + (mysql ? "AUTO_INCREMENT" : "AUTOINCREMENT") + ",uuid VARCHAR(36),reason TEXT,created_at VARCHAR(40),expires_at VARCHAR(40))");
        }} catch (SQLException exception) { plugin.getLogger().severe("Database unavailable: " + exception.getMessage()); }
    }
    private String url() {
        String type = plugin.getConfig().getString("database.type", "SQLITE").toUpperCase();
        if (type.equals("MYSQL")) { ConfigurationSection mysql = plugin.getConfig().getConfigurationSection("database.mysql"); return "jdbc:mysql://" + mysql.getString("host") + ":" + mysql.getInt("port") + "/" + mysql.getString("database") + "?useSSL=false&serverTimezone=UTC"; }
        return "jdbc:sqlite:" + new File(plugin.getDataFolder(), plugin.getConfig().getString("database.file", "data.db"));
    }
    public void violation(PlayerData data, Violation violation) { execute(() -> update("INSERT INTO violations(uuid,check_name,amount,detail,created_at) VALUES(?,?,?,?,?)", data.uuid().toString(), violation.check().name(), violation.addedVl(), violation.detail(), violation.time().toString())); }
    public PlayerData load(UUID uuid, String fallbackName, int maxHistory) {
        if (connection == null) return new PlayerData(uuid, fallbackName, Instant.now());
        try (PreparedStatement player = connection.prepareStatement("SELECT name,first_join,checks,rare_blocks,mined_blocks FROM players WHERE uuid=?")) {
            player.setString(1, uuid.toString()); try (ResultSet result = player.executeQuery()) {
                if (!result.next()) return new PlayerData(uuid, fallbackName, Instant.now());
                PlayerData data = new PlayerData(uuid, result.getString(1), Instant.parse(result.getString(2))); data.restoreStats(result.getLong(3), result.getLong(5), result.getLong(4));
                for (CheckType type : CheckType.values()) { try (PreparedStatement violations = connection.prepareStatement("SELECT amount,detail,created_at FROM violations WHERE uuid=? AND check_name=? ORDER BY id DESC LIMIT ?")) { violations.setString(1, uuid.toString()); violations.setString(2, type.name()); violations.setInt(3, maxHistory); try (ResultSet rows = violations.executeQuery()) { while (rows.next()) data.restoreViolation(new Violation(type, rows.getDouble(1), rows.getString(2), Instant.parse(rows.getString(3))), maxHistory); } } }
                return data;
            }
        } catch (SQLException | RuntimeException exception) { plugin.getLogger().warning("Database load failed: " + exception.getMessage()); return new PlayerData(uuid, fallbackName, Instant.now()); }
    }
    public void player(PlayerData data) { execute(() -> update(mysql ? "INSERT INTO players(uuid,name,first_join,last_seen,checks,vl,rare_blocks,mined_blocks) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=VALUES(name),last_seen=VALUES(last_seen),checks=VALUES(checks),vl=VALUES(vl),rare_blocks=VALUES(rare_blocks),mined_blocks=VALUES(mined_blocks)" : "INSERT INTO players(uuid,name,first_join,last_seen,checks,vl,rare_blocks,mined_blocks) VALUES(?,?,?,?,?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name,last_seen=excluded.last_seen,checks=excluded.checks,vl=excluded.vl,rare_blocks=excluded.rare_blocks,mined_blocks=excluded.mined_blocks", data.uuid().toString(), data.name(), data.firstJoin().toString(), java.time.Instant.now().toString(), data.checks(), data.totalVl(), data.rareBlocks(), data.minedBlocks())); }
    public void action(UUID uuid, String action, String detail) { execute(() -> update("INSERT INTO actions(uuid,action,detail,created_at) VALUES(?,?,?,?)", uuid.toString(), action, detail, java.time.Instant.now().toString())); }
    private void update(String sql, Object... values) { if (connection == null) return; try (PreparedStatement statement = connection.prepareStatement(sql)) { for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]); statement.executeUpdate(); } catch (SQLException exception) { plugin.getLogger().warning("Database write failed: " + exception.getMessage()); } }
    private void execute(Runnable task) { if (!executor.isShutdown()) executor.execute(task); }
    @Override public void close() { executor.shutdown(); try { if (connection != null) connection.close(); } catch (SQLException ignored) { } }
}