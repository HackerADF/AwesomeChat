package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChatColorTabCompleter implements TabCompleter {

    private final AwesomeChat plugin;

    public ChatColorTabCompleter(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player player)) return completions;

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : List.of("reset", "create", "delete", "list")) {
                if (sub.startsWith(partial)) {
                    if (sub.equals("reset")) {
                        completions.add(sub);
                    } else if (player.hasPermission("awesomechat.chatcolor.admin")) {
                        completions.add(sub);
                    }
                }
            }
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            String partial = args[1].toLowerCase();

            if (sub.equals("delete") && player.hasPermission("awesomechat.chatcolor.admin")) {
                if (plugin.getChatColorManager() != null) {
                    Set<String> names = plugin.getChatColorManager().getAdminGradientNames();
                    for (String name : names) {
                        if (name.toLowerCase().startsWith(partial)) {
                            completions.add(name);
                        }
                    }
                }
            } else if (sub.equals("create") && player.hasPermission("awesomechat.chatcolor.admin")) {
                if (partial.isEmpty()) {
                    completions.add("<name>");
                }
            }
            return completions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("create") && player.hasPermission("awesomechat.chatcolor.admin")) {
            String partial = args[2].toUpperCase();
            for (Material mat : Material.values()) {
                if (mat.isItem() && mat.name().startsWith(partial)) {
                    completions.add(mat.name());
                }
                if (completions.size() >= 30) break;
            }
            return completions;
        }

        if (args.length >= 4 && args.length <= 7 && args[0].equalsIgnoreCase("create") && player.hasPermission("awesomechat.chatcolor.admin")) {
            if (args[args.length - 1].isEmpty()) {
                completions.add("#");
            }
            return completions;
        }

        return completions;
    }
}
