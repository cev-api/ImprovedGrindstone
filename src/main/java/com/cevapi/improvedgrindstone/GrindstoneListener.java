package com.cevapi.improvedgrindstone;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

public final class GrindstoneListener implements Listener {
    private final GrindstoneBookPlugin plugin;

    public GrindstoneListener(GrindstoneBookPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (!plugin.isFeatureEnabled()) {
            return;
        }

        GrindstoneInventory inventory = event.getInventory();
        ItemStack input = inventory.getItem(0);
        ItemStack bookSlot = inventory.getItem(1);

        if (!isBook(bookSlot) || isEmpty(input)) {
            return;
        }

        Map<Enchantment, Integer> stored = getEnchantmentsToStore(input);
        if (stored.isEmpty()) {
            event.setResult(null);
            return;
        }

        ItemStack result = buildDisenchantedResult(input, stored);
        event.setResult(result);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.isFeatureEnabled()) {
            return;
        }

        InventoryView view = event.getView();
        Inventory topInventory = view.getTopInventory();
        if (!(topInventory instanceof GrindstoneInventory)) {
            return;
        }

        if (handleBookSlotClick(event, (GrindstoneInventory) topInventory)) {
            return;
        }

        if (handleShiftClickBook(event, (GrindstoneInventory) topInventory)) {
            return;
        }

        if (event.getSlotType() == SlotType.RESULT) {
            handleResultClick(event, (GrindstoneInventory) topInventory);
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory instanceof GrindstoneInventory)) {
            return;
        }

        if (!event.getRawSlots().contains(1)) {
            return;
        }

        if (!plugin.isFeatureEnabled()) {
            return;
        }

        ItemStack cursor = event.getOldCursor();
        if (!isBook(cursor)) {
            return;
        }

        GrindstoneInventory grindstone = (GrindstoneInventory) inventory;
        if (!isEmpty(grindstone.getItem(1))) {
            return;
        }

        event.setCancelled(true);
        grindstone.setItem(1, new ItemStack(Material.BOOK));
        event.setCursor(removeOne(cursor));
    }

    private void handleResultClick(InventoryClickEvent event, GrindstoneInventory inventory) {
        ItemStack current = event.getCurrentItem();
        if (isEmpty(current)) {
            return;
        }

        ItemStack input = inventory.getItem(0);
        ItemStack bookSlot = inventory.getItem(1);
        if (isEmpty(input) || !isBook(bookSlot)) {
            return;
        }

        Map<Enchantment, Integer> enchantments = getEnchantmentsToStore(input);
        if (enchantments.isEmpty()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (!plugin.isGrantXpWhenBook()) {
            if (!moveResultWithoutXp(event, player, current)) {
                return;
            }

            inventory.setItem(0, null);
            inventory.setItem(1, null);
            inventory.setItem(2, null);

            giveEnchantedBook(player, enchantments);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1f);
            player.updateInventory();
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!plugin.isFeatureEnabled() || !player.isOnline()) {
                return;
            }

            if (player.getOpenInventory().getTopInventory() != inventory) {
                return;
            }

            ItemStack slot1 = inventory.getItem(1);
            if (isBook(slot1)) {
                inventory.setItem(1, null);
            }

            giveEnchantedBook(player, enchantments);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1f);
        });
    }

    private boolean handleBookSlotClick(InventoryClickEvent event, GrindstoneInventory inventory) {
        if (event.getRawSlot() != 1) {
            return false;
        }

        if (event.getClickedInventory() != inventory) {
            return false;
        }

        ItemStack cursor = event.getCursor();
        ItemStack slotItem = event.getCurrentItem();

        if (isBook(cursor) && isEmpty(slotItem)) {
            event.setCancelled(true);
            inventory.setItem(1, new ItemStack(Material.BOOK));
            event.setCursor(removeOne(cursor));
            return true;
        }

        if (isEmpty(cursor) && isBook(slotItem)) {
            event.setCancelled(true);
            event.setCursor(slotItem);
            inventory.setItem(1, null);
            return true;
        }

        return false;
    }

    private boolean handleShiftClickBook(InventoryClickEvent event, GrindstoneInventory inventory) {
        if (!event.isShiftClick()) {
            return false;
        }

        if (!(event.getClickedInventory() instanceof org.bukkit.inventory.PlayerInventory)) {
            return false;
        }

        ItemStack current = event.getCurrentItem();
        if (!isBook(current)) {
            return false;
        }

        if (!isEmpty(inventory.getItem(1))) {
            return false;
        }

        event.setCancelled(true);
        inventory.setItem(1, new ItemStack(Material.BOOK));
        event.getClickedInventory().setItem(event.getSlot(), removeOne(current));
        return true;
    }

    private ItemStack buildEnchantedBook(Map<Enchantment, Integer> enchantments) {
        ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) enchantedBook.getItemMeta();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
        }
        enchantedBook.setItemMeta(meta);
        return enchantedBook;
    }

    private void giveEnchantedBook(Player player, Map<Enchantment, Integer> enchantments) {
        ItemStack enchantedBook = buildEnchantedBook(enchantments);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(enchantedBook);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private boolean moveResultWithoutXp(InventoryClickEvent event, Player player, ItemStack result) {
        if (!event.isShiftClick()) {
            ItemStack cursor = event.getCursor();
            if (!isEmpty(cursor)) {
                return false;
            }
            event.setCancelled(true);
            event.setCursor(result.clone());
            return true;
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(result.clone());
        if (!leftovers.isEmpty()) {
            return false;
        }
        event.setCancelled(true);
        return true;
    }

    private ItemStack buildDisenchantedResult(ItemStack input, Map<Enchantment, Integer> toRemove) {
        ItemStack result = input.clone();
        result.setAmount(1);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            for (Enchantment enchantment : toRemove.keySet()) {
                meta.removeEnchant(enchantment);
            }
            result.setItemMeta(meta);
        }
        return result;
    }

    private Map<Enchantment, Integer> getEnchantmentsToStore(ItemStack input) {
        Map<Enchantment, Integer> filtered = new LinkedHashMap<>();
        input.getEnchantments().forEach((ench, level) -> {
            if (!ench.isCursed() || plugin.isCaptureCursed()) {
                filtered.put(ench, level);
            }
        });
        return filtered;
    }

    private boolean isBook(ItemStack stack) {
        return stack != null && stack.getType() == Material.BOOK;
    }

    private boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType() == Material.AIR;
    }

    private ItemStack removeOne(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        int amount = stack.getAmount();
        if (amount <= 1) {
            return null;
        }
        ItemStack remaining = stack.clone();
        remaining.setAmount(amount - 1);
        return remaining;
    }
}
