package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.ChatLogManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class ChatLogCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public ChatLogCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getChatPrefix() + org.bukkit.ChatColor.RED + "Only players can use this command.");
            return true;
        }

        ChatLogManager logManager = plugin.getChatLogManager();
        if (logManager == null) {
            player.sendMessage(Component.text(plugin.getChatPrefix()).append(
                    Component.text("Chat logging is not available.").color(NamedTextColor.RED)));
            return true;
        }

        if (args.length == 0) {
            sendUsage(player, label);
            return true;
        }

        // Handle "page" subcommand
        if (args[0].equalsIgnoreCase("page")) {
            handlePage(player, args, label);
            return true;
        }

        // Search by player
        handleSearch(player, args, label);
        return true;
    }

    private void handlePage(Player player, String[] args, String label) {
        ChatLogManager logManager = plugin.getChatLogManager();
        ChatLogManager.SearchState state = logManager.getSearchState(player.getUniqueId());
        if (state == null) {
            player.sendMessage(Component.text(plugin.getChatPrefix()).append(
                    Component.text("No active search. Use /" + label + " <player> first.").color(NamedTextColor.RED)));
            return;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text(plugin.getChatPrefix()).append(
                        Component.text("Invalid page number.").color(NamedTextColor.RED)));
                return;
            }
        }

        if (page < 1) page = 1;
        if (page > state.totalPages) page = state.totalPages;

        final int finalPage = page;
        logManager.searchAsync(player.getUniqueId(), state.targetUuid, state.afterMs, state.beforeMs, finalPage,
                new ChatLogManager.SearchCallback() {
                    @Override
                    public void onResult(List<ChatLogManager.ChatLogEntry> entries, int pg, int totalPages, int total) {
                        displayResults(player, entries, pg, totalPages, total, state.targetUuid, label);
                    }

                    @Override
                    public void onError(String message) {
                        player.sendMessage(Component.text(plugin.getChatPrefix()).append(
                                Component.text("Search failed: " + message).color(NamedTextColor.RED)));
                    }
                });
    }

    private void handleSearch(Player player, String[] args, String label) {
        // First arg is player name
        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null) {
            // Try exact match from online players
            Player onlineTarget = Bukkit.getPlayerExact(targetName);
            if (onlineTarget != null) {
                target = onlineTarget;
            } else {
                player.sendMessage(Component.text(plugin.getChatPrefix()).append(
                        Component.text("Player not found: " + targetName).color(NamedTextColor.RED)));
                return;
            }
        }

        UUID targetUuid = target.getUniqueId();
        String resolvedName = target.getName() != null ? target.getName() : targetName;

        // Parse flags
        Long afterMs = null;
        Long beforeMs = null;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i].toLowerCase();
            if (arg.startsWith("time:")) {
                String val = arg.substring(5);
                Long duration = ChatLogManager.parseDuration(val);
                if (duration != null) {
                    afterMs = System.currentTimeMillis() - duration;
                }
            } else if (arg.startsWith("after:")) {
                String val = arg.substring(6);
                Long duration = ChatLogManager.parseDuration(val);
                if (duration != null) {
                    afterMs = System.currentTimeMillis() - duration;
                }
            } else if (arg.startsWith("before:")) {
                String val = arg.substring(7);
                Long duration = ChatLogManager.parseDuration(val);
                if (duration != null) {
                    beforeMs = System.currentTimeMillis() - duration;
                }
            }
        }

        final Long finalAfterMs = afterMs;
        final Long finalBeforeMs = beforeMs;

        ChatLogManager logManager = plugin.getChatLogManager();
        logManager.searchAsync(player.getUniqueId(), targetUuid, finalAfterMs, finalBeforeMs, 1,
                new ChatLogManager.SearchCallback() {
                    @Override
                    public void onResult(List<ChatLogManager.ChatLogEntry> entries, int page, int totalPages, int total) {
                        displayResults(player, entries, page, totalPages, total, targetUuid, label);
                    }

                    @Override
                    public void onError(String message) {
                        player.sendMessage(Component.text(plugin.getChatPrefix()).append(
                                Component.text("Search failed: " + message).color(NamedTextColor.RED)));
                    }
                });
    }

    private void displayResults(Player player, List<ChatLogManager.ChatLogEntry> entries, int page, int totalPages, int total, UUID targetUuid, String label) {
        String targetName = entries.isEmpty() ? "Unknown" : entries.get(0).username;
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUuid);
        if (target.getName() != null) targetName = target.getName();

        // Header
        player.sendMessage(Component.empty());
        Component header = Component.text(" ")
                .append(Component.text("Chat Logs for ").color(NamedTextColor.GRAY))
                .append(Component.text(targetName).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                .append(Component.text(" (" + total + " results)").color(NamedTextColor.DARK_GRAY));
        player.sendMessage(header);

        Component divider = Component.text(" " + "\u2500".repeat(35)).color(NamedTextColor.DARK_GRAY);
        player.sendMessage(divider);

        if (entries.isEmpty()) {
            player.sendMessage(Component.text("  No messages found.").color(NamedTextColor.GRAY));
        } else {
            // Get configurable format
            String format = plugin.getPluginConfig().getString("chat-logging.log-format",
                    "&7{timestamp} &8- &f{player}&7: &f{message}");

            for (ChatLogManager.ChatLogEntry entry : entries) {
                String shortTs = ChatLogManager.formatShortTimestamp(entry.timestamp);

                Component line = Component.text("  ")
                        .append(Component.text("[" + shortTs + "]").color(NamedTextColor.GRAY))
                        .append(Component.text(" - ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(entry.username).color(NamedTextColor.AQUA))
                        .append(Component.text(": ").color(NamedTextColor.GRAY))
                        .append(Component.text(entry.message).color(
                                entry.filtered ? NamedTextColor.RED : NamedTextColor.WHITE));

                if (entry.channel != null) {
                    line = line.append(Component.text(" [" + entry.channel + "]").color(NamedTextColor.DARK_AQUA));
                }

                if (entry.filtered) {
                    line = line.hoverEvent(HoverEvent.showText(
                            Component.text("This message was filtered").color(NamedTextColor.RED)));
                }

                player.sendMessage(line);
            }
        }

        player.sendMessage(divider);

        // Paginator
        if (totalPages > 1) {
            player.sendMessage(buildPaginator(page, totalPages, label));
        }

        player.sendMessage(Component.empty());
    }

    private Component buildPaginator(int currentPage, int totalPages, String label) {
        Component paginator = Component.text("  ");

        // Left arrow
        if (currentPage > 1) {
            paginator = paginator.append(
                    Component.text(" \u25C0 ")
                            .color(NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/" + label + " page " + (currentPage - 1)))
                            .hoverEvent(HoverEvent.showText(Component.text("Previous page").color(NamedTextColor.YELLOW)))
            );
        } else {
            paginator = paginator.append(Component.text(" \u25C0 ").color(NamedTextColor.DARK_GRAY));
        }

        // Page numbers
        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, startPage + 4);
        startPage = Math.max(1, endPage - 4);

        for (int i = startPage; i <= endPage; i++) {
            if (i == currentPage) {
                paginator = paginator.append(
                        Component.text(" " + i + " ")
                                .color(NamedTextColor.GREEN)
                                .decorate(TextDecoration.BOLD)
                );
            } else {
                paginator = paginator.append(
                        Component.text(" " + i + " ")
                                .color(NamedTextColor.AQUA)
                                .clickEvent(ClickEvent.runCommand("/" + label + " page " + i))
                                .hoverEvent(HoverEvent.showText(Component.text("Go to page " + i).color(NamedTextColor.YELLOW)))
                );
            }
        }

        // Right arrow
        if (currentPage < totalPages) {
            paginator = paginator.append(
                    Component.text(" \u25B6 ")
                            .color(NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.runCommand("/" + label + " page " + (currentPage + 1)))
                            .hoverEvent(HoverEvent.showText(Component.text("Next page").color(NamedTextColor.YELLOW)))
            );
        } else {
            paginator = paginator.append(Component.text(" \u25B6 ").color(NamedTextColor.DARK_GRAY));
        }

        paginator = paginator.append(
                Component.text(" (Page " + currentPage + "/" + totalPages + ")").color(NamedTextColor.DARK_GRAY));

        return paginator;
    }

    private void sendUsage(Player player, String label) {
        player.sendMessage(Component.empty());
        player.sendMessage(Component.text(" Chat Logs Usage").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        player.sendMessage(Component.text(" " + "\u2500".repeat(30)).color(NamedTextColor.DARK_GRAY));
        player.sendMessage(Component.text("  /" + label + " <player>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - View player's chat history").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /" + label + " <player> time:<duration>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Filter by time (12h, 1d, 2w)").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /" + label + " <player> after:<duration>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Messages after X ago").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /" + label + " <player> before:<duration>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Messages before X ago").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /" + label + " page <number>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Navigate pages").color(NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  Durations: ").color(NamedTextColor.GRAY)
                .append(Component.text("30s, 5m, 12h, 1d, 2w").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.empty());
    }
}
