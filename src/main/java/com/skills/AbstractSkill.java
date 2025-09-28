package com.skills;

import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

public abstract class AbstractSkill implements Skill {
    protected final String name;
    protected final String description;
    protected final int cooldown;
    protected final SkillCategory category;
    protected final String permission;

    public AbstractSkill(String name, String description, int cooldown, SkillCategory category) {
        this.name = name;
        this.description = description;
        this.cooldown = cooldown;
        this.category = category;
        this.permission = "sao.skill." + name.toLowerCase().replace(" ", "");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getCooldown() {
        return cooldown;
    }

    @Override
    public SkillCategory getCategory() {
        return category;
    }

    @Override
    public String getPermission() {
        return permission;
    }

    @Override
    public boolean canActivate(Player player) {
        // Check both permission and weapon requirement
        boolean hasPermission = player.hasPermission(getPermission());
        boolean hasWeapon = hasRequiredWeapon(player);

        if (!hasPermission) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this skill! (" + getPermission() + ")");
            return false;
        }

        if (!hasWeapon) {
            player.sendMessage(ChatColor.RED + "You need a " + getWeaponRequirementMessage() + " to use this skill!");
            return false;
        }

        return true;
    }
}