package com.utils;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class NPC {

    private static final Map<String, String> npcNames = new HashMap<>();
    private static final Map<String, List<String>> npcDialogues = new HashMap<>();
    private static final Map<String, List<Sound>> npcSounds = new HashMap<>();
    private static FileConfiguration config;

    public static void loadNPCConfig(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "npc.yml");
        if (!file.exists()) {
            plugin.saveResource("npc.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(file);

        npcNames.clear();
        npcDialogues.clear();
        npcSounds.clear();

        for (String key : config.getKeys(false)) {
            // Name with color codes
            String name = config.getString(key + ".name", key);
            npcNames.put(key, ChatColor.translateAlternateColorCodes('&', name));

            // Dialogues with color codes
            List<String> dialogues = config.getStringList(key + ".dialogues");
            List<String> coloredDialogues = new ArrayList<>();
            for (String d : dialogues) {
                if (d != null && !d.isEmpty()) {
                    coloredDialogues.add(ChatColor.translateAlternateColorCodes('&', d));
                }
            }
            npcDialogues.put(key, coloredDialogues);
            plugin.getLogger().info("Loaded " + coloredDialogues.size() + " dialogues for NPC: " + key);

            // Sounds safely
            List<String> soundsStr = config.getStringList(key + ".sounds");
            List<Sound> soundsList = new ArrayList<>();
            for (String s : soundsStr) {
                try {
                    soundsList.add(Sound.valueOf(s.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid sound '" + s + "' for NPC: " + key);
                }
            }
            npcSounds.put(key, soundsList);
            plugin.getLogger().info("Loaded " + soundsList.size() + " sounds for NPC: " + key);
        }
    }

    public static String getNPCName(String key) {
        return npcNames.getOrDefault(key, key);
    }

    public static List<String> getNPCDialogues(String key) {
        return npcDialogues.getOrDefault(key, Collections.emptyList());
    }

    public static List<Sound> getNPCSounds(String key) {
        return npcSounds.getOrDefault(key, Collections.emptyList());
    }
}
