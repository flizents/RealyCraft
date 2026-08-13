package me.realycraft.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public final class AdminGuiListener implements Listener {
    private final AntiCheatManager manager;
    public AdminGuiListener(AntiCheatManager manager) { this.manager = manager; }
    @EventHandler public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !event.getView().getTitle().equals(ChatColor.DARK_RED + "RealCraftAC")) return;
        event.setCancelled(true); ItemStack item = event.getCurrentItem(); if (item == null || item.getType() == Material.AIR) return;
        if (event.getSlot() < 18 && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) { String name = ChatColor.stripColor(item.getItemMeta().getDisplayName()); PlayerData data = manager.find(name); if (data != null) { manager.reset(data); player.sendMessage(ChatColor.GREEN + "VL игрока " + name + " сброшен."); } return; }
        if (event.getSlot() == 22) { manager.setAlertsEnabled(!manager.alertsEnabled()); player.sendMessage(ChatColor.GREEN + "Alerts: " + manager.alertsEnabled()); return; }
        if (event.getSlot() == 24) { PlayerData watched = manager.all().values().stream().filter(data -> data.totalVl() >= manager.highVl()).findFirst().orElse(null); if (watched != null) player.sendMessage(ChatColor.AQUA + "Watch mode " + (manager.toggleWatch(watched.uuid()) ? "включен для " : "выключен для ") + watched.name()); else player.sendMessage(ChatColor.GRAY + "Нет игроков с высоким VL."); }
    }
    @EventHandler public void onClose(InventoryCloseEvent event) { if (event.getView().getTitle().equals(ChatColor.DARK_RED + "RealCraftAC")) event.getPlayer().sendMessage(ChatColor.GRAY + "RealCraftAC GUI закрыто."); }
}