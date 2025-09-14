package com.listeners;

import com.utils.LocationSelection;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionListener implements Listener {

    private final Map<UUID, LocationSelection> selections = new HashMap<>();

    public static ItemStack getWand() {
        ItemStack axe = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Region Wand");
        meta.setUnbreakable(true);
        meta.setLore(java.util.Arrays.asList(
                ChatColor.YELLOW + "Left-click: set pos1",
                ChatColor.YELLOW + "Right-click: set pos2"
        ));
        axe.setItemMeta(meta);
        return axe;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.NETHERITE_AXE || !item.hasItemMeta()) return;
        if (!ChatColor.stripColor(item.getItemMeta().getDisplayName()).equals("Region Wand")) return;

        event.setCancelled(true);

        LocationSelection sel = selections.computeIfAbsent(player.getUniqueId(), k -> new LocationSelection());

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            sel.setPos1(event.getClickedBlock().getLocation());
            player.sendMessage(ChatColor.GREEN + "Pos1 set to " + format(sel.getPos1()));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            sel.setPos2(event.getClickedBlock().getLocation());
            player.sendMessage(ChatColor.GREEN + "Pos2 set to " + format(sel.getPos2()));
        }
    }

    public LocationSelection getSelection(Player player) {
        return selections.get(player.getUniqueId());
    }

    private String format(org.bukkit.Location loc) {
        return ChatColor.GRAY + "(" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
