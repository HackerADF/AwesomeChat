package dev.adf.awesomeChat.api.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ChatFilterViolationEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled = false;

    private final Player player;
    private final String message;
    private final String ruleName;
    private final int violationCount;

    public ChatFilterViolationEvent(Player player, String message, String ruleName, int violationCount) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.message = message;
        this.ruleName = ruleName;
        this.violationCount = violationCount;
    }

    public Player getPlayer() {
        return player;
    }

    public String getMessage() {
        return message;
    }

    public String getRuleName() {
        return ruleName;
    }

    public int getViolationCount() {
        return violationCount;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
