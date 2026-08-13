package me.realycraft.anticheat;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

public final class CheckListener implements Listener {
    private final AntiCheatManager manager;
    private final Map<UUID, Long> lastPlace = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBreak = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> airTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> rareBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> totalBlocks = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> clicks = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> attacks = new ConcurrentHashMap<>();
    private final Map<UUID, Float> lastYaw = new ConcurrentHashMap<>();
    public CheckListener(AntiCheatManager manager) { this.manager = manager; }

    @EventHandler public void onJoin(PlayerJoinEvent event) { manager.data(event.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent event) { manager.save(); }

    @EventHandler public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer(); if (!eligible(player) || event.getTo() == null) return;
        Vector delta = event.getTo().toVector().subtract(event.getFrom().toVector()); double horizontal = Math.hypot(delta.getX(), delta.getZ());
        double speedLimit = managerValue("checks.movement.speed.max-horizontal", .72); if (player.hasPotionEffect(org.bukkit.potion.PotionEffectType.SPEED)) speedLimit += .2 * player.getPotionEffect(org.bukkit.potion.PotionEffectType.SPEED).getAmplifier(); if (player.isInsideVehicle() || player.getLocation().getBlock().getType().name().contains("ICE")) speedLimit += .45; if (player.isSwimming()) speedLimit += .3;
        if (horizontal > speedLimit && player.isOnGround()) manager.flag(player, CheckType.SPEED, 2, "horizontal=" + round(horizontal) + " limit=" + round(speedLimit));
        int air = player.isOnGround() ? 0 : airTicks.merge(player.getUniqueId(), 1, Integer::sum); if (air > managerValue("checks.movement.fly.max-air-ticks", 14) && !player.isGliding()) manager.flag(player, CheckType.FLY, 2, "air-ticks=" + air);
        if (player.isSwimming() && delta.getY() > .25) manager.flag(player, CheckType.JESUS, 1, "water-rise");
        if (player.isHandRaised() && horizontal > managerValue("checks.movement.no-slow.max-horizontal", .25)) manager.flag(player, CheckType.NO_SLOW, 1, "using-item speed=" + round(horizontal));
        Float previousYaw = lastYaw.put(player.getUniqueId(), event.getTo().getYaw()); if (previousYaw != null && Math.abs(event.getTo().getYaw() - previousYaw) > 180) manager.flag(player, CheckType.AIM_ASSIST, 1, "impossible-rotation");
    }
    @EventHandler public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !(event.getEntity() instanceof Entity target) || !eligible(player)) return;
        double reach = player.getEyeLocation().distance(target.getLocation()); double compensation = player.getPing() * managerValue("checks.reach.ping-compensation", .015); if (reach > managerValue("checks.reach.max-distance", 3.15) + compensation) manager.flag(player, CheckType.REACH, 3, "distance=" + round(reach) + " ping=" + player.getPing());
        if (player.getFallDistance() == 0 && event.getDamage() > 1 && !player.isOnGround()) manager.flag(player, CheckType.CRITICALS, 1, "invalid-critical");
        long now = System.currentTimeMillis(); Deque<Long> intervals = attacks.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>()); if (!intervals.isEmpty()) { long interval = now - intervals.peekLast(); intervals.addLast(now); while (intervals.size() > 12) intervals.removeFirst(); if (interval > 0 && intervals.size() >= 6 && intervals.stream().skip(1).mapToLong(value -> value).distinct().count() <= 2) manager.flag(player, CheckType.KILLAURA, 2, "identical-attack-interval=" + interval); } else intervals.add(now);
        Deque<Long> cps = clicks.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>()); cps.addLast(now); while (!cps.isEmpty() && now - cps.peekFirst() > 1000) cps.removeFirst(); if (cps.size() > managerValue("checks.autoclicker.max-cps", 18)) manager.flag(player, CheckType.AUTOCLICKER, 2, "cps=" + cps.size());
        if (player.getAttackCooldown() > .99 && target instanceof Player victim && victim.getNoDamageTicks() > 0) manager.flag(player, CheckType.KILLAURA, 1, "repeated-hit-window");
    }
    @EventHandler public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer(); if (!eligible(player)) return; long now = System.currentTimeMillis(); Long previous = lastBreak.put(player.getUniqueId(), now);
        if (previous != null && now - previous < managerValue("checks.fast-break.min-interval-ms", 100)) manager.flag(player, CheckType.FAST_BREAK, 2, "interval=" + (now - previous) + "ms");
        Material type = event.getBlock().getType(); manager.data(player).mined(isRare(type)); int total = totalBlocks.merge(player.getUniqueId(), 1, Integer::sum); if (isRare(type)) {
            int rare = rareBlocks.merge(player.getUniqueId(), 1, Integer::sum); if (total > 20 && rare * 100.0 / total > managerValue("checks.xray.rare-percent", 8)) manager.flag(player, CheckType.XRAY, 4, "rare=" + rare + "/" + total);
        }
        Vector direction = player.getEyeLocation().getDirection(); Vector toOre = event.getBlock().getLocation().add(.5, .5, .5).toVector().subtract(player.getEyeLocation().toVector()); if (isRare(type) && direction.dot(toOre.normalize()) > .92) manager.flag(player, CheckType.XRAY, manager.configDouble("checks.xray.direct-mining-vl", 3), "direct-ore-line");
        for (Block nearby : event.getBlock().getWorld().getNearbyEntities(event.getBlock().getLocation(), 5, 5, 5).stream().map(entity -> entity.getLocation().getBlock()).toList()) if (isRare(nearby.getType())) manager.flag(player, CheckType.XRAY, 1, "ore-proximity");
    }
    @EventHandler public void onPlace(BlockPlaceEvent event) { Player player = event.getPlayer(); if (!eligible(player)) return; long now = System.currentTimeMillis(); Long previous = lastPlace.put(player.getUniqueId(), now); if (previous != null && now - previous < managerValue("checks.fast-place.min-interval-ms", 90)) manager.flag(player, CheckType.FAST_PLACE, 1, "interval=" + (now - previous) + "ms"); if (player.getLocation().getPitch() > managerValue("checks.movement.scaffold.max-place-angle", 78)) manager.flag(player, CheckType.SCAFFOLD, 1, "place-angle=" + player.getLocation().getPitch()); }
    @EventHandler public void onClick(InventoryClickEvent event) { if (event.getWhoClicked() instanceof Player player && !player.isInsideVehicle() && player.getOpenInventory().getTopInventory().getSize() > 0 && player.isSprinting()) manager.flag(player, CheckType.INVENTORY_MOVE, 1, "inventory-while-moving"); }
    @EventHandler public void onInteract(PlayerInteractEvent event) { ItemStack item = event.getItem(); if (item != null && item.getType().name().contains("SWORD") && event.getPlayer().isSneaking()) manager.flag(event.getPlayer(), CheckType.SCAFFOLD, .5, "placement-pattern"); }
    private boolean eligible(Player player) { return player.getGameMode() == GameMode.SURVIVAL && !player.hasPermission("realycraft.ac.bypass"); }
    private double managerValue(String key, double fallback) { return managerValueRaw(key, fallback); }
    private double managerValueRaw(String key, double fallback) { return managerConfig(key, fallback); }
    private double managerConfig(String key, double fallback) { return managerConfigValue(key, fallback); }
    private double managerConfigValue(String key, double fallback) { return managerConfigLookup(key, fallback); }
    private double managerConfigLookup(String key, double fallback) { return manager.configDouble(key, fallback); }
    private boolean isRare(Material material) { return material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE || material == Material.EMERALD_ORE || material == Material.ANCIENT_DEBRIS; }
    private String round(double value) { return String.format(java.util.Locale.US, "%.2f", value); }
}