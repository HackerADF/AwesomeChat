package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.ChannelManager;
import dev.adf.awesomeChat.managers.ChannelManager.ChatChannel;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ChannelCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public ChannelCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        ChannelManager manager = plugin.getChannelManager();
        if (manager == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Chat channels are not enabled.");
            return true;
        }

        if (args.length == 0) {
            showHelp(player, label);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "list" -> handleList(player);
            case "join", "toggle" -> handleToggle(player, args);
            case "leave" -> handleLeave(player);
            case "send", "s" -> handleSend(player, args);
            default -> {
                // Treat as shorthand: /ch <channel> or /ch <channel> <message>
                ChatChannel channel = manager.getChannel(sub);
                if (channel != null) {
                    if (args.length == 1) {
                        handleToggle(player, new String[]{"toggle", sub});
                    } else {
                        String[] sendArgs = new String[args.length + 1];
                        sendArgs[0] = "send";
                        System.arraycopy(args, 0, sendArgs, 1, args.length);
                        handleSend(player, sendArgs);
                    }
                } else {
                    showHelp(player, label);
                }
            }
        }

        return true;
    }

    private void showHelp(Player player, String label) {
        player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "Channel Commands:");
        player.sendMessage(ChatColor.GOLD + "/" + label + " list" + ChatColor.GRAY + " - View available channels");
        player.sendMessage(ChatColor.GOLD + "/" + label + " join <channel>" + ChatColor.GRAY + " - Toggle into a channel");
        player.sendMessage(ChatColor.GOLD + "/" + label + " leave" + ChatColor.GRAY + " - Leave your current channel");
        player.sendMessage(ChatColor.GOLD + "/" + label + " send <channel> <message>" + ChatColor.GRAY + " - Send a message to a channel");
        player.sendMessage(ChatColor.GOLD + "/" + label + " <channel>" + ChatColor.GRAY + " - Quick toggle into a channel");
        player.sendMessage(ChatColor.GOLD + "/" + label + " <channel> <message>" + ChatColor.GRAY + " - Quick send to a channel");
    }

    private void handleList(Player player) {
        ChannelManager manager = plugin.getChannelManager();
        List<ChatChannel> accessible = manager.getAccessibleChannels(player);

        if (accessible.isEmpty()) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "No channels available.");
            return;
        }

        String activeChannel = manager.getActiveChannel(player);

        player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "Available Channels:");
        for (ChatChannel ch : accessible) {
            boolean isActive = ch.getName().equalsIgnoreCase(activeChannel);
            String status = isActive ? ChatColor.GREEN + " [ACTIVE]" : "";
            String perm = ch.getPermission().isEmpty() ? "" : ChatColor.DARK_GRAY + " (" + ch.getPermission() + ")";
            player.sendMessage(ChatColor.GOLD + "  - " + ChatColor.WHITE + ch.getDisplayName() + status + perm);
        }
    }

    private void handleToggle(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Usage: /ch join <channel>");
            return;
        }

        ChannelManager manager = plugin.getChannelManager();
        String channelName = args[1].toLowerCase();
        ChatChannel channel = manager.getChannel(channelName);

        if (channel == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Channel '" + channelName + "' not found.");
            return;
        }

        if (!manager.hasAccess(player, channel)) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You don't have permission to join this channel.");
            return;
        }

        String current = manager.getActiveChannel(player);
        if (channel.getName().equalsIgnoreCase(current)) {
            manager.setActiveChannel(player, null);
            player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "Left channel " + ChatColor.WHITE + channel.getDisplayName() + ChatColor.YELLOW + ". You are now in global chat.");
        } else {
            manager.setActiveChannel(player, channel.getName());
            player.sendMessage(plugin.getChatPrefix() + ChatColor.GREEN + "Switched to channel " + ChatColor.WHITE + channel.getDisplayName() + ChatColor.GREEN + ". All messages will go to this channel.");
        }
    }

    private void handleLeave(Player player) {
        ChannelManager manager = plugin.getChannelManager();
        String current = manager.getActiveChannel(player);

        if (current == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "You are not in any channel.");
            return;
        }

        ChatChannel channel = manager.getChannel(current);
        String name = channel != null ? channel.getDisplayName() : current;
        manager.setActiveChannel(player, null);
        player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "Left channel " + ChatColor.WHITE + name + ChatColor.YELLOW + ". Back to global chat.");
    }

    private void handleSend(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Usage: /ch send <channel> <message>");
            return;
        }

        ChannelManager manager = plugin.getChannelManager();
        String channelName = args[1].toLowerCase();
        ChatChannel channel = manager.getChannel(channelName);

        if (channel == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Channel '" + channelName + "' not found.");
            return;
        }

        if (!manager.hasAccess(player, channel)) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You don't have permission to send to this channel.");
            return;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
        manager.sendToChannel(player, channel.getName(), message);
    }
}
