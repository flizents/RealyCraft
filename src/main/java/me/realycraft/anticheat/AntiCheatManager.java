package me.realycraft.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiCheatManager {
    private final JavaPlugin plugin;
    private final Database database;
    private final AuditLog auditLog;
    private final Map<UUID, PlayerData> players = new ConcurrentHashMap<>();
    private final Set<String> appliedPunishments = ConcurrentHashMap.newKeySet();
    private final Set<UUID> watched = ConcurrentHashMap.newKeySet();
    private boolean alertsEnabled;

    public AntiCheatManager(JavaPlugin plugin, Database database, AuditLog auditLog) { this.plugin = plugin; this.database = database; this.auditLog = auditLog; reload(); }
    public void reload() { alertsEnabled = plugin.getConfig().getBoolean("settings.alerts-enabled", true); }
    public PlayerData data(Player player) { return data(player.getUniqueId(), player.getName()); }
    public PlayerData data(UUID uuid, String name) {
        return players.computeIfAbsent(uuid, ignored -> database.load(uuid, name, plugin.getConfig().getInt("settings.max-history", 100)));
    }
    public PlayerData find(String name) {
        Player online = Bukkit.getPlayerExact(name); if (online != null) return data(online);
        for (PlayerData value : players.values()) if (value.name().equalsIgnoreCase(name)) return value;
        return null;
    }
    public Map<UUID, PlayerData> all() { return players; }
    public boolean alertsEnabled() { return alertsEnabled; }
    public void setAlertsEnabled(boolean enabled) { alertsEnabled = enabled; }
    public double highVl() { return plugin.getConfig().getDouble("settings.high-vl", 50); }
    public boolean toggleWatch(UUID uuid) { if (watched.remove(uuid)) return false; watched.add(uuid); return true; }
    public boolean isWatched(UUID uuid) { return watched.contains(uuid); }
    public double configDouble(String path, double fallback) { return plugin.getConfig().getDouble(path, fallback); }

    public void flag(Player player, CheckType type, double amount, String detail) {
        if (player.hasPermission("realycraft.ac.bypass")) return;
        String section = type == CheckType.REACH ? "checks.reach.enabled" : type == CheckType.XRAY ? "checks.xray.enabled" : "checks.enabled";
        if (!plugin.getConfig().getBoolean(section, true)) return;
        PlayerData data = data(player); data.ping(player.getPing()); data.checked();
        double before = data.vl().getOrDefault(type, 0.0); data.addViolation(type, amount, detail, plugin.getConfig().getInt("settings.max-history", 100));
        double after = data.vl().getOrDefault(type, 0.0);
        Violation violation = data.history().peekFirst(); auditLog.violation(data, violation); database.violation(data, violation);
        if (alertsEnabled && (before < highVl() || Math.floor(before / 10) != Math.floor(after / 10))) alert(player, type, after, detail);
        applyPunishments(player, data.totalVl());
    }
    private void alert(Player player, CheckType type, double vl, String detail) {
        String message = ChatColor.RED + "[RealCraftAC] " + ChatColor.GRAY + player.getName() + " " + type.displayName() + " VL=" + format(vl) + " " + detail;
        Bukkit.getOnlinePlayers().stream().filter(p -> p.hasPermission("realycraft.ac.alerts")).forEach(p -> p.sendMessage(message));
        plugin.getLogger().info(ChatColor.stripColor(message));
    }
    private void applyPunishments(Player player, double total) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("punishments"); if (section == null) return;
        for (String key : section.getKeys(false)) {
            if (total < Double.parseDouble(key)) continue;
            String punishmentKey = player.getUniqueId() + ":" + key; if (!appliedPunishments.add(punishmentKey)) continue;
            for (String action : section.getStringList(key + ".command")) {
                String command = action.replace("%player%", player.getName()).replace("%vl%", format(total)).replace("%check%", "RealCraftAC");
                if (command.startsWith("ac alert")) { alert(player, CheckType.XRAY, total, "threshold=" + key); continue; }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
            database.action(player.getUniqueId(), "punishment", key);
        }
    }
    public void save() {
        File file = new File(plugin.getDataFolder(), plugin.getConfig().getString("storage.file", "player-data.yml"));
        org.bukkit.configuration.file.YamlConfiguration yaml = new org.bukkit.configuration.file.YamlConfiguration();
        for (PlayerData data : players.values()) {
            String path = "players." + data.uuid(); yaml.set(path + ".name", data.name()); yaml.set(path + ".first-join", data.firstJoin().toString()); yaml.set(path + ".checks", data.checks()); yaml.set(path + ".ping", data.ping());
            for (CheckType type : CheckType.values()) yaml.set(path + ".vl." + type.name(), data.vl().getOrDefault(type, 0.0));
        }
        players.values().forEach(database::player);
        try { file.getParentFile().mkdirs(); yaml.save(file); } catch (Exception exception) { plugin.getLogger().warning("Could not save player data: " + exception.getMessage()); }
    }
    private String format(double value) { return String.valueOf(Math.round(value)); }
    public OfflinePlayer offline(String name) { return Bukkit.getOfflinePlayer(name); }
    public void reset(PlayerData data) { data.resetVl(); appliedPunishments.removeIf(key -> key.startsWith(data.uuid() + ":")); database.action(data.uuid(), "reset-vl", "admin"); }
}