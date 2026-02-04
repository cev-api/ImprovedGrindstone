package com.cevapi.improvedgrindstone;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public final class GrindstoneTabCompleter implements TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "toggle",
            "status",
            "cursed",
            "xp",
            "transfer",
            "transferxp",
            "specialbook",
            "reload");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("improvedgrindstone.command")) {
            return List.of();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream()
                    .filter(value -> value.startsWith(prefix))
                    .collect(Collectors.toList());
        }

        // Returning an empty list here prevents Bukkit from falling back to player-name completion.
        return List.of();
    }
}
