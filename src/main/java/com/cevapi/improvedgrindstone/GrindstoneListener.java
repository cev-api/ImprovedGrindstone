package com.cevapi.improvedgrindstone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.inventory.GrindstoneInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.util.io.BukkitObjectOutputStream;

public final class GrindstoneListener implements Listener {
    private static final Pattern NAMESPACED_ID_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9/._-]+$");

    private final GrindstoneBookPlugin plugin;

    public GrindstoneListener(GrindstoneBookPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (!plugin.isFeatureEnabled()) {
            return;
        }

        OperationResolution resolution = resolveOperation(event.getInventory());
        if (resolution.blockedReason() != null) {
            event.setResult(null);
            return;
        }

        OperationContext context = resolution.context();
        if (context != null) {
            event.setResult(context.result());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!plugin.isFeatureEnabled()) {
            return;
        }

        InventoryView view = event.getView();
        Inventory topInventory = view.getTopInventory();
        if (!(topInventory instanceof GrindstoneInventory inventory)) {
            return;
        }

        if (handleBookSlotClick(event, inventory)) {
            return;
        }

        if (handleShiftClickBook(event, inventory)) {
            return;
        }

        if (event.getRawSlot() != 2 || event.getClickedInventory() != inventory) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        OperationResolution resolution = resolveOperation(inventory);
        if (resolution.blockedReason() != null) {
            event.setCancelled(true);
            player.sendMessage(color("&c" + resolution.blockedReason()));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.9f);
            return;
        }

        OperationContext context = resolution.context();
        if (context == null) {
            return;
        }

        event.setCancelled(true);

        int xpCost = context.xpCostLevels();
        if (xpCost > 0 && player.getLevel() < xpCost) {
            player.sendMessage(color("&cYou need " + xpCost + " levels to " + context.xpReason() + "."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.9f);
            return;
        }

        if (!moveResultToPlayer(event, player, context.result())) {
            return;
        }

        if (xpCost > 0) {
            player.giveExpLevels(-xpCost);
        }

        consumeOne(inventory, 0);
        consumeOne(inventory, 1);
        inventory.setItem(2, null);

        giveItem(player, context.bonusItem());
        player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.8f, 1f);
        player.updateInventory();
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory instanceof GrindstoneInventory grindstone)) {
            return;
        }

        if (!plugin.isFeatureEnabled() || !event.getRawSlots().contains(1)) {
            return;
        }

        ItemStack cursor = event.getOldCursor();
        if (!isBook(cursor) || !isEmpty(grindstone.getItem(1))) {
            return;
        }

        event.setCancelled(true);
        grindstone.setItem(1, extractOne(cursor));
        event.setCursor(removeOne(cursor));
    }

    private OperationResolution resolveOperation(GrindstoneInventory inventory) {
        ItemStack source = inventory.getItem(0);
        ItemStack secondSlot = inventory.getItem(1);

        if (isEmpty(source) || isEmpty(secondSlot)) {
            return OperationResolution.none();
        }

        Map<Enchantment, Integer> enchantments = getEnchantmentsToStore(source);
        if (enchantments.isEmpty()) {
            return OperationResolution.none();
        }

        if (isValidBookForExtraction(secondSlot)) {
            ItemStack result = buildDisenchantedResult(source, enchantments);
            ItemStack bonus = buildEnchantedBook(enchantments);
            if (bonus == null) {
                return OperationResolution.blocked("Those enchantments cannot be stored safely in a book.");
            }

            String validationError = validateOperation(source, result, bonus, enchantments.size());
            if (validationError != null) {
                return OperationResolution.blocked(validationError);
            }

            int xpCost = plugin.isBookXpCostEnabled()
                    ? calculateXpCost(enchantments, plugin.getBookXpCostPercent())
                    : 0;
            return OperationResolution.ready(new OperationContext(result, bonus, xpCost, "extract enchantments into a book"));
        }

        if (!plugin.isTransferEnabled() || !isTransferCandidate(source, secondSlot)) {
            return OperationResolution.none();
        }

        ItemStack result = buildTransferredItem(secondSlot, enchantments);
        ItemStack bonus = buildDisenchantedResult(source, enchantments);
        String validationError = validateOperation(source, result, bonus, enchantments.size());
        if (validationError != null) {
            return OperationResolution.blocked(validationError);
        }
        int xpCost = plugin.isTransferXpCostEnabled()
                ? calculateXpCost(enchantments, plugin.getTransferXpCostPercent())
                : 0;
        return OperationResolution.ready(new OperationContext(result, bonus, xpCost, "transfer enchantments to another item"));
    }

    private boolean handleBookSlotClick(InventoryClickEvent event, GrindstoneInventory inventory) {
        if (event.getRawSlot() != 1 || event.getClickedInventory() != inventory) {
            return false;
        }

        ItemStack cursor = event.getCursor();
        ItemStack slotItem = event.getCurrentItem();

        if (isBook(cursor) && isEmpty(slotItem)) {
            event.setCancelled(true);
            inventory.setItem(1, extractOne(cursor));
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
        if (!isBook(current) || !isEmpty(inventory.getItem(1))) {
            return false;
        }

        event.setCancelled(true);
        inventory.setItem(1, extractOne(current));
        event.getClickedInventory().setItem(event.getSlot(), removeOne(current));
        return true;
    }

    private boolean moveResultToPlayer(InventoryClickEvent event, Player player, ItemStack result) {
        if (!event.isShiftClick()) {
            if (event.getClick().isKeyboardClick() || !isEmpty(event.getCursor())) {
                return false;
            }

            event.setCursor(result.clone());
            return true;
        }

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(result.clone());
        return leftovers.isEmpty();
    }

    private ItemStack buildEnchantedBook(Map<Enchantment, Integer> enchantments) {
        ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) enchantedBook.getItemMeta();
        if (meta == null) {
            return null;
        }

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            boolean added = meta.addStoredEnchant(entry.getKey(), entry.getValue(), true);
            if (!added) {
                return null;
            }
        }

        enchantedBook.setItemMeta(meta);
        return enchantedBook;
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

    private ItemStack buildTransferredItem(ItemStack target, Map<Enchantment, Integer> enchantments) {
        ItemStack result = target.clone();
        result.setAmount(1);

        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return result;
        }

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            int mergedLevel = Math.max(meta.getEnchantLevel(entry.getKey()), entry.getValue());
            meta.addEnchant(entry.getKey(), mergedLevel, true);
        }

        result.setItemMeta(meta);
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

    private String validateOperation(ItemStack source, ItemStack result, ItemStack bonus, int enchantCount) {
        if (enchantCount > plugin.getMaxEnchantsPerOperation()) {
            return "This item has too many enchantments (" + enchantCount + "/" + plugin.getMaxEnchantsPerOperation()
                    + ") to process safely.";
        }

        int maxBytes = plugin.getMaxOperationItemBytes();
        if (estimateSerializedBytes(source) > maxBytes
                || estimateSerializedBytes(result) > maxBytes
                || estimateSerializedBytes(bonus) > maxBytes) {
            return "This item's NBT payload is too large to process safely in the grindstone.";
        }

        return null;
    }

    private int estimateSerializedBytes(ItemStack item) {
        if (isEmpty(item)) {
            return 0;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BukkitObjectOutputStream out = new BukkitObjectOutputStream(baos)) {
            out.writeObject(item);
            out.flush();
            return baos.size();
        } catch (IOException ignored) {
            // If serialization fails, treat as unsafe rather than risking item loss.
            return Integer.MAX_VALUE;
        }
    }

    private int calculateXpCost(Map<Enchantment, Integer> enchantments, double percent) {
        if (percent <= 0.0d) {
            return 0;
        }

        int baseCost = estimateEnchantingTableCost(enchantments);
        if (baseCost <= 0) {
            return 0;
        }

        int computed = (int) Math.ceil(baseCost * (percent / 100.0d));
        return Math.max(1, computed);
    }

    private int estimateEnchantingTableCost(Map<Enchantment, Integer> enchantments) {
        int highest = 1;
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            highest = Math.max(highest, estimateEnchantingTableCost(entry.getKey(), entry.getValue()));
        }
        // Enchanting table level cost caps at 30, so match that scale.
        return Math.max(1, Math.min(30, highest));
    }

    private int estimateEnchantingTableCost(Enchantment enchantment, int level) {
        if (!isVanillaEnchantment(enchantment)) {
            return estimateCustomEquivalentCost(level);
        }

        Integer min = invokeIntWithArg(enchantment, "getMinModifiedCost", level);
        Integer max = invokeIntWithArg(enchantment, "getMaxModifiedCost", level);

        if (min == null) {
            min = invokeIntWithArg(enchantment, "getMinCost", level);
        }

        if (max == null) {
            max = invokeIntWithArg(enchantment, "getMaxCost", level);
        }

        if (min != null && max != null) {
            return Math.max(1, (int) Math.ceil((min + max) / 2.0d));
        }

        if (min != null) {
            return Math.max(1, min);
        }

        Integer anvilCost = invokeIntNoArg(enchantment, "getAnvilCost");
        if (anvilCost != null) {
            return Math.max(1, anvilCost * Math.max(1, level));
        }

        return Math.max(1, 5 + (level * 3));
    }

    private int estimateCustomEquivalentCost(int level) {
        int safeLevel = Math.max(1, level);
        int highest = 0;
        int highestAtMaxVanilla = 0;
        int highestVanillaLevel = 1;

        for (Enchantment enchantment : Enchantment.values()) {
            if (!isVanillaEnchantment(enchantment)) {
                continue;
            }

            int maxLevel = Math.max(1, enchantment.getMaxLevel());
            int boundedLevel = Math.min(safeLevel, maxLevel);
            int cost = estimateEnchantingTableCost(enchantment, boundedLevel);
            if (cost > highest) {
                highest = cost;
            }

            int maxLevelCost = estimateEnchantingTableCost(enchantment, maxLevel);
            if (maxLevelCost > highestAtMaxVanilla) {
                highestAtMaxVanilla = maxLevelCost;
                highestVanillaLevel = maxLevel;
            }
        }

        if (highest > 0) {
            return highest;
        }

        // Fallback path for atypical environments: keep costs increasing for very high levels.
        int extrapolated = highestAtMaxVanilla + Math.max(0, safeLevel - highestVanillaLevel) * 6;
        return Math.max(1, extrapolated);
    }

    private boolean isVanillaEnchantment(Enchantment enchantment) {
        return enchantment.getKey().getNamespace().equalsIgnoreCase("minecraft");
    }

    private Integer invokeIntWithArg(Object target, String methodName, int arg) {
        try {
            Method method = target.getClass().getMethod(methodName, int.class);
            Object result = method.invoke(target, arg);
            if (result instanceof Number number) {
                return number.intValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // API method does not exist in this server version.
        }
        return null;
    }

    private Integer invokeIntNoArg(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof Number number) {
                return number.intValue();
            }
        } catch (ReflectiveOperationException ignored) {
            // API method does not exist in this server version.
        }
        return null;
    }

    private boolean isValidBookForExtraction(ItemStack stack) {
        if (!isBook(stack)) {
            return false;
        }

        if (!plugin.isRequireSpecialBook()) {
            return true;
        }

        return isSpecialBook(stack);
    }

    private boolean isSpecialBook(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }

        Set<String> configuredIds = plugin.getSpecialBookIds();
        String itemModel = getItemModelId(meta);
        if (itemModel != null) {
            if (configuredIds.isEmpty() && !itemModel.startsWith("minecraft:")) {
                return true;
            }

            if (configuredIds.contains(itemModel)) {
                return true;
            }
        }

        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
        for (org.bukkit.NamespacedKey key : dataContainer.getKeys()) {
            String keyText = key.asString().toLowerCase(Locale.ROOT);
            if (configuredIds.contains(keyText)) {
                return true;
            }

            if (configuredIds.isEmpty() && !key.getNamespace().equalsIgnoreCase("minecraft")) {
                return true;
            }
        }

        if (meta.hasDisplayName()) {
            String displayName = ChatColor.stripColor(meta.getDisplayName());
            if (displayName != null) {
                String normalized = displayName.toLowerCase(Locale.ROOT).trim();
                if (configuredIds.contains(normalized)) {
                    return true;
                }

                if (configuredIds.isEmpty()
                        && NAMESPACED_ID_PATTERN.matcher(normalized).matches()
                        && !normalized.startsWith("minecraft:")) {
                    return true;
                }
            }
        }

        return false;
    }

    private String getItemModelId(ItemMeta meta) {
        try {
            Method hasItemModel = meta.getClass().getMethod("hasItemModel");
            Object hasValue = hasItemModel.invoke(meta);
            if (!(hasValue instanceof Boolean hasModel) || !hasModel) {
                return null;
            }

            Method getItemModel = meta.getClass().getMethod("getItemModel");
            Object value = getItemModel.invoke(meta);
            if (value == null) {
                return null;
            }

            return value.toString().toLowerCase(Locale.ROOT);
        } catch (ReflectiveOperationException ignored) {
            // API method does not exist in this server version.
            return null;
        }
    }

    private boolean isTransferCandidate(ItemStack source, ItemStack target) {
        if (isBook(target)) {
            return false;
        }

        String sourceArchetype = getArchetype(source.getType());
        String targetArchetype = getArchetype(target.getType());
        return sourceArchetype.equals(targetArchetype);
    }

    private String getArchetype(Material material) {
        String name = material.name();

        if (name.endsWith("_HELMET")) {
            return "helmet";
        }

        if (name.endsWith("_CHESTPLATE")) {
            return "chestplate";
        }

        if (name.endsWith("_LEGGINGS")) {
            return "leggings";
        }

        if (name.endsWith("_BOOTS")) {
            return "boots";
        }

        if (name.endsWith("_SWORD")) {
            return "sword";
        }

        if (name.endsWith("_AXE")) {
            return "axe";
        }

        if (name.endsWith("_PICKAXE")) {
            return "pickaxe";
        }

        if (name.endsWith("_SHOVEL")) {
            return "shovel";
        }

        if (name.endsWith("_HOE")) {
            return "hoe";
        }

        if (name.endsWith("_HORSE_ARMOR")) {
            return "horse_armor";
        }

        if (name.equals("BOW") || name.equals("CROSSBOW") || name.equals("TRIDENT") || name.equals("MACE")
                || name.equals("ELYTRA") || name.equals("FISHING_ROD") || name.equals("SHEARS")
                || name.equals("SHIELD")) {
            return name.toLowerCase(Locale.ROOT);
        }

        return name;
    }

    private void giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private void consumeOne(GrindstoneInventory inventory, int slot) {
        ItemStack stack = inventory.getItem(slot);
        if (isEmpty(stack)) {
            return;
        }

        if (stack.getAmount() <= 1) {
            inventory.setItem(slot, null);
            return;
        }

        ItemStack reduced = stack.clone();
        reduced.setAmount(stack.getAmount() - 1);
        inventory.setItem(slot, reduced);
    }

    private boolean isBook(ItemStack stack) {
        return stack != null && stack.getType() == Material.BOOK;
    }

    private boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType() == Material.AIR;
    }

    private ItemStack extractOne(ItemStack stack) {
        if (isEmpty(stack)) {
            return null;
        }
        ItemStack one = stack.clone();
        one.setAmount(1);
        return one;
    }

    private ItemStack removeOne(ItemStack stack) {
        if (isEmpty(stack)) {
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

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private record OperationContext(ItemStack result, ItemStack bonusItem, int xpCostLevels, String xpReason) {
    }

    private record OperationResolution(OperationContext context, String blockedReason) {
        private static OperationResolution none() {
            return new OperationResolution(null, null);
        }

        private static OperationResolution ready(OperationContext context) {
            return new OperationResolution(context, null);
        }

        private static OperationResolution blocked(String blockedReason) {
            return new OperationResolution(null, blockedReason);
        }
    }
}
