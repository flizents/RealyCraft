package me.realycraft.jobs;

import me.realycraft.jobs.commands.JobCommand;
import me.realycraft.jobs.gui.JobGui;
import me.realycraft.jobs.listeners.BlockBreakListener;
import me.realycraft.jobs.listeners.BlockPlaceListener;
import me.realycraft.jobs.listeners.PlayerQuitListener;
import me.realycraft.jobs.managers.ConfigurationManager;
import me.realycraft.jobs.managers.JobManager;
import me.realycraft.jobs.managers.StorageManager;
import me.realycraft.jobs.utils.MessageUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private Economy economy;
    private ConfigurationManager configurationManager;
    private JobManager jobManager;
    private StorageManager storageManager;
    private JobGui jobGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        configurationManager = new ConfigurationManager(this);
        storageManager = new StorageManager(this);
        jobManager = new JobManager(configurationManager, storageManager);
        jobGui = new JobGui(this, jobManager, configurationManager);

        if (!setupEconomy()) {
            getLogger().severe("Vault не найден или экономика не настроена.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        BlockPlaceListener blockPlaceListener = new BlockPlaceListener(jobManager, configurationManager);
        if (getCommand("job") != null) {
            getCommand("job").setExecutor(new JobCommand(this, jobManager, jobGui, configurationManager));
        }
        getServer().getPluginManager().registerEvents(new BlockBreakListener(this, jobManager, configurationManager, blockPlaceListener), this);
        getServer().getPluginManager().registerEvents(blockPlaceListener, this);
        getServer().getPluginManager().registerEvents(jobGui, this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(storageManager), this);

        MessageUtils.setPrefix(configurationManager.getPrefix());
        getServer().getScheduler().runTaskTimer(this, storageManager::saveAll, 20L * 60L * 5L, 20L * 60L * 5L);
    }

    @Override
    public void onDisable() {
        storageManager.saveAll();
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ConfigurationManager getConfigurationManager() {
        return configurationManager;
    }
}
