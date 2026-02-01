package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.ChatLogManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ChatLogTabCompleter implements TabCompleter {

    private final AwesomeChat plugin;

    private static final List<String> FLAGS = Arrays.asList("time:", "before:", "after:");
    private static final List<String> DURATIONS = Arrays.asList(
            "30s", "5m", "15m", "30m",
            "1h", "6h", "12h", "24h",
            "1d", "3d", "7d",
            "1w", "2w", "4w"
    );

    public ChatLogTabCompleter(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // First arg: player names + "page"
            String input = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(input)) {
                    completions.add(p.getName());
                }
            }
            if ("page".startsWith(input)) {
                completions.add("page");
            }
            return completions;
        }

        // If first arg is "page", suggest page numbers
        if (args[0].equalsIgnoreCase("page")) {
            if (args.length == 2) {
                if (sender instanceof Player player) {
                    ChatLogManager logManager = plugin.getChatLogManager();
                    if (logManager != null) {
                        ChatLogManager.SearchState state = logManager.getSearchState(player.getUniqueId());
                        if (state != null) {
                            String input = args[1];
                            for (int i = 1; i <= state.totalPages; i++) {
                                String pageStr = String.valueOf(i);
                                if (pageStr.startsWith(input)) {
                                    completions.add(pageStr);
                                }
                            }
                        }
                    }
                }
            }
            return completions;
        }

        // After player name: suggest flags not already used
        String input = args[args.length - 1].toLowerCase();

        // Check which flags are already used
        List<String> usedFlags = new ArrayList<>();
        for (int i = 1; i < args.length - 1; i++) {
            String arg = args[i].toLowerCase();
            for (String flag : FLAGS) {
                if (arg.startsWith(flag)) {
                    usedFlags.add(flag);
                    // "time:" and "after:" are functionally similar, remove both if either used
                    if (flag.equals("time:")) usedFlags.add("after:");
                    if (flag.equals("after:")) usedFlags.add("time:");
                }
            }
        }

        // Check if current input is a partial flag with a value being typed
        for (String flag : FLAGS) {
            if (input.startsWith(flag) && input.length() > flag.length()) {
                // User is typing the duration after the flag, suggest flag+durations
                String partial = input.substring(flag.length());
                for (String dur : DURATIONS) {
                    if (dur.startsWith(partial)) {
                        completions.add(flag + dur);
                    }
                }
                return completions;
            }
        }

        // Suggest available flags
        for (String flag : FLAGS) {
            if (!usedFlags.contains(flag) && flag.startsWith(input)) {
                // Suggest flag with common durations
                for (String dur : DURATIONS) {
                    completions.add(flag + dur);
                }
            }
        }

        return completions;
    }
}
