package me.realycraft.anticheat;

public enum CheckType {
    KILLAURA("KillAura", "Combat"), REACH("Reach", "Combat"), AUTOCLICKER("AutoClicker", "Combat"), AIM_ASSIST("AimAssist", "Combat"), CRITICALS("Criticals", "Combat"), VELOCITY("Velocity", "Combat"),
    FLY("Fly", "Movement"), SPEED("Speed", "Movement"), JESUS("Jesus", "Movement"), NO_SLOW("NoSlow", "Movement"), SCAFFOLD("Scaffold", "Movement"), TIMER("Timer", "Movement"),
    FAST_BREAK("FastBreak", "Player"), FAST_PLACE("FastPlace", "Player"), INVENTORY_MOVE("InventoryMove", "Player"), XRAY("Xray", "World");
    private final String displayName; private final String category;
    CheckType(String displayName, String category) { this.displayName = displayName; this.category = category; }
    public String displayName() { return displayName; }
    public String category() { return category; }
}