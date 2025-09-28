package com.skills;

import com.SwordArtOnline;
import com.google.common.collect.Multimap;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LeapIntoTheFutureAbility {

    public static final Map<UUID, Long> damageReductionPlayers = new HashMap<>();
    public static final Map<UUID, Double> damageReductionAmount = new HashMap<>();

    public static void activateAbility(Player player) {
        // Get the item player is holding
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        // Check if player is holding an item
        if (heldItem.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "You must be holding a weapon to use this skill!");
            return;
        }

        // Check cooldown based on held item
        if (player.hasCooldown(heldItem.getType())) {
            long remaining = player.getCooldown(heldItem.getType());
            player.sendMessage(ChatColor.RED + "Ability on cooldown! " + (remaining / 20) + "s remaining");
            return;
        }

        // Set cooldown on the held item (12 seconds)
        player.setCooldown(heldItem.getType(), 12 * 20);

        // Play activation sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

        // Start activation sequence (1.1 seconds)

        // Prevent player movement during activation
        player.setWalkSpeed(0.0f); // Stop movement
        player.setFlySpeed(0.0f);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 30) { // 1.1 seconds (20 ticks per second)
                    // Restore movement speed
                    player.setWalkSpeed(0.2f); // Default walk speed
                    player.setFlySpeed(0.1f);  // Default fly speed

                    executeSlash(player);
                    this.cancel();
                    return;
                }

                ticks++;
            }
        }.runTaskTimer(SwordArtOnline.getInstance(), 0L, 1L);
    }

    private static void executeSlash(Player player) {
        Location playerLoc = player.getLocation();
        Vector direction = playerLoc.getDirection();

        // Play slash sounds
        player.getWorld().playSound(playerLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.2f);
        player.getWorld().playSound(playerLoc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 2.0f);
        player.swingMainHand();

        // Create SAO-themed green cone-shaped slash effect
        createSAOSlashCone(player, playerLoc, direction);

        // Detect single target in a 120° cone, 3m range
        LivingEntity target = findSingleTargetInCone(player, playerLoc, direction, 3.0, 120.0);

        if (target != null) {
            // Get the player's held item
            ItemStack heldItem = player.getInventory().getItemInMainHand();
            double baseDamage = 1.0; // default if no weapon found

            if (heldItem != null && heldItem.getType() != Material.AIR) {
                // Get attack damage from item modifiers
                Multimap<Attribute, AttributeModifier> modifiers = heldItem.getItemMeta().getAttributeModifiers();
                if (modifiers != null && modifiers.containsKey(Attribute.ATTACK_DAMAGE)) {
                    baseDamage = 0.0;
                    for (AttributeModifier mod : modifiers.get(Attribute.ATTACK_DAMAGE)) {
                        baseDamage += mod.getAmount();
                    }
                    // Add default hand damage
                    baseDamage += 1.0;
                }
            }

            // Apply 173% damage multiplier (1.73× base damage)
            double finalDamage = baseDamage * 3.73;
            target.damage(finalDamage, player);

            // Apply 20-second attack nerf to target (players or mobs)
            applyDamageReductionToTarget(target);
        }
    }



    private static void createSAOSlashCone(Player player, Location center, Vector direction) {
        Particle.DustOptions greenDust = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.5f);

        // Flatten direction so it ignores pitch (no up/down)
        direction.setY(0).normalize();

        // Y level = player's body (about chest height)
        double yOffset = 1.0;

        new BukkitRunnable() {
            double step = 0;

            @Override
            public void run() {
                if (step > 3.0) { // Stop after 3 blocks forward
                    this.cancel();
                    return;
                }

                // Forward vector scaled by current step
                Vector forward = direction.clone().multiply(step);

                // Sweep across ±60° (120° cone)
                for (double angle = -60; angle <= 60; angle += 10) {
                    Vector rotated = rotateAroundY(forward.clone(), Math.toRadians(angle));
                    Location particleLoc = center.clone().add(rotated);
                    particleLoc.setY(center.getY() + yOffset); // lock Y to body

                    // SAO slash particles

                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0.05, 0.05, 0.05, 0, greenDust);
                }

                step += 0.3; // controls wave speed
            }
        }.runTaskTimer(SwordArtOnline.getInstance(), 0L, 1L);
    }

    // Rotate vector around Y axis
    private static Vector rotateAroundY(Vector vec, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double x = vec.getX() * cos - vec.getZ() * sin;
        double z = vec.getX() * sin + vec.getZ() * cos;
        return new Vector(x, vec.getY(), z);
    }

    private static LivingEntity findSingleTargetInCone(Player player, Location center, Vector direction, double range, double angleDegrees) {
        LivingEntity closestTarget = null;
        double closestDistance = range + 1.0;
        double angleRad = Math.toRadians(angleDegrees / 2);

        for (Entity entity : center.getWorld().getNearbyEntities(center, range, range, range)) {
            if (!(entity instanceof LivingEntity) || entity == player) continue;

            LivingEntity livingEntity = (LivingEntity) entity;
            Vector toEntity = livingEntity.getLocation().toVector().subtract(center.toVector());

            // Check distance
            double distance = toEntity.length();
            if (distance > range) continue;

            // Check angle (cone check)
            double entityAngle = direction.angle(toEntity);
            if (entityAngle <= angleRad) {
                // Find the closest target
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestTarget = livingEntity;
                }
            }
        }

        return closestTarget;
    }

    private static void applyDamageReductionToTarget(LivingEntity target) {
        UUID targetId = target.getUniqueId();

        // Random reduction between 25% and 50%
        Random random = new Random();
        double reduction = 25 + (random.nextDouble() * 25);
        double multiplier = 1.0 - (reduction / 100.0);

        damageReductionPlayers.put(targetId, System.currentTimeMillis());
        damageReductionAmount.put(targetId, multiplier);

        // Visual effects only for players
        if (target instanceof Player targetPlayer) {
            targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 20, 0));

        }

        // Remove effect after 20 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                damageReductionPlayers.remove(targetId);
                damageReductionAmount.remove(targetId);

                if (target instanceof Player targetPlayer) {
                    targetPlayer.removePotionEffect(PotionEffectType.GLOWING);
                }
            }
        }.runTaskLater(SwordArtOnline.getInstance(), 20 * 20);
    }


    public static boolean hasDamageReduction(Player player) {
        return damageReductionPlayers.containsKey(player.getUniqueId());
    }

    public static double getDamageReductionMultiplier(Player player) {
        UUID playerId = player.getUniqueId();
        return damageReductionAmount.getOrDefault(playerId, 1.0);
    }



    // Add this method to remove damage reduction
    public static void removeDamageReduction(Player player) {
        UUID playerId = player.getUniqueId();
        damageReductionPlayers.remove(playerId);
        damageReductionAmount.remove(playerId);
        player.removePotionEffect(PotionEffectType.GLOWING);
    }
}