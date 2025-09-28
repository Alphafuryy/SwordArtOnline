package com.skills;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Skill {
    void activate(Player player);
    String getName();
    String getDescription();
    int getCooldown();
    SkillCategory getCategory();
    String getPermission();

    // New method for weapon requirement check
    default boolean canActivate(Player player) {
        return hasRequiredWeapon(player);
    }

    default boolean hasRequiredWeapon(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        return WeaponRequirementManager.getInstance().canUseSkillWithWeapon(getCategory(), mainHand);
    }

    default String getWeaponRequirementMessage() {
        return WeaponRequirementManager.getInstance().getRequiredWeaponType(getCategory());
    }
}