package com.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GUI {

    private static final Map<String, FileConfiguration> configs = new HashMap<>();

    public static void loadConfigs(JavaPlugin plugin) {
        String[] files = {"SmithingMenu.yml", "Smithing.yml", "Crafting.yml"};

        for (String fileName : files) {
            File file = new File(plugin.getDataFolder() + "/Guis", fileName);
            if (!file.exists()) {
                plugin.saveResource("Guis/" + fileName, false);
            }
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            configs.put(fileName, config);
        }
    }
    public static Map<String, FileConfiguration> getConfigs() {
        return configs;
    }

    public static Inventory createInventory(String fileName, int defaultSize, String defaultTitle) {
        FileConfiguration config = configs.get(fileName);
        if (config == null) return Bukkit.createInventory(null, defaultSize, ChatColor.translateAlternateColorCodes('&', defaultTitle));

        int size = config.getInt("size", defaultSize);
        String title = config.getString("title", defaultTitle);

        // Translate color codes for title
        title = ChatColor.translateAlternateColorCodes('&', title);

        Inventory gui = Bukkit.createInventory(null, size, title);

        // Fill with filler item if defined
        if (config.isConfigurationSection("filler")) {
            String materialName = config.getString("filler.material", "LIGHT_GRAY_STAINED_GLASS_PANE");
            Material material = Material.getMaterial(materialName);
            if (material == null) {
                material = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            }

            ItemStack filler = new ItemStack(material);
            ItemMeta meta = filler.getItemMeta();
            if (meta != null) {
                String displayName = config.getString("filler.displayname", " ");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                filler.setItemMeta(meta);
            }

            for (int i = 0; i < size; i++) {
                gui.setItem(i, filler);
            }
        }

        // Set items - ensure slots start from 0
        if (config.isConfigurationSection("items")) {
            for (String key : config.getConfigurationSection("items").getKeys(false)) {
                int slot = config.getInt("items." + key + ".slot");
                // Ensure slot is within valid range (0 to size-1)
                if (slot < 0 || slot >= size) {
                    Bukkit.getLogger().warning("Invalid slot " + slot + " for item " + key + " in " + fileName + ". Slot must be between 0 and " + (size-1));
                    continue;
                }
                ItemStack item = createItemFromConfig(config, "items." + key);
                if (item != null) {
                    gui.setItem(slot, item);
                }
            }
        }

        // Clear air slots if defined
        if (config.isList("air_slots")) {
            for (int slot : config.getIntegerList("air_slots")) {
                // Ensure slot is within valid range (0 to size-1)
                if (slot >= 0 && slot < size) {
                    gui.setItem(slot, null);
                } else {
                    Bukkit.getLogger().warning("Invalid air_slot " + slot + " in " + fileName + ". Slot must be between 0 and " + (size-1));
                }
            }
        }

        return gui;
    }

    private static ItemStack createItemFromConfig(FileConfiguration config, String path) {
        String materialName = config.getString(path + ".material");
        if (materialName == null) return null;

        Material material = Material.getMaterial(materialName);
        if (material == null) return null;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Set display name
            String displayName = config.getString(path + ".displayname");
            if (displayName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }

            // Set lore
            if (config.isList(path + ".lore")) {
                java.util.List<String> lore = config.getStringList(path + ".lore");
                for (int i = 0; i < lore.size(); i++) {
                    lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
                }
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack getItem(String fileName, String key) {
        FileConfiguration config = configs.get(fileName);
        if (config != null && config.isConfigurationSection("items." + key)) {
            return createItemFromConfig(config, "items." + key);
        }
        return null;
    }

    public static void saveItem(String fileName, String key, int slot, ItemStack item) {
        FileConfiguration config = configs.get(fileName);
        if (config == null) return;

        config.set("items." + key + ".slot", slot);

        // Save the item properties instead of the ItemStack object
        if (item != null) {
            config.set("items." + key + ".material", item.getType().name());

            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName()) {
                    config.set("items." + key + ".displayname", meta.getDisplayName());
                }
                if (meta.hasLore()) {
                    config.set("items." + key + ".lore", meta.getLore());
                }
            }
        }

        try {
            config.save(new File("plugins/SwordArtOnline/Guis/" + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper method to get the actual title from config
    public static String getGuiTitle(String fileName, String defaultTitle) {
        FileConfiguration config = configs.get(fileName);
        if (config != null) {
            String title = config.getString("title", defaultTitle);
            return ChatColor.translateAlternateColorCodes('&', title);
        }
        return ChatColor.translateAlternateColorCodes('&', defaultTitle);
    }
}