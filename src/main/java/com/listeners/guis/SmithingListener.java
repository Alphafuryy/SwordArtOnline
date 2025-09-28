package com.listeners.guis;

import com.managers.ItemManager;
import com.managers.RecipeManager;
import com.managers.SoundManager;
import com.managers.LoreManager;
import com.tasks.SmithingTask;
import com.utils.GUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class SmithingListener implements Listener {

    private final Plugin plugin;
    private final ItemManager itemManager;
    private final RecipeManager recipeManager;
    private final SoundManager soundManager;
    private final LoreManager loreManager;
    private final Set<UUID> inSmithingGUI = new HashSet<>();

    private int[] airSlots = {10, 11, 19, 20, 22, 24, 28, 29};
    private int itemSlot = 22;
    private int resultSlot = 24;

    private final String guiTitle;

    public SmithingListener(Plugin plugin, ItemManager itemManager, RecipeManager recipeManager, SoundManager soundManager, LoreManager loreManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
        this.recipeManager = recipeManager;
        this.soundManager = soundManager;
        this.loreManager = loreManager;

        this.guiTitle = GUI.getGuiTitle("Smithing.yml", "Smithing");

        List<Integer> airSlotList = GUI.getConfigs().get("Smithing.yml").getIntegerList("air_slots");
        this.airSlots = airSlotList.stream().mapToInt(Integer::intValue).toArray();

        List<Integer> itemSlotList = GUI.getConfigs().get("Smithing.yml").getIntegerList("item_slot");
        this.itemSlot = itemSlotList.isEmpty() ? 22 : itemSlotList.get(0);

        List<Integer> resultSlotList = GUI.getConfigs().get("Smithing.yml").getIntegerList("result_slot");
        this.resultSlot = resultSlotList.isEmpty() ? 24 : resultSlotList.get(0);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();
        String title = event.getView().getTitle();

        if (!isOurSmithingTitle(title)) return;
        inSmithingGUI.add(player.getUniqueId());

        int slot = event.getRawSlot();
        if (slot < 0) return;

        // Click inside GUI
        if (slot < event.getView().getTopInventory().getSize()) {
            handleSmithingGUIClick(event, player, inventory, slot);
        } else {
            // Click in player inventory (shift-click to place items)
            if (event.getClick().isShiftClick()) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    handleShiftClickFromInventory(event, player, inventory, clickedItem);
                }
            }
        }
    }
    private void handleShiftClickFromInventory(InventoryClickEvent event, Player player, Inventory inventory, ItemStack clickedItem) {
        boolean moved = false;

        // Check if it's an upgradable weapon and place it in the item slot automatically
        if (isUpgradableWeapon(clickedItem)) {
            ItemStack currentItemSlot = inventory.getItem(itemSlot);
            if (currentItemSlot == null || currentItemSlot.getType() == Material.AIR) {
                inventory.setItem(itemSlot, clickedItem.clone());
                event.setCurrentItem(null);
                moved = true;
                soundManager.play(player, "smithing", "place"); // FIX: Added sound here
            }
        } else {
            // Regular material placement logic
            for (int airSlot : airSlots) {
                // Skip result slot and item slot for material placement
                if (airSlot == resultSlot || airSlot == itemSlot) continue;

                ItemStack slotItem = inventory.getItem(airSlot);
                if (slotItem == null || slotItem.getType() == Material.AIR) {
                    inventory.setItem(airSlot, clickedItem.clone());
                    event.setCurrentItem(null);
                    moved = true;
                    soundManager.play(player, "smithing", "place"); // FIX: Added sound here
                    break;
                } else if (slotItem.isSimilar(clickedItem) && slotItem.getAmount() < slotItem.getMaxStackSize()) {
                    int space = slotItem.getMaxStackSize() - slotItem.getAmount();
                    int toMove = Math.min(space, clickedItem.getAmount());
                    slotItem.setAmount(slotItem.getAmount() + toMove);
                    clickedItem.setAmount(clickedItem.getAmount() - toMove);
                    if (clickedItem.getAmount() <= 0) event.setCurrentItem(null);
                    moved = true;
                    soundManager.play(player, "smithing", "place"); // FIX: Added sound here
                    break;
                }
            }
        }

        if (moved) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> updateResultSlot(inventory), 1L);
        }
        event.setCancelled(true);
    }
    private boolean isWeapon(Material material) {
        // Define what constitutes a weapon
        return material.name().contains("SWORD") ||
                material.name().contains("AXE") ||
                material.name().contains("BOW") ||
                material.name().contains("CROSSBOW") ||
                material.name().contains("TRIDENT") ||
                material.name().contains("PICKAXE") || // Tools can also be upgraded
                material.name().contains("SHOVEL") ||
                material.name().contains("HOE") ||
                material.name().contains("HELMET") || // Armor can also be upgraded
                material.name().contains("CHESTPLATE") ||
                material.name().contains("MACE") ||
                material.name().contains("LEGGINGS") ||
                material.name().contains("BOOTS");
    }
    private boolean isUpgradableWeapon(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        if (isWeapon(item.getType())) {
            // Get current stars
            int currentStars = loreManager.getStarLevel(item);

            // Get custom item id from metadata
            String itemId = getItemIdFromItemStack(item);
            if (itemId == null) return false; // Not a custom item

            // Get the custom item definition
            ItemManager.CustomItem customItem = itemManager.getItem(itemId);
            if (customItem == null) return false;

            // Get rarity-based max stars
            int maxStars = SmithingTask.getMaxStarsForRarity(customItem.getRarity());

            return currentStars < maxStars;
        }
        return false;
    }

    private void handleSmithingGUIClick(InventoryClickEvent event, Player player, Inventory inventory, int slot) {
        if (isFillerSlot(slot, inventory.getSize())) {
            event.setCancelled(true);
            return;
        }

        if (slot == resultSlot) {
            handleResultSlotClick(event, player, inventory);
            return;
        }

        if (isSmithingSlot(slot)) {
            // FIX: Play place sound when manually placing items in GUI slots
            if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                soundManager.play(player, "smithing", "place");
            }
            // Allow the click to proceed normally for manual placement
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

        // Prevent taking barrier items (max stars or cost preview)
        if (resultItem.getType() == Material.BARRIER) {
            event.setCancelled(true);

            // Check if it's max stars barrier
            if (resultItem.hasItemMeta() && resultItem.getItemMeta().hasDisplayName()) {
                String displayName = ChatColor.stripColor(resultItem.getItemMeta().getDisplayName());
                if (displayName.contains("Maximum") || displayName.contains("max")) {
                    player.sendMessage(ChatColor.RED + "This item has reached the maximum star level!");
                    soundManager.play(player, "smithing", "fail");
                }
            }
            return;
        }

        if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT ||
                event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {

            event.setCancelled(true);

            if (event.getClick().isShiftClick()) {
                // FIX: Improved shift-click logic to prevent bugs
                boolean success = false;
                int upgradesDone = 0;
                int maxUpgrades = 64; // Safety limit to prevent infinite loops

                while (upgradesDone < maxUpgrades) {
                    if (!canPlayerTakeItem(player, resultItem)) {
                        if (upgradesDone == 0) {
                            player.sendMessage(ChatColor.RED + "Not enough space in your inventory!");
                            soundManager.play(player, "smithing", "fail");
                        }
                        break;
                    }
                    boolean upgradeSuccess = consumeMaterialsAndGiveResult(player, inventory, resultItem);
                    if (!upgradeSuccess) {
                        if (upgradesDone == 0) {
                            soundManager.play(player, "smithing", "fail");
                        }
                        break;
                    }
                    success = true;
                    upgradesDone++;

                    // Stop if we can't upgrade anymore (max stars reached or no materials)
                    ItemStack currentItem = inventory.getItem(itemSlot);
                    if (currentItem == null || currentItem.getType() == Material.AIR) {
                        break;
                    }
                }

                if (success) {
                    soundManager.play(player, "smithing", "success");
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> updateResultSlot(inventory), 1L);
            } else {
                if (consumeMaterialsAndGiveResult(player, inventory, resultItem)) {
                    soundManager.play(player, "smithing", "success");
                    Bukkit.getScheduler().runTaskLater(plugin, () -> updateResultSlot(inventory), 1L);
                } else {
                    player.sendMessage(ChatColor.RED + "Failed to upgrade item!");
                    soundManager.play(player, "smithing", "fail");
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
        if (!isOurSmithingTitle(title)) return;

        for (int slot : event.getRawSlots()) {
            if (slot < event.getInventory().getSize()) {
                if (isFillerSlot(slot, event.getInventory().getSize()) || slot == resultSlot) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // FIX: Play sound on drag as well
        Player player = (Player) event.getWhoClicked();
        soundManager.play(player, "smithing", "place");

        Bukkit.getScheduler().runTaskLater(plugin, () -> updateResultSlot(event.getInventory()), 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String title = event.getView().getTitle();
        if (isOurSmithingTitle(title)) {
            inSmithingGUI.remove(player.getUniqueId());
            returnItemsToPlayer(player, event.getInventory());
        }
    }

    private void updateResultSlot(Inventory inventory) {
        ItemStack itemToUpgrade = inventory.getItem(itemSlot);

        if (itemToUpgrade == null || itemToUpgrade.getType() == Material.AIR) {
            inventory.setItem(resultSlot, null);
            return;
        }

        // Find the recipe for this item
        RecipeManager.CustomRecipe recipe = findRecipeForItem(itemToUpgrade);
        if (recipe == null) {
            inventory.setItem(resultSlot, null);
            return;
        }

        // Calculate current star level using LoreManager
        int currentStars = loreManager.getStarLevel(itemToUpgrade);
        int nextStarLevel = currentStars + 1;

        // Check if max stars reached using LoreManager
        int maxStars = loreManager.getMaxStarLevel();
        if (nextStarLevel > maxStars) {
            inventory.setItem(resultSlot, createMaxStarsItem());
            return;
        }

        // Calculate required materials for next star (25% increase per star)
        Map<String, Integer> requiredMaterials = calculateRequiredMaterials(recipe, nextStarLevel);

        // Check if player has enough materials
        boolean canUpgrade = hasEnoughMaterials(inventory, requiredMaterials);

        if (canUpgrade) {
            // Create upgraded item using LoreManager
            ItemStack upgradedItem = loreManager.updateItemStats(itemToUpgrade.clone(), nextStarLevel);

            // Create a preview version with cost lore
            ItemStack previewItem = addCostLorePreview(upgradedItem, requiredMaterials);

            inventory.setItem(resultSlot, previewItem);
        } else {
            inventory.setItem(resultSlot, createCostPreviewItem(requiredMaterials));
        }
    }


    private RecipeManager.CustomRecipe findRecipeForItem(ItemStack item) {
        String itemId = getItemIdFromItemStack(item);
        if (itemId == null) return null;

        List<RecipeManager.CustomRecipe> recipes = recipeManager.getRecipesForItem(itemId);
        if (recipes.isEmpty()) return null;

        return recipes.get(0);
    }

    private String getItemIdFromItemStack(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        NamespacedKey key = new NamespacedKey(plugin, "id");

        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }


    private Map<String, Integer> calculateRequiredMaterials(RecipeManager.CustomRecipe recipe, int starLevel) {
        Map<String, Integer> requiredMaterials = new HashMap<>();

        double multiplier = 1.0 + (SmithingTask.getPriceMultiplierPerStar() * starLevel);

        for (Map.Entry<String, Integer> entry : recipe.getIngredients().entrySet()) {
            String material = entry.getKey();
            int baseAmount = entry.getValue();

            int increasedAmount = (int) Math.ceil(baseAmount * multiplier);
            requiredMaterials.put(material, increasedAmount);
        }

        return requiredMaterials;
    }


    private boolean hasEnoughMaterials(Inventory inventory, Map<String, Integer> requiredMaterials) {
        Map<String, Integer> availableMaterials = getAvailableMaterials(inventory);

        for (Map.Entry<String, Integer> required : requiredMaterials.entrySet()) {
            String material = required.getKey();
            int requiredAmount = required.getValue();
            int availableAmount = availableMaterials.getOrDefault(material, 0);

            if (availableAmount < requiredAmount) {
                return false;
            }
        }

        return true;
    }

    private Map<String, Integer> getAvailableMaterials(Inventory inventory) {
        Map<String, Integer> available = new HashMap<>();

        for (int slot : airSlots) {
            if (slot == itemSlot || slot == resultSlot) continue;

            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                String materialId = recipeManager.getIngredientId(item);
                if (materialId != null) {
                    available.put(materialId, available.getOrDefault(materialId, 0) + item.getAmount());
                }
            }
        }

        return available;
    }

    private ItemStack addCostLorePreview(ItemStack item, Map<String, Integer> requiredMaterials) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

        // Remove any existing cost lore first
        lore.removeIf(line -> {
            String stripped = ChatColor.stripColor(line);
            return stripped.contains("Upgrade Cost") || stripped.contains("Required materials") ||
                    stripped.contains("Click to upgrade") || stripped.contains("Add the required materials");
        });

        // Add cost information (preview only)
        lore.add("");
        lore.add(ChatColor.GRAY + "Upgrade Cost:");
        for (Map.Entry<String, Integer> entry : requiredMaterials.entrySet()) {
            String materialName = getMaterialDisplayName(entry.getKey());
            lore.add(ChatColor.WHITE + "• " + materialName + ChatColor.GRAY + " x" + entry.getValue());
        }
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to upgrade!");

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCostPreviewItem(Map<String, Integer> requiredMaterials) {
        ItemStack preview = new ItemStack(Material.BARRIER);
        ItemMeta meta = preview.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Missing Materials");

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Required materials:");

            for (Map.Entry<String, Integer> entry : requiredMaterials.entrySet()) {
                String materialName = getMaterialDisplayName(entry.getKey());
                lore.add(ChatColor.WHITE + "• " + materialName + ChatColor.GRAY + " x" + entry.getValue());
            }

            lore.add("");
            lore.add(ChatColor.RED + "Add the required materials to upgrade");

            meta.setLore(lore);
            preview.setItemMeta(meta);
        }

        return preview;
    }

    private ItemStack createMaxStarsItem() {
        ItemStack maxStars = new ItemStack(Material.BARRIER);
        ItemMeta meta = maxStars.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Maximum Stars Reached");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "This item has reached the maximum star level.");
            lore.add(ChatColor.GRAY + "Cannot upgrade further.");
            meta.setLore(lore);
            maxStars.setItemMeta(meta);
        }

        return maxStars;
    }


    private boolean consumeMaterialsAndGiveResult(Player player, Inventory inventory, ItemStack resultItem) {
        ItemStack originalItem = inventory.getItem(itemSlot);
        if (originalItem == null) return false;

        // Find the recipe for this item
        RecipeManager.CustomRecipe recipe = findRecipeForItem(originalItem);
        if (recipe == null) return false;

        int currentStars = loreManager.getStarLevel(originalItem);
        int newStarLevel = currentStars + 1;

        // Check if max stars reached
        int maxStars = loreManager.getMaxStarLevel();
        if (newStarLevel > maxStars) {
            player.sendMessage(ChatColor.RED + "This item has reached the maximum star level!");
            return false;
        }

        Map<String, Integer> requiredMaterials = calculateRequiredMaterials(recipe, newStarLevel);

        if (!hasEnoughMaterials(inventory, requiredMaterials)) {
            return false;
        }

        // Consume materials - FIX: Improved material consumption logic
        Map<String, Integer> materialsToConsume = new HashMap<>(requiredMaterials);

        // Create a list of material slots to process
        List<Integer> materialSlots = new ArrayList<>();
        for (int slot : airSlots) {
            if (slot != itemSlot && slot != resultSlot) {
                materialSlots.add(slot);
            }
        }

        // Sort by amount to consume smaller stacks first (optimization)
        materialSlots.sort((s1, s2) -> {
            ItemStack i1 = inventory.getItem(s1);
            ItemStack i2 = inventory.getItem(s2);
            if (i1 == null && i2 == null) return 0;
            if (i1 == null) return 1;
            if (i2 == null) return -1;
            return Integer.compare(i1.getAmount(), i2.getAmount());
        });

        for (int slot : materialSlots) {
            if (materialsToConsume.isEmpty()) break;

            ItemStack materialItem = inventory.getItem(slot);
            if (materialItem == null || materialItem.getType() == Material.AIR) continue;

            String materialId = recipeManager.getIngredientId(materialItem);
            if (materialId == null || !materialsToConsume.containsKey(materialId)) continue;

            int needed = materialsToConsume.get(materialId);
            int available = materialItem.getAmount();

            if (available >= needed) {
                if (available == needed) {
                    inventory.setItem(slot, null);
                } else {
                    materialItem.setAmount(available - needed);
                    inventory.setItem(slot, materialItem);
                }
                materialsToConsume.remove(materialId);
            } else {
                inventory.setItem(slot, null);
                materialsToConsume.put(materialId, needed - available);
            }
        }

        // If we still have materials to consume, it means something went wrong
        if (!materialsToConsume.isEmpty()) {
            return false;
        }

        // Remove original item
        ItemStack originalItemClone = originalItem.clone();
        inventory.setItem(itemSlot, null);

        // Create the actual upgraded item using LoreManager (without cost lore)
        ItemStack actualUpgradedItem = loreManager.updateItemStats(originalItemClone, newStarLevel);

        // Give upgraded item to player
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(actualUpgradedItem);
        if (!leftover.isEmpty()) {
            for (ItemStack item : leftover.values()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
        }

        return true;
    }
    private void returnItemsToPlayer(Player player, Inventory inventory) {
        for (int slot : airSlots) {
            if (slot == resultSlot) continue;

            ItemStack item = inventory.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack leftItem : leftover.values()) {
                        player.getWorld().dropItem(player.getLocation(), leftItem);
                    }
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
        return slot < invSize && !isSmithingSlot(slot) && slot != resultSlot;
    }

    private boolean isSmithingSlot(int slot) {
        for (int airSlot : airSlots) {
            if (airSlot == slot) return true;
        }
        return false;
    }

    private boolean isOurSmithingTitle(String title) {
        if (title == null) return false;
        String stripped = title.trim();
        return stripped.equalsIgnoreCase(guiTitle);
    }

    private String getMaterialDisplayName(String materialId) {
        RecipeManager.CustomIngredient ingredient = recipeManager.getIngredient(materialId);
        if (ingredient != null) {
            return ChatColor.translateAlternateColorCodes('&', ingredient.getDisplayName());
        }

        Material material = Material.matchMaterial(materialId.toUpperCase());
        if (material != null) {
            return material.name().toLowerCase().replace('_', ' ');
        }

        return materialId;
    }
}