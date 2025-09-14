package com.Abilitys;

import com.commands.SkillCommand;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import java.util.*;

public class AvalancheAbility implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Integer> chargeLevels = new HashMap<>();
    private final Map<UUID, BukkitRunnable> chargeTasks = new HashMap<>();
    private final Map<UUID, Integer> comboCounters = new HashMap<>();
    private final Map<UUID, Long> lastAttackTimes = new HashMap<>();
    private final Map<UUID, Boolean> isCharging = new HashMap<>();
    private final Map<UUID, Location> chargeStartLocations = new HashMap<>();
    private final Map<UUID, Long> lastAbilityUse = new HashMap<>();
    private final Map<UUID, Boolean> abilityUsedThisCombo = new HashMap<>();
    private final long CHARGE_TIME_PER_LEVEL = 20; // 1 second per level (20 ticks)
    private final long ABILITY_COOLDOWN = 5000; // 5 seconds cooldown
    private final long COMBO_TIMEOUT = 3000; // 3 seconds timeout for combo

    // SAO-themed colors and effects
    private final Color CHARGE_COLOR_1 = Color.fromRGB(135, 206, 250);   // Light Blue
    private final Color CHARGE_COLOR_2 = Color.fromRGB(30, 144, 255);    // Dodger Blue
    private final Color CHARGE_COLOR_3 = Color.fromRGB(0, 0, 139);       // Dark Blue
    private final Particle.DustOptions DUST_OPTIONS_1 = new Particle.DustOptions(CHARGE_COLOR_1, 2.0f);
    private final Particle.DustOptions DUST_OPTIONS_2 = new Particle.DustOptions(CHARGE_COLOR_2, 2.5f);
    private final Particle.DustOptions DUST_OPTIONS_3 = new Particle.DustOptions(CHARGE_COLOR_3, 3.0f);

    public AvalancheAbility(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        // Clean up any leftover data every minute
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanUpOldData();
            }
        }.runTaskTimer(plugin, 1200, 1200); // Run every minute
    }
    // Cancel offhand swapping if player holds the two-handed sword
    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (hasTwoHandedSword(player)) {
            event.setCancelled(true);
            sendActionBar(player, "§cCannot hold anything in offhand with this sword!");
        }
    }

    // Cancel changing main hand slot if it would break two-handed restriction
    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack offHand = player.getInventory().getItemInOffHand();
        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (isSword(newItem, player) && offHand != null && !offHand.getType().isAir()) {
            event.setCancelled(true);
            sendActionBar(player, "§cCannot hold a two-handed sword while something is in offhand!");
        }

    }

    // Optionally, cancel right-click placing items into offhand
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (hasTwoHandedSword(player) && event.getHand() == EquipmentSlot.OFF_HAND) {
            event.setCancelled(true);
            sendActionBar(player, "§cCannot put anything in offhand while wielding this sword!");
        }
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        // Only allow if item has Avalanche metadata

        UUID playerId = player.getUniqueId();

        // Check if player has already used ability this combo
        if (abilityUsedThisCombo.getOrDefault(playerId, false)) {
            return;
        }

        // Check cooldown
        if (lastAbilityUse.containsKey(playerId)) {
            long timeSinceLastUse = System.currentTimeMillis() - lastAbilityUse.get(playerId);
            if (timeSinceLastUse < ABILITY_COOLDOWN) {
                long remaining = (ABILITY_COOLDOWN - timeSinceLastUse) / 1000;
                sendActionBar(player, "§cAvalanche on cooldown: §6" + remaining + "s");
                return;
            }
        }

        // Player started sneaking - check if they can charge
        if (event.isSneaking()) {
            // Check if player is holding a two-handed sword
            if (!hasTwoHandedSword(player)) {
                sendActionBar(player, "§cRequires two-handed sword (no shield)");
                return;
            }

            // Check if player has full combo (3 hits)
            int comboCount = comboCounters.getOrDefault(playerId, 0);
            if (comboCount < 3) {
                sendActionBar(player, "§cNeed full combo (3 hits) for Avalanche!");
                return;
            }

            // Start charging
            startCharging(player);
            chargeStartLocations.put(playerId, player.getLocation().clone());
        }
        // Player stopped sneaking - release the ability
        else {
            if (isCharging.getOrDefault(playerId, false)) {
                releaseAvalanche(player, chargeLevels.getOrDefault(playerId, 0));
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player player = (Player) event.getDamager();
        UUID playerId = player.getUniqueId();

        // Reset ability usage if combo was reset
        int currentCombo = comboCounters.getOrDefault(playerId, 0);
        if (currentCombo == 0) {
            abilityUsedThisCombo.put(playerId, false);
        }

        // Update last attack time
        lastAttackTimes.put(playerId, System.currentTimeMillis());

        // Update combo counter
        currentCombo = comboCounters.getOrDefault(playerId, 0);
        comboCounters.put(playerId, Math.min(currentCombo + 1, 3)); // Cap at 3

        // Show combo indicator
        if (currentCombo > 0) {
            String comboText = "§aCombo: §e" + "✦".repeat(currentCombo) + "§7" + "✦".repeat(3 - currentCombo);
            sendActionBar(player, comboText);
        }

        // Reset combo after timeout
        int finalCurrentCombo = currentCombo;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (comboCounters.getOrDefault(playerId, 0) == finalCurrentCombo) {
                    comboCounters.put(playerId, 0);
                    abilityUsedThisCombo.put(playerId, false);
                    sendActionBar(player, "§cCombo reset!");
                }
            }
        }.runTaskLater(plugin, COMBO_TIMEOUT / 50); // Convert ms to ticks
    }

    private void startCharging(Player player) {
        UUID playerId = player.getUniqueId();

        // Cancel existing charge task if any
        if (chargeTasks.containsKey(playerId)) {
            chargeTasks.get(playerId).cancel();
            chargeTasks.remove(playerId);
        }

        // Set charging state
        isCharging.put(playerId, true);

        // Reset charge level
        chargeLevels.put(playerId, 0);

        // Play initial charge sound
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.5f);

        // Initial particle effect - blue energy swirl
        Location loc = player.getLocation().add(0, 0.5, 0);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 15, 0.5, 0.5, 0.5, 0.1);
        player.getWorld().spawnParticle(Particle.DUST, loc, 10, 0.5, 0.5, 0.5, 0.1, DUST_OPTIONS_1);

        // Start charge task
        BukkitRunnable chargeTask = new BukkitRunnable() {
            int chargeTicks = 0;

            @Override
            public void run() {
                // Check if player is still sneaking and holding a sword
                if (!player.isOnline() || !player.isSneaking() || !hasTwoHandedSword(player) ||
                        !isCharging.getOrDefault(playerId, false)) {
                    cancel();
                    chargeTasks.remove(playerId);
                    isCharging.put(playerId, false);
                    return;
                }

                chargeTicks++;

                // Update charge level every CHARGE_TIME_PER_LEVEL ticks
                int newChargeLevel = (int) (chargeTicks / CHARGE_TIME_PER_LEVEL) + 1;
                if (newChargeLevel > 3) newChargeLevel = 3; // Max level 3

                // If charge level increased
                if (newChargeLevel > chargeLevels.getOrDefault(playerId, 0)) {
                    chargeLevels.put(playerId, newChargeLevel);

                    // Play level up effect
                    Location eyeLoc = player.getEyeLocation();
                    player.getWorld().playSound(eyeLoc, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 0.8f + (newChargeLevel * 0.2f));

                    // Particle effect based on charge level
                    Particle.DustOptions dustOptions;
                    switch (newChargeLevel) {
                        case 1:
                            dustOptions = DUST_OPTIONS_1;
                            break;
                        case 2:
                            dustOptions = DUST_OPTIONS_2;
                            break;
                        case 3:
                            dustOptions = DUST_OPTIONS_3;
                            break;
                        default:
                            dustOptions = DUST_OPTIONS_1;
                    }

                    // Energy burst effect
                    Location center = player.getLocation().add(0, 1, 0);
                    for (int i = 0; i < 20; i++) {
                        double angle = 2 * Math.PI * i / 20;
                        double x = Math.cos(angle) * 1.5;
                        double z = Math.sin(angle) * 1.5;
                        Location particleLoc = center.clone().add(x, 0, z);
                        player.getWorld().spawnParticle(Particle.DUST, particleLoc, 2, 0.1, 0.1, 0.1, 0, dustOptions);
                    }

                    // Action bar message with SAO style
                    String levelText = "§bAvalanche Charge: §3" + "■".repeat(newChargeLevel) + "§8" + "■".repeat(3 - newChargeLevel);
                    sendActionBar(player, levelText);
                }

                // Continuous charging particles (swirling energy)
                if (chargeTicks % 5 == 0) {
                    Location center = player.getLocation().add(0, 1, 0);
                    double radius = 1.0 + (chargeLevels.getOrDefault(playerId, 0) * 0.3);
                    for (int i = 0; i < 12; i++) {
                        double angle = 2 * Math.PI * i / 12 + (chargeTicks * 0.1);
                        double x = Math.cos(angle) * radius;
                        double z = Math.sin(angle) * radius;
                        Location particleLoc = center.clone().add(x, 0.5, z);

                        Particle.DustOptions dustOptions;
                        switch (chargeLevels.getOrDefault(playerId, 0)) {
                            case 1:
                                dustOptions = DUST_OPTIONS_1;
                                break;
                            case 2:
                                dustOptions = DUST_OPTIONS_2;
                                break;
                            case 3:
                                dustOptions = DUST_OPTIONS_3;
                                break;
                            default:
                                dustOptions = DUST_OPTIONS_1;
                        }

                        player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions);
                        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0, 0, 0, 0.05);
                    }
                }
            }
        };

        chargeTask.runTaskTimer(plugin, 0, 1);
        chargeTasks.put(playerId, chargeTask);
    }

    private void releaseAvalanche(Player player, int chargeLevel) {
        UUID playerId = player.getUniqueId();

        // Cancel charging task
        if (chargeTasks.containsKey(playerId)) {
            chargeTasks.get(playerId).cancel();
            chargeTasks.remove(playerId);
        }

        isCharging.put(playerId, false);

        if (chargeLevel == 0) {
            // Only show message if player actually tried to charge
            if (chargeStartLocations.containsKey(playerId)) {
                sendActionBar(player, "§cAvalanche failed! Charge longer.");
                chargeStartLocations.remove(playerId);
            }
            return;
        }

        // Mark ability as used for this combo
        abilityUsedThisCombo.put(playerId, true);

        // Set cooldown
        lastAbilityUse.put(playerId, System.currentTimeMillis());

        Location loc = player.getLocation();
        Vector direction = loc.getDirection().normalize();

        // Get base damage from player's sword and attributes
        double baseDamage = calculateBaseDamage(player);

        // Effects based on charge level
        double damageMultiplier;
        double knockback;
        double range;
        double dashPower;
        Particle.DustOptions dustOptions;
        Sound sound;

        switch (chargeLevel) {
            case 1:
                damageMultiplier = 1.5;
                knockback = 1.0;
                range = 5.0;  // Increased from 4.0
                dashPower = 0.8;
                dustOptions = DUST_OPTIONS_1;
                sound = Sound.ENTITY_PLAYER_ATTACK_STRONG;
                break;
            case 2:
                damageMultiplier = 2.0;
                knockback = 1.5;
                range = 6.0;  // Increased from 5.0
                dashPower = 1.2;
                dustOptions = DUST_OPTIONS_2;
                sound = Sound.ENTITY_PLAYER_ATTACK_CRIT;
                break;
            case 3:
                damageMultiplier = 3.0;
                knockback = 2.0;
                range = 7.0;  // Increased from 6.0
                dashPower = 1.8;
                dustOptions = DUST_OPTIONS_3;
                sound = Sound.ENTITY_LIGHTNING_BOLT_IMPACT;
                break;
            default:
                damageMultiplier = 1.5;
                knockback = 1.0;
                range = 5.0;
                dashPower = 0.8;
                dustOptions = DUST_OPTIONS_1;
                sound = Sound.ENTITY_PLAYER_ATTACK_STRONG;
        }

        // Calculate bonus damage based on charge distance (SAO-style)
        Location chargeStart = chargeStartLocations.get(playerId);
        double chargeDistanceBonus = 0;
        if (chargeStart != null) {
            chargeDistanceBonus = chargeStart.distance(loc) * 0.5; // Bonus damage based on charge distance
        }
        chargeStartLocations.remove(playerId);

        // Calculate final damage with all bonuses
        double finalDamage = (baseDamage * damageMultiplier) + chargeDistanceBonus;

        // Visual effect: Sword raise and slam
        player.getWorld().playSound(loc, Sound.ITEM_TRIDENT_THUNDER, 1.5f, 0.8f);

        // Freeze player briefly for dramatic effect
        player.setFreezeTicks(10);

        // SAO-style dash forward - more powerful and direct
        Vector launchVector = direction.clone().multiply(dashPower).setY(0.2);
        player.setVelocity(launchVector);

        // Create dash trail effect
        createDashTrail(player, chargeLevel);

        // Delay the actual impact slightly for dramatic effect
        new BukkitRunnable() {
            @Override
            public void run() {
                // Damage and knockback in a cone in front of the player
                Location playerLoc = player.getLocation();
                Vector playerDir = playerLoc.getDirection().normalize();

                // Create avalanche effect (wave of energy)
                createAvalancheWave(player, playerLoc, playerDir, chargeLevel, range, finalDamage);

                int hitCount = 0;
                List<LivingEntity> hitEntities = new ArrayList<>();

                // Use a more reliable method to detect entities in a cone
                for (Entity entity : player.getWorld().getNearbyEntities(playerLoc, range, range/2, range)) {
                    if (entity instanceof LivingEntity target && target != player && !target.isDead()) {
                        // Check if entity is in the cone using more accurate math
                        if (isInCone(playerLoc, playerDir, target.getLocation(), 60.0)) { // 60 degree cone
                            // Calculate damage based on charge level and distance
                            double distance = target.getLocation().distance(playerLoc);
                            double distanceMultiplier = Math.max(0.3, 1 - (distance / (range * 1.5)));

                            // Apply damage with custom damage source for better feedback
                            target.damage(finalDamage * distanceMultiplier, player);

                            // Apply durability damage to armor (2x the damage dealt)
                            damageArmor(target, finalDamage * distanceMultiplier * 2);

                            // Custom knockback that pushes up and away
                            Vector toTarget = target.getLocation().toVector().subtract(playerLoc.toVector()).normalize();
                            Vector kbDirection = toTarget.multiply(knockback * distanceMultiplier)
                                    .add(new Vector(0, 0.4 * distanceMultiplier, 0));
                            target.setVelocity(kbDirection);

                            // Hit effect
                            player.getWorld().spawnParticle(Particle.CRIT,
                                    target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                            player.getWorld().playSound(target.getLocation(),
                                    Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.8f);

                            hitCount++;
                            hitEntities.add(target);
                        }
                    }
                }

                // Ground impact effect
                Location groundLoc = playerLoc.clone().add(playerDir.multiply(2));
                groundLoc.setY(player.getWorld().getHighestBlockYAt(groundLoc) + 0.1);

                player.getWorld().playSound(groundLoc, sound, 1.5f, 0.9f);

                // Impact particles based on charge level
                int particleCount = 30 + (chargeLevel * 20);
                player.getWorld().spawnParticle(Particle.CRIT, groundLoc, particleCount,
                        1.0 + chargeLevel * 0.5, 0.1, 1.0 + chargeLevel * 0.5, 0.2);

                player.getWorld().spawnParticle(Particle.DUST, groundLoc, particleCount,
                        1.0 + chargeLevel * 0.5, 0.5, 1.0 + chargeLevel * 0.5, 0.1, dustOptions);

                // Sword arc visual
                spawnSwordArc(player, chargeLevel);

                // SAO-style hit counter message
                if (hitCount > 0) {
                    String hitMessage = "§3Avalanche hit §b" + hitCount + "§3 enemies for §b" +
                            String.format("%.1f", finalDamage) + "§3 damage!";
                    player.sendMessage(hitMessage);
                }

                // Reset combo counter
                comboCounters.put(player.getUniqueId(), 0);
            }
        }.runTaskLater(plugin, 5);
    }

    private double calculateBaseDamage(Player player) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        double baseDamage = 5.0; // Default fist damage

        // Get base damage from weapon type
        if (isSword(weapon, player)) { // <-- pass ItemStack and Player
            switch (weapon.getType()) {
                case WOODEN_SWORD: baseDamage = 4.0; break;
                case STONE_SWORD: baseDamage = 5.0; break;
                case IRON_SWORD: baseDamage = 6.0; break;
                case GOLDEN_SWORD: baseDamage = 4.0; break;
                case DIAMOND_SWORD: baseDamage = 7.0; break;
                case NETHERITE_SWORD: baseDamage = 8.0; break;
            }
        }

        // Add Sharpness enchantment bonus
        if (weapon.hasItemMeta() && weapon.getItemMeta().hasEnchant(Enchantment.SHARPNESS)) {
            int sharpnessLevel = weapon.getItemMeta().getEnchantLevel(Enchantment.SHARPNESS);
            baseDamage += sharpnessLevel * 1.25;
        }

        // Add player's attack damage attribute
        if (player.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            baseDamage += player.getAttribute(Attribute.ATTACK_DAMAGE).getValue() - 1.0;
        }

        return baseDamage;
    }


    private void damageArmor(LivingEntity entity, double damageAmount) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) return;

        // Damage each armor piece
        damageArmorPiece(equipment.getHelmet(), damageAmount);
        damageArmorPiece(equipment.getChestplate(), damageAmount);
        damageArmorPiece(equipment.getLeggings(), damageAmount);
        damageArmorPiece(equipment.getBoots(), damageAmount);
    }

    private void damageArmorPiece(ItemStack armor, double damageAmount) {
        if (armor == null || armor.getType().isAir()) return;

        if (armor.getItemMeta() instanceof Damageable) {
            Damageable damageable = (Damageable) armor.getItemMeta();
            int currentDamage = damageable.getDamage();
            int newDamage = currentDamage + (int) damageAmount;

            // Check if armor would break
            int maxDurability = armor.getType().getMaxDurability();
            if (newDamage >= maxDurability) {
                // Break the armor
                if (armor.getAmount() > 1) {
                    armor.setAmount(armor.getAmount() - 1);
                } else {
                    armor.setType(Material.AIR);
                }
            } else {
                // Apply damage
                damageable.setDamage(newDamage);
                armor.setItemMeta((ItemMeta) damageable);
            }
        }
    }

    private boolean isInCone(Location source, Vector direction, Location target, double angleDegrees) {
        Vector toTarget = target.toVector().subtract(source.toVector()).normalize();
        double dot = direction.dot(toTarget);
        double angle = Math.toDegrees(Math.acos(dot));
        return angle <= angleDegrees;
    }

    private void createDashTrail(Player player, int chargeLevel) {
        // Create a trail of particles behind the player during the dash
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 10) { // Run for 10 ticks (0.5 seconds)
                    cancel();
                    return;
                }

                Location loc = player.getLocation().subtract(0, 0.5, 0);

                Particle.DustOptions dustOptions;
                switch (chargeLevel) {
                    case 1:
                        dustOptions = DUST_OPTIONS_1;
                        break;
                    case 2:
                        dustOptions = DUST_OPTIONS_2;
                        break;
                    case 3:
                        dustOptions = DUST_OPTIONS_3;
                        break;
                    default:
                        dustOptions = DUST_OPTIONS_1;
                }

                // Create a trail behind the player
                Vector direction = player.getLocation().getDirection().normalize().multiply(-1);
                for (int i = 0; i < 5; i++) {
                    double offsetX = (Math.random() - 0.5) * 0.5;
                    double offsetY = Math.random() * 0.5;
                    double offsetZ = (Math.random() - 0.5) * 0.5;

                    Location particleLoc = loc.clone().add(offsetX, offsetY, offsetZ);
                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, dustOptions);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void createAvalancheWave(Player player, Location start, Vector direction, int chargeLevel, double range, double damage) {
        // Create a wave of energy that moves forward
        new BukkitRunnable() {
            double distance = 0;
            Set<UUID> hitEntities = new HashSet<>();

            @Override
            public void run() {
                if (distance > range) {
                    cancel();
                    return;
                }

                Location waveFront = start.clone().add(direction.clone().multiply(distance));

                // Spawn particles in a circle at the wave front
                for (int i = 0; i < 16; i++) {
                    double angle = 2 * Math.PI * i / 16;
                    double radius = 1.5 * (1 - (distance / range)); // Cone shape
                    Vector offset = new Vector(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);

                    // Rotate offset to match direction
                    offset = rotateVector(offset, direction);

                    Location particleLoc = waveFront.clone().add(offset);

                    Particle.DustOptions dustOptions;
                    switch (chargeLevel) {
                        case 1:
                            dustOptions = DUST_OPTIONS_1;
                            break;
                        case 2:
                            dustOptions = DUST_OPTIONS_2;
                            break;
                        case 3:
                            dustOptions = DUST_OPTIONS_3;
                            break;
                        default:
                            dustOptions = DUST_OPTIONS_1;
                    }

                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 2, 0, 0, 0, 0, dustOptions);
                }

                // Check for entities to damage using improved cone detection
                for (Entity entity : player.getWorld().getNearbyEntities(waveFront, 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity && entity != player && !hitEntities.contains(entity.getUniqueId())) {
                        LivingEntity target = (LivingEntity) entity;

                        // Use cone detection instead of simple radius
                        if (isInCone(start, direction, target.getLocation(), 60.0)) {
                            // Calculate damage based on distance
                            double distanceMultiplier = Math.max(0.3, 1 - (distance / range));
                            double finalDamage = damage * distanceMultiplier;

                            target.damage(finalDamage, player);

                            // Apply durability damage to armor (2x the damage dealt)
                            damageArmor(target, finalDamage * 2);

                            hitEntities.add(entity.getUniqueId());

                            // Knockback
                            Vector kbDir = target.getLocation().toVector().subtract(waveFront.toVector()).normalize();
                            target.setVelocity(kbDir.multiply(chargeLevel * 0.5).setY(0.3));
                        }
                    }
                }

                distance += 0.5;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private Vector rotateVector(Vector vector, Vector direction) {
        // Simple rotation to align with direction vector
        Vector axis = new Vector(0, 1, 0).crossProduct(direction);
        double angle = Math.acos(new Vector(0, 1, 0).dot(direction));
        return vector.clone().rotateAroundAxis(axis, angle);
    }

    private void spawnSwordArc(Player player, int chargeLevel) {
        Location loc = player.getLocation();
        Vector forward = loc.getDirection().normalize();
        Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();

        Particle.DustOptions dustOptions;
        int count;
        double arcSize;

        switch (chargeLevel) {
            case 1:
                dustOptions = DUST_OPTIONS_1;
                count = 8;
                arcSize = Math.PI / 3; // 60 degrees
                break;
            case 2:
                dustOptions = DUST_OPTIONS_2;
                count = 12;
                arcSize = Math.PI / 2; // 90 degrees
                break;
            case 3:
                dustOptions = DUST_OPTIONS_3;
                count = 16;
                arcSize = 2 * Math.PI / 3; // 120 degrees
                break;
            default:
                dustOptions = DUST_OPTIONS_1;
                count = 8;
                arcSize = Math.PI / 3;
        }

        // Create an arc of particles in front of the player
        for (double angle = -arcSize/2; angle <= arcSize/2; angle += arcSize/count) {
            double x = Math.cos(angle) * 3;
            double z = Math.sin(angle) * 3;

            Vector offset = right.clone().multiply(x).add(forward.clone().multiply(z));
            Location particleLoc = loc.clone().add(0, 1, 0).add(offset);

            player.getWorld().spawnParticle(Particle.DUST, particleLoc, 3, 0.1, 0.1, 0.1, 0, dustOptions);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, particleLoc, 1, 0, 0, 0, 0.05);
        }
    }

    private boolean hasTwoHandedSword(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        // Two-handed sword requires:
        // 1. Sword in main hand with Avalanche metadata
        // 2. Offhand must be empty
        boolean hasSword = isSword(mainHand, player);
        boolean offHandEmpty = offHand == null || offHand.getType().isAir();

        return hasSword && offHandEmpty;
    }


    private boolean isSword(ItemStack item, Player player) {
        if (item == null || item.getType().isAir()) return false;
        if (!item.getType().name().endsWith("_SWORD")) return false;
        return SkillCommand.hasMetadata(player, "Avalanche");
    }

    private void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
    }

    private void cleanUpOldData() {
        // Clean up data for players who are no longer online
        Set<UUID> onlinePlayers = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlinePlayers.add(player.getUniqueId());
        }

        chargeLevels.keySet().removeIf(id -> !onlinePlayers.contains(id));

        // Cancel and remove charge tasks
        for (UUID id : new HashSet<>(chargeTasks.keySet())) {
            if (!onlinePlayers.contains(id)) {
                chargeTasks.get(id).cancel();
                chargeTasks.remove(id);
            }
        }

        comboCounters.keySet().removeIf(id -> !onlinePlayers.contains(id));
        lastAttackTimes.keySet().removeIf(id -> !onlinePlayers.contains(id));
        isCharging.keySet().removeIf(id -> !onlinePlayers.contains(id));
        chargeStartLocations.keySet().removeIf(id -> !onlinePlayers.contains(id));
        lastAbilityUse.keySet().removeIf(id -> !onlinePlayers.contains(id) ||
                System.currentTimeMillis() - lastAbilityUse.get(id) > 60000); // Remove if cooldown expired
        abilityUsedThisCombo.keySet().removeIf(id -> !onlinePlayers.contains(id));
    }
}