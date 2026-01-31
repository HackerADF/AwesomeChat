package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.ChannelManager;
import dev.adf.awesomeChat.managers.ChannelManager.ChatChannel;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChannelTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("list", "join", "leave", "send");

    private final AwesomeChat plugin;

    public ChannelTabCompleter(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) return completions;

        ChannelManager manager = plugin.getChannelManager();
        if (manager == null) return completions;

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }

            for (ChatChannel ch : manager.getAccessibleChannels(player)) {
                if (ch.getName().toLowerCase().startsWith(partial)) {
                    completions.add(ch.getName());
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("join") || sub.equals("toggle") || sub.equals("send")) {
                String partial = args[1].toLowerCase();
                for (ChatChannel ch : manager.getAccessibleChannels(player)) {
                    if (ch.getName().toLowerCase().startsWith(partial)) {
                        completions.add(ch.getName());
                    }
                }
            }
        }

        return completions;
    }
}
