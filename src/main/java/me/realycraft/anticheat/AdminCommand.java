package me.realycraft.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public final class AdminCommand implements CommandExecutor, TabCompleter {
    private final RealCraftAntiCheat plugin; private final AntiCheatManager manager;
    public AdminCommand(RealCraftAntiCheat plugin, AntiCheatManager manager) { this.plugin = plugin; this.manager = manager; }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sender.sendMessage(ChatColor.YELLOW + "/ac player|violations|alerts|staff|reload|info|gui|reset"); return true; }
        String sub = args[0].toLowerCase();
        if (sub.equals("reload")) { plugin.reloadConfig(); manager.reload(); sender.sendMessage(ChatColor.GREEN + "Конфигурация перезагружена."); return true; }
        if (sub.equals("info")) { sender.sendMessage(ChatColor.GOLD + "RealCraftAntiCheat 1.0.0 | Paper 1.21.8 | Java 21"); return true; }
        if (sub.equals("alerts")) { manager.setAlertsEnabled(!manager.alertsEnabled()); sender.sendMessage(ChatColor.GREEN + "Alerts: " + manager.alertsEnabled()); return true; }
        if (sub.equals("staff")) { manager.all().values().stream().filter(data -> data.totalVl() >= 50).forEach(data -> sender.sendMessage(ChatColor.RED + data.name() + " - " + Math.round(data.totalVl()) + " VL")); return true; }
        if (sub.equals("reset") && args.length > 1) { PlayerData data = manager.find(args[1]); if (data != null) { manager.reset(data); sender.sendMessage(ChatColor.GREEN + "VL сброшен."); } return true; }
        if ((sub.equals("player") || sub.equals("violations")) && args.length > 1) { showDetails(sender, args[1], sub.equals("violations")); return true; }
        if (sub.equals("gui") && sender instanceof Player player) { new AdminGui(manager).open(player); return true; }
        sender.sendMessage(ChatColor.YELLOW + "Использование: /ac player <игрок>"); return true;
    }
    private void showDetails(CommandSender sender, String name, boolean history) {
        PlayerData data = manager.find(name); if (data == null) { sender.sendMessage(ChatColor.RED + "Игрок не найден."); return; }
        sender.sendMessage(ChatColor.GOLD + "RealCraftAC " + (history ? "Violations" : "Player") + ": " + data.name());
        if (!history) { sender.sendMessage(ChatColor.GRAY + "UUID: " + data.uuid()); sender.sendMessage(ChatColor.GRAY + "Первый вход: " + data.firstJoin()); sender.sendMessage(ChatColor.GRAY + "Нарушений: " + data.history().size() + " | VL: " + Math.round(data.totalVl())); }
        data.history().stream().limit(20).forEach(v -> sender.sendMessage(ChatColor.GRAY + v.time().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " " + v.check().displayName() + " +" + v.addedVl() + " " + v.detail()));
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("player", "violations", "alerts", "staff", "reload", "info", "gui", "reset");
        if (args.length == 2 && (args[0].equalsIgnoreCase("player") || args[0].equalsIgnoreCase("violations"))) return new ArrayList<>(manager.all().values().stream().map(PlayerData::name).toList());
        return List.of();
    }
}