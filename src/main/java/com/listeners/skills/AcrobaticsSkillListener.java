package com.listeners.skills;

import com.managers.SkillManager;
import com.utils.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class AcrobaticsSkillListener implements Listener {

    private final SkillManager skillManager;

    public AcrobaticsSkillListener(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if player is jumping (vertical movement upward)
        if (isJumping(event)) {
            double jumpExperience = calculateJumpExperience(player);
            skillManager.addSkillExperience(player.getUniqueId(), SkillType.ACROBATICS, jumpExperience);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Check if damage is from falling
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            double originalDamage = event.getDamage();
            int acrobaticsLevel = skillManager.getSkillLevel(player.getUniqueId(), SkillType.ACROBATICS);

            // Calculate damage reduction based on acrobatics level (percentage-based)
            double damageReductionPercent = calculateDamageReductionPercent(acrobaticsLevel);
            double reducedDamage = originalDamage * (1 - damageReductionPercent);

            // Apply reduced damage (minimum 0.5 damage to prevent complete immunity too early)
            if (reducedDamage < 0.5 && originalDamage > 0) {
                reducedDamage = 0.5;
            }

            event.setDamage(reducedDamage);

            // Grant experience based on damage reduced
            double fallExperience = calculateFallExperience(originalDamage, reducedDamage);
            skillManager.addSkillExperience(player.getUniqueId(), SkillType.ACROBATICS, fallExperience);

            // Send message if significant damage reduction occurred
            }

    }

    private boolean isJumping(PlayerMoveEvent event) {
        // Check if player is moving upward (jumping) and not in water/lava
        Vector from = event.getFrom().toVector();
        Vector to = event.getTo().toVector();

        if (to.getY() > from.getY()) {
            Player player = event.getPlayer();
            return !player.isInWater() && !player.isInLava() && player.getVelocity().getY() > 0;
        }
        return false;
    }

    private double calculateJumpExperience(Player player) {
        // Base experience for jumping
        double baseExp = 1.0;

        // Additional experience based on jump height
        double jumpVelocity = Math.abs(player.getVelocity().getY());
        double velocityBonus = jumpVelocity * 2.0;

        // No level scaling penalty - consistent XP gain
        return baseExp + velocityBonus;
    }

    private double calculateDamageReductionPercent(int acrobaticsLevel) {
        if (acrobaticsLevel <= 0) return 0;

        // Progressive damage reduction with diminishing returns
        if (acrobaticsLevel <= 100) {
            // Early levels: 0.45% per level up to 45%
            return Math.min(0.45, acrobaticsLevel * 0.0045);
        } else if (acrobaticsLevel <= 500) {
            // Mid levels: 45% + 0.0875% per level up to 80%
            return Math.min(0.80, 0.45 + (acrobaticsLevel - 100) * 0.000875);
        } else {
            // High levels: 80% + 0.03% per level up to 95%
            return Math.min(0.95, 0.80 + (acrobaticsLevel - 500) * 0.0003);
        }
    }

    private double calculateFallExperience(double originalDamage, double reducedDamage) {
        if (originalDamage <= 0) return 0;

        double damageReduced = originalDamage - reducedDamage;

        // Base experience based on damage reduced (no level scaling)
        double baseExp = damageReduced * 2.0;

        // Bonus for percentage reduction
        double damageReductionPercent = damageReduced / originalDamage;
        double percentageBonus = damageReductionPercent * 15;

        return baseExp + percentageBonus;
    }

    // Utility method to get current damage reduction percentage for a player
    public double getCurrentDamageReductionPercent(Player player) {
        int acrobaticsLevel = skillManager.getSkillLevel(player.getUniqueId(), SkillType.ACROBATICS);
        return calculateDamageReductionPercent(acrobaticsLevel);
    }

    // Utility method to get maximum possible damage reduction
    public double getMaxDamageReductionPercent() {
        return calculateDamageReductionPercent(1000);
    }

    // Utility method to calculate reduced damage for display purposes
    public double calculateReducedDamage(double originalDamage, int acrobaticsLevel) {
        double reductionPercent = calculateDamageReductionPercent(acrobaticsLevel);
        double reducedDamage = originalDamage * (1 - reductionPercent);
        return Math.max(0.5, reducedDamage);
    }
}