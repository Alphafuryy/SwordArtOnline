package com.listeners.guis;

import com.guis.CraftingGUI;
import com.guis.SmithingGUI;
import com.utils.GUI;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor;

public class SmithingMenuGUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || !clicked.hasItemMeta()) return;

        // Get GUI title from YAML configuration
        String configTitle = getGuiTitle("SmithingMenu.yml", "&7Smithing Menu");
        String inventoryTitle = ChatColor.stripColor(event.getView().getTitle());
        String expectedTitle = ChatColor.stripColor(configTitle);

        if (!inventoryTitle.equals(expectedTitle)) return;

        event.setCancelled(true); // Prevent taking items

        ItemStack smithingItem = GUI.getItem("SmithingMenu.yml", "smithing");
        ItemStack craftingItem = GUI.getItem("SmithingMenu.yml", "crafting");

        if (smithingItem != null && clicked.isSimilar(smithingItem)) {
            SmithingGUI.openSmithingGUI(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        } else if (craftingItem != null && clicked.isSimilar(craftingItem)) {
            CraftingGUI.openCraftingGUI(player);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }

    private String getGuiTitle(String fileName, String defaultTitle) {
        // Get the title directly from the config
        if (GUI.getConfigs() != null && GUI.getConfigs().containsKey(fileName)) {
            org.bukkit.configuration.file.FileConfiguration config = GUI.getConfigs().get(fileName);
            String title = config.getString("title", defaultTitle);
            return ChatColor.translateAlternateColorCodes('&', title);
        }
        return ChatColor.translateAlternateColorCodes('&', defaultTitle);
    }
}