package com.managers;

import com.tasks.SmithingTask;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class LoreManager {

    private final Plugin plugin;
    private final File configFile;
    private final YamlConfiguration config;

    private final Map<String, String> rarityFormats = new HashMap<>();
    private final Map<String, String> statFormats = new HashMap<>();
    private final Map<Integer, String> starFormats = new HashMap<>();
    private final DecimalFormat numberFormat = new DecimalFormat("#.##");
    private final StatsManager statsManager;
    private ItemManager itemManager; // Add this

    public LoreManager(Plugin plugin, StatsManager statsManager, ItemManager itemManager) { // Modified constructor
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.itemManager = itemManager; // Add this
        this.configFile = new File(plugin.getDataFolder(), "lore.yml");

        if (!configFile.exists()) {
            plugin.saveResource("lore.yml", false);
        }

        this.config = YamlConfiguration.loadConfiguration(configFile);
        loadMappings();
    }

    public void setItemManager(ItemManager itemManager) {
        this.itemManager = itemManager;
    }

    private void loadMappings() {
        rarityFormats.clear();
        statFormats.clear();
        starFormats.clear();

        // Load rarities
        if (config.contains("rarities")) {
            for (String key : config.getConfigurationSection("rarities").getKeys(false)) {
                rarityFormats.put(key.toLowerCase(), config.getString("rarities." + key));
            }
        }

        // Load stat names
        if (config.contains("stat-names")) {
            for (String key : config.getConfigurationSection("stat-names").getKeys(false)) {
                statFormats.put(key.toLowerCase(), config.getString("stat-names." + key));
            }
        }

        // Load star lores
        if (config.contains("stars")) {
            for (String key : config.getConfigurationSection("stars").getKeys(false)) {
                try {
                    int starLevel = Integer.parseInt(key);
                    starFormats.put(starLevel, config.getString("stars." + key));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        plugin.getLogger().info("Loaded " + rarityFormats.size() + " rarities, " +
                statFormats.size() + " stats, " +
                starFormats.size() + " star levels");
    }

    public String formatRarity(String rarity) {
        String template = rarityFormats.getOrDefault(rarity.toLowerCase(), "&7" + rarity);
        return ChatColor.translateAlternateColorCodes('&', template);
    }

    public String formatStat(String statKey, double value) {
        String template = statFormats.getOrDefault(statKey.toLowerCase(), "&8 ● " + statKey + " &f%sign%%value%");
        String sign = value >= 0 ? "+" : "";

        template = template.replace("%sign%", sign)
                .replace("%value%", numberFormat.format(value));

        return ChatColor.translateAlternateColorCodes('&', template);
    }

    public String formatStars(int stars) {
        stars = Math.max(1, Math.min(stars, 10));
        String template = starFormats.getOrDefault(stars, "&7" + "★".repeat(stars));
        return ChatColor.translateAlternateColorCodes('&', template);
    }

    public int getStarLevel(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return 0;

        List<String> lore = meta.getLore();
        for (int i = lore.size() - 1; i >= 0; i--) {
            String line = ChatColor.stripColor(lore.get(i));
            if (line.contains("★")) {
                return (int) line.chars().filter(ch -> ch == '★').count();
            }
        }
        return 0;
    }

    public int getMaxStarLevel() {
        return 10;
    }

    public ItemStack updateItemStats(ItemStack item, int newStarLevel) {
        if (item == null || !item.hasItemMeta()) return item;

        ItemStack upgraded = item.clone();
        ItemMeta meta = upgraded.getItemMeta();
        if (meta == null) return upgraded;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // 1️⃣ Update the star line
        updateStarLine(lore, newStarLevel);

        // 2️⃣ Get the original base stats from ItemManager by identifying the item
        Map<Attribute, Double> originalBaseStats = getOriginalBaseStatsFromItemManager(item);

        // 3️⃣ Calculate multiplier based on new star level
        double multiplier = 1.0 + (SmithingTask.getBuffRatePerStar() * newStarLevel);

        // 4️⃣ Update actual attribute modifiers using ORIGINAL base stats
        Map<Attribute, Double> newAttributeValues = new HashMap<>();
        if (meta.hasAttributeModifiers()) {
            for (Attribute attribute : new HashSet<>(meta.getAttributeModifiers().keySet())) {
                String statKey = getStatKeyFromAttribute(attribute);
                if (statKey == null) continue;

                Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
                if (modifiers != null && !modifiers.isEmpty()) {
                    AttributeModifier oldModifier = modifiers.iterator().next();

                    // Get the original base value for this attribute
                    Double originalBaseValue = originalBaseStats.get(attribute);
                    double newValue;

                    if (originalBaseValue != null && statsManager.isUpgradeable(statKey)) {
                        // Use original base * multiplier instead of current value * multiplier
                        newValue = originalBaseValue * multiplier;
                    } else {
                        // For non-upgradeable stats, keep the current value
                        newValue = oldModifier.getAmount();
                    }

                    // Remove old modifier and add new
                    meta.removeAttributeModifier(attribute);
                    AttributeModifier newModifier = new AttributeModifier(
                            oldModifier.getUniqueId(),
                            oldModifier.getName(),
                            newValue,
                            oldModifier.getOperation(),
                            oldModifier.getSlot()
                    );
                    meta.addAttributeModifier(attribute, newModifier);
                    newAttributeValues.put(attribute, newValue);
                }
            }
        }

        // 5️⃣ COMPLETELY REPLACE stat lore lines with new formatted ones (preserving order)
        lore = replaceStatLoreLines(lore, newAttributeValues);

        // 6️⃣ Apply updated lore
        meta.setLore(lore);
        upgraded.setItemMeta(meta);
        return upgraded;
    }

    // New method to identify item and get original base stats from ItemManager
    private Map<Attribute, Double> getOriginalBaseStatsFromItemManager(ItemStack item) {
        Map<Attribute, Double> originalStats = new HashMap<>();

        // Try to identify which custom item this is
        ItemManager.CustomItem identifiedItem = identifyCustomItem(item);

        if (identifiedItem != null) {
            plugin.getLogger().info("Found original item: " + identifiedItem.getId());

            // Convert the item's stats to Attribute-Double map
            for (Map.Entry<String, Double> entry : identifiedItem.getStats().entrySet()) {
                String fullKey = entry.getKey().toLowerCase();
                String statName = fullKey.contains("_") ?
                        fullKey.substring(fullKey.indexOf("_") + 1) : fullKey;

                Attribute attribute = statsManager.getAttribute(statName);
                if (attribute != null) {
                    originalStats.put(attribute, entry.getValue());
                    plugin.getLogger().info("Original stat: " + statName + " = " + entry.getValue());
                }
            }
        } else {
            plugin.getLogger().warning("Could not identify original item from ItemManager!");
        }

        return originalStats;
    }

    // Method to identify which custom item this is based on its properties
    private ItemManager.CustomItem identifyCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "id");

        String id = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id != null) {
            return itemManager.getItem(id); // add a getter in ItemManager if missing
        }

        return null;
    }


    private List<String> replaceStatLoreLines(List<String> lore, Map<Attribute, Double> attributeValues) {
        if (lore == null || lore.isEmpty()) {
            return createNewStatLore(attributeValues);
        }

        List<String> newLore = new ArrayList<>();
        boolean statSectionReplaced = false;
        int lastStatLineIndex = -1;

        // First pass: identify stat lines and their positions
        List<Integer> statLineIndices = new ArrayList<>();
        Map<Integer, String> statKeyByIndex = new HashMap<>();

        for (int i = 0; i < lore.size(); i++) {
            String line = lore.get(i);
            String cleanLine = ChatColor.stripColor(line);

            if (cleanLine.contains("●")) {
                statLineIndices.add(i);
                // Try to identify the stat key for this line
                String statKey = identifyStatKeyFromLine(cleanLine);
                if (statKey != null) {
                    statKeyByIndex.put(i, statKey);
                }
            }
        }

        // If we found stat lines, replace them in order
        if (!statLineIndices.isEmpty()) {
            statSectionReplaced = true;

            // Add everything before the first stat line
            newLore.addAll(lore.subList(0, statLineIndices.get(0)));

            // Replace each stat line with the updated version
            for (int i = 0; i < statLineIndices.size(); i++) {
                int lineIndex = statLineIndices.get(i);
                String originalStatKey = statKeyByIndex.get(lineIndex);

                if (originalStatKey != null) {
                    Attribute attribute = statsManager.attributeMap.get(originalStatKey);
                    if (attribute != null && attributeValues.containsKey(attribute)) {
                        newLore.add(formatStat(originalStatKey, attributeValues.get(attribute)));
                        lastStatLineIndex = newLore.size() - 1;
                    } else {
                        // Keep the original line if we can't find the attribute
                        newLore.add(lore.get(lineIndex));
                    }
                } else {
                    // Keep the original line if we can't identify the stat key
                    newLore.add(lore.get(lineIndex));
                }
            }

            // Add everything after the last stat line
            int lastStatIndex = statLineIndices.get(statLineIndices.size() - 1);
            if (lastStatIndex + 1 < lore.size()) {
                newLore.addAll(lore.subList(lastStatIndex + 1, lore.size()));
            }
        }

        // If no stat lines were found, insert them before the star line
        if (!statSectionReplaced) {
            int starLineIndex = findStarLineIndex(lore);
            if (starLineIndex != -1) {
                // Insert before star line
                newLore.addAll(lore.subList(0, starLineIndex));
                newLore.addAll(createNewStatLore(attributeValues));
                newLore.addAll(lore.subList(starLineIndex, lore.size()));
            } else {
                // Insert at the end
                newLore.addAll(lore);
                newLore.addAll(createNewStatLore(attributeValues));
            }
        }

        return newLore;
    }

    private List<String> createNewStatLore(Map<Attribute, Double> attributeValues) {
        List<String> statLore = new ArrayList<>();
        for (Map.Entry<Attribute, Double> entry : attributeValues.entrySet()) {
            String statKey = getStatKeyFromAttribute(entry.getKey());
            if (statKey != null) {
                statLore.add(formatStat(statKey, entry.getValue()));
            }
        }
        return statLore;
    }

    private String identifyStatKeyFromLine(String cleanLine) {
        cleanLine = cleanLine.toLowerCase();
        for (String statKey : statFormats.keySet()) {
            String template = statFormats.get(statKey);
            if (template != null) {
                String baseName = ChatColor.stripColor(
                        ChatColor.translateAlternateColorCodes('&', template)
                ).toLowerCase();

                baseName = baseName.replace("%sign%", "").replace("%value%", "").replace("●", "").trim();

                if (!baseName.isEmpty() && cleanLine.contains(baseName)) {
                    return statKey;
                }
            }
        }
        return null; // fallback if no match
    }

    private int findStarLineIndex(List<String> lore) {
        for (int i = 0; i < lore.size(); i++) {
            String line = ChatColor.stripColor(lore.get(i));
            for (String starTemplate : starFormats.values()) {
                String starClean = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', starTemplate));
                if (!starClean.isEmpty() && line.contains(starClean.substring(0, 1))) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String getStatKeyFromAttribute(Attribute attribute) {
        for (Map.Entry<String, Attribute> entry : statsManager.attributeMap.entrySet()) {
            if (entry.getValue() == attribute) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void updateStarLine(List<String> lore, int newStarLevel) {
        int index = findStarLineIndex(lore);
        String newStarDisplay = formatStars(newStarLevel);
        if (index != -1) {
            lore.set(index, newStarDisplay);
        } else {
            lore.add(newStarDisplay);
        }
    }
}