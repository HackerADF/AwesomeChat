package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.IgnoreManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

public class IgnoreCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public IgnoreCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        IgnoreManager manager = plugin.getIgnoreManager();
        if (manager == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Ignore system is not enabled.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "Usage: /" + label + " <player> or /" + label + " list");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            handleList(player);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Player not found.");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You cannot ignore yourself.");
            return true;
        }

        if (target.hasPermission("awesomechat.ignore.bypass")) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You cannot ignore this player.");
            return true;
        }

        boolean nowIgnored = manager.toggleIgnore(player, target);

        if (nowIgnored) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "You are now ignoring " + ChatColor.WHITE + target.getName() + ChatColor.YELLOW + ".");
        } else {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.GREEN + "You are no longer ignoring " + ChatColor.WHITE + target.getName() + ChatColor.GREEN + ".");
        }

        return true;
    }

    private void handleList(Player player) {
        IgnoreManager manager = plugin.getIgnoreManager();
        Set<UUID> ignored = manager.getIgnoredPlayers(player.getUniqueId());

        if (ignored.isEmpty()) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "You are not ignoring anyone.");
            return;
        }

        player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "Ignored players:");
        for (UUID id : ignored) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(id);
            String name = target.getName() != null ? target.getName() : id.toString();
            player.sendMessage(ChatColor.GRAY + "  - " + ChatColor.WHITE + name);
        }
    }
}
