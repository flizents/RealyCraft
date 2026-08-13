package me.realycraft.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Proxy;

public final class PacketEventsBridge {
    private final JavaPlugin plugin;
    public PacketEventsBridge(JavaPlugin plugin) { this.plugin = plugin; }
    public void initialize() {
        if (!Bukkit.getPluginManager().isPluginEnabled("packetevents")) { plugin.getLogger().info("PacketEvents is optional; using Paper event checks."); return; }
        try {
            Class<?> packetEvents = Class.forName("com.github.retrooper.packetevents.PacketEvents"); Object api = packetEvents.getMethod("getAPI").invoke(null); Object eventManager = api.getClass().getMethod("getEventManager").invoke(api);
            Class<?> listenerType = Class.forName("com.github.retrooper.packetevents.event.PacketListener"); Object listener = Proxy.newProxyInstance(listenerType.getClassLoader(), new Class[]{listenerType}, (proxy, method, args) -> { if ("onPacketReceive".equals(method.getName()) && args != null && args.length > 0) plugin.getLogger().fine("PacketEvents packet received: " + args[0].getClass().getSimpleName()); return null; });
            eventManager.getClass().getMethod("registerListener", listenerType).invoke(eventManager, listener); plugin.getLogger().info("PacketEvents rotation listener registered.");
        } catch (ReflectiveOperationException exception) { plugin.getLogger().warning("PacketEvents found but listener registration failed: " + exception.getMessage()); }
    }
}