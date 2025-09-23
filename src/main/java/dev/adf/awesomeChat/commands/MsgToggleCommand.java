package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MsgToggleCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public MsgToggleCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        boolean nowDisabled;

        if (args.length == 0) {
            nowDisabled = plugin.getPrivateMessageManager().toggleMessages(player);
        } else {
            String option = args[0].toLowerCase();
            switch (option) {
                case "on":
                case "enable":
                    plugin.getPrivateMessageManager().setMessagesDisabled(player, false);
                    nowDisabled = false;
                    break;
                case "off":
                case "disable":
                    plugin.getPrivateMessageManager().setMessagesDisabled(player, true);
                    nowDisabled = true;
                    break;
                default:
                    player.sendMessage("§cUsage: /" + label + " [on|off]");
                    return true;
            }
        }

        if (nowDisabled) {
            player.sendMessage(plugin.getFormattedConfigString(
                    "private-messages.toggle.disabled",
                    "&cPrivate messages disabled!"
            ));
        } else {
            player.sendMessage(plugin.getFormattedConfigString(
                    "private-messages.toggle.enabled",
                    "&aPrivate messages enabled!"
            ));
        }

        return true;
    }
}
