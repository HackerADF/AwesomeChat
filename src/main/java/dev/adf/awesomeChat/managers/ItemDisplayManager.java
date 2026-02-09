package dev.adf.awesomeChat.managers;

import dev.adf.awesomeChat.AwesomeChat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

import static dev.adf.awesomeChat.listeners.ChatListener.formatColors;
import static dev.adf.awesomeChat.listeners.ChatListener.deserializeLegacy;

public class ItemDisplayManager {

    private final AwesomeChat plugin;
    private final NamespacedKey displayTagKey;
    private final Map<UUID, InventorySnapshot> snapshots = new HashMap<>();
    private Pattern triggerPattern;

    private static final String KEYWORDS = "item|hand|this|inventory|inv|enderchest|echest|ec";

    public ItemDisplayManager(AwesomeChat plugin) {
        this.plugin = plugin;
        this.displayTagKey = new NamespacedKey(plugin, "display_item");
        this.triggerPattern = buildTriggerPattern(plugin.getPluginConfig());
    }

    public void reloadConfig() {
        this.triggerPattern = buildTriggerPattern(plugin.getPluginConfig());
    }

    private Pattern buildTriggerPattern(FileConfiguration config) {
        String prefix = config.getString("item-display.trigger-prefix", "[");
        String suffix = config.getString("item-display.trigger-suffix", "]");

        String escapedPrefix = Pattern.quote(prefix);

        if (suffix.isEmpty()) {
            return Pattern.compile(
                    escapedPrefix + "(" + KEYWORDS + ")\\b",
                    Pattern.CASE_INSENSITIVE
            );
        }

        String escapedSuffix = Pattern.quote(suffix);
        String commandPart = "|/(?:(?!" + escapedSuffix + ").)+";

        return Pattern.compile(
                escapedPrefix + "(" + KEYWORDS + commandPart + ")" + escapedSuffix,
                Pattern.CASE_INSENSITIVE
        );
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

        enum SnapshotType { INVENTORY, ENDER_CHEST, ITEM }

        boolean isExpired(long ttlMs) {
            return System.currentTimeMillis() - timestamp > ttlMs;
        }
    }

    public boolean hasTriggers(String message) {
        return triggerPattern.matcher(message).find();
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

        Matcher matcher = triggerPattern.matcher(message);
        if (!matcher.find()) return null;

        matcher.reset();
        Component result = Component.empty();
        int lastEnd = 0;

        // Track the last seen color in the chat so item components inherit it
        TextColor lastColor = null;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String textPart = message.substring(lastEnd, matcher.start());
                Component formatted = textFormatter.apply(textPart);
                result = result.append(formatted);

                TextColor color = formatted.color();
                if (color != null) {
                    lastColor = color;
                }
            }

            String trigger = matcher.group(1);
            Component triggerComp = createTriggerComponent(player, trigger, lastColor);
            if (triggerComp != null) {
                result = result.append(triggerComp);
            } else {
                // No permission or invalid trigger, keep as plain text
                Component plain = textFormatter.apply(matcher.group());
                result = result.append(plain);

                TextColor color = plain.color();
                if (color != null) {
                    lastColor = color;
                }
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < message.length()) {
            String textPart = message.substring(lastEnd);
            Component formatted = textFormatter.apply(textPart);
            result = result.append(formatted);

            TextColor color = formatted.color();
            if (color != null) {
                lastColor = color;
            }
        }

        return result;
    }

    private Component createTriggerComponent(Player player, String trigger, TextColor baseColor) {
        FileConfiguration config = plugin.getPluginConfig();
        String lower = trigger.toLowerCase();

        return switch (lower) {
            case "item", "hand", "this" -> {
                if (!player.hasPermission("awesomechat.display.item")) yield null;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType() == Material.AIR) {
                    String emptyFormat = config.getString("item-display.formats.empty-hand", "&7[Empty Hand]");
                    yield buildSimpleFormatComponent(emptyFormat);
                }
                UUID snapshotId = createItemSnapshot(player, held);
                yield buildItemFormatComponent(config, held, baseColor)
                        .clickEvent(ClickEvent.runCommand("/ac _view " + snapshotId));
            }
            case "inventory", "inv" -> {
                if (!player.hasPermission("awesomechat.display.inventory")) yield null;
                UUID snapshotId = createSnapshot(player, InventorySnapshot.SnapshotType.INVENTORY);
                String format = config.getString("item-display.formats.inventory", "&a[{player}'s Inventory]");
                String hoverText = config.getString("item-display.hover-text.inventory", "&eClick to view inventory");
                yield buildSimpleFormatComponent(format, "{player}", player.getName())
                        .clickEvent(ClickEvent.runCommand("/ac _view " + snapshotId))
                        .hoverEvent(HoverEvent.showText(
                                buildSimpleFormatComponent(hoverText, "{player}", player.getName())));
            }
            case "enderchest", "echest", "ec" -> {
                if (!player.hasPermission("awesomechat.display.enderchest")) yield null;
                UUID snapshotId = createSnapshot(player, InventorySnapshot.SnapshotType.ENDER_CHEST);
                String format = config.getString("item-display.formats.enderchest", "&5[{player}'s Ender Chest]");
                String hoverText = config.getString("item-display.hover-text.enderchest", "&eClick to view ender chest");
                yield buildSimpleFormatComponent(format, "{player}", player.getName())
                        .clickEvent(ClickEvent.runCommand("/ac _view " + snapshotId))
                        .hoverEvent(HoverEvent.showText(
                                buildSimpleFormatComponent(hoverText, "{player}", player.getName())));
            }
            default -> {
                if (trigger.startsWith("/")) {
                    if (!player.hasPermission("awesomechat.display.command")) yield null;
                    String format = config.getString("item-display.formats.command", "&6[{command}]");
                    String hoverText = config.getString("item-display.hover-text.command", "&eClick to use {command}");
                    yield buildSimpleFormatComponent(format, "{command}", trigger)
                            .clickEvent(ClickEvent.suggestCommand(trigger))
                            .hoverEvent(HoverEvent.showText(
                                    buildSimpleFormatComponent(hoverText, "{command}", trigger)));
                }
                yield null;
            }
        };
    }

    /**
     * Returns the item name component:
     * - If the item has a custom display name, that full component (colors + formatting).
     * - Otherwise, a nicely formatted material name, colored with:
     *   1. Item rarity color (if available)
     *   2. Current chat color (fallback)
     */
    private Component getItemNameComponent(ItemStack item, TextColor baseColor) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            Component displayName = meta.displayName();
            if (displayName != null) {
                return displayName;
            }
        }

        String materialName = formatMaterialName(item.getType().name());
        Component comp = Component.text(materialName);

        // Try to get rarity color first
        TextColor rarityColor = getRarityColor(item);
        if (rarityColor != null) {
            comp = comp.color(rarityColor);
        } else if (baseColor != null) {
            comp = comp.color(baseColor);
        }

        return comp;
    }

    /**
     * Gets the color associated with an item's rarity.
     * Returns null if rarity cannot be determined.
     */
    private TextColor getRarityColor(ItemStack item) {
        try {
            // Get the item's rarity (available in 1.16+)
            Object rarity = item.getClass().getMethod("getRarity").invoke(item);
            if (rarity == null) return null;

            String rarityName = rarity.toString().toUpperCase();

            // Map Minecraft rarity to colors
            return switch (rarityName) {
                case "COMMON" -> TextColor.color(255, 255, 255);      // White
                case "UNCOMMON" -> TextColor.color(255, 255, 85);     // Yellow
                case "RARE" -> TextColor.color(85, 255, 255);         // Aqua
                case "EPIC" -> TextColor.color(255, 85, 255);         // Light Purple
                default -> null;
            };
        } catch (Exception e) {
            // getRarity() not available or failed - return null
            return null;
        }
    }

    /**
     * Builds something like:
     * - "[Stone x34]"
     * - "[Enchanted Golden Apple x2]"
     * - "[(dark red bold)Blood Sword(then back)x1]" (if you keep x1)
     *
     * Uses the current chat color around the component for non‑custom names,
     * and keeps hover showing the full item.
     */
    private Component buildItemFormatComponent(FileConfiguration config, ItemStack item, TextColor baseColor) {
        String format = config.getString("item-display.formats.item", "&f[{item}&f x{count}&f]");
        int amount = item.getAmount();

        String[] parts = format.split("\\{item}", -1);

        Component result = Component.empty();
        TextColor currentColor = baseColor;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];

            // Always show count, including x1 for single items
            part = part.replace("{count}", String.valueOf(amount));

            Component textComp = deserializeLegacy(formatColors(part));
            result = result.append(textComp);

            TextColor segColor = textComp.color();
            if (segColor != null) {
                currentColor = segColor;
            }

            // After each {item} position (except the last part), insert the item name
            if (i < parts.length - 1) {
                Component itemNameComp = getItemNameComponent(item, currentColor);
                result = result.append(itemNameComp);
            }
        }

        return result.hoverEvent(item.asHoverEvent());
    }

    private Component buildSimpleFormatComponent(String format, String... replacements) {
        for (int i = 0; i < replacements.length - 1; i += 2) {
            format = format.replace(replacements[i], replacements[i + 1]);
        }
        return deserializeLegacy(formatColors(format));
    }

    private UUID createItemSnapshot(Player player, ItemStack item) {
        UUID id = UUID.randomUUID();
        InventorySnapshot snapshot = new InventorySnapshot();
        snapshot.playerName = player.getName();
        snapshot.timestamp = System.currentTimeMillis();
        snapshot.type = InventorySnapshot.SnapshotType.ITEM;
        snapshot.items = new ItemStack[]{ item.clone() };

        snapshots.put(id, snapshot);
        cleanExpired();
        return id;
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

        switch (snapshot.type) {
            case ITEM -> openItemGUI(viewer, snapshot);
            case ENDER_CHEST -> openEnderChestGUI(viewer, snapshot);
            default -> openInventoryGUI(viewer, snapshot);
        }
    }

    private void openItemGUI(Player viewer, InventorySnapshot snapshot) {
        FileConfiguration config = plugin.getPluginConfig();
        String title = config.getString("item-display.gui-titles.item", "{player}'s Item")
                .replace("{player}", snapshot.playerName);
        Inventory gui = Bukkit.createInventory(
                new DisplayInventoryHolder(), 27,
                deserializeLegacy(formatColors(title)));

        ItemStack glass = createGlassPane();
        for (int i = 0; i < 27; i++) {
            gui.setItem(i, glass.clone());
        }

        if (snapshot.items.length > 0 && snapshot.items[0] != null) {
            gui.setItem(13, tagItem(snapshot.items[0].clone()));
        }

        viewer.openInventory(gui);
    }

    private void openInventoryGUI(Player viewer, InventorySnapshot snapshot) {
        FileConfiguration config = plugin.getPluginConfig();
        String title = config.getString("item-display.gui-titles.inventory", "{player}'s Inventory")
                .replace("{player}", snapshot.playerName);
        Inventory gui = Bukkit.createInventory(
                new DisplayInventoryHolder(), 54,
                deserializeLegacy(formatColors(title)));

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
        FileConfiguration config = plugin.getPluginConfig();
        String title = config.getString("item-display.gui-titles.enderchest", "{player}'s Ender Chest")
                .replace("{player}", snapshot.playerName);
        Inventory gui = Bukkit.createInventory(
                new DisplayInventoryHolder(), 27,
                deserializeLegacy(formatColors(title)));

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
