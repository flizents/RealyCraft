package me.realycraft.jobs.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class StorageManager {

    private final JavaPlugin plugin;
    private final File storageFile;
    private final FileConfiguration storageConfig;
    private final Map<UUID, PlayerJobData> cachedData = new HashMap<>();
    private final Set<UUID> dirtyData = new HashSet<>();

    public StorageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "storage.yml");
        if (!storageFile.exists()) {
            plugin.saveResource("storage.yml", false);
        }
        this.storageConfig = YamlConfiguration.loadConfiguration(storageFile);
    }

    public Optional<PlayerJobData> getPlayerData(UUID playerUuid) {
        return Optional.ofNullable(cachedData.computeIfAbsent(playerUuid, this::loadPlayerData));
    }

    public PlayerJobData getOrCreatePlayerData(UUID playerUuid) {
        return cachedData.computeIfAbsent(playerUuid, this::loadPlayerData);
    }

    public void removeCachedPlayerData(UUID playerUuid) {
        cachedData.remove(playerUuid);
    }

    public void saveAll() {
        Set<UUID> toSave = new HashSet<>(dirtyData);
        for (UUID playerUuid : toSave) {
            PlayerJobData data = cachedData.get(playerUuid);
            if (data != null) {
                persistPlayerData(data);
            }
        }
        try {
            storageConfig.save(storageFile);
            dirtyData.clear();
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить storage.yml: " + e.getMessage());
        }
    }

    public void savePlayerData(PlayerJobData data) {
        persistPlayerData(data);
        cachedData.put(data.getPlayerUuid(), data);
        dirtyData.add(data.getPlayerUuid());
    }

    private void persistPlayerData(PlayerJobData data) {
        String key = data.getPlayerUuid().toString();
        storageConfig.set(key + ".job", data.getJobKey().orElse(null));
        storageConfig.set(key + ".balance", data.getAccumulatedBalance());
    }

    private PlayerJobData loadPlayerData(UUID uuid) {
        String basePath = uuid.toString();
        String jobKey = storageConfig.getString(basePath + ".job", null);
        double balance = storageConfig.getDouble(basePath + ".balance", 0.0);
        return new PlayerJobData(uuid, jobKey, balance);
    }

    public static final class PlayerJobData {
        private final UUID playerUuid;
        private String jobKey;
        private double accumulatedBalance;

        public PlayerJobData(UUID playerUuid, String jobKey, double accumulatedBalance) {
            this.playerUuid = playerUuid;
            this.jobKey = jobKey;
            this.accumulatedBalance = accumulatedBalance;
        }

        public UUID getPlayerUuid() {
            return playerUuid;
        }

        public Optional<String> getJobKey() {
            return Optional.ofNullable(jobKey);
        }

        public void setJobKey(String jobKey) {
            this.jobKey = jobKey;
        }

        public double getAccumulatedBalance() {
            return accumulatedBalance;
        }

        public void setAccumulatedBalance(double accumulatedBalance) {
            this.accumulatedBalance = accumulatedBalance;
        }
    }
}
