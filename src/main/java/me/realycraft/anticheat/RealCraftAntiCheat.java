package me.realycraft.anticheat;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

public final class RealCraftAntiCheat extends JavaPlugin {
    private AntiCheatManager manager;
    private Database database;
    private RealCraftAntiCheatAPI api;
    @Override public void onEnable() {
        saveDefaultConfig(); database = new Database(this); database.open(); manager = new AntiCheatManager(this, database, new AuditLog(this)); api = new RealCraftAntiCheatAPI(manager); getServer().getServicesManager().register(RealCraftAntiCheatAPI.class, api, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(new CheckListener(manager), this);
        getServer().getPluginManager().registerEvents(new AdminGuiListener(manager), this);
        getCommand("check").setExecutor(new CheckCommand(manager));
        AdminCommand admin = new AdminCommand(this, manager); getCommand("ac").setExecutor(admin); getCommand("ac").setTabCompleter(admin);
        new PacketEventsBridge(this).initialize();
        long period = Math.max(20, getConfig().getInt("settings.data-save-seconds", 30) * 20L);
        getServer().getScheduler().runTaskTimerAsynchronously(this, manager::save, period, period);
        getLogger().info("RealCraftAntiCheat 1.1.0 enabled for Paper 1.21.8.");
    }
    @Override public void onDisable() { if (manager != null) manager.save(); if (database != null) database.close(); }
    public AntiCheatManager manager() { return manager; }
    public RealCraftAntiCheatAPI api() { return api; }
}