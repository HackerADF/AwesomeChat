package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.IgnoreManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UnignoreTabCompleter implements TabCompleter {

    private final AwesomeChat plugin;

    public UnignoreTabCompleter(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias,
                                      String[] args) {

        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) return completions;

        IgnoreManager manager = plugin.getIgnoreManager();
        if (manager == null) return completions;

        if (args.length == 1) {
            String partial = args[0].toLowerCase();

            for (UUID id : manager.getIgnoredPlayers(player.getUniqueId())) {
                OfflinePlayer ignored = Bukkit.getOfflinePlayer(id);
                String name = ignored.getName();
                if (name != null && name.toLowerCase().startsWith(partial)) {
                    completions.add(name);
                }
            }
        }

        return completions;
    }
}
