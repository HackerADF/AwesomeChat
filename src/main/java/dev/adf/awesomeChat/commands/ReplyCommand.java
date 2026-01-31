package dev.adf.awesomeChat.commands;

import dev.adf.awesomeChat.AwesomeChat;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ReplyCommand implements CommandExecutor {

    private final AwesomeChat plugin;

    public ReplyCommand(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage("§cUsage: /" + label + " <message>");
            return true;
        }

        // Get last messaged player from PM manager
        Player target = plugin.getPrivateMessageManager().getLastMessaged(player);
        if (target == null || !target.isOnline()) {
            player.sendMessage(plugin.getFormattedConfigString(
                    "private-messages.errors.reply-target-not-found",
                    "&cYou have nobody to reply to!"
            ));
            return true;
        }

        // Check if sender is ignored by target
        if (plugin.getIgnoreManager() != null && plugin.getIgnoreManager().isIgnoring(target, player)
                && !player.hasPermission("awesomechat.ignore.bypass")) {
            player.sendMessage(plugin.getFormattedConfigString(
                    "private-messages.errors.messages-disabled",
                    "&cThat player has private messages disabled!"
            ));
            return true;
        }

        // Check if they have PMs disabled
        if (plugin.getPrivateMessageManager().hasMessagesDisabled(target)) {
            player.sendMessage(plugin.getFormattedConfigString(
                    "private-messages.errors.messages-disabled",
                    "&cThat player has private messages disabled!"
            ));
            return true;
        }

        String message = String.join(" ", args);

        // Format sender & receiver messages
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

        player.sendMessage(AwesomeChat.formatColors(senderFormat));
        target.sendMessage(AwesomeChat.formatColors(receiverFormat));

        if (plugin.getConfig().isConfigurationSection("private-messages.sound")) {
            if (!plugin.getConfig().getString("private-messages.sound.sent").equalsIgnoreCase("NONE")) {
                String sentSoundName = plugin.getConfig().getString("private-messages.sound.sent.name", "ENTITY_CHICKEN_EGG");
                float sentVolume = (float) plugin.getConfig().getDouble("private-messages.sound.sent.volume", 100);
                float sentPitch = (float) plugin.getConfig().getDouble("private-messages.sound.sent.pitch", 2.0);

                Sound sound;
                try {
                    sound = Sound.valueOf(sentSoundName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sound = Sound.ENTITY_CHICKEN_EGG;
                }
                player.playSound(target.getLocation(), sound, sentVolume, sentPitch);
            }
            if (!plugin.getConfig().getString("private-messages.sound.received").equalsIgnoreCase("NONE")) {
                String recSoundName = plugin.getConfig().getString("private-messages.sound.sent.name", "ENTITY_CHICKEN_EGG");
                float recVolume = (float) plugin.getConfig().getDouble("private-messages.sound.sent.volume", 100);
                float recPitch = (float) plugin.getConfig().getDouble("private-messages.sound.sent.pitch", 2.0);

                Sound sound;
                try {
                    sound = Sound.valueOf(recSoundName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    sound = Sound.ENTITY_CHICKEN_EGG;
                }
                target.playSound(target.getLocation(), sound, recVolume, recPitch);
            }
        }

        plugin.getPrivateMessageManager().setLastMessaged(player, target);

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
