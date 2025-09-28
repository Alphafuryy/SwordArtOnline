package com.skills;

import com.SwordArtOnline;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class SwordSkillManager implements Listener {

    private static SwordSkillManager instance;
    private final Map<UUID, SkillSlot[]> playerSlots;
    private final Map<String, Skill> registeredSkills;
    private final Map<SkillCategory, List<Skill>> skillsByCategory;

    // Slot constants
    public static final int SLOT_COUNT = 3;
    public static final int SLOT_1 = 0; // Shift + Right Click
    public static final int SLOT_2 = 1; // Shift + Left Click
    public static final int SLOT_3 = 2; // Shift + Offhand Swap (F key)

    public SwordSkillManager() {
        this.playerSlots = new HashMap<>();
        this.registeredSkills = new HashMap<>();
        this.skillsByCategory = new HashMap<>();

        // Initialize categories
        for (SkillCategory category : SkillCategory.values()) {
            skillsByCategory.put(category, new ArrayList<>());
        }

        Bukkit.getPluginManager().registerEvents(this, SwordArtOnline.getInstance());
    }

    public static SwordSkillManager getInstance() {
        if (instance == null) {
            instance = new SwordSkillManager();
        }
        return instance;
    }

    public void registerSkill(String skillName, Skill skill) {
        registeredSkills.put(skillName.toLowerCase(), skill);
        skillsByCategory.get(skill.getCategory()).add(skill);
    }

    public Skill getSkill(String skillName) {
        return registeredSkills.get(skillName.toLowerCase());
    }

    public Collection<Skill> getRegisteredSkills() {
        return registeredSkills.values();
    }

    public List<Skill> getSkillsByCategory(SkillCategory category) {
        return new ArrayList<>(skillsByCategory.get(category));
    }

    public Map<SkillCategory, List<Skill>> getSkillsByCategory() {
        return new HashMap<>(skillsByCategory);
    }

    public List<Skill> getAvailableSkills(Player player) {
        List<Skill> available = new ArrayList<>();
        for (Skill skill : registeredSkills.values()) {
            if (player.hasPermission(skill.getPermission())) {
                available.add(skill);
            }
        }
        return available;
    }

    public boolean setPlayerSkill(Player player, int slot, String skillName) {
        if (slot < 0 || slot >= SLOT_COUNT) {
            player.sendMessage(ChatColor.RED + "Slot must be between 1 and 3!");
            return false;
        }

        Skill skill = getSkill(skillName);
        if (skill == null) {
            player.sendMessage(ChatColor.RED + "Skill '" + skillName + "' not found!");
            return false;
        }

        // Check permission
        if (!player.hasPermission(skill.getPermission())) {
            player.sendMessage(ChatColor.RED + "You don't have permission to use this skill! (" + skill.getPermission() + ")");
            return false;
        }

        UUID playerId = player.getUniqueId();
        if (!playerSlots.containsKey(playerId)) {
            playerSlots.put(playerId, new SkillSlot[SLOT_COUNT]);
        }

        SkillSlot[] slots = playerSlots.get(playerId);
        slots[slot] = new SkillSlot(skill);

        player.sendMessage(ChatColor.GREEN + "Skill '" + skill.getName() +
                ChatColor.GREEN + "' assigned to slot " + (slot + 1) +
                ChatColor.GRAY + " (" + skill.getCategory().getDisplayName() + ")");
        return true;
    }

    public boolean activateSkill(Player player, int slot) {
        UUID playerId = player.getUniqueId();
        if (!playerSlots.containsKey(playerId)) {
            return false;
        }

        SkillSlot[] slots = playerSlots.get(playerId);
        if (slot < 0 || slot >= SLOT_COUNT || slots[slot] == null) {
            String[] slotNames = {"Shift + Right Click", "Shift + Left Click", "Shift + F"};
            return false;
        }

        Skill skill = slots[slot].getSkill();

        // Check permission again (in case permissions changed)
        if (!player.hasPermission(skill.getPermission())) {
            player.sendMessage(ChatColor.RED + "You no longer have permission to use this skill! (" + skill.getPermission() + ")");
            slots[slot] = null; // Clear the slot
            return false;
        }

        boolean success = slots[slot].activate(player);
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Activated: " +
                    skill.getCategory().getColor() + skill.getName());
        }
        return success;
    }

    public SkillSlot[] getPlayerSlots(Player player) {
        UUID playerId = player.getUniqueId();
        if (!playerSlots.containsKey(playerId)) {
            playerSlots.put(playerId, new SkillSlot[SLOT_COUNT]);
        }
        return playerSlots.get(playerId);
    }

    public void clearPlayerSlots(Player player) {
        playerSlots.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Optionally clear player data on quit, or keep it for next login
        // clearPlayerSlots(event.getPlayer());
    }

    public void reload() {
        playerSlots.clear();
        // Re-register skills if needed
    }
}