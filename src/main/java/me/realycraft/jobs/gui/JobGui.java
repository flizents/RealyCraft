package me.realycraft.jobs.gui;

import me.realycraft.jobs.managers.ConfigurationManager;
import me.realycraft.jobs.managers.JobManager;
import me.realycraft.jobs.models.JobDefinition;
import me.realycraft.jobs.utils.ItemBuilder;
import me.realycraft.jobs.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;

import java.util.Optional;

public final class JobGui implements Listener {

    private final Plugin plugin;
    private final JobManager jobManager;
    private final ConfigurationManager configurationManager;

    public JobGui(Plugin plugin, JobManager jobManager, ConfigurationManager configurationManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.configurationManager = configurationManager;
    }

    public void openJobMenu(Player player) {
        Inventory inventory = createInventory();
        player.openInventory(inventory);
    }

    private Inventory createInventory() {
        Inventory inventory = Bukkit.createInventory(null, configurationManager.getGuiSize(), MessageUtils.format(configurationManager.getJobMenuTitle()));
        fillInventory(inventory);
        configurationManager.getJobs().forEach(job -> addJobItem(inventory, job));
        setActionItems(inventory);
        return inventory;
    }

    private void fillInventory(Inventory inventory) {
        ItemStack filler = ItemBuilder.of(configurationManager.getGuiFillMaterial())
                .name(configurationManager.getGuiFillName())
                .build();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    private void addJobItem(Inventory inventory, JobDefinition job) {
        ItemStack item = ItemBuilder.of(job.getIcon())
                .name(job.getDisplayName())
                .lore("&7Нажмите, чтобы выбрать")
                .build();
        inventory.setItem(job.getSlot(), item);
    }

    private void setActionItems(Inventory inventory) {
        ItemStack withdrawItem = ItemBuilder.of(configurationManager.getTakeSalaryMaterial())
                .name(configurationManager.getWithdrawTitle())
                .lore("&7Забрать зарплату")
                .build();
        inventory.setItem(configurationManager.getTakeSalarySlot(), withdrawItem);

        ItemStack quitItem = ItemBuilder.of(configurationManager.getQuitMaterial())
                .name(configurationManager.getQuitTitle())
                .lore("&7Уволиться")
                .build();
        inventory.setItem(configurationManager.getQuitSlot(), quitItem);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory() == null || event.getView().getTitle() == null) {
            return;
        }
        if (!event.getView().getTitle().equals(MessageUtils.format(configurationManager.getJobMenuTitle()))) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        if (slot == configurationManager.getTakeSalarySlot()) {
            onWithdraw(player);
            player.closeInventory();
            return;
        }
        if (slot == configurationManager.getQuitSlot()) {
            onQuit(player);
            player.closeInventory();
            return;
        }
        Optional<JobDefinition> job = configurationManager.getJobs().stream()
                .filter(def -> def.getSlot() == slot)
                .findFirst();
        job.ifPresent(def -> {
            jobManager.setJob(player.getUniqueId(), def.getKey());
            MessageUtils.sendIfPresent(player, configurationManager.format("messages.selected-job", "{job}", def.getDisplayName()));
            player.closeInventory();
        });
    }

    private void onWithdraw(Player player) {
        double amount = jobManager.getAccumulatedSalary(player.getUniqueId());
        if (amount <= 0) {
            MessageUtils.sendIfPresent(player, configurationManager.format("messages.no-salary"));
            return;
        }
        if (plugin instanceof me.realycraft.jobs.Main main) {
            var economy = main.getEconomy();
            var response = economy.depositPlayer(player, amount);
            if (response.transactionSuccess()) {
                jobManager.withdrawSalary(player.getUniqueId());
                MessageUtils.sendIfPresent(player, configurationManager.format("messages.withdraw-success", "{amount}", String.format("%.2f %s", amount, configurationManager.getCurrency())));
            } else {
                MessageUtils.sendIfPresent(player, configurationManager.format("messages.cannot-pay"));
            }
        }
    }

    private void onQuit(Player player) {
        jobManager.getPlayerJob(player.getUniqueId()).ifPresentOrElse(job -> {
            if (configurationManager.allowPaidQuit()) {
                onWithdraw(player);
            }
            jobManager.removeJob(player.getUniqueId());
            MessageUtils.sendIfPresent(player, configurationManager.format("messages.quit-success", "{job}", job.getDisplayName()));
        }, () -> MessageUtils.sendIfPresent(player, configurationManager.format("messages.no-job")));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // placeholder for future state tracking if needed
    }
}
