package me.realycraft.jobs.managers;

import me.realycraft.jobs.models.JobDefinition;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public final class ConfigurationManager {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final Set<String> warnedInvalidIcons = new HashSet<>();

    public ConfigurationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public String getPrefix() {
        return config.getString("messages.prefix", "&7[&6Job&7] &r");
    }

    public Set<String> getAllowedWorlds() {
        return new HashSet<>(config.getStringList("settings.allowed-worlds"));
    }

    public Collection<JobDefinition> getJobs() {
        ConfigurationSection jobsSection = config.getConfigurationSection("jobs");
        if (jobsSection == null) {
            return Collections.emptyList();
        }
        return jobsSection.getKeys(false).stream()
                .map(this::loadJobDefinition)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());
    }

    public Optional<JobDefinition> getJob(String key) {
        ConfigurationSection jobSection = config.getConfigurationSection("jobs." + key);
        if (jobSection == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadJobDefinition(key));
    }

    private JobDefinition loadJobDefinition(String key) {
        ConfigurationSection jobSection = config.getConfigurationSection("jobs." + key);
        if (jobSection == null) {
            return null;
        }
        String displayName = jobSection.getString("display-name", key);
        String rawIcon = jobSection.getString("icon", "PAPER");
        Material icon = resolveIcon(rawIcon, key);
        int slot = jobSection.getInt("slot", 10);
        double salaryPerBlock = jobSection.getDouble("salary-per-block", 1.0);
        List<Material> blocks = jobSection.getStringList("blocks").stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .toList();
        Set<String> worlds = new HashSet<>(jobSection.getStringList("worlds"));
        return new JobDefinition(key, displayName, icon, slot, salaryPerBlock, blocks, worlds);
    }

    private Material resolveIcon(String rawIcon, String jobKey) {
        String iconName = rawIcon == null ? "" : rawIcon.trim()
                .replace("'", "")
                .replace("\"", "")
                .toUpperCase(Locale.ROOT);
        Material icon = Material.matchMaterial(iconName);
        if (icon == null && !iconName.isEmpty()) {
            icon = Arrays.stream(Material.values())
                    .filter(material -> material.name().equalsIgnoreCase(iconName))
                    .findFirst()
                    .orElse(null);
        }
        if (icon != null) {
            return icon;
        }
        if (warnedInvalidIcons.add(jobKey + ":" + iconName)) {
            plugin.getLogger().warning("Неверный материал для job.icon: " + rawIcon + " у профессии " + jobKey
                    + ". Используется PAPER.");
        }
        return Material.PAPER;
    }

    public int getGuiSize() {
        return Math.max(9, Math.min(54, config.getInt("gui.size", 27)));
    }

    public Material getGuiFillMaterial() {
        Material material = Material.matchMaterial(config.getString("gui.fill-item", "BLACK_STAINED_GLASS_PANE"));
        return material == null ? Material.BLACK_STAINED_GLASS_PANE : material;
    }

    public String getGuiFillName() {
        return config.getString("gui.fill-name", "");
    }

    public int getTakeSalarySlot() {
        return config.getInt("gui.take-salary-slot", 16);
    }

    public int getQuitSlot() {
        return config.getInt("gui.quit-slot", 26);
    }

    public Material getTakeSalaryMaterial() {
        Material mat = Material.matchMaterial(config.getString("gui.take-salary-item", "GOLD_INGOT"));
        return mat == null ? Material.GOLD_INGOT : mat;
    }

    public Material getQuitMaterial() {
        Material mat = Material.matchMaterial(config.getString("gui.quit-item", "BARRIER"));
        return mat == null ? Material.BARRIER : mat;
    }

    public String format(String path, Object... replacements) {
        String message = config.getString(path, "");
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(String.valueOf(replacements[i]), String.valueOf(replacements[i + 1]));
        }
        return message;
    }

    public String getJobMenuTitle() {
        return config.getString("settings.gui-title", "&6Выберите профессию");
    }

    public String getWithdrawTitle() {
        return config.getString("settings.withdraw-title", "&6Ваши выплаты");
    }

    public String getQuitTitle() {
        return config.getString("settings.quit-title", "&6Увольнение");
    }

    public String getCurrency() {
        return config.getString("settings.currency", "$");
    }

    public boolean allowPaidQuit() {
        return config.getBoolean("settings.allow-paid-quit", true);
    }

    public boolean removeJobOnQuit() {
        return config.getBoolean("settings.remove-job-on-quit", true);
    }

    public double getMaxAccumulatedBalance() {
        return config.getDouble("settings.max-accumulated-balance", 1000000.0);
    }
}
