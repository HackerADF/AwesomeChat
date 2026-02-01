package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemDisplayManager {

    private final AwesomeChat plugin;
    private final NamespacedKey displayTagKey;
    private final Map<UUID, InventorySnapshot> snapshots = new HashMap<>();

    private static final Pattern TRIGGER_PATTERN = Pattern.compile(
            "\\[(item|hand|inventory|inv|enderchest|echest|ec|/[^\\]]+)\\]",
            Pattern.CASE_INSENSITIVE
    );

    public ItemDisplayManager(AwesomeChat plugin) {
        this.plugin = plugin;
        this.displayTagKey = new NamespacedKey(plugin, "display_item");
    }

    public static class DisplayInventoryHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static class InventorySnapshot {
        ItemStack[] items;
        String playerName;
        long timestamp;
        SnapshotType type;

        enum SnapshotType { INVENTORY, ENDER_CHEST }

        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }

    public boolean hasTriggers(String message) {
        return TRIGGER_PATTERN.matcher(message).find();
    }

    /**
     * Builds a rich message Component with display triggers replaced by interactive components.
     * Returns null if the feature is disabled or no triggers were found.
     *
     * @param player        The player who sent the message
     * @param message       The raw message text (after filter/emoji/mention processing)
     * @param textFormatter A function that formats plain text segments into Components
     * @return The rich Component, or null if no triggers were processed
     */
    public Component buildRichMessageComponent(
            Player player,
            String message,
            Function<String, Component> textFormatter
    ) {
        FileConfiguration config = plugin.getPluginConfig();
        if (!config.getBoolean("item-display.enabled", false)) return null;

        Matcher matcher = TRIGGER_PATTERN.matcher(message);
        if (!matcher.find()) return null;

        matcher.reset();
        Component result = Component.empty();
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String textPart = message.substring(lastEnd, matcher.start());
                result = result.append(textFormatter.apply(textPart));
            }

            String trigger = matcher.group(1);
            Component triggerComp = createTriggerComponent(player, trigger);
            if (triggerComp != null) {
                result = result.append(triggerComp);
            } else {
                // No permission or invalid trigger, keep as plain text
                result = result.append(textFormatter.apply(matcher.group()));
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < message.length()) {
            result = result.append(textFormatter.apply(message.substring(lastEnd)));
        }

        return result;
    }

    private Component createTriggerComponent(Player player, String trigger) {
        String lower = trigger.toLowerCase();

        return switch (lower) {
            case "item", "hand" -> {
                if (!player.hasPermission("awesomechat.display.item")) yield null;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType() == Material.AIR) {
                    yield Component.text("[Empty Hand]").color(NamedTextColor.GRAY);
                }
                String name = getItemDisplayName(held);
                yield Component.text("[" + name + "]")
                        .color(NamedTextColor.AQUA)
                        .decorate(TextDecoration.BOLD)
                        .hoverEvent(held.asHoverEvent());
            }
            case "inventory", "inv" -> {
                if (!player.hasPermission("awesomechat.display.inventory")) yield null;
                UUID snapshotId = createSnapshot(player, InventorySnapshot.SnapshotType.INVENTORY);
                yield Component.text("[" + player.getName() + "'s Inventory]")
                        .color(NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.runCommand("/ac _view " + snapshotId))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Click to view inventory").color(NamedTextColor.YELLOW)));
            }
            case "enderchest", "echest", "ec" -> {
                if (!player.hasPermission("awesomechat.display.enderchest")) yield null;
                UUID snapshotId = createSnapshot(player, InventorySnapshot.SnapshotType.ENDER_CHEST);
                yield Component.text("[" + player.getName() + "'s Ender Chest]")
                        .color(NamedTextColor.DARK_PURPLE)
                        .clickEvent(ClickEvent.runCommand("/ac _view " + snapshotId))
                        .hoverEvent(HoverEvent.showText(
                                Component.text("Click to view ender chest").color(NamedTextColor.YELLOW)));
            }
            default -> {
                if (trigger.startsWith("/")) {
                    if (!player.hasPermission("awesomechat.display.command")) yield null;
                    yield Component.text("[" + trigger + "]")
                            .color(NamedTextColor.GOLD)
                            .clickEvent(ClickEvent.suggestCommand(trigger))
                            .hoverEvent(HoverEvent.showText(
                                    Component.text("Click to use " + trigger).color(NamedTextColor.YELLOW)));
                }
                yield null;
            }
        };
    }

    private UUID createSnapshot(Player player, InventorySnapshot.SnapshotType type) {
        UUID id = UUID.randomUUID();
        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.playerName = player.getName();
        snapshot.timestamp = System.currentTimeMillis();
        snapshot.type = type;

        if (type == InventorySnapshot.SnapshotType.ENDER_CHEST) {
            snapshot.items = cloneArray(player.getEnderChest().getContents());
        } else {
            snapshot.items = cloneArray(player.getInventory().getContents());
        }

        snapshots.put(id, snapshot);
        cleanExpired();
        return id;
    }

    public void openSnapshot(Player viewer, UUID snapshotId) {
        FileConfiguration config = plugin.getPluginConfig();
        long ttl = config.getLong("item-display.snapshot-ttl-seconds", 300) * 1000L;

        InventorySnapshot snapshot = snapshots.get(snapshotId);
        if (snapshot == null || snapshot.isExpired(ttl)) {
            snapshots.remove(snapshotId);
            viewer.sendMessage(AwesomeChat.formatColors(
                    config.getString("item-display.expired-message", "&cThis inventory snapshot has expired.")));
            return;
        }

        if (snapshot.type == InventorySnapshot.SnapshotType.ENDER_CHEST) {
            openEnderChestGUI(viewer, snapshot);
        } else {
            openInventoryGUI(viewer, snapshot);
        }
    }

    private void openInventoryGUI(Player viewer, InventorySnapshot snapshot) {
        Inventory gui = Bukkit.createInventory(
                new DisplayInventoryHolder(), 54,
                Component.text(snapshot.playerName + "'s Inventory"));

        ItemStack glass = createGlassPane();

        // Fill row 1 and row 6 with glass
        for (int i = 0; i < 9; i++) {
            gui.setItem(i, glass.clone());
            gui.setItem(45 + i, glass.clone());
        }

        ItemStack[] items = snapshot.items;
        // Player inventory: 0-8 hotbar, 9-35 main, 36 boots, 37 leggings, 38 chestplate, 39 helmet, 40 offhand

        // Row 1: Armor + offhand
        if (items.length > 39 && items[39] != null) gui.setItem(0, tagItem(items[39].clone()));
        if (items.length > 38 && items[38] != null) gui.setItem(1, tagItem(items[38].clone()));
        if (items.length > 37 && items[37] != null) gui.setItem(2, tagItem(items[37].clone()));
        if (items.length > 36 && items[36] != null) gui.setItem(3, tagItem(items[36].clone()));
        if (items.length > 40 && items[40] != null) gui.setItem(5, tagItem(items[40].clone()));

        // Rows 2-4: Main inventory (player slots 9-35 -> GUI slots 9-35)
        for (int i = 9; i <= 35 && i < items.length; i++) {
            if (items[i] != null) gui.setItem(i, tagItem(items[i].clone()));
        }

        // Row 5: Hotbar (player slots 0-8 -> GUI slots 36-44)
        for (int i = 0; i < 9 && i < items.length; i++) {
            if (items[i] != null) gui.setItem(36 + i, tagItem(items[i].clone()));
        }

        viewer.openInventory(gui);
    }

    private void openEnderChestGUI(Player viewer, InventorySnapshot snapshot) {
        Inventory gui = Bukkit.createInventory(
                new DisplayInventoryHolder(), 27,
                Component.text(snapshot.playerName + "'s Ender Chest"));

        for (int i = 0; i < snapshot.items.length && i < 27; i++) {
            if (snapshot.items[i] != null) {
                gui.setItem(i, tagItem(snapshot.items[i].clone()));
            }
        }

        viewer.openInventory(gui);
    }

    private ItemStack createGlassPane() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.getPersistentDataContainer().set(displayTagKey, PersistentDataType.BYTE, (byte) 1);
            glass.setItemMeta(meta);
        }
        return glass;
    }

    public ItemStack tagItem(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(displayTagKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isDisplayItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(displayTagKey, PersistentDataType.BYTE);
    }

    public void clearDisplayItems(Player player) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isDisplayItem(item)) {
                player.getInventory().setItem(i, null);
            }
        }
    }

    private void cleanExpired() {
        long ttl = plugin.getPluginConfig().getLong("item-display.snapshot-ttl-seconds", 300) * 1000L;
        snapshots.entrySet().removeIf(e -> e.getValue().isExpired(ttl));
    }

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            Component displayName = item.getItemMeta().displayName();
            if (displayName != null) {
                return PlainTextComponentSerializer.plainText().serialize(displayName);
            }
        }
        return formatMaterialName(item.getType().name());
    }

    private String formatMaterialName(String name) {
        String[] parts = name.toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private ItemStack[] cloneArray(ItemStack[] original) {
        ItemStack[] clone = new ItemStack[original.length];
        for (int i = 0; i < original.length; i++) {
            if (original[i] != null) clone[i] = original[i].clone();
        }
        return clone;
    }
}
