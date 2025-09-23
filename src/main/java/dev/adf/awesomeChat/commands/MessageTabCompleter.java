package dev.adf.awesomeChat.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MessageTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Only players can use this
        if (!(sender instanceof Player)) {
            return completions;
        }

        // First argument -> player names
        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            for (Player online : Bukkit.getOnlinePlayers()) {
                String name = online.getName();

                // skip self
                if (online.equals(sender)) continue;

                // filter by whats being typed
                if (name.toLowerCase().startsWith(partial)) {
                    completions.add(name);
                }
            }
        }

        // no tabcomplete for following args (the message)
        return completions;
    }
}
