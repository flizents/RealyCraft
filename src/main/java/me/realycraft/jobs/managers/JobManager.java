package me.realycraft.jobs.managers;

import me.realycraft.jobs.models.JobDefinition;
import org.bukkit.Material;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class JobManager {

    private final ConfigurationManager configurationManager;
    private final StorageManager storageManager;

    public JobManager(ConfigurationManager configurationManager, StorageManager storageManager) {
        this.configurationManager = configurationManager;
        this.storageManager = storageManager;
    }

    public Collection<JobDefinition> getJobs() {
        return configurationManager.getJobs();
    }

    public Optional<JobDefinition> getJob(String key) {
        return configurationManager.getJob(key);
    }

    public Optional<JobDefinition> getJobByBlock(Material material, String worldName) {
        return configurationManager.getJobs().stream()
                .filter(job -> job.supportsWorld(worldName))
                .filter(job -> job.supportsBlock(material))
                .findFirst();
    }

    public StorageManager.PlayerJobData getOrCreatePlayerData(UUID playerUuid) {
        return storageManager.getOrCreatePlayerData(playerUuid);
    }

    public void setJob(UUID playerUuid, String jobKey) {
        StorageManager.PlayerJobData data = getOrCreatePlayerData(playerUuid);
        data.setJobKey(jobKey);
        storageManager.savePlayerData(data);
    }

    public Optional<JobDefinition> getPlayerJob(UUID playerUuid) {
        return getOrCreatePlayerData(playerUuid).getJobKey().flatMap(this::getJob);
    }

    public double getAccumulatedSalary(UUID playerUuid) {
        return getOrCreatePlayerData(playerUuid).getAccumulatedBalance();
    }

    public void addSalary(UUID playerUuid, double amount) {
        StorageManager.PlayerJobData data = getOrCreatePlayerData(playerUuid);
        double current = data.getAccumulatedBalance();
        double max = configurationManager.getMaxAccumulatedBalance();
        data.setAccumulatedBalance(Math.min(max, current + amount));
    }

    public double withdrawSalary(UUID playerUuid) {
        StorageManager.PlayerJobData data = getOrCreatePlayerData(playerUuid);
        double amount = data.getAccumulatedBalance();
        if (amount <= 0.0) {
            return 0.0;
        }
        data.setAccumulatedBalance(0.0);
        storageManager.savePlayerData(data);
        return amount;
    }

    public void removeJob(UUID playerUuid) {
        StorageManager.PlayerJobData data = getOrCreatePlayerData(playerUuid);
        data.setJobKey(null);
        storageManager.savePlayerData(data);
    }
}
