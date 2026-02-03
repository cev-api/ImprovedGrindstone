package com.cevapi.improvedgrindstone;

import java.util.logging.Level;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class GrindstoneBookPlugin extends JavaPlugin {
    private static final String CONFIG_KEY = "feature-enabled";
    private static final String CONFIG_CAPTURE_CURSED_KEY = "capture-cursed";
    private static final String CONFIG_GRANT_XP_KEY = "grant-xp-when-book";

    private boolean featureEnabled;
    private boolean captureCursed;
    private boolean grantXpWhenBook;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        getServer().getPluginManager().registerEvents(new GrindstoneListener(this), this);
        if (getCommand("improvedgrindstone") != null) {
            getCommand("improvedgrindstone").setExecutor(new GrindstoneBookCommand(this));
        }
        getLogger().log(Level.INFO,
                "Grindstone book transfer is {0}. Cursed capture is {1}. Grant XP when book is {2}.",
                new Object[]{
                        featureEnabled ? "enabled" : "disabled",
                        captureCursed ? "enabled" : "disabled",
                        grantXpWhenBook ? "enabled" : "disabled"
                });
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        featureEnabled = config.getBoolean(CONFIG_KEY, true);
        captureCursed = config.getBoolean(CONFIG_CAPTURE_CURSED_KEY, false);
        grantXpWhenBook = config.getBoolean(CONFIG_GRANT_XP_KEY, false);
        config.set(CONFIG_KEY, featureEnabled);
        config.set(CONFIG_CAPTURE_CURSED_KEY, captureCursed);
        config.set(CONFIG_GRANT_XP_KEY, grantXpWhenBook);
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

    public boolean isGrantXpWhenBook() {
        return grantXpWhenBook;
    }

    public void setGrantXpWhenBook(boolean grantXpWhenBook) {
        this.grantXpWhenBook = grantXpWhenBook;
        getConfig().set(CONFIG_GRANT_XP_KEY, grantXpWhenBook);
        saveConfig();
    }

}
