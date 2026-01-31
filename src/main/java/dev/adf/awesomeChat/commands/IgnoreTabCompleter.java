package dev.adf.awesomeChat.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class IgnoreTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) return completions;

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            if ("list".startsWith(partial)) {
                completions.add("list");
            }

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
