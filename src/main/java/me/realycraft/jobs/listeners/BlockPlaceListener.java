package me.realycraft.jobs.listeners;

import me.realycraft.jobs.managers.ConfigurationManager;
import me.realycraft.jobs.managers.JobManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public final class BlockPlaceListener implements Listener {

    private static final long TRACK_TTL_MS = 5 * 60 * 1000L;

    private final JobManager jobManager;
    private final ConfigurationManager configurationManager;
    private final Map<String, Long> trackedBlocks = new HashMap<>();

    public BlockPlaceListener(JobManager jobManager, ConfigurationManager configurationManager) {
        this.jobManager = jobManager;
        this.configurationManager = configurationManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!configurationManager.getAllowedWorlds().contains(player.getWorld().getName())) {
            return;
        }
        if (jobManager.getPlayerJob(player.getUniqueId()).isEmpty()) {
            return;
        }
        Block block = event.getBlock();
        Material material = block.getType();
        if (jobManager.getJobs().stream().noneMatch(job -> job.supportsBlock(material))) {
            return;
        }
        cleanupExpiredTrackedBlocks();
        trackedBlocks.put(generateKey(block), System.currentTimeMillis());
    }

    private String generateKey(Block block) {
        return block.getWorld().getName() + ':' + block.getX() + ':' + block.getY() + ':' + block.getZ();
    }

    private void cleanupExpiredTrackedBlocks() {
        long now = System.currentTimeMillis();
        trackedBlocks.entrySet().removeIf(entry -> now - entry.getValue() > TRACK_TTL_MS);
    }

    public boolean isPlayerPlaced(Block block) {
        cleanupExpiredTrackedBlocks();
        return trackedBlocks.containsKey(generateKey(block));
    }

    public void removeTrackedBlock(Block block) {
        cleanupExpiredTrackedBlocks();
        trackedBlocks.remove(generateKey(block));
    }
}
