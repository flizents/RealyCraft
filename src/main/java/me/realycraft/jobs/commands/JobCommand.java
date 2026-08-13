package me.realycraft.jobs.commands;

import me.realycraft.jobs.Main;
import me.realycraft.jobs.gui.JobGui;
import me.realycraft.jobs.managers.ConfigurationManager;
import me.realycraft.jobs.managers.JobManager;
import me.realycraft.jobs.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class JobCommand implements CommandExecutor {

    private final Main plugin;
    private final JobManager jobManager;
    private final JobGui jobGui;
    private final ConfigurationManager configurationManager;

    public JobCommand(Main plugin, JobManager jobManager, JobGui jobGui, ConfigurationManager configurationManager) {
        this.plugin = plugin;
        this.jobManager = jobManager;
        this.jobGui = jobGui;
        this.configurationManager = configurationManager;
    }

    private void withdrawSalary(Player player) {
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendIfPresent(sender, configurationManager.format("messages.no-permission"));
            return true;
        }

        if (args.length > 0 && "quit".equalsIgnoreCase(args[0])) {
            jobManager.getPlayerJob(player.getUniqueId()).ifPresentOrElse(job -> {
                if (configurationManager.allowPaidQuit()) {
                    withdrawSalary(player);
                }
                if (configurationManager.removeJobOnQuit()) {
                    jobManager.removeJob(player.getUniqueId());
                }
                MessageUtils.sendIfPresent(player, configurationManager.format("messages.quit-success", "{job}", job.getDisplayName()));
            }, () -> MessageUtils.sendIfPresent(player, configurationManager.format("messages.no-job")));
            return true;
        }

        jobGui.openJobMenu(player);
        return true;
    }
}
