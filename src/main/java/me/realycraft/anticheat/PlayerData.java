package me.realycraft.anticheat;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerData {
    private final UUID uuid; private String name; private final Instant firstJoin; private Instant lastViolation;
    private final Map<CheckType, Double> vl = new EnumMap<>(CheckType.class); private final Deque<Violation> history = new ArrayDeque<>(); private long checks; private int ping; private long minedBlocks; private long rareBlocks;
    public PlayerData(UUID uuid, String name, Instant firstJoin) { this.uuid = uuid; this.name = name; this.firstJoin = firstJoin; }
    public UUID uuid() { return uuid; } public String name() { return name; } public void name(String name) { this.name = name; }
    public Instant firstJoin() { return firstJoin; } public Instant lastViolation() { return lastViolation; } public Map<CheckType, Double> vl() { return vl; }
    public Deque<Violation> history() { return history; } public long checks() { return checks; } public void checked() { checks++; }
    public int ping() { return ping; } public void ping(int ping) { this.ping = Math.max(0, ping); }
    public long minedBlocks() { return minedBlocks; } public long rareBlocks() { return rareBlocks; }
    public void mined(boolean rare) { minedBlocks++; if (rare) rareBlocks++; }
    public double totalVl() { return vl.values().stream().mapToDouble(Double::doubleValue).sum(); }
    public boolean suspicious(double threshold) { return totalVl() >= threshold; }
    public void addViolation(CheckType type, double amount, String detail, int maxHistory) {
        vl.merge(type, amount, Double::sum); lastViolation = Instant.now(); history.addFirst(new Violation(type, amount, detail, lastViolation));
        while (history.size() > maxHistory) history.removeLast();
    }
    public void decay(double amount) { vl.replaceAll((type, value) -> Math.max(0, value - amount)); }
    public void resetVl() { vl.clear(); }
    public void restore(CheckType type, double value) { if (value > 0) vl.put(type, value); }
    public void restoreStats(long checks, long minedBlocks, long rareBlocks) { this.checks = checks; this.minedBlocks = minedBlocks; this.rareBlocks = rareBlocks; }
    public void restoreViolation(Violation violation, int maxHistory) { history.addLast(violation); lastViolation = violation.time(); while (history.size() > maxHistory) history.removeFirst(); }
}