package me.realycraft.jobs.listeners;

import me.realycraft.jobs.Main;
import me.realycraft.jobs.managers.ConfigurationManager;
import me.realycraft.jobs.managers.JobManager;
import me.realycraft.jobs.models.JobDefinition;
import me.realycraft.jobs.utils.MessageUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class BlockBreakListener implements Listener {

    private final Main plugin;
    private final JobManager jobManager;
    private final ConfigurationManager configurationManager;
    private final BlockPlaceListener blockPlaceListener;

    public BlockBreakListener(Main plugin, JobManager jobManager, ConfigurationManager configurationManager, BlockPlaceListener blockPlaceListener) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.configurationManager = configurationManager;
        this.blockPlaceListener = blockPlaceListener;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!configurationManager.getAllowedWorlds().contains(player.getWorld().getName())) {
            return;
        }
        JobDefinition playerJob = jobManager.getPlayerJob(player.getUniqueId()).orElse(null);
        if (playerJob == null) {
            return;
        }
        if (!playerJob.supportsWorld(player.getWorld().getName())) {
            return;
        }
        Material broken = event.getBlock().getType();
        if (!playerJob.supportsBlock(broken)) {
            return;
        }
        if (blockPlaceListener.isPlayerPlaced(event.getBlock())) {
            blockPlaceListener.removeTrackedBlock(event.getBlock());
            return;
        }
        double amount = playerJob.getSalaryPerBlock();
        jobManager.addSalary(player.getUniqueId(), amount);
        MessageUtils.sendIfPresent(player, configurationManager.format("messages.salary-earned", "{job}", playerJob.getDisplayName(), "{amount}", String.format("%.2f %s", amount, configurationManager.getCurrency())));
    }
}
