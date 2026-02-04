package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ChatColorCommand implements CommandExecutor {

    private final AwesomeChat plugin;
    private static final Pattern HEX_PATTERN = Pattern.compile("^#[A-Fa-f0-9]{6}$");

    public ChatColorCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            // Allow console for list subcommand
            if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
                return handleList(sender);
            }
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

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "reset" -> {
                    plugin.getChatColorManager().clearPlayerColor(player.getUniqueId());
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                            plugin.getChatPrefix() + "&aYour chat color has been reset."));
                    return true;
                }
                case "create" -> {
                    return handleCreate(player, args);
                }
                case "delete" -> {
                    return handleDelete(player, args);
                }
                case "list" -> {
                    return handleList(player);
                }
            }
        }

        if (plugin.getChatColorGUI() == null) {
            player.sendMessage(plugin.getChatPrefix() + ChatColor.RED + "Chat color system is not available.");
            return true;
        }

        plugin.getChatColorGUI().openGUI(player);
        return true;
    }

    private boolean handleCreate(Player player, String[] args) {
        if (!player.hasPermission("awesomechat.chatcolor.admin")) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&cYou don't have permission to do this."));
            return true;
        }

        // /chatcolor create <name> <material> <color1> <color2> [color3] [color4]
        if (args.length < 5) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&cUsage: /chatcolor create <name> <material> <color1> <color2> [color3] [color4]"));
            return true;
        }

        String name = args[1].toLowerCase();

        // Validate name (alphanumeric + underscores only)
        if (!name.matches("^[a-z0-9_]+$")) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&cGradient name must be alphanumeric (a-z, 0-9, _)."));
            return true;
        }

        String materialStr = args[2].toUpperCase();
        Material material;
        try {
            material = Material.valueOf(materialStr);
        } catch (IllegalArgumentException e) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&cInvalid material: &f" + materialStr));
            return true;
        }

        List<String> colors = new ArrayList<>();
        for (int i = 3; i < args.length && i < 7; i++) {
            String color = args[i].toUpperCase();
            if (!color.startsWith("#")) color = "#" + color;
            if (!HEX_PATTERN.matcher(color).matches()) {
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        plugin.getChatPrefix() + "&cInvalid hex color: &f" + args[i] + " &c(use format #RRGGBB)"));
                return true;
            }
            colors.add(color);
        }

        if (colors.size() < 2) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&cYou must provide at least 2 colors."));
            return true;
        }

        boolean created = plugin.getChatColorManager().createAdminGradient(name, material, colors);
        if (!created) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&cA gradient with that name already exists."));
            return true;
        }

        // Reload gradients in GUI
        if (plugin.getChatColorGUI() != null) {
            plugin.getChatColorGUI().loadGradients();
        }

        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                plugin.getChatPrefix() + "&aGradient &f" + name + "&a created! Permission: &fawesomechat.chatcolor.gradient." + name));
        return true;
    }

    private boolean handleDelete(Player player, String[] args) {
        if (!player.hasPermission("awesomechat.chatcolor.admin")) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&cYou don't have permission to do this."));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&cUsage: /chatcolor delete <name>"));
            return true;
        }

        String name = args[1].toLowerCase();
        boolean deleted = plugin.getChatColorManager().deleteAdminGradient(name);
        if (!deleted) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&cNo admin gradient found with name: &f" + name));
            return true;
        }

        // Reload gradients in GUI
        if (plugin.getChatColorGUI() != null) {
            plugin.getChatColorGUI().loadGradients();
        }

        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                plugin.getChatPrefix() + "&aGradient &f" + name + "&a deleted."));
        return true;
    }

    private boolean handleList(CommandSender sender) {
        if (sender instanceof Player player && !player.hasPermission("awesomechat.chatcolor.admin")) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&cYou don't have permission to do this."));
            return true;
        }

        Set<String> names = plugin.getChatColorManager().getAdminGradientNames();
        if (names.isEmpty()) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    plugin.getChatPrefix() + "&7No admin-created gradients."));
            return true;
        }

        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                plugin.getChatPrefix() + "&eAdmin Gradients (&f" + names.size() + "&e):"));
        for (String name : names) {
            List<String> colors = plugin.getPluginConfig().getStringList("chatcolor.custom-gradients." + name + ".colors");
            String materialName = plugin.getPluginConfig().getString("chatcolor.custom-gradients." + name + ".material", "PAPER");
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                    " &7- &f" + name + " &7(" + materialName + ") &7Colors: &f" + String.join(", ", colors)));
        }
        return true;
    }
}
