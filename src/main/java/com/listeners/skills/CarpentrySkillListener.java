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

public class CarpentrySkillListener implements Listener {

    private final SkillManager skillManager;
    private final int MAX_LEVEL;
    private final JavaPlugin plugin;
    private final Random random = new Random();

    // Configuration options
    private final double MAX_DOUBLE_CHANCE = 0.25; // 25% max chance
    private final double MAX_REFUND_CHANCE = 0.20; // 20% max chance
    private final double REFUND_PERCENTAGE = 0.50; // 50% of items refunded

    // Sound configuration
    private final Sound DOUBLE_OUTPUT_SOUND = Sound.ENTITY_PLAYER_LEVELUP;
    private final Sound REFUND_SOUND = Sound.ENTITY_ITEM_PICKUP;
    private final Sound LEVEL_UP_SOUND = Sound.ENTITY_PLAYER_LEVELUP;

    // Volume and pitch settings
    private final float DOUBLE_OUTPUT_VOLUME = 1.0f;
    private final float DOUBLE_OUTPUT_PITCH = 1.2f;
    private final float REFUND_VOLUME = 0.8f;
    private final float REFUND_PITCH = 1.0f;

    // Set of all wooden materials for efficient lookup
    private static final Set<Material> WOODEN_ITEMS = EnumSet.of(
            // Planks
            Material.OAK_PLANKS, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
            Material.JUNGLE_PLANKS, Material.ACACIA_PLANKS, Material.DARK_OAK_PLANKS,
            Material.MANGROVE_PLANKS, Material.CHERRY_PLANKS, Material.CRIMSON_PLANKS,
            Material.WARPED_PLANKS,

            // Sticks and wooden components
            Material.STICK,

            // Wooden tools
            Material.WOODEN_SWORD, Material.WOODEN_AXE, Material.WOODEN_PICKAXE,
            Material.WOODEN_SHOVEL, Material.WOODEN_HOE,

            // Wooden building blocks
            Material.OAK_SLAB, Material.SPRUCE_SLAB, Material.BIRCH_SLAB,
            Material.JUNGLE_SLAB, Material.ACACIA_SLAB, Material.DARK_OAK_SLAB,
            Material.MANGROVE_SLAB, Material.CHERRY_SLAB, Material.CRIMSON_SLAB,
            Material.WARPED_SLAB,
            Material.OAK_STAIRS, Material.SPRUCE_STAIRS, Material.BIRCH_STAIRS,
            Material.JUNGLE_STAIRS, Material.ACACIA_STAIRS, Material.DARK_OAK_STAIRS,
            Material.MANGROVE_STAIRS, Material.CHERRY_STAIRS, Material.CRIMSON_STAIRS,
            Material.WARPED_STAIRS,
            Material.OAK_FENCE, Material.SPRUCE_FENCE, Material.BIRCH_FENCE,
            Material.JUNGLE_FENCE, Material.ACACIA_FENCE, Material.DARK_OAK_FENCE,
            Material.MANGROVE_FENCE, Material.CHERRY_FENCE, Material.CRIMSON_FENCE,
            Material.WARPED_FENCE,
            Material.OAK_FENCE_GATE, Material.SPRUCE_FENCE_GATE, Material.BIRCH_FENCE_GATE,
            Material.JUNGLE_FENCE_GATE, Material.ACACIA_FENCE_GATE, Material.DARK_OAK_FENCE_GATE,
            Material.MANGROVE_FENCE_GATE, Material.CHERRY_FENCE_GATE, Material.CRIMSON_FENCE_GATE,
            Material.WARPED_FENCE_GATE,
            Material.OAK_DOOR, Material.SPRUCE_DOOR, Material.BIRCH_DOOR,
            Material.JUNGLE_DOOR, Material.ACACIA_DOOR, Material.DARK_OAK_DOOR,
            Material.MANGROVE_DOOR, Material.CHERRY_DOOR, Material.CRIMSON_DOOR,
            Material.WARPED_DOOR,
            Material.OAK_TRAPDOOR, Material.SPRUCE_TRAPDOOR, Material.BIRCH_TRAPDOOR,
            Material.JUNGLE_TRAPDOOR, Material.ACACIA_TRAPDOOR, Material.DARK_OAK_TRAPDOOR,
            Material.MANGROVE_TRAPDOOR, Material.CHERRY_TRAPDOOR, Material.CRIMSON_TRAPDOOR,
            Material.WARPED_TRAPDOOR,
            Material.OAK_PRESSURE_PLATE, Material.SPRUCE_PRESSURE_PLATE, Material.BIRCH_PRESSURE_PLATE,
            Material.JUNGLE_PRESSURE_PLATE, Material.ACACIA_PRESSURE_PLATE, Material.DARK_OAK_PRESSURE_PLATE,
            Material.MANGROVE_PRESSURE_PLATE, Material.CHERRY_PRESSURE_PLATE, Material.CRIMSON_PRESSURE_PLATE,
            Material.WARPED_PRESSURE_PLATE,
            Material.OAK_BUTTON, Material.SPRUCE_BUTTON, Material.BIRCH_BUTTON,
            Material.JUNGLE_BUTTON, Material.ACACIA_BUTTON, Material.DARK_OAK_BUTTON,
            Material.MANGROVE_BUTTON, Material.CHERRY_BUTTON, Material.CRIMSON_BUTTON,
            Material.WARPED_BUTTON,

            // Signs
            Material.OAK_SIGN, Material.SPRUCE_SIGN, Material.BIRCH_SIGN,
            Material.JUNGLE_SIGN, Material.ACACIA_SIGN, Material.DARK_OAK_SIGN,
            Material.MANGROVE_SIGN, Material.CHERRY_SIGN, Material.CRIMSON_SIGN,
            Material.WARPED_SIGN,
            Material.OAK_HANGING_SIGN, Material.SPRUCE_HANGING_SIGN, Material.BIRCH_HANGING_SIGN,
            Material.JUNGLE_HANGING_SIGN, Material.ACACIA_HANGING_SIGN, Material.DARK_OAK_HANGING_SIGN,
            Material.MANGROVE_HANGING_SIGN, Material.CHERRY_HANGING_SIGN, Material.CRIMSON_HANGING_SIGN,
            Material.WARPED_HANGING_SIGN,

            // Boats
            Material.OAK_BOAT, Material.SPRUCE_BOAT, Material.BIRCH_BOAT,
            Material.JUNGLE_BOAT, Material.ACACIA_BOAT, Material.DARK_OAK_BOAT,
            Material.MANGROVE_BOAT, Material.CHERRY_BOAT,
            Material.OAK_CHEST_BOAT, Material.SPRUCE_CHEST_BOAT, Material.BIRCH_CHEST_BOAT,
            Material.JUNGLE_CHEST_BOAT, Material.ACACIA_CHEST_BOAT, Material.DARK_OAK_CHEST_BOAT,
            Material.MANGROVE_CHEST_BOAT, Material.CHERRY_CHEST_BOAT,

            // Chests and barrels
            Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,

            // Other wooden items
            Material.LADDER, Material.CRAFTING_TABLE, Material.LOOM,
            Material.CARTOGRAPHY_TABLE, Material.FLETCHING_TABLE, Material.SMITHING_TABLE,
            Material.COMPOSTER, Material.BOOKSHELF, Material.LECTERN,
            Material.JUKEBOX, Material.NOTE_BLOCK,

            // Beds (wooden parts)
            Material.WHITE_BED, Material.ORANGE_BED, Material.MAGENTA_BED,
            Material.LIGHT_BLUE_BED, Material.YELLOW_BED, Material.LIME_BED,
            Material.PINK_BED, Material.GRAY_BED, Material.LIGHT_GRAY_BED,
            Material.CYAN_BED, Material.PURPLE_BED, Material.BLUE_BED,
            Material.BROWN_BED, Material.GREEN_BED, Material.RED_BED,
            Material.BLACK_BED,

            // Banners (wooden sticks)
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
            Material.BLACK_WALL_BANNER
    );

    public CarpentrySkillListener(JavaPlugin plugin, SkillManager skillManager, int maxLevel) {
        this.plugin = plugin;
        this.skillManager = skillManager;
        this.MAX_LEVEL = maxLevel;
    }

    @EventHandler
    public void onPlayerCraftItem(CraftItemEvent event) {
        if (event.isCancelled()) return;

        ItemStack result = event.getRecipe().getResult();
        if (result == null) return;

        // Only wooden items count
        if (!isWoodenItem(result.getType())) return;

        Player player = (Player) event.getWhoClicked();
        int level = skillManager.getSkillLevel(player.getUniqueId(), SkillType.CARPENTRY);

        // Add XP with diminishing returns at higher levels
        double xp = calculateXpGain(level, result);
        boolean leveledUp = skillManager.addSkillExperience(player.getUniqueId(), SkillType.CARPENTRY, xp);

        // Play level up sound if player leveled up
        if (leveledUp) {
            player.playSound(player.getLocation(), LEVEL_UP_SOUND, 1.0f, 1.0f);
            player.sendMessage("§6Carpentry Skill: You've reached level " +
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

    private boolean isWoodenItem(Material material) {
        return WOODEN_ITEMS.contains(material);
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

        if (type.name().contains("BOAT")) return 0.5;
        if (type.name().contains("CHEST") || type.name().contains("BARREL")) return 0.4;
        if (type.name().contains("DOOR") || type.name().contains("TRAPDOOR")) return 0.3;
        if (type.name().contains("TABLE") || type.name().contains("BED")) return 0.2;
        if (type.name().contains("FENCE") || type.name().contains("GATE")) return 0.1;

        return 0.0;
    }

    private void handleDoubleOutput(CraftItemEvent event, ItemStack result, Player player) {
        result.setAmount(result.getAmount() * 2);
        event.getInventory().setResult(result);
        player.sendMessage("§6Carpentry Skill: You crafted double!");
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
            player.sendMessage("§6Carpentry Skill: Some ingredients were refunded!");
        }
    }
}