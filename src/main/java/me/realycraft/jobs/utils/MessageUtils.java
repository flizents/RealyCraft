package me.realycraft.jobs.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class MessageUtils {

    private static String prefix = "";

    private MessageUtils() {
    }

    public static void setPrefix(String prefix) {
        MessageUtils.prefix = prefix == null ? "" : prefix;
    }

    public static String format(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', prefix + message);
    }

    public static void sendIfPresent(CommandSender sender, String message) {
        if (message == null) {
            return;
        }
        String normalized = message.trim();
        String withoutQuotes = normalized.replace("'", "").replace("\"", "").trim();
        if (normalized.isEmpty() || normalized.equalsIgnoreCase("null") || withoutQuotes.isEmpty()) {
            return;
        }
        String formatted = format(normalized);
        if (!formatted.isBlank()) {
            sender.sendMessage(formatted);
        }
    }
}
