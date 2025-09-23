package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MessageCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public MessageCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage("§cUsage: /" + label + " <player> <message>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            player.sendMessage(plugin.getFormattedConfigString("private-messages.errors.player-not-found", "&cThat player could not be found!"));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(plugin.getFormattedConfigString("private-messages.errors.cannot-message-yourself", "&cYou cannot message yourself!"));
            return true;
        }
        // prevent the player from sending messages if they have their messages disabled
        if (plugin.getPrivateMessageManager().hasMessagesDisabled(player)) {
            player.sendMessage(plugin.getFormattedConfigString(
                    "private-messages.errors.self-messages-disabled",
                    "&cYou have private messages disabled!"
            ));
            return true;
        }

        String message = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));

        // format the message
        String senderFormat = plugin.getFormattedConfigString(
                        "private-messages.format.sender",
                        "&7[&bYou &7-> &b{receiver}&7] &f{message}"
                )
                .replace("{receiver}", target.getName())
                .replace("{message}", message);

        String receiverFormat = plugin.getFormattedConfigString(
                        "private-messages.format.receiver",
                        "&7[&b{sender} &7-> &bYou&7] &f{message}"
                )
                .replace("{sender}", player.getName())
                .replace("{message}", message);

        if (plugin.getPrivateMessageManager().hasMessagesDisabled(target)) {
            player.sendMessage(plugin.getFormattedConfigString(
                    "private-messages.errors.messages-disabled",
                    "&cThat player has private messages disabled!"
            ));
            return true;
        }
        player.sendMessage(AwesomeChat.formatColors(senderFormat));
        target.sendMessage(AwesomeChat.formatColors(receiverFormat));

        // TODO: Store last messaged players for /reply
        plugin.getPrivateMessageManager().setLastMessaged(player, target);

        // TODO: Hook into /msgtoggle and /socialspy later
        String spyFormat = plugin.getFormattedConfigString(
                "socialspy.format.spy",
                "&7[&cSpy&7] &b" + sender.getName() + " &7-> &b" + target.getName() + "&7: &f" + message
        );

        for (UUID uuid : plugin.getSocialSpyManager().getSpies()) {
            Player spy = plugin.getServer().getPlayer(uuid);
            if (spy != null && spy.isOnline() && spy.hasPermission("awesomechat.socialspy")) {
                spy.sendMessage(AwesomeChat.formatColors(spyFormat));
            }
        }

        return true;
    }
}
