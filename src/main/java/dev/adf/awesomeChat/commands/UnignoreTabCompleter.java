package dev.adf.awesomeChat.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class UnignoreTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {

        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) return completions;

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;
                if (online.getName().toLowerCase().startsWith(partial)) {
                    completions.add(online.getName());
                }
            }
        }

        return completions;
    }
}
