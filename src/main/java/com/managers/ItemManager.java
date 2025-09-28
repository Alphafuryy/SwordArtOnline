package com.managers;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class ItemManager {

    private final Plugin plugin;
    private LoreManager loreManager;
    private final StatsManager statsManager;
    private final File itemsFolder;
    private final Map<String, CustomItem> items = new HashMap<>();
    private final String[] categories = {
            "Swords", "Rapiers", "Maces", "Daggers", "Axes", "Spears", "Bows", "Shields", "Armor"
    };

    public ItemManager(Plugin plugin, LoreManager loreManager, StatsManager statsManager) {
        this.plugin = plugin;
        this.loreManager = loreManager;
        this.statsManager = statsManager;
        this.itemsFolder = new File(plugin.getDataFolder(), "Items");
        if (!itemsFolder.exists()) itemsFolder.mkdirs();
        loadAllItems();
    }
    public void setLoreManager(LoreManager loreManager) {
        this.loreManager = loreManager;
    }
    public void loadAllItems() {
        items.clear();
        int totalLoaded = 0;

        // First, ensure all category files exist
        ensureCategoryFilesExist();

        for (String category : categories) {
            File categoryFile = new File(itemsFolder, category.toLowerCase() + ".yml");
            if (!categoryFile.exists()) {
                plugin.getLogger().warning("Item category file not found: " + categoryFile.getPath());
                continue;
            }

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(categoryFile);
                int categoryLoaded = 0;

                for (String itemId : config.getKeys(false)) {
                    try {
                        String name = config.getString(itemId + ".name", "Unnamed Item");
                        Material material = Material.matchMaterial(config.getString(itemId + ".material", "IRON_SWORD"));
                        String rarity = config.getString(itemId + ".rarity", "Normal");
                        List<String> lore = config.getStringList(itemId + ".lore");
                        List<String> flags = config.getStringList(itemId + ".flags");
                        List<String> enchantStrings = config.getStringList(itemId + ".enchants");

                        // Validate material
                        if (material == null) {
                            plugin.getLogger().warning("Invalid material for item " + itemId + ", using IRON_SWORD");
                            material = Material.IRON_SWORD;
                        }

                        // Stats
                        Map<String, Double> stats = new HashMap<>();
                        String[] slots = {"HAND", "OFF_HAND", "HEAD", "CHEST", "LEGS", "FEET"};

                        for (String slot : slots) {
                            String statsPath = itemId + ".stats." + slot;
                            if (config.contains(statsPath)) {
                                for (String stat : config.getConfigurationSection(statsPath).getKeys(false)) {
                                    String statKey = slot.toLowerCase() + "_" + stat.toLowerCase();
                                    stats.put(statKey, config.getDouble(statsPath + "." + stat));
                                }
                            }
                        }

                        // Enchants
                        Map<Enchantment, Integer> enchantments = new HashMap<>();
                        for (String ench : enchantStrings) {
                            String[] parts = ench.split(" ");
                            if (parts.length < 2) continue;
                            Enchantment enchant = Enchantment.getByName(parts[0].toUpperCase());
                            if (enchant != null) {
                                try {
                                    enchantments.put(enchant, Integer.parseInt(parts[1]));
                                } catch (NumberFormatException e) {
                                    plugin.getLogger().warning("Invalid enchant level for " + parts[0] + " in item " + itemId);
                                }
                            }
                        }


                        items.put(itemId, new CustomItem(itemId, name, material, category, rarity, stats, lore, enchantments, flags));
                        categoryLoaded++;

                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error loading item " + itemId + " from " + category, e);
                    }
                }
                plugin.getLogger().info("Loaded " + categoryLoaded + " items from " + category);
                totalLoaded += categoryLoaded;

            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error loading item file: " + categoryFile.getName(), e);
            }
        }
        plugin.getLogger().info("Total loaded " + totalLoaded + " custom items from " + categories.length + " categories");
    }
    private void ensureCategoryFilesExist() {
        for (String category : categories) {
            File categoryFile = new File(itemsFolder, category.toLowerCase() + ".yml");
            if (!categoryFile.exists()) {
                try {
                    // Try to extract from JAR
                    InputStream inputStream = plugin.getResource("Items/" + category.toLowerCase() + ".yml");
                    if (inputStream != null) {
                        Files.copy(inputStream, categoryFile.toPath());
                        plugin.getLogger().info("Extracted default item file: " + categoryFile.getName());
                    } else {
                        // Create empty file
                        categoryFile.createNewFile();
                        plugin.getLogger().info("Created empty item file: " + categoryFile.getName());
                    }
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Could not create item file: " + categoryFile.getName(), e);
                }
            }
        }
    }

    public ItemStack getItemStack(String id) {
        CustomItem customItem = items.get(id);
        if (customItem == null) {
            plugin.getLogger().warning("Item not found: " + id);
            return null;
        }

        ItemStack item = new ItemStack(customItem.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            plugin.getLogger().warning("Invalid material for item: " + id);
            return item;
        }

        // Name
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', customItem.getName()));

        // Lore - Start building lore
        List<String> lore = new ArrayList<>();

        // Add stats to lore FIRST
        if (!customItem.getStats().isEmpty()) {
            for (Map.Entry<String, Double> entry : customItem.getStats().entrySet()) {
                // Extract stat name from key (e.g., "hand_attack_damage" -> "attack_damage")
                String fullKey = entry.getKey().toLowerCase();
                String statName = fullKey.contains("_") ?
                        fullKey.substring(fullKey.indexOf("_") + 1) : fullKey;

                // Only add to lore if it's a supported stat
                if (statsManager.isStatSupported(statName)) {
                    String statLore = loreManager.formatStat(statName, entry.getValue());
                    lore.add(statLore);
                    plugin.getLogger().info("Added stat lore for " + id + ": " + statName + " = " + entry.getValue());
                }
            }
            lore.add(""); // Empty line after stats
        }

        // Custom lore from config
        for (String line : customItem.getLore()) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        // Enchantments in lore
        if (!customItem.getEnchantments().isEmpty()) {
            if (!lore.isEmpty()) lore.add(""); // Add empty line if there's previous lore
            for (Map.Entry<Enchantment, Integer> entry : customItem.getEnchantments().entrySet()) {
                lore.add(ChatColor.BLUE + formatEnchantmentName(entry.getKey()) + " " + entry.getValue());
            }
        }

        // Rarity at the bottom
        if (!lore.isEmpty()) lore.add(""); // Add empty line before rarity
        lore.add(loreManager.formatRarity(customItem.getRarity()));

        meta.setLore(lore);

        // Apply enchantments
        for (Map.Entry<Enchantment, Integer> entry : customItem.getEnchantments().entrySet()) {
            meta.addEnchant(entry.getKey(), entry.getValue(), true);
        }

        // Apply item flags - FIXED: Use proper enum values
        for (String flagName : customItem.getFlags()) {
            try {
                org.bukkit.inventory.ItemFlag flag = org.bukkit.inventory.ItemFlag.valueOf(flagName.toUpperCase());
                meta.addItemFlags(flag);
                plugin.getLogger().info("Applied flag: " + flag + " to item " + id);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid ItemFlag in " + customItem.getId() + ": " + flagName);
            }
        }
        NamespacedKey key = new NamespacedKey(plugin, "id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);

        // Apply attribute modifiers - FIXED: Proper attribute application
        if (!customItem.getStats().isEmpty()) {
            for (Map.Entry<String, Double> entry : customItem.getStats().entrySet()) {
                String fullKey = entry.getKey().toLowerCase();
                double value = entry.getValue();

                // Extract stat name (remove slot prefix)
                String statName = fullKey.contains("_") ?
                        fullKey.substring(fullKey.indexOf("_") + 1) : fullKey;

                Attribute attribute = statsManager.getAttribute(statName);
                if (attribute == null) {
                    plugin.getLogger().warning("Unknown attribute for stat: " + statName + " in item " + id);
                    continue;
                }

                EquipmentSlot slot = parseSlot(fullKey);
                AttributeModifier modifier = new AttributeModifier(
                        UUID.randomUUID(),
                        statName,
                        value,
                        AttributeModifier.Operation.ADD_NUMBER,
                        slot
                );

                meta.addAttributeModifier(attribute, modifier);
                plugin.getLogger().info("Applied attribute: " + attribute + " = " + value + " to slot " + slot + " for item " + id);
            }
        }

        item.setItemMeta(meta);

        // Debug: Verify attributes were applied
        if (item.hasItemMeta()) {
            ItemMeta finalMeta = item.getItemMeta();
            if (finalMeta.hasAttributeModifiers()) {
                plugin.getLogger().info("Final item " + id + " has " + finalMeta.getAttributeModifiers().size() + " attributes");
            } else {
                plugin.getLogger().warning("Final item " + id + " has NO attributes!");
            }
        }

        return item;
    }
    private EquipmentSlot parseSlot(String key) {
        // Convert key to uppercase for consistent matching
        String upperKey = key.toUpperCase();

        if (upperKey.startsWith("HAND_")) return EquipmentSlot.HAND;
        if (upperKey.startsWith("OFF_HAND_")) return EquipmentSlot.OFF_HAND;
        if (upperKey.startsWith("HEAD_")) return EquipmentSlot.HEAD;
        if (upperKey.startsWith("CHEST_")) return EquipmentSlot.CHEST;
        if (upperKey.startsWith("LEGS_")) return EquipmentSlot.LEGS;
        if (upperKey.startsWith("FEET_")) return EquipmentSlot.FEET;

        // Default to HAND if no slot specified
        return EquipmentSlot.HAND;
    }
    private String formatEnchantmentName(Enchantment enchant) {
        String name = enchant.getKey().getKey();
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    // --- Data Access ---
    public CustomItem getItem(String id) { return items.get(id); }
    public Map<String, CustomItem> getAllItems() { return new HashMap<>(items); }
    public List<CustomItem> getItemsByCategory(String category) {
        List<CustomItem> out = new ArrayList<>();
        for (CustomItem i : items.values()) if (i.getCategory().equalsIgnoreCase(category)) out.add(i);
        return out;
    }
    public String[] getCategories() { return categories.clone(); }

    // --- Inner Class ---
    public static class CustomItem {
        private final String id;
        private String name;
        private Material material;
        private String category;
        private String rarity;
        private Map<String, Double> stats;
        private List<String> lore;
        private Map<Enchantment, Integer> enchantments;
        private List<String> flags;

        public CustomItem(String id, String name, Material material, String category, String rarity,
                          Map<String, Double> stats, List<String> lore,
                          Map<Enchantment, Integer> enchantments, List<String> flags) {
            this.id = id;
            this.name = name;
            this.material = material;
            this.category = category;
            this.rarity = rarity;
            this.stats = stats != null ? stats : new HashMap<>();
            this.lore = lore != null ? lore : new ArrayList<>();
            this.enchantments = enchantments != null ? enchantments : new HashMap<>();
            this.flags = flags != null ? flags : new ArrayList<>();
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public Material getMaterial() { return material; }
        public String getCategory() { return category; }
        public String getRarity() { return rarity; }
        public Map<String, Double> getStats() { return stats; }
        public List<String> getLore() { return lore; }
        public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
        public List<String> getFlags() { return flags; }

        // Setters
        public void setName(String name) { this.name = name; }
        public void setMaterial(Material material) { this.material = material; }
        public void setCategory(String category) { this.category = category; }
        public void setRarity(String rarity) { this.rarity = rarity; }
        public void setStats(Map<String, Double> stats) { this.stats = stats; }
        public void setLore(List<String> lore) { this.lore = lore; }
        public void setEnchantments(Map<Enchantment, Integer> enchantments) { this.enchantments = enchantments; }
        public void setFlags(List<String> flags) { this.flags = flags; }
    }
}
