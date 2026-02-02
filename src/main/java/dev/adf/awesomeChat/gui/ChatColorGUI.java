package dev.adf.awesomeChat.gui;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.ChatColorManager;
import dev.adf.awesomeChat.managers.ChatColorManager.ChatColorData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ChatColorGUI implements Listener {

    private final AwesomeChat plugin;

    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER =
            LegacyComponentSerializer.legacyAmpersand();

    public ChatColorGUI(AwesomeChat plugin) {
        this.plugin = plugin;
    }

    public static class ChatColorHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    // ========== GUI Building ==========

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(
                new ChatColorHolder(), 54,
                Component.text("Chat Color Picker")
                        .color(NamedTextColor.DARK_PURPLE)
                        .decorate(TextDecoration.BOLD)
        );

        ChatColorManager manager = plugin.getChatColorManager();
        ChatColorData current = manager != null ? manager.getPlayerColor(player.getUniqueId()) : null;

        // Row 1: Header
        fillGlass(inv, 0, 8);
        inv.setItem(4, createItem(Material.NETHER_STAR, "&d&lChat Color",
                List.of("&7Select a color for your", "&7chat messages!", "", "&7Current: " + getCurrentDisplay(current))));

        // Row 2: Standard colors &0 through &8
        setColorItem(inv, 9,  Material.BLACK_CONCRETE,      "&0&lBlack",        "#000000", current);
        setColorItem(inv, 10, Material.BLUE_CONCRETE,        "&1&lDark Blue",    "#0000AA", current);
        setColorItem(inv, 11, Material.GREEN_CONCRETE,       "&2&lDark Green",   "#00AA00", current);
        setColorItem(inv, 12, Material.CYAN_CONCRETE,        "&3&lDark Aqua",    "#00AAAA", current);
        setColorItem(inv, 13, Material.RED_CONCRETE,         "&4&lDark Red",     "#AA0000", current);
        setColorItem(inv, 14, Material.PURPLE_CONCRETE,      "&5&lDark Purple",  "#AA00AA", current);
        setColorItem(inv, 15, Material.ORANGE_CONCRETE,      "&6&lGold",         "#FFAA00", current);
        setColorItem(inv, 16, Material.LIGHT_GRAY_CONCRETE,  "&7&lGray",         "#AAAAAA", current);
        setColorItem(inv, 17, Material.GRAY_CONCRETE,        "&8&lDark Gray",    "#555555", current);

        // Row 3: Standard colors &9 through &f
        setColorItem(inv, 18, Material.BLUE_WOOL,        "&9&lBlue",         "#5555FF", current);
        setColorItem(inv, 19, Material.LIME_CONCRETE,    "&a&lGreen",        "#55FF55", current);
        setColorItem(inv, 20, Material.LIGHT_BLUE_WOOL,  "&b&lAqua",         "#55FFFF", current);
        setColorItem(inv, 21, Material.RED_WOOL,         "&c&lRed",          "#FF5555", current);
        setColorItem(inv, 22, Material.PINK_CONCRETE,    "&d&lLight Purple", "#FF55FF", current);
        setColorItem(inv, 23, Material.YELLOW_CONCRETE,  "&e&lYellow",       "#FFFF55", current);
        setColorItem(inv, 24, Material.WHITE_CONCRETE,   "&f&lWhite",        "#FFFFFF", current);
        fillGlass(inv, 25, 26);

        // Row 4: Dual gradient presets
        setGradientItem(inv, 27, Material.RED_CONCRETE,     "&c&lSunset",  List.of("#FF0000", "#610000"),  current, player);
        setGradientItem(inv, 28, Material.BLUE_CONCRETE,    "&9&lOcean",   List.of("#0000FF", "#00BFFF"),  current, player);
        setGradientItem(inv, 29, Material.PURPLE_CONCRETE,  "&5&lRoyal",   List.of("#8A2BE2", "#4B0082"),  current, player);
        setGradientItem(inv, 30, Material.LIME_CONCRETE,    "&a&lMint",    List.of("#00FF7F", "#006400"),  current, player);
        setGradientItem(inv, 31, Material.PINK_CONCRETE,    "&d&lSakura",  List.of("#FFB7C5", "#FF69B4"),  current, player);
        setGradientItem(inv, 32, Material.ORANGE_CONCRETE,  "&6&lEmber",   List.of("#FF4500", "#FF8C00"),  current, player);
        setGradientItem(inv, 33, Material.YELLOW_CONCRETE,  "&e&lGolden",  List.of("#FFD700", "#DAA520"),  current, player);
        setGradientItem(inv, 34, Material.CYAN_CONCRETE,    "&3&lTeal",    List.of("#008080", "#20B2AA"),  current, player);
        setGlass(inv, 35);

        // Row 5: Triple gradient presets + custom
        setGradientItem(inv, 36, Material.PACKED_ICE,      "&b&lIce",          List.of("#A5F3FC", "#38BDF8", "#1E3A5F"), current, player);
        setGradientItem(inv, 37, Material.MAGMA_BLOCK,     "&c&lFire",         List.of("#FFFF00", "#FF4500", "#8B0000"), current, player);
        setGradientItem(inv, 38, Material.PRISMARINE,      "&a&lAurora",       List.of("#00FF00", "#00BFFF", "#8A2BE2"), current, player);
        setGradientItem(inv, 39, Material.AMETHYST_BLOCK,  "&d&lCotton Candy", List.of("#FF69B4", "#DDA0DD", "#87CEEB"), current, player);
        setGradientItem(inv, 40, Material.EMERALD_BLOCK,   "&2&lForest",       List.of("#228B22", "#32CD32", "#ADFF2F"), current, player);
        fillGlass(inv, 41, 43);

        if (player.hasPermission("awesomechat.chatcolor.custom")) {
            inv.setItem(44, createItem(Material.ANVIL, "&d&lCustom Gradient",
                    List.of("&7Create your own gradient!", "", "&7Click to enter up to 4", "&7hex color codes in chat.", "", "&eClick to customize!")));
        } else {
            setGlass(inv, 44);
        }

        // Row 6: Style toggles + reset
        setStyleItem(inv, 45, Material.IRON_INGOT,  "&f&lBold",          "bold",          current, player, "awesomechat.styling.bold");
        setStyleItem(inv, 46, Material.FEATHER,      "&f&lItalic",        "italic",        current, player, "awesomechat.styling.italic");
        setStyleItem(inv, 47, Material.CHAIN,        "&f&lUnderline",     "underline",     current, player, "awesomechat.styling.underline");
        setStyleItem(inv, 48, Material.STRING,       "&f&lStrikethrough", "strikethrough", current, player, "awesomechat.styling.strikethrough");
        setStyleItem(inv, 49, Material.ENDER_PEARL,  "&f&lObfuscated",   "obfuscated",    current, player, "awesomechat.styling.obfuscated");
        fillGlass(inv, 50, 51);

        inv.setItem(52, createItem(Material.LAVA_BUCKET, "&c&lReset Color",
                List.of("&7Click to remove your", "&7custom chat color.")));
        setGlass(inv, 53);

        player.openInventory(inv);
    }

    // ========== Item Helpers ==========

    private void fillGlass(Inventory inv, int from, int to) {
        for (int i = from; i <= to; i++) {
            setGlass(inv, i);
        }
    }

    private void setGlass(Inventory inv, int slot) {
        inv.setItem(slot, createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));
    }

    private void setColorItem(Inventory inv, int slot, Material material, String name, String hex, ChatColorData current) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Hex: &f" + hex);
        if (isCurrentColor(current, "solid", List.of(hex))) {
            lore.add("");
            lore.add("&a&lCurrently Selected!");
        } else {
            lore.add("");
            lore.add("&eClick to select!");
        }
        inv.setItem(slot, createItem(material, name, lore));
    }

    private void setGradientItem(Inventory inv, int slot, Material material, String name,
                                  List<String> colors, ChatColorData current, Player player) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Colors: &f" + String.join(" &7\u2192 &f", colors));
        if (!player.hasPermission("awesomechat.chatcolor.gradient")) {
            lore.add("");
            lore.add("&c&lNo Permission!");
            inv.setItem(slot, createItem(Material.GRAY_STAINED_GLASS_PANE, "&8Locked", lore));
            return;
        }
        if (isCurrentColor(current, "gradient", colors)) {
            lore.add("");
            lore.add("&a&lCurrently Selected!");
        } else {
            lore.add("");
            lore.add("&eClick to select!");
        }
        inv.setItem(slot, createItem(material, name, lore));
    }

    private void setStyleItem(Inventory inv, int slot, Material material, String name,
                               String styleKey, ChatColorData current, Player player, String permission) {
        if (!player.hasPermission(permission)) {
            setGlass(inv, slot);
            return;
        }
        boolean active = isStyleActive(current, styleKey);
        List<String> lore = new ArrayList<>();
        lore.add(active ? "&a&lEnabled" : "&c&lDisabled");
        lore.add("");
        lore.add("&eClick to toggle!");
        inv.setItem(slot, createItem(material, name, lore));
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("").decoration(TextDecoration.ITALIC, false)
                    .append(AMPERSAND_SERIALIZER.deserialize(name)));
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(Component.text("").decoration(TextDecoration.ITALIC, false)
                        .append(AMPERSAND_SERIALIZER.deserialize(line)));
            }
            meta.lore(loreComponents);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isCurrentColor(ChatColorData current, String type, List<String> colors) {
        if (current == null) return false;
        return type.equals(current.getType()) && colors.equals(current.getColors());
    }

    private boolean isStyleActive(ChatColorData current, String style) {
        if (current == null) return false;
        return switch (style) {
            case "bold"          -> current.isBold();
            case "italic"        -> current.isItalic();
            case "underline"     -> current.isUnderline();
            case "strikethrough" -> current.isStrikethrough();
            case "obfuscated"    -> current.isObfuscated();
            default -> false;
        };
    }

    private String getCurrentDisplay(ChatColorData current) {
        if (current == null) return "&7None";
        if ("solid".equals(current.getType())) {
            return "&f" + current.getColors().get(0);
        }
        return "&f" + String.join(" &7\u2192 &f", current.getColors());
    }

    // ========== Click Handling ==========

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ChatColorHolder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        ChatColorManager manager = plugin.getChatColorManager();
        if (manager == null) return;
        int slot = event.getRawSlot();

        // Solid colors (slots 9-24)
        String solidColor = getSolidColorForSlot(slot);
        if (solidColor != null) {
            ChatColorData data = getOrCreate(manager, player);
            data.setType("solid");
            data.setColors(List.of(solidColor));
            manager.setPlayerColor(player.getUniqueId(), data);
            player.sendMessage(AMPERSAND_SERIALIZER.deserialize(
                    plugin.getChatPrefix() + "&aChat color set to &f" + solidColor + "&a!"));
            player.closeInventory();
            return;
        }

        // Gradient presets (slots 27-34, 36-40)
        List<String> gradientColors = getGradientColorsForSlot(slot);
        if (gradientColors != null) {
            if (!player.hasPermission("awesomechat.chatcolor.gradient")) {
                player.sendMessage(AMPERSAND_SERIALIZER.deserialize(
                        plugin.getChatPrefix() + "&cYou don't have permission for gradient colors."));
                return;
            }
            ChatColorData data = getOrCreate(manager, player);
            data.setType("gradient");
            data.setColors(gradientColors);
            manager.setPlayerColor(player.getUniqueId(), data);
            player.sendMessage(AMPERSAND_SERIALIZER.deserialize(
                    plugin.getChatPrefix() + "&aGradient color set!"));
            player.closeInventory();
            return;
        }

        // Custom gradient (slot 44)
        if (slot == 44) {
            if (!player.hasPermission("awesomechat.chatcolor.custom")) {
                player.sendMessage(AMPERSAND_SERIALIZER.deserialize(
                        plugin.getChatPrefix() + "&cYou don't have permission for custom gradients."));
                return;
            }
            manager.setAwaitingCustomInput(player.getUniqueId(), true);
            player.closeInventory();
            player.sendMessage(AMPERSAND_SERIALIZER.deserialize(
                    plugin.getChatPrefix() + "&ePlease enter 2-4 hex color codes separated by spaces."));
            player.sendMessage(AMPERSAND_SERIALIZER.deserialize(
                    plugin.getChatPrefix() + "&7Example: &f#FF0000 #00FF00 #0000FF"));
            player.sendMessage(AMPERSAND_SERIALIZER.deserialize(
                    plugin.getChatPrefix() + "&7Type &ccancel &7to abort."));
            return;
        }

        // Style toggles (slots 45-49)
        String style = getStyleForSlot(slot);
        if (style != null) {
            manager.toggleStyle(player.getUniqueId(), style);
            openGUI(player);
            return;
        }

        // Reset (slot 52)
        if (slot == 52) {
            manager.clearPlayerColor(player.getUniqueId());
            player.sendMessage(AMPERSAND_SERIALIZER.deserialize(
                    plugin.getChatPrefix() + "&aChat color has been reset."));
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ChatColorHolder) {
            event.setCancelled(true);
        }
    }

    // ========== Slot Mappings ==========

    private String getSolidColorForSlot(int slot) {
        return switch (slot) {
            case 9  -> "#000000";
            case 10 -> "#0000AA";
            case 11 -> "#00AA00";
            case 12 -> "#00AAAA";
            case 13 -> "#AA0000";
            case 14 -> "#AA00AA";
            case 15 -> "#FFAA00";
            case 16 -> "#AAAAAA";
            case 17 -> "#555555";
            case 18 -> "#5555FF";
            case 19 -> "#55FF55";
            case 20 -> "#55FFFF";
            case 21 -> "#FF5555";
            case 22 -> "#FF55FF";
            case 23 -> "#FFFF55";
            case 24 -> "#FFFFFF";
            default -> null;
        };
    }

    private List<String> getGradientColorsForSlot(int slot) {
        return switch (slot) {
            case 27 -> List.of("#FF0000", "#610000");
            case 28 -> List.of("#0000FF", "#00BFFF");
            case 29 -> List.of("#8A2BE2", "#4B0082");
            case 30 -> List.of("#00FF7F", "#006400");
            case 31 -> List.of("#FFB7C5", "#FF69B4");
            case 32 -> List.of("#FF4500", "#FF8C00");
            case 33 -> List.of("#FFD700", "#DAA520");
            case 34 -> List.of("#008080", "#20B2AA");
            case 36 -> List.of("#A5F3FC", "#38BDF8", "#1E3A5F");
            case 37 -> List.of("#FFFF00", "#FF4500", "#8B0000");
            case 38 -> List.of("#00FF00", "#00BFFF", "#8A2BE2");
            case 39 -> List.of("#FF69B4", "#DDA0DD", "#87CEEB");
            case 40 -> List.of("#228B22", "#32CD32", "#ADFF2F");
            default -> null;
        };
    }

    private String getStyleForSlot(int slot) {
        return switch (slot) {
            case 45 -> "bold";
            case 46 -> "italic";
            case 47 -> "underline";
            case 48 -> "strikethrough";
            case 49 -> "obfuscated";
            default -> null;
        };
    }

    private ChatColorData getOrCreate(ChatColorManager manager, Player player) {
        ChatColorData data = manager.getPlayerColor(player.getUniqueId());
        if (data == null) {
            data = new ChatColorData();
        }
        return data;
    }
}
