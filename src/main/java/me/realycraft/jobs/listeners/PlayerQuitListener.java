package me.realycraft.jobs.listeners;

import me.realycraft.jobs.managers.StorageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {

    private final StorageManager storageManager;

    public PlayerQuitListener(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        storageManager.savePlayerData(storageManager.getOrCreatePlayerData(event.getPlayer().getUniqueId()));
        storageManager.removeCachedPlayerData(event.getPlayer().getUniqueId());
    }
}
