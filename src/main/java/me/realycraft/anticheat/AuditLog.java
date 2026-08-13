package me.realycraft.anticheat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class AuditLog {
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final RealCraftAntiCheat plugin;
    public AuditLog(RealCraftAntiCheat plugin) { this.plugin = plugin; }
    public synchronized void violation(PlayerData player, Violation violation) {
        if (!plugin.getConfig().getBoolean("logs.enabled", true)) return;
        File directory = new File(plugin.getDataFolder(), "logs"); if (!directory.exists()) directory.mkdirs();
        File file = new File(directory, player.name() + ".log");
        try (FileWriter writer = new FileWriter(file, true)) { writer.write("[" + violation.time().atZone(ZoneId.systemDefault()).format(FORMAT) + "]\nPlayer " + player.name() + "\nCheck " + violation.check().displayName() + "\n+" + violation.addedVl() + " VL: " + violation.detail() + "\n\n"); }
        catch (IOException exception) { plugin.getLogger().warning("Could not write audit log: " + exception.getMessage()); }
    }
}