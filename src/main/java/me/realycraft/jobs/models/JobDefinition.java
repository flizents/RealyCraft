package me.realycraft.jobs.models;

import org.bukkit.Material;

import java.util.List;
import java.util.Set;

public final class JobDefinition {

    private final String key;
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final double salaryPerBlock;
    private final List<Material> blocks;
    private final Set<String> worlds;

    public JobDefinition(String key, String displayName, Material icon, int slot, double salaryPerBlock, List<Material> blocks, Set<String> worlds) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.slot = slot;
        this.salaryPerBlock = salaryPerBlock;
        this.blocks = blocks;
        this.worlds = worlds;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getSlot() {
        return slot;
    }

    public double getSalaryPerBlock() {
        return salaryPerBlock;
    }

    public List<Material> getBlocks() {
        return blocks;
    }

    public Set<String> getWorlds() {
        return worlds;
    }

    public boolean supportsWorld(String worldName) {
        return worlds.isEmpty() || worlds.contains(worldName);
    }

    public boolean supportsBlock(Material material) {
        return blocks.contains(material);
    }
}
