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
            sendHelp(sender, label);
            return true;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "toggle" -> handleToggle(sender);
            case "status" -> sendStatus(sender);
            case "cursed" -> handleCursedToggle(sender);
            case "xp" -> handleBookXpToggle(sender);
            case "transfer" -> handleTransferToggle(sender);
            case "transferxp" -> handleTransferXpToggle(sender);
            case "specialbook" -> handleSpecialBookToggle(sender);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage(color("&cUnknown subcommand: &f" + args[0]));
                sendHelp(sender, label);
            }
        }

        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(color("&8[&bImprovedGrindstone&8] &7Available Commands"));
        sender.sendMessage(color("&f/" + label + " status &8- &7Show current settings"));
        sender.sendMessage(color("&f/" + label + " toggle &8- &7Enable/disable plugin feature"));
        sender.sendMessage(color("&f/" + label + " cursed &8- &7Toggle cursed enchant capture"));
        sender.sendMessage(color("&f/" + label + " xp &8- &7Toggle extraction XP cost"));
        sender.sendMessage(color("&f/" + label + " transfer &8- &7Toggle cross-material transfer"));
        sender.sendMessage(color("&f/" + label + " transferxp &8- &7Toggle transfer XP cost"));
        sender.sendMessage(color("&f/" + label + " specialbook &8- &7Toggle special book requirement"));
        sender.sendMessage(color("&f/" + label + " reload &8- &7Reload config"));
    }

    private void handleToggle(CommandSender sender) {
        boolean newState = !plugin.isFeatureEnabled();
        plugin.setFeatureEnabled(newState);
        if (newState) {
            sender.sendMessage(color("&aPlugin enabled. Improved grindstone features are active."));
            return;
        }
        sender.sendMessage(color("&cPlugin disabled. Grindstone behavior is now vanilla."));
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage(color("&7ImprovedGrindstone Status"));
        sender.sendMessage(color("&fFeature: " + stateColor(plugin.isFeatureEnabled())));
        sender.sendMessage(color("&fCapture Cursed: " + stateColor(plugin.isCaptureCursed())));
        sender.sendMessage(color("&fBook XP Cost: " + stateColor(plugin.isBookXpCostEnabled())
                + " &7(" + formatPercent(plugin.getBookXpCostPercent()) + "%)"));
        sender.sendMessage(color("&fTransfer Mode: " + stateColor(plugin.isTransferEnabled())));
        sender.sendMessage(color("&fTransfer XP Cost: " + stateColor(plugin.isTransferXpCostEnabled())
                + " &7(" + formatPercent(plugin.getTransferXpCostPercent()) + "%)"));
        sender.sendMessage(color("&fRequire Special Book: " + stateColor(plugin.isRequireSpecialBook())));
    }

    private void handleCursedToggle(CommandSender sender) {
        boolean newState = !plugin.isCaptureCursed();
        plugin.setCaptureCursed(newState);
        sender.sendMessage(color("&bCapture cursed enchantments is now " + (newState ? "&aenabled" : "&cdisabled") + "&b."));
    }

    private void handleBookXpToggle(CommandSender sender) {
        boolean newState = !plugin.isBookXpCostEnabled();
        plugin.setBookXpCostEnabled(newState);
        sender.sendMessage(color("&bBook XP cost is now " + (newState ? "&aenabled" : "&cdisabled") + "&b."));
    }

    private void handleTransferToggle(CommandSender sender) {
        boolean newState = !plugin.isTransferEnabled();
        plugin.setTransferEnabled(newState);
        sender.sendMessage(color("&bCross-material transfer is now " + (newState ? "&aenabled" : "&cdisabled") + "&b."));
    }

    private void handleTransferXpToggle(CommandSender sender) {
        boolean newState = !plugin.isTransferXpCostEnabled();
        plugin.setTransferXpCostEnabled(newState);
        sender.sendMessage(color("&bTransfer XP cost is now " + (newState ? "&aenabled" : "&cdisabled") + "&b."));
    }

    private void handleSpecialBookToggle(CommandSender sender) {
        boolean newState = !plugin.isRequireSpecialBook();
        plugin.setRequireSpecialBook(newState);
        sender.sendMessage(color("&bSpecial book requirement is now " + (newState ? "&aenabled" : "&cdisabled") + "&b."));
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(color("&aImprovedGrindstone config reloaded."));
    }

    private String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String stateColor(boolean enabled) {
        return enabled ? "&aEnabled" : "&cDisabled";
    }

    private String color(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}
