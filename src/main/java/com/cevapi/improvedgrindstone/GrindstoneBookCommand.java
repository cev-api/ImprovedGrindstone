package com.cevapi.improvedgrindstone;

import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class GrindstoneBookCommand implements CommandExecutor {
    private final GrindstoneBookPlugin plugin;

    public GrindstoneBookCommand(GrindstoneBookPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("improvedgrindstone.command")) {
            sender.sendMessage(color("&cYou do not have permission to use this command."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(color("&eUsage: /" + label + " <toggle|status|cursed|xp>"));
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "toggle" -> handleToggle(sender);
            case "status" -> sendStatus(sender);
            case "cursed" -> handleCursedToggle(sender);
            case "xp" -> handleXpToggle(sender);
            default -> sender.sendMessage(color("&eUsage: /" + label + " <toggle|status|cursed|xp>"));
        }

        return true;
    }

    private void handleToggle(CommandSender sender) {
        boolean newState = !plugin.isFeatureEnabled();
        plugin.setFeatureEnabled(newState);
        sender.sendMessage(color("&aGrindstone book transfer is now " + (newState ? "&benabled" : "&cdisabled") + "&a."));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(color("&eGrindstone book transfer is currently "
                + (plugin.isFeatureEnabled() ? "&aenabled" : "&cdisabled")
                + "&e; cursed capture is "
                + (plugin.isCaptureCursed() ? "&aenabled" : "&cdisabled")
                + "&e; grant XP when book is "
                + (plugin.isGrantXpWhenBook() ? "&aenabled" : "&cdisabled")
                + "&e."));
    }

    private void handleCursedToggle(CommandSender sender) {
        boolean newState = !plugin.isCaptureCursed();
        plugin.setCaptureCursed(newState);
        sender.sendMessage(color("&bCapture cursed enchantments is now " + (newState ? "&aenabled" : "&cdisabled") + "&b."));
    }

    private void handleXpToggle(CommandSender sender) {
        boolean newState = !plugin.isGrantXpWhenBook();
        plugin.setGrantXpWhenBook(newState);
        sender.sendMessage(color("&bGrant XP when using a book is now " + (newState ? "&aenabled" : "&cdisabled") + "&b."));
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
