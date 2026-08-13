package me.realycraft.anticheat;

import java.util.Deque;
import java.util.UUID;

public final class RealCraftAntiCheatAPI {
    private final AntiCheatManager manager;
    RealCraftAntiCheatAPI(AntiCheatManager manager) { this.manager = manager; }
    public double getViolationLevel(UUID uuid) { PlayerData data = manager.all().get(uuid); return data == null ? 0 : data.totalVl(); }
    public double getViolationLevel(UUID uuid, CheckType check) { PlayerData data = manager.all().get(uuid); return data == null ? 0 : data.vl().getOrDefault(check, 0.0); }
    public boolean isSuspicious(UUID uuid) { return getViolationLevel(uuid) >= manager.highVl(); }
    public Deque<Violation> getHistory(UUID uuid) { PlayerData data = manager.all().get(uuid); return data == null ? new java.util.ArrayDeque<>() : data.history(); }
}