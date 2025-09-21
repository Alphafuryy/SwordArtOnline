package com.listeners.npcs;

import com.guis.SmithingMenuGUI;
import com.utils.NPC;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SmithingEntityListener implements Listener {

    private static final Random random = new Random();
    private static final Map<Player, Long> lastInteract = new HashMap<>();
    private static final long COOLDOWN_MS = 1000; // 1 second

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();

        if (entity.getCustomName() == null) return;

        String npcName = NPC.getNPCName("smithing");

        // Compare names ignoring color codes
        if (!ChatColor.stripColor(entity.getCustomName()).equalsIgnoreCase(ChatColor.stripColor(npcName))) return;

        // Check cooldown
        long now = System.currentTimeMillis();
        if (lastInteract.containsKey(player) && now - lastInteract.get(player) < COOLDOWN_MS) {
            event.setCancelled(true);
            return;
        }
        lastInteract.put(player, now);

        event.setCancelled(true);

        // Debug
        System.out.println("[SmithingNPC] NPC clicked by " + player.getName());

        // Play random dialogue
        List<String> dialogues = NPC.getNPCDialogues("smithing");
        if (!dialogues.isEmpty()) {
            String dialogue = dialogues.get(random.nextInt(dialogues.size()));
            player.sendMessage(dialogue);
        } else {
            player.sendMessage(ChatColor.RED + "No dialogues loaded for this NPC!");
        }

        // Play random sound
        List<Sound> sounds = NPC.getNPCSounds("smithing");
        if (!sounds.isEmpty()) {
            Sound sound = sounds.get(random.nextInt(sounds.size()));
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } else {
            System.out.println("[SmithingNPC] No sounds loaded for NPC: smithing");
        }

        // Open GUI
        try {
            SmithingMenuGUI.openSmithingMenuGUI(player);
        } catch (Exception e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "Failed to open Smithing GUI!");
        }
    }
}
