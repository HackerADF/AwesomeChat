package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatColorCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public ChatColorCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (!plugin.getPluginConfig().getBoolean("chatcolor.enabled", true)) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Chat color is not enabled.");
            return true;
        }

        if (plugin.getChatColorManager() == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Chat color system is not available.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reset")) {
            plugin.getChatColorManager().clearPlayerColor(player.getUniqueId());
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&aYour chat color has been reset."));
            return true;
        }

        if (plugin.getChatColorGUI() == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Chat color system is not available.");
            return true;
        }

        plugin.getChatColorGUI().openGUI(player);
        return true;
    }
}
