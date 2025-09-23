package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SocialSpyCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public SocialSpyCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("awesomechat.socialspy")) {
            player.sendMessage(plugin.getFormattedConfigString(
                    "socialspy.errors.no-permission",
                    "&cYou do not have permission to use SocialSpy!"
            ));
            return true;
        }

        if (args.length > 0) {
            String arg = args[0].toLowerCase();
            if (arg.equals("on")) {
                plugin.getSocialSpyManager().setSpy(player, true);
                player.sendMessage(plugin.getFormattedConfigString(
                        "socialspy.toggle.enabled",
                        "&aSocialSpy enabled!"
                ));
                return true;
            } else if (arg.equals("off")) {
                plugin.getSocialSpyManager().setSpy(player, false);
                player.sendMessage(plugin.getFormattedConfigString(
                        "socialspy.toggle.disabled",
                        "&cSocialSpy disabled!"
                ));
                return true;
            }
        }

        boolean enabled = plugin.getSocialSpyManager().toggleSpy(player);

        if (enabled) {
            player.sendMessage(plugin.getFormattedConfigString(
                    "socialspy.toggle.enabled",
                    "&aSocialSpy enabled!"
            ));
        } else {
            player.sendMessage(plugin.getFormattedConfigString(
                    "socialspy.toggle.disabled",
                    "&cSocialSpy disabled!"
            ));
        }

        return true;
    }
}
