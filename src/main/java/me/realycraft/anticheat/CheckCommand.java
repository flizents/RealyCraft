package me.realycraft.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CheckCommand implements CommandExecutor {
    private final AntiCheatManager manager;
    public CheckCommand(AntiCheatManager manager) { this.manager = manager; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1 || args.length > 2 || (args.length == 2 && !args[1].equalsIgnoreCase("silent"))) { sender.sendMessage(ChatColor.YELLOW + "Использование: /check <игрок> [silent]"); return true; }
        PlayerData data = manager.find(args[0]); if (data == null) { sender.sendMessage(ChatColor.RED + "Игрок не найден."); return true; }
        Player online = org.bukkit.Bukkit.getPlayerExact(data.name()); int ping = online == null ? data.ping() : online.getPing(); double tps = org.bukkit.Bukkit.getTPS()[0];
        sender.sendMessage(ChatColor.GOLD + "RealCraftAC Check: " + ChatColor.WHITE + data.name());
        sender.sendMessage(ChatColor.GRAY + "UUID: " + data.uuid() + " | Ping: " + ping + "ms | TPS: " + String.format(java.util.Locale.US, "%.2f", tps));
        sender.sendMessage(ChatColor.GRAY + "Статус: " + (data.suspicious(50) ? ChatColor.RED + "Suspicious" : ChatColor.GREEN + "Clean"));
        for (String category : new String[]{"Combat", "Movement", "Player", "World"}) {
            sender.sendMessage(ChatColor.AQUA + category + ":");
            for (CheckType type : CheckType.values()) if (type.category().equals(category)) sender.sendMessage(ChatColor.GRAY + "  " + type.displayName() + ": " + Math.round(data.vl().getOrDefault(type, 0.0)) + " VL");
        }
        sender.sendMessage(ChatColor.GOLD + "Общий VL: " + Math.round(data.totalVl()) + " | Проверок: " + data.checks());
        sender.sendMessage(ChatColor.GOLD + "Risk: " + risk(data.totalVl()));
        return true;
    }
    private String risk(double total) { return total >= 100 ? ChatColor.RED + "High" : total >= 50 ? ChatColor.YELLOW + "Medium" : ChatColor.GREEN + "Low"; }
}