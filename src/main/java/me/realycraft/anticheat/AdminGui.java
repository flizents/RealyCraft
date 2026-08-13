package me.realycraft.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class AdminGui {
    private final AntiCheatManager manager;
    public AdminGui(AntiCheatManager manager) { this.manager = manager; }
    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, ChatColor.DARK_RED + "RealCraftAC"); int slot = 0;
        for (PlayerData data : manager.all().values()) if (data.totalVl() >= 50 && slot < 18) inventory.setItem(slot++, item(Material.PLAYER_HEAD, ChatColor.RED + data.name(), ChatColor.GRAY + "VL: " + Math.round(data.totalVl()) + " | Graph: " + graph(data)));
        inventory.setItem(22, item(Material.COMPARATOR, ChatColor.GOLD + "Alerts", ChatColor.GRAY + String.valueOf(manager.alertsEnabled())));
        inventory.setItem(23, item(Material.BARRIER, ChatColor.RED + "Reset VL", ChatColor.GRAY + "Выберите игрока и используйте /ac reset"));
        inventory.setItem(24, item(Material.SPYGLASS, ChatColor.AQUA + "Watch mode", ChatColor.GRAY + "Наблюдение за игроками с высоким VL"));
        inventory.setItem(25, item(Material.BOOK, ChatColor.WHITE + "Violation history", ChatColor.GRAY + "Последние нарушения в /ac violations")); player.openInventory(inventory);
    }
    private String graph(PlayerData data) { StringBuilder graph = new StringBuilder(); data.history().stream().limit(8).forEach(violation -> graph.append("|").append(Math.min(9, Math.max(1, Math.round(violation.addedVl()))))); return graph.length() == 0 ? "-" : graph.toString(); }
    private ItemStack item(Material material, String name, String lore) { ItemStack item = new ItemStack(material); ItemMeta meta = item.getItemMeta(); meta.setDisplayName(name); meta.setLore(java.util.List.of(lore)); item.setItemMeta(meta); return item; }
}