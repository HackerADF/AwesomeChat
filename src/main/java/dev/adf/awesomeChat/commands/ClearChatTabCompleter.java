package dev.adf.awesomeChat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClearChatTabCompleter implements TabCompleter {

    private static final List<String> OPTIONS = Arrays.asList("self", "all");

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String option : OPTIONS) {
                if (option.startsWith(partial)) {
                    completions.add(option);
                }
            }
        }

        return completions;
    }
}
