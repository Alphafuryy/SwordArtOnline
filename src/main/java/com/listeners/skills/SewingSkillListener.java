package com.listeners.skills;

import com.managers.SkillManager;
import com.utils.SkillType;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

public class SewingSkillListener implements Listener {

    private final SkillManager skillManager;
    private final int MAX_LEVEL;
    private final JavaPlugin plugin;
    private final Random random = new Random();

    // Configuration options
    private final double MAX_DOUBLE_CHANCE = 0.25; // 25% max chance
    private final double MAX_REFUND_CHANCE = 0.20; // 20% max chance
    private final double REFUND_PERCENTAGE = 0.50; // 50% of items refunded

    // Sound configuration
    private final Sound DOUBLE_OUTPUT_SOUND = Sound.UI_LOOM_TAKE_RESULT;
    private final Sound REFUND_SOUND = Sound.ENTITY_SHEEP_SHEAR;
    private final Sound LEVEL_UP_SOUND = Sound.ENTITY_PLAYER_LEVELUP;

    // Volume and pitch settings
    private final float DOUBLE_OUTPUT_VOLUME = 1.0f;
    private final float DOUBLE_OUTPUT_PITCH = 1.2f;
    private final float REFUND_VOLUME = 0.8f;
    private final float REFUND_PITCH = 1.0f;

    // Set of all wool-related materials for efficient lookup
    private static final Set<Material> WOOL_ITEMS = EnumSet.of(
            // Wool blocks
            Material.WHITE_WOOL, Material.ORANGE_WOOL, Material.MAGENTA_WOOL,
            Material.LIGHT_BLUE_WOOL, Material.YELLOW_WOOL, Material.LIME_WOOL,
            Material.PINK_WOOL, Material.GRAY_WOOL, Material.LIGHT_GRAY_WOOL,
            Material.CYAN_WOOL, Material.PURPLE_WOOL, Material.BLUE_WOOL,
            Material.BROWN_WOOL, Material.GREEN_WOOL, Material.RED_WOOL,
            Material.BLACK_WOOL,

            // Carpets
            Material.WHITE_CARPET, Material.ORANGE_CARPET, Material.MAGENTA_CARPET,
            Material.LIGHT_BLUE_CARPET, Material.YELLOW_CARPET, Material.LIME_CARPET,
            Material.PINK_CARPET, Material.GRAY_CARPET, Material.LIGHT_GRAY_CARPET,
            Material.CYAN_CARPET, Material.PURPLE_CARPET, Material.BLUE_CARPET,
            Material.BROWN_CARPET, Material.GREEN_CARPET, Material.RED_CARPET,
            Material.BLACK_CARPET,

            // Beds
            Material.WHITE_BED, Material.ORANGE_BED, Material.MAGENTA_BED,
            Material.LIGHT_BLUE_BED, Material.YELLOW_BED, Material.LIME_BED,
            Material.PINK_BED, Material.GRAY_BED, Material.LIGHT_GRAY_BED,
            Material.CYAN_BED, Material.PURPLE_BED, Material.BLUE_BED,
            Material.BROWN_BED, Material.GREEN_BED, Material.RED_BED,
            Material.BLACK_BED,

            // Banners
            Material.WHITE_BANNER, Material.ORANGE_BANNER, Material.MAGENTA_BANNER,
            Material.LIGHT_BLUE_BANNER, Material.YELLOW_BANNER, Material.LIME_BANNER,
            Material.PINK_BANNER, Material.GRAY_BANNER, Material.LIGHT_GRAY_BANNER,
            Material.CYAN_BANNER, Material.PURPLE_BANNER, Material.BLUE_BANNER,
            Material.BROWN_BANNER, Material.GREEN_BANNER, Material.RED_BANNER,
            Material.BLACK_BANNER,
            Material.WHITE_WALL_BANNER, Material.ORANGE_WALL_BANNER, Material.MAGENTA_WALL_BANNER,
            Material.LIGHT_BLUE_WALL_BANNER, Material.YELLOW_WALL_BANNER, Material.LIME_WALL_BANNER,
            Material.PINK_WALL_BANNER, Material.GRAY_WALL_BANNER, Material.LIGHT_GRAY_WALL_BANNER,
            Material.CYAN_WALL_BANNER, Material.PURPLE_WALL_BANNER, Material.BLUE_WALL_BANNER,
            Material.BROWN_WALL_BANNER, Material.GREEN_WALL_BANNER, Material.RED_WALL_BANNER,
            Material.BLACK_WALL_BANNER,

            // Wool armor
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE,
            Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,

            // Other wool items
            Material.PAINTING, Material.ITEM_FRAME, Material.GLOW_ITEM_FRAME,
            Material.LOOM, Material.SPYGLASS,

            // Strings and threads
            Material.STRING, Material.TRIPWIRE,

            // Flags and sails (if applicable)
            Material.WHITE_BANNER, Material.WHITE_WALL_BANNER
    );

    public SewingSkillListener(JavaPlugin plugin, SkillManager skillManager, int maxLevel) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.MAX_LEVEL = maxLevel;
    }

    @EventHandler
    public void onPlayerCraftItem(CraftItemEvent event) {
        if (event.isCancelled()) return;

        ItemStack result = event.getRecipe().getResult();
        if (result == null) return;

        // Only wool items count
        if (!isWoolItem(result.getType())) return;

        Player player = (Player) event.getWhoClicked();
        int level = skillManager.getSkillLevel(player.getUniqueId(), SkillType.SEWING);

        // Add XP with diminishing returns at higher levels
        double xp = calculateXpGain(level, result);
        boolean leveledUp = skillManager.addSkillExperience(player.getUniqueId(), SkillType.SEWING, xp);

        // Play level up sound if player leveled up
        if (leveledUp) {
            player.playSound(player.getLocation(), LEVEL_UP_SOUND, 1.0f, 1.0f);
            player.sendMessage("§dSewing Skill: You've reached level " +
                    (level + 1) + "!");
        }

        // Calculate chances for special events
        double doubleChance = calculateDoubleChance(level);
        double refundChance = calculateRefundChance(level);

        // Determine which event happens (only one can occur)
        double randomValue = random.nextDouble();
        double totalChance = doubleChance + refundChance;

        // Scale the random value to the total chance range
        double scaledRandom = randomValue * totalChance;

        if (scaledRandom < doubleChance) {
            // Double output event
            handleDoubleOutput(event, result, player);
        } else if (scaledRandom < doubleChance + refundChance) {
            // Refund event (only 50% of items)
            handleRefund(event, player, level);
        }
    }

    private boolean isWoolItem(Material material) {
        return WOOL_ITEMS.contains(material);
    }

    private double calculateXpGain(int level, ItemStack result) {
        // Base XP + level-based scaling, with diminishing returns
        double baseXp = 1.0;
        double levelFactor = Math.min(level / (double) MAX_LEVEL, 1.0);
        double itemComplexityFactor = calculateItemComplexity(result);

        return baseXp + (levelFactor * 2.0) + itemComplexityFactor;
    }

    private double calculateDoubleChance(int level) {
        // Scales from 0 to MAX_DOUBLE_CHANCE based on level
        return (double) level / MAX_LEVEL * MAX_DOUBLE_CHANCE;
    }

    private double calculateRefundChance(int level) {
        // Scales from 0 to MAX_REFUND_CHANCE based on level
        return (double) level / MAX_LEVEL * MAX_REFUND_CHANCE;
    }

    private double calculateItemComplexity(ItemStack item) {
        // More complex items give more XP
        Material type = item.getType();

        if (type.name().contains("BANNER")) return 0.6;
        if (type.name().contains("BED")) return 0.5;
        if (type.name().contains("CHESTPLATE") || type.name().contains("LEGGINGS")) return 0.4;
        if (type.name().contains("HELMET") || type.name().contains("BOOTS")) return 0.3;
        if (type.name().contains("CARPET")) return 0.2;
        if (type.name().contains("WOOL")) return 0.1;

        return 0.0;
    }

    private void handleDoubleOutput(CraftItemEvent event, ItemStack result, Player player) {
        result.setAmount(result.getAmount() * 2);
        event.getInventory().setResult(result);
        player.sendMessage("§dSewing Skill: You crafted double output!");
        player.playSound(player.getLocation(), DOUBLE_OUTPUT_SOUND, DOUBLE_OUTPUT_VOLUME, DOUBLE_OUTPUT_PITCH);
    }

    private void handleRefund(CraftItemEvent event, Player player, int level) {
        boolean refunded = false;
        double refundChance = calculateRefundChance(level);

        for (ItemStack item : event.getInventory().getMatrix()) {
            if (item != null && !item.getType().isAir() && random.nextDouble() < refundChance) {
                // Calculate how many items to refund (50% of stack)
                int refundAmount = (int) Math.max(1, Math.round(item.getAmount() * REFUND_PERCENTAGE));

                ItemStack clone = item.clone();
                clone.setAmount(refundAmount);

                // Add to inventory or drop if full
                player.getInventory().addItem(clone).values().forEach(leftover -> {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                });

                refunded = true;
            }
        }

        if (refunded) {
            player.playSound(player.getLocation(), REFUND_SOUND, REFUND_VOLUME, REFUND_PITCH);
            player.sendMessage("§dSewing Skill: Some materials were refunded!");
        }
    }
}