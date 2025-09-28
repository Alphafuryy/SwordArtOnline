package com.skills;

import com.SwordArtOnline;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

public class SkillActivationListener implements Listener {

    private final SwordSkillManager skillManager;

    public SkillActivationListener() {
        this.skillManager = SwordSkillManager.getInstance();
        Bukkit.getPluginManager().registerEvents(this, SwordArtOnline.getInstance());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Check if player is sneaking (shift)
        if (!player.isSneaking()) {
            return;
        }

        // Check for right click (slot 1)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Only trigger for main hand to avoid double events
            if (event.getHand() == EquipmentSlot.HAND) {
                skillManager.activateSkill(player, SwordSkillManager.SLOT_1);
            }
        }

        // Check for left click (slot 2)
        else if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            // Only trigger for main hand to avoid double events
            if (event.getHand() == EquipmentSlot.HAND) {
                skillManager.activateSkill(player, SwordSkillManager.SLOT_2);
            }
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        // Check if player is sneaking (shift)
        if (player.isSneaking()) {
            event.setCancelled(true); // Cancel the item swap
            skillManager.activateSkill(player, SwordSkillManager.SLOT_3);
        }
        // If not sneaking, allow normal offhand swap
    }
}