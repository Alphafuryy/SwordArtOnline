package com.Abilitys;

import com.commands.SkillCommand;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DoubleCleaveAbility implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Integer> hitCounters = new HashMap<>();
    private final Map<UUID, Long> lastActivation = new HashMap<>();
    private final int TRIGGER_HITS = 3;
    private final double CLEAVE_RANGE = 4.5;
    private final float CLEAVE_KNOCKBACK = 0.6f;
    private final double DAMAGE_MULTIPLIER = 1.8;
    private final long COOLDOWN_MS = 8000; // 8 second cooldown

    public DoubleCleaveAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        if (!SkillCommand.hasMetadata(player, "DoubleCleave")) return;

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Check custom 5-second cooldown
        if (lastActivation.containsKey(playerId)) {
            long lastTime = lastActivation.get(playerId);
            if (now - lastTime < 5000) { // 5000ms = 5 seconds
                long remaining = (5000 - (now - lastTime)) / 1000;
                player.sendActionBar(ChatColor.RED + "Double Cleave cooldown: " + remaining + "s");
                return;
            }
        }

        // Only count hits if attack cooldown > 0.9 (90% charged)
        if (player.getAttackCooldown() < 0.9f) return;

        int hits = hitCounters.getOrDefault(playerId, 0) + 1;

        if (hits >= TRIGGER_HITS) {
            hitCounters.put(playerId, 0);
            lastActivation.put(playerId, now); // set cooldown timestamp
            executeDoubleCleave(player, event.getDamage());
            event.setDamage(event.getDamage() * 1.2); // Bonus damage on triggering hit
        } else {
            hitCounters.put(playerId, hits);
            player.sendActionBar(ChatColor.BLUE + "Double Cleave: " + (TRIGGER_HITS - hits) + " hits remaining");
        }
    }



    private void executeDoubleCleave(Player player, double baseDamage) {
        // SAO Skill Activation Effects
        playActivationEffects(player);

        // Calculate enhanced damage
        double enhancedDamage = baseDamage * DAMAGE_MULTIPLIER;

        // First cleave (right side)
        performCleave(player, enhancedDamage, 1, 0);

        // Second cleave (left side) with SAO combo timing
    }

    private void performCleave(Player player, double damage, int direction, int delayTicks) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        Vector forward = loc.getDirection().setY(0).normalize();

        // Calculate perpendicular direction for the cleave
        Vector perpendicular = new Vector(-forward.getZ(), 0, forward.getX()).normalize().multiply(direction);

        // SAO Skill Visuals and Sound
        if (delayTicks == 0) {
            createSwordArc(player, loc, forward, perpendicular);
            playSwordSkillSounds(loc);
        } else {
            // Delayed effects for the second cleave
            new BukkitRunnable() {
                @Override
                public void run() {
                    createSwordArc(player, loc, forward, perpendicular);
                    playSwordSkillSounds(loc);
                }
            }.runTaskLater(plugin, delayTicks);
        }

        // Find targets in cleave arc
        for (Entity entity : world.getNearbyEntities(loc, CLEAVE_RANGE, CLEAVE_RANGE, CLEAVE_RANGE)) {
            if (!(entity instanceof LivingEntity) || entity == player || entity.isDead()) continue;

            LivingEntity target = (LivingEntity) entity;
            Vector toTarget = target.getLocation().subtract(loc).toVector().setY(0).normalize();

            // Check if target is within the cleave arc (90 degree cone)
            double dotProduct = forward.dot(toTarget);
            if (dotProduct > 0.7) { // Approximately 45 degrees each side
                if (delayTicks == 0) {
                    applyCleaveHit(player, target, damage, toTarget);
                } else {
                    // Delayed hit for the second cleave
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            applyCleaveHit(player, target, damage, toTarget);
                        }
                    }.runTaskLater(plugin, delayTicks);
                }
            }
        }
    }

    private void applyCleaveHit(Player player, LivingEntity target, double damage, Vector direction) {
        // Apply damage
        target.damage(damage, player);

        // SAO-style knockback (slightly upward)
        Vector kb = direction.multiply(CLEAVE_KNOCKBACK).setY(0.3);
        target.setVelocity(kb);

        // Hit effects
        Location hitLoc = target.getLocation().add(0, 1, 0);
        World world = target.getWorld();

        // SAO hit particles - using GLOW and ELECTRIC_SPARK instead of REDSTONE
        world.spawnParticle(Particle.GLOW, hitLoc, 20, 0.3, 0.3, 0.3, 0.1);
        world.spawnParticle(Particle.ELECTRIC_SPARK, hitLoc, 12, 0.2, 0.2, 0.2, 0.1);
        world.spawnParticle(Particle.CRIT, hitLoc, 8, 0.2, 0.2, 0.2, 0.5);

        // SAO hit sound
        world.playSound(hitLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.9f, 1.5f);
        world.playSound(hitLoc, Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.7f, 1.7f);
        world.playSound(hitLoc, Sound.ITEM_TRIDENT_HIT, 0.5f, 1.3f);

        // SAO damage number effect
        createDamageNumber(target, damage);
    }

    private void createSwordArc(Player player, Location center, Vector forward, Vector perpendicular) {
        World world = player.getWorld();
        Location effectLoc = center.clone().add(0, 1.2, 0);

        // Sword arc particles - using GLOW and ELECTRIC_SPARK for SAO blue energy trail
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI / 12;
            double radius = CLEAVE_RANGE * (0.5 + (i * 0.02));

            Vector point = forward.clone().multiply(Math.cos(angle) * radius)
                    .add(perpendicular.clone().multiply(Math.sin(angle) * radius));

            Location particleLoc = effectLoc.clone().add(point);

            // SAO blue energy particles with variation
            if (i % 3 == 0) {
                // Light blue effect
                world.spawnParticle(Particle.GLOW, particleLoc, 3, 0.1, 0.1, 0.1, 0.05);
            } else {
                // Blue effect
                world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 2, 0.05, 0.05, 0.05, 0.03);
            }

            world.spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0, 0, 0, 0.05);

            if (i % 4 == 0) {
                // Bright flash particles
                world.spawnParticle(Particle.FLASH, particleLoc, 1);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.1, 0.1, 0.1, 0);
            }
        }

        // Sword sweep effect - more pronounced
        world.spawnParticle(Particle.SWEEP_ATTACK, effectLoc, 8, 0.8, 0.8, 0.8, 0);

        // Energy burst at player
        for (int i = 0; i < 10; i++) {
            double angle = i * Math.PI / 5;
            double radius = 0.5;
            Location burstLoc = effectLoc.clone().add(
                    Math.cos(angle) * radius,
                    Math.sin(angle) * 0.5,
                    Math.sin(angle) * radius
            );
            world.spawnParticle(Particle.GLOW, burstLoc, 2, 0, 0, 0, 0.1);
        }
    }

    private void playSwordSkillSounds(Location loc) {
        World world = loc.getWorld();
        // SAO sword skill activation sounds - more immersive
        world.playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 1.2f, 1.8f);
        world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.4f);
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.9f);
        world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.7f, 2.0f);
    }

    private void playActivationEffects(Player player) {
        Location loc = player.getLocation();
        World world = player.getWorld();

        // SAO skill activation circle - using GLOW and ELECTRIC_SPARK for energy ring
        for (int i = 0; i < 36; i++) {
            double angle = i * Math.PI / 18;
            double height = 0.1 + (i % 3) * 0.2;
            Location circleLoc = loc.clone().add(Math.cos(angle) * 2.0, height, Math.sin(angle) * 2.0);

            if (i % 3 == 0) {
                // Light blue particles
                world.spawnParticle(Particle.GLOW, circleLoc, 2, 0, 0, 0, 0.1);
            } else {
                // Blue particles
                world.spawnParticle(Particle.ELECTRIC_SPARK, circleLoc, 2, 0, 0, 0, 0.05);
            }

            world.spawnParticle(Particle.ELECTRIC_SPARK, circleLoc, 1, 0, 0.1, 0, 0.05);
        }

        // Vertical energy column
        for (int i = 0; i < 10; i++) {
            Location columnLoc = loc.clone().add(0, i * 0.3, 0);
            world.spawnParticle(Particle.GLOW, columnLoc, 3, 0.2, 0, 0.2, 0.1);
        }

        // SAO skill name activation sounds
        world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.7f);
        world.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f, 1.2f);
        world.playSound(loc, Sound.ITEM_TRIDENT_RETURN, 1.0f, 1.9f);
        world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

        // Player visual feedback with SAO-style title
        player.sendActionBar(ChatColor.AQUA + "✦ Double Cleave activated! ✦");

        // Apply brief speed boost for SAO skill feeling
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SPEED, 40, 0, true, false, false));
    }

    private void createDamageNumber(LivingEntity target, double damage) {
        Location loc = target.getLocation().add(0, 2.2, 0);
        World world = target.getWorld();

        // SAO-style damage numbers with particle variation based on damage
        int damageValue = (int) damage;

        // Damage number particles (floating numbers)
        for (int i = 0; i < 5; i++) {
            Location numLoc = loc.clone().add((Math.random() - 0.5) * 0.5, i * 0.2, (Math.random() - 0.5) * 0.5);
            if (damageValue > 15) {
                // Critical hit - yellow particles
                world.spawnParticle(Particle.GLOW, numLoc, 3, 0.1, 0.1, 0.1, 0.1);
            } else {
                // Normal hit - blue particles
                world.spawnParticle(Particle.ELECTRIC_SPARK, numLoc, 2, 0.1, 0.1, 0.1, 0.05);
            }
        }

        // Critical hit sparkle effect for high damage
        if (damageValue > 15) {
            world.spawnParticle(Particle.FIREWORK, loc, 10, 0.3, 0.3, 0.3, 0.2);
            world.spawnParticle(Particle.FLASH, loc, 1);
            world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.8f, 1.7f);
        }
    }

    // Clean up when player quits
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        hitCounters.remove(playerId);
        lastActivation.remove(playerId);
    }

    // Reset all data on plugin disable
    public void cleanup() {
        hitCounters.clear();
        lastActivation.clear();
    }
}