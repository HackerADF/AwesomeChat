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
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class UnignoreCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public UnignoreCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        IgnoreManager manager = plugin.getIgnoreManager();
        if (manager == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Ignore system is not enabled.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "Usage: /" + label + " <player>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Player not found.");
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "You cannot ignore nor unignore yourself.");
            return true;
        }

        boolean isCurrentlyIgnored = manager.isIgnoring(player, target);

        if (isCurrentlyIgnored) {
            manager.setIgnore(player, target, false);
            player.sendMessage(plugin.getChatPrefix() + ChatColor.GREEN + "You are no longer ignoring " + ChatColor.WHITE + target.getName() + ChatColor.GREEN + ".");
        } else {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.YELLOW + "You are not currently ignoring " + ChatColor.WHITE + target.getName() + ChatColor.YELLOW + ".");
        }

        return true;
    }
}
