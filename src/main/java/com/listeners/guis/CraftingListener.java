package com.listeners.guis;

import com.managers.ItemManager;
import com.managers.RecipeManager;
import com.managers.SoundManager;
import com.utils.GUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class CraftingListener implements Listener {

    private final Plugin plugin;
    private final ItemManager itemManager;
    private final RecipeManager recipeManager;
    private final SoundManager soundManager;
    private final Set<UUID> inCraftingGUI = new HashSet<>();

    private int[] airSlots = {10, 11, 15, 16, 19, 20, 24, 25, 28, 29, 33, 34};
    private int resultSlot = 22;

    private final String guiTitle;

    public CraftingListener(Plugin plugin, ItemManager itemManager, RecipeManager recipeManager, SoundManager soundManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.recipeManager = recipeManager;
        this.soundManager = soundManager;

        // Read from GUI config
        this.guiTitle = GUI.getGuiTitle("Crafting.yml", "Crafting");

        List<Integer> airSlotList = GUI.getConfigs().get("Crafting.yml").getIntegerList("air_slots");
        this.airSlots = airSlotList.stream().mapToInt(Integer::intValue).toArray();

        List<Integer> resultSlotList = GUI.getConfigs().get("Crafting.yml").getIntegerList("result_slot");
        this.resultSlot = resultSlotList.isEmpty() ? 22 : resultSlotList.get(0); // fallback
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();

        if (!isOurCraftingTitle(title)) return;
        inCraftingGUI.add(player.getUniqueId());

        int slot = event.getRawSlot();
        if (slot < 0) return;

        // Click inside GUI
        if (slot < event.getView().getTopInventory().getSize()) {
            handleCraftingGUIClick(event, player, inventory, slot);
        } else {
            // Click in player inventory (shift-click to place ingredients)
            if (event.getClick().isShiftClick()) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    boolean moved = false;
                    for (int airSlot : airSlots) {
                        ItemStack slotItem = inventory.getItem(airSlot);
                        if (slotItem == null || slotItem.getType() == Material.AIR) {
                            inventory.setItem(airSlot, clickedItem.clone());
                            event.setCurrentItem(null);
                            moved = true;
                            break;
                        } else if (slotItem.isSimilar(clickedItem) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                            int space = slotItem.getMaxStackSize() - slotItem.getAmount();
                            int toMove = Math.min(space, clickedItem.getAmount());
                            slotItem.setAmount(slotItem.getAmount() + toMove);
                            clickedItem.setAmount(clickedItem.getAmount() - toMove);
                            if (clickedItem.getAmount() <= 0) event.setCurrentItem(null);
                            moved = true;
                        }
                    }
                    if (moved)soundManager.play(player, "crafting", "place");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> updateResultSlot(inventory), 1L);
                    event.setCancelled(true);
                }
            }
        }
    }

    private void handleCraftingGUIClick(InventoryClickEvent event, Player player, Inventory inventory, int slot) {
        if (isFillerSlot(slot, inventory.getSize())) {
            event.setCancelled(true);
            return;
        }

        if (slot == resultSlot) {
            handleResultSlotClick(event, player, inventory);
            return;
        }

        if (isCraftingSlot(slot)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateResultSlot(inventory), 1L);
            return;
        }

        event.setCancelled(true);
    }

    private void handleResultSlotClick(InventoryClickEvent event, Player player, Inventory inventory) {
        ItemStack resultItem = inventory.getItem(resultSlot);
        if (resultItem == null || resultItem.getType() == Material.AIR) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT ||
                event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {

            event.setCancelled(true);

            if (event.getClick().isShiftClick()) {
                while (true) {
                    if (!canPlayerTakeItem(player, resultItem)) {
                        player.sendMessage(ChatColor.RED + "Not enough space in your inventory!");
                        soundManager.play(player, "crafting", "fail");
                        break;
                    }
                    boolean success = consumeIngredientsAndGiveResult(player, inventory, resultItem);
                    if (!success) break;
                    soundManager.play(player, "crafting", "success");
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> updateResultSlot(inventory), 1L);
            } else {
                if (consumeIngredientsAndGiveResult(player, inventory, resultItem)) {
                    soundManager.play(player, "crafting", "success");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> updateResultSlot(inventory), 1L);
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to craft item!");
                    soundManager.play(player, "crafting", "fail");
                }
            }

        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (!isOurCraftingTitle(title)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                if (isFillerSlot(slot, event.getInventory().getSize()) || slot == resultSlot) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> updateResultSlot(event.getInventory()), 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        if (isOurCraftingTitle(title)) {
            inCraftingGUI.remove(player.getUniqueId());
            returnItemsToPlayer(player, event.getInventory());
        }
    }

    private void updateResultSlot(Inventory inventory) {
        Map<Integer, ItemStack> craftingItems = getCraftingItems(inventory);
        String matchedRecipe = null;

        for (RecipeManager.CustomRecipe recipe : recipeManager.getAllRecipes().values()) {
            Map<String, Integer> required = new HashMap<>();
            for (Map.Entry<String, Integer> e : recipe.getIngredients().entrySet())
                required.put(e.getKey().toLowerCase(), e.getValue());

            Map<String, Integer> provided = new HashMap<>();
            for (ItemStack item : craftingItems.values()) {
                String ingredientName = getIngredientName(item);
                if (ingredientName == null) continue;
                ingredientName = ingredientName.toLowerCase();
                provided.put(ingredientName, provided.getOrDefault(ingredientName, 0) + item.getAmount());
            }

            boolean ok = true;
            for (Map.Entry<String, Integer> req : required.entrySet()) {
                if (provided.getOrDefault(req.getKey(), 0) < req.getValue()) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                matchedRecipe = recipe.getResult();
                break;
            }
        }

        if (matchedRecipe != null) {
            ItemStack resultItem = itemManager.getItemStack(matchedRecipe);
            inventory.setItem(resultSlot, resultItem);
        } else {
            inventory.setItem(resultSlot, null);
        }
    }

    private Map<Integer, ItemStack> getCraftingItems(Inventory inventory) {
        Map<Integer, ItemStack> items = new HashMap<>();
        for (int slot : airSlots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR)
                items.put(slot, item);
        }
        return items;
    }

    private String getIngredientName(ItemStack item) {
        if (item == null) return null;

        String ingredientId = recipeManager.getIngredientId(item);
        if (ingredientId != null) return ingredientId.toLowerCase();

        for (ItemManager.CustomItem customItem : itemManager.getAllItems().values()) {
            ItemStack customItemStack = itemManager.getItemStack(customItem.getId());
            if (customItemStack != null && isSimilarItem(customItemStack, item))
                return customItem.getId().toLowerCase();
        }

        return item.getType().name().toLowerCase();
    }

    private boolean isSimilarItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) return false;
        if (item1.getType() != item2.getType()) return false;
        if (item1.isSimilar(item2)) return true;

        if (item1.hasItemMeta() && item2.hasItemMeta()) {
            ItemMeta meta1 = item1.getItemMeta();
            ItemMeta meta2 = item2.getItemMeta();
            return meta1.hasDisplayName() && meta2.hasDisplayName() && meta1.getDisplayName().equals(meta2.getDisplayName());
        }
        return false;
    }

    private boolean consumeIngredientsAndGiveResult(Player player, Inventory inventory, ItemStack resultItem) {
        Map<Integer, ItemStack> craftingItems = getCraftingItems(inventory);
        RecipeManager.CustomRecipe matchedRecipe = null;

        for (RecipeManager.CustomRecipe r : recipeManager.getAllRecipes().values()) {
            Map<String, Integer> required = new HashMap<>();
            for (Map.Entry<String, Integer> e : r.getIngredients().entrySet())
                required.put(e.getKey().toLowerCase(), e.getValue());

            Map<String, Integer> provided = new HashMap<>();
            for (ItemStack it : craftingItems.values()) {
                String name = getIngredientName(it);
                if (name == null) continue;
                name = name.toLowerCase();
                provided.put(name, provided.getOrDefault(name, 0) + it.getAmount());
            }

            boolean ok = true;
            for (Map.Entry<String, Integer> req : required.entrySet()) {
                if (provided.getOrDefault(req.getKey(), 0) < req.getValue()) {
                    ok = false;
                    break;
                }
            }

            if (ok) {
                matchedRecipe = r;
                break;
            }
        }

        if (matchedRecipe == null) return false;

        Map<String, Integer> requiredIngredients = new HashMap<>();
        for (Map.Entry<String, Integer> e : matchedRecipe.getIngredients().entrySet())
            requiredIngredients.put(e.getKey().toLowerCase(), e.getValue());

        Map<String, Integer> consumed = new HashMap<>();
        for (String k : requiredIngredients.keySet()) consumed.put(k, 0);

        for (Map.Entry<Integer, ItemStack> entry : craftingItems.entrySet()) {
            int slot = entry.getKey();
            ItemStack item = entry.getValue();
            String ingredientName = getIngredientName(item);
            if (ingredientName == null) continue;
            ingredientName = ingredientName.toLowerCase();

            if (requiredIngredients.containsKey(ingredientName)) {
                int needed = requiredIngredients.get(ingredientName) - consumed.get(ingredientName);
                int toConsume = Math.min(needed, item.getAmount());

                if (toConsume > 0) {
                    if (item.getAmount() == toConsume) {
                        inventory.setItem(slot, null);
                    } else {
                        item.setAmount(item.getAmount() - toConsume);
                        inventory.setItem(slot, item);
                    }
                    consumed.put(ingredientName, consumed.get(ingredientName) + toConsume);
                }
            }
        }

        ItemStack give = resultItem.clone();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(give);
        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) player.getWorld().dropItem(player.getLocation(), item);
        }

        return true;
    }

    private void returnItemsToPlayer(Player player, Inventory inventory) {
        for (int slot : airSlots) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack leftItem : leftover.values()) player.getWorld().dropItem(player.getLocation(), leftItem);
                }
                inventory.setItem(slot, null);
            }
        }
    }

    private boolean canPlayerTakeItem(Player player, ItemStack item) {
        Inventory playerInv = player.getInventory();
        ItemStack testItem = item.clone();

        for (ItemStack invItem : playerInv.getContents()) {
            if (invItem == null || invItem.getType() == Material.AIR) return true;
            if (invItem.isSimilar(testItem) && invItem.getAmount() < invItem.getMaxStackSize()) {
                int space = invItem.getMaxStackSize() - invItem.getAmount();
                if (space >= testItem.getAmount()) return true;
                testItem.setAmount(testItem.getAmount() - space);
            }
        }

        return testItem.getAmount() == 0;
    }

    private boolean isFillerSlot(int slot, int invSize) {
        return slot < invSize && !isCraftingSlot(slot) && slot != resultSlot;
    }

    private boolean isCraftingSlot(int slot) {
        for (int airSlot : airSlots) if (airSlot == slot) return true;
        return false;
    }

    private boolean isOurCraftingTitle(String title) {
        if (title == null) return false;
        String stripped = title.trim();
        return stripped.equalsIgnoreCase(guiTitle);
    }
}
