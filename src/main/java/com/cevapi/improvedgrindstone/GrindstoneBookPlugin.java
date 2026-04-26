package com.cevapi.improvedgrindstone;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class GrindstoneBookPlugin extends JavaPlugin {
    private static final String CONFIG_KEY = "feature-enabled";
    private static final String CONFIG_CAPTURE_CURSED_KEY = "capture-cursed";
    private static final String CONFIG_LEGACY_GRANT_XP_KEY = "grant-xp-when-book";
    private static final String CONFIG_BOOK_XP_COST_ENABLED_KEY = "book-xp-cost-enabled";
    private static final String CONFIG_BOOK_XP_COST_PERCENT_KEY = "book-xp-cost-percent";
    private static final String CONFIG_TRANSFER_ENABLED_KEY = "transfer-enabled";
    private static final String CONFIG_TRANSFER_XP_COST_ENABLED_KEY = "transfer-xp-cost-enabled";
    private static final String CONFIG_TRANSFER_XP_COST_PERCENT_KEY = "transfer-xp-cost-percent";
    private static final String CONFIG_REQUIRE_SPECIAL_BOOK_KEY = "require-special-book";
    private static final String CONFIG_SPECIAL_BOOK_IDS_KEY = "special-book-ids";
    private static final String CONFIG_MAX_ENCHANTS_PER_OPERATION_KEY = "max-enchants-per-operation";
    private static final String CONFIG_MAX_OPERATION_ITEM_BYTES_KEY = "max-operation-item-bytes";

    private boolean featureEnabled;
    private boolean captureCursed;
    private boolean bookXpCostEnabled;
    private double bookXpCostPercent;
    private boolean transferEnabled;
    private boolean transferXpCostEnabled;
    private double transferXpCostPercent;
    private boolean requireSpecialBook;
    private Set<String> specialBookIds = new LinkedHashSet<>();
    private int maxEnchantsPerOperation;
    private int maxOperationItemBytes;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        getServer().getPluginManager().registerEvents(new GrindstoneListener(this), this);
        PluginCommand pluginCommand = getCommand("improvedgrindstone");
        if (pluginCommand != null) {
            pluginCommand.setExecutor(new GrindstoneBookCommand(this));
            pluginCommand.setTabCompleter(new GrindstoneTabCompleter());
        }
        getLogger().log(Level.INFO,
                "Grindstone book transfer is {0}. Cursed capture is {1}. Book XP cost is {2} ({3}%). Transfer mode is {4}. Transfer XP cost is {5} ({6}%). Special books required is {7}.",
                new Object[]{
                        featureEnabled ? "enabled" : "disabled",
                        captureCursed ? "enabled" : "disabled",
                        bookXpCostEnabled ? "enabled" : "disabled",
                        bookXpCostPercent,
                        transferEnabled ? "enabled" : "disabled",
                        transferXpCostEnabled ? "enabled" : "disabled",
                        transferXpCostPercent,
                        requireSpecialBook ? "enabled" : "disabled"
                });
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        featureEnabled = config.getBoolean(CONFIG_KEY, true);
        captureCursed = config.getBoolean(CONFIG_CAPTURE_CURSED_KEY, true);
        bookXpCostEnabled = config.contains(CONFIG_BOOK_XP_COST_ENABLED_KEY)
                ? config.getBoolean(CONFIG_BOOK_XP_COST_ENABLED_KEY, true)
                : config.getBoolean(CONFIG_LEGACY_GRANT_XP_KEY, true);
        bookXpCostPercent = clampPercent(config.getDouble(CONFIG_BOOK_XP_COST_PERCENT_KEY, 50.0d));
        transferEnabled = config.getBoolean(CONFIG_TRANSFER_ENABLED_KEY, true);
        transferXpCostEnabled = config.getBoolean(CONFIG_TRANSFER_XP_COST_ENABLED_KEY, true);
        transferXpCostPercent = clampPercent(config.getDouble(CONFIG_TRANSFER_XP_COST_PERCENT_KEY, 50.0d));
        requireSpecialBook = config.getBoolean(CONFIG_REQUIRE_SPECIAL_BOOK_KEY, false);
        specialBookIds = config.getStringList(CONFIG_SPECIAL_BOOK_IDS_KEY)
                .stream()
                .map(value -> value.toLowerCase(Locale.ROOT).trim())
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        maxEnchantsPerOperation = clampPositiveInt(config.getInt(CONFIG_MAX_ENCHANTS_PER_OPERATION_KEY, 16), 1, 200, 16);
        maxOperationItemBytes = clampPositiveInt(config.getInt(CONFIG_MAX_OPERATION_ITEM_BYTES_KEY, 24576), 1024, 262144,
                24576);

        config.set(CONFIG_KEY, featureEnabled);
        config.set(CONFIG_CAPTURE_CURSED_KEY, captureCursed);
        config.set(CONFIG_BOOK_XP_COST_ENABLED_KEY, bookXpCostEnabled);
        config.set(CONFIG_BOOK_XP_COST_PERCENT_KEY, bookXpCostPercent);
        config.set(CONFIG_TRANSFER_ENABLED_KEY, transferEnabled);
        config.set(CONFIG_TRANSFER_XP_COST_ENABLED_KEY, transferXpCostEnabled);
        config.set(CONFIG_TRANSFER_XP_COST_PERCENT_KEY, transferXpCostPercent);
        config.set(CONFIG_REQUIRE_SPECIAL_BOOK_KEY, requireSpecialBook);
        config.set(CONFIG_SPECIAL_BOOK_IDS_KEY, specialBookIds.stream().toList());
        config.set(CONFIG_MAX_ENCHANTS_PER_OPERATION_KEY, maxEnchantsPerOperation);
        config.set(CONFIG_MAX_OPERATION_ITEM_BYTES_KEY, maxOperationItemBytes);
        saveConfig();
    }

    public boolean isFeatureEnabled() {
        return featureEnabled;
    }

    public void setFeatureEnabled(boolean enabled) {
        featureEnabled = enabled;
        getConfig().set(CONFIG_KEY, enabled);
        saveConfig();
    }

    public boolean isCaptureCursed() {
        return captureCursed;
    }

    public void setCaptureCursed(boolean captureCursed) {
        this.captureCursed = captureCursed;
        getConfig().set(CONFIG_CAPTURE_CURSED_KEY, captureCursed);
        saveConfig();
    }

    public boolean isBookXpCostEnabled() {
        return bookXpCostEnabled;
    }

    public void setBookXpCostEnabled(boolean enabled) {
        bookXpCostEnabled = enabled;
        getConfig().set(CONFIG_BOOK_XP_COST_ENABLED_KEY, enabled);
        saveConfig();
    }

    public double getBookXpCostPercent() {
        return bookXpCostPercent;
    }

    public void setBookXpCostPercent(double percent) {
        bookXpCostPercent = clampPercent(percent);
        getConfig().set(CONFIG_BOOK_XP_COST_PERCENT_KEY, bookXpCostPercent);
        saveConfig();
    }

    public boolean isTransferEnabled() {
        return transferEnabled;
    }

    public void setTransferEnabled(boolean enabled) {
        transferEnabled = enabled;
        getConfig().set(CONFIG_TRANSFER_ENABLED_KEY, enabled);
        saveConfig();
    }

    public boolean isTransferXpCostEnabled() {
        return transferXpCostEnabled;
    }

    public void setTransferXpCostEnabled(boolean enabled) {
        transferXpCostEnabled = enabled;
        getConfig().set(CONFIG_TRANSFER_XP_COST_ENABLED_KEY, enabled);
        saveConfig();
    }

    public double getTransferXpCostPercent() {
        return transferXpCostPercent;
    }

    public void setTransferXpCostPercent(double percent) {
        transferXpCostPercent = clampPercent(percent);
        getConfig().set(CONFIG_TRANSFER_XP_COST_PERCENT_KEY, transferXpCostPercent);
        saveConfig();
    }

    public boolean isRequireSpecialBook() {
        return requireSpecialBook;
    }

    public void setRequireSpecialBook(boolean required) {
        requireSpecialBook = required;
        getConfig().set(CONFIG_REQUIRE_SPECIAL_BOOK_KEY, required);
        saveConfig();
    }

    public Set<String> getSpecialBookIds() {
        return specialBookIds;
    }

    public int getMaxEnchantsPerOperation() {
        return maxEnchantsPerOperation;
    }

    public int getMaxOperationItemBytes() {
        return maxOperationItemBytes;
    }

    private double clampPercent(double percent) {
        if (Double.isNaN(percent) || Double.isInfinite(percent)) {
            return 50.0d;
        }
        return Math.max(0.0d, Math.min(100.0d, percent));
    }

    private int clampPositiveInt(int value, int min, int max, int fallback) {
        if (value < min || value > max) {
            return fallback;
        }
        return value;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadConfigValues();
    }

    public void setSpecialBookIds(Set<String> keys) {
        specialBookIds = new LinkedHashSet<>(keys);
        getConfig().set(CONFIG_SPECIAL_BOOK_IDS_KEY, specialBookIds.stream().toList());
        saveConfig();
    }

}
