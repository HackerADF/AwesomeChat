package dev.adf.awesomeChat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class AwesomeChatTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        // First argument (/awesomechat <arg>)
        if (args.length == 1) {
            if (sender.hasPermission("awesomechat.use")) {
                suggestions.add("info");
            }
            if (sender.hasPermission("awesomechat.reload")) {
                suggestions.add("reload");
            }
        }

        return suggestions;
    }
}
