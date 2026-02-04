package dev.adf.awesomeChat.gui;

import dev.adf.awesomeChat.AwesomeChat;
import dev.adf.awesomeChat.managers.ChatColorManager;
import dev.adf.awesomeChat.managers.ChatColorManager.ChatColorData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

import static dev.adf.awesomeChat.listeners.ChatListener.formatColors;
import static dev.adf.awesomeChat.listeners.ChatListener.deserializeLegacy;

public class ChatColorGUI implements Listener {

    private final AwesomeChat plugin;
    private final List<GradientDefinition> allGradients = new ArrayList<>();
    private final Map<UUID, Integer> playerGradientPage = new HashMap<>();

    // ========== Gradient Definition ==========

    public static class GradientDefinition {
        private final String name;
        private final String permissionKey;
        private final List<String> colors;
        private final Material material;
        private final boolean builtIn;

        public GradientDefinition(String name, String permissionKey, List<String> colors, Material material, boolean builtIn) {
            this.name = name;
            this.permissionKey = permissionKey;
            this.colors = colors;
            this.material = material;
            this.builtIn = builtIn;
        }

        public String getName() { return name; }
        public String getPermissionKey() { return permissionKey; }
        public List<String> getColors() { return colors; }
        public Material getMaterial() { return material; }
        public boolean isBuiltIn() { return builtIn; }
        public String getPermission() { return "awesomechat.chatcolor.gradient." + permissionKey; }
    }

    // ========== Solid Color Definition ==========

    private record SolidColor(String name, String permissionKey, String hex, Material material) {
        String getPermission() { return "awesomechat.chatcolor.color." + permissionKey; }
    }

    private static final List<SolidColor> SOLID_COLORS = List.of(
            new SolidColor("Black",       "black",       "#000000", Material.BLACK_CONCRETE),
            new SolidColor("Dark Blue",   "darkblue",    "#0000AA", Material.BLUE_CONCRETE),
            new SolidColor("Dark Green",  "darkgreen",   "#00AA00", Material.GREEN_CONCRETE),
            new SolidColor("Dark Aqua",   "darkaqua",    "#00AAAA", Material.CYAN_CONCRETE),
            new SolidColor("Dark Red",    "darkred",     "#AA0000", Material.RED_CONCRETE),
            new SolidColor("Dark Purple", "darkpurple",  "#AA00AA", Material.PURPLE_CONCRETE),
            new SolidColor("Gold",        "gold",        "#FFAA00", Material.ORANGE_CONCRETE),
            new SolidColor("Gray",        "gray",        "#AAAAAA", Material.LIGHT_GRAY_CONCRETE),
            new SolidColor("Dark Gray",   "darkgray",    "#555555", Material.GRAY_CONCRETE),
            new SolidColor("Blue",        "blue",        "#5555FF", Material.BLUE_WOOL),
            new SolidColor("Green",       "green",       "#55FF55", Material.LIME_CONCRETE),
            new SolidColor("Aqua",        "aqua",        "#55FFFF", Material.LIGHT_BLUE_WOOL),
            new SolidColor("Red",         "red",         "#FF5555", Material.RED_WOOL),
            new SolidColor("Light Purple","lightpurple", "#FF55FF", Material.PINK_CONCRETE),
            new SolidColor("Yellow",      "yellow",      "#FFFF55", Material.YELLOW_CONCRETE),
            new SolidColor("White",       "white",       "#FFFFFF", Material.WHITE_CONCRETE)
    );

    private static final int GRADIENTS_PER_PAGE = 21;

    // ========== Inventory Holders ==========

    public static class ChatColorHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }

    public static class GradientPageHolder implements InventoryHolder {
        private final int page;
        public GradientPageHolder(int page) { this.page = page; }
        public int getPage() { return page; }
        @Override
        public Inventory getInventory() { return null; }
    }

    // ========== Constructor ==========

    public ChatColorGUI(AwesomeChat plugin) {
        this.plugin = plugin;
        loadGradients();
    }

    public void loadGradients() {
        allGradients.clear();

        // Built-in gradients
        allGradients.add(new GradientDefinition("Sunset",       "sunset",      List.of("#FFA700", "#FF2700"),                    Material.RED_CONCRETE,     true));
        allGradients.add(new GradientDefinition("Ocean",        "ocean",       List.of("#0000FF", "#00BFFF"),                    Material.BLUE_CONCRETE,    true));
        allGradients.add(new GradientDefinition("Royal",        "royal",       List.of("#8A2BE2", "#4B0082"),                    Material.PURPLE_CONCRETE,  true));
        allGradients.add(new GradientDefinition("Mint",         "mint",        List.of("#00FF7F", "#006400"),                    Material.LIME_CONCRETE,    true));
        allGradients.add(new GradientDefinition("Sakura",       "sakura",      List.of("#FFB7C5", "#FF69B4"),                    Material.PINK_CONCRETE,    true));
        allGradients.add(new GradientDefinition("Ember",        "ember",       List.of("#FF4500", "#FF8C00"),                    Material.ORANGE_CONCRETE,  true));
        allGradients.add(new GradientDefinition("Golden",       "golden",      List.of("#FFD700", "#DAA520"),                    Material.YELLOW_CONCRETE,  true));
        allGradients.add(new GradientDefinition("Teal",         "teal",        List.of("#008080", "#20B2AA"),                    Material.CYAN_CONCRETE,    true));
        allGradients.add(new GradientDefinition("Ice",          "ice",         List.of("#A5F3FC", "#38BDF8", "#1E3A5F"),         Material.PACKED_ICE,       true));
        allGradients.add(new GradientDefinition("Fire",         "fire",        List.of("#FFFF00", "#FF4500", "#8B0000"),         Material.MAGMA_BLOCK,      true));
        allGradients.add(new GradientDefinition("Aurora",       "aurora",      List.of("#00FF00", "#00BFFF", "#8A2BE2"),         Material.PRISMARINE,       true));
        allGradients.add(new GradientDefinition("Cotton Candy", "cottoncandy", List.of("#FF69B4", "#DDA0DD", "#87CEEB"),         Material.AMETHYST_BLOCK,   true));
        allGradients.add(new GradientDefinition("Forest",       "forest",      List.of("#228B22", "#32CD32", "#ADFF2F"),         Material.EMERALD_BLOCK,    true));

        // Admin-created gradients from config
        var config = plugin.getPluginConfig();
        var section = config.getConfigurationSection("chatcolor.custom-gradients");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                List<String> colors = config.getStringList("chatcolor.custom-gradients." + key + ".colors");
                String materialName = config.getString("chatcolor.custom-gradients." + key + ".material", "PAPER");
                Material material;
                try {
                    material = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    material = Material.PAPER;
                }
                if (colors.size() >= 2) {
                    String displayName = key.substring(0, 1).toUpperCase() + key.substring(1);
                    allGradients.add(new GradientDefinition(displayName, key.toLowerCase(), colors, material, false));
                }
            }
        }
    }

    public List<GradientDefinition> getAllGradients() {
        return allGradients;
    }

    // ========== Page 1: Solid Colors ==========

    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(
                new ChatColorHolder(), 54,
                deserializeLegacy(formatColors("&5&lChat Color Picker"))
        );

        ChatColorManager manager = plugin.getChatColorManager();
        ChatColorData current = manager != null ? manager.getPlayerColor(player.getUniqueId()) : null;

        // Row 1: Header
        fillGlass(inv, 0, 8);
        inv.setItem(4, createItem(Material.NETHER_STAR, "&d&lChat Color",
                List.of("", "&7Pick a color or gradient", "&7for your chat messages.", "", "&7Current: " + getCurrentDisplay(current))));

        // Check if player has ANY color permission
        boolean hasAnyColor = false;
        for (SolidColor color : SOLID_COLORS) {
            if (player.hasPermission(color.getPermission())) {
                hasAnyColor = true;
                break;
            }
        }

        if (!hasAnyColor) {
            // Fill rows 2-3 with glass, barrier in center
            fillGlass(inv, 9, 26);
            inv.setItem(22, createItem(Material.BARRIER, "&c&lNo Colors Available",
                    List.of("", "&cYou don't have permission", "&cto use any colors")));
        } else {
            // Row 2: Solid colors 0-8 (Black through Dark Gray)
            for (int i = 0; i < 9; i++) {
                SolidColor color = SOLID_COLORS.get(i);
                if (player.hasPermission(color.getPermission())) {
                    setColorItem(inv, 9 + i, color.material, "&" + getColorCode(i) + "&l" + color.name, color.hex, current);
                } else {
                    setGlass(inv, 9 + i);
                }
            }

            // Row 3: Solid colors 9-f (Blue through White) + glass padding
            for (int i = 9; i < 16; i++) {
                SolidColor color = SOLID_COLORS.get(i);
                if (player.hasPermission(color.getPermission())) {
                    setColorItem(inv, 18 + (i - 9), color.material, "&" + getColorCode(i) + "&l" + color.name, color.hex, current);
                } else {
                    setGlass(inv, 18 + (i - 9));
                }
            }
            fillGlass(inv, 25, 26);
        }

        // Row 4: Style toggles + glass
        setStyleItem(inv, 27, Material.IRON_INGOT,  "&f&lBold",          "bold",          current, player, "awesomechat.styling.bold");
        setStyleItem(inv, 28, Material.FEATHER,      "&f&lItalic",        "italic",        current, player, "awesomechat.styling.italic");
        setStyleItem(inv, 29, Material.CHAIN,        "&f&lUnderline",     "underline",     current, player, "awesomechat.styling.underline");
        setStyleItem(inv, 30, Material.STRING,       "&f&lStrikethrough", "strikethrough", current, player, "awesomechat.styling.strikethrough");
        setStyleItem(inv, 31, Material.ENDER_PEARL,  "&f&lObfuscated",   "obfuscated",    current, player, "awesomechat.styling.obfuscated");
        fillGlass(inv, 32, 35);

        // Row 5: Glass filler
        fillGlass(inv, 36, 44);

        // Row 6: Navigation
        inv.setItem(45, createItem(Material.SPECTRAL_ARROW, "&a&lGradients \u2192",
                List.of("", "&7Browse gradient colors")));
        fillGlass(inv, 46, 51);
        inv.setItem(52, createItem(Material.LAVA_BUCKET, "&c&lReset Color",
                List.of("", "&7Remove your custom", "&7chat color and styles.")));
        setGlass(inv, 53);

        player.openInventory(inv);
    }

    private String getColorCode(int index) {
        return switch (index) {
            case 0 -> "0";
            case 1 -> "1";
            case 2 -> "2";
            case 3 -> "3";
            case 4 -> "4";
            case 5 -> "5";
            case 6 -> "6";
            case 7 -> "7";
            case 8 -> "8";
            case 9 -> "9";
            case 10 -> "a";
            case 11 -> "b";
            case 12 -> "c";
            case 13 -> "d";
            case 14 -> "e";
            case 15 -> "f";
            default -> "f";
        };
    }

    // ========== Gradient Pages==========

    public void openGradientPage(Player player, int page) {
        // Filter gradients by permission
        List<GradientDefinition> permitted = new ArrayList<>();
        for (GradientDefinition grad : allGradients) {
            if (player.hasPermission(grad.getPermission())) {
                permitted.add(grad);
            }
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) permitted.size() / GRADIENTS_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        playerGradientPage.put(player.getUniqueId(), page);

        ChatColorManager manager = plugin.getChatColorManager();
        ChatColorData current = manager != null ? manager.getPlayerColor(player.getUniqueId()) : null;

        Inventory inv = Bukkit.createInventory(
                new GradientPageHolder(page), 54,
                deserializeLegacy(formatColors("&5&lGradient Colors"))
        );

        // Row 1: Header
        fillGlass(inv, 0, 8);
        inv.setItem(4, createItem(Material.NETHER_STAR, "&d&lGradient Colors",
                List.of("", "&7Browse gradient colors", "&7for your chat messages.",
                        "", "&7Page &f" + (page + 1) + "&7/&f" + totalPages,
                        "", "&7Current: " + getCurrentDisplay(current))));

        // Rows 2-4: Gradients (slots 9-35, up to 21 per page)
        if (permitted.isEmpty()) {
            fillGlass(inv, 9, 35);
            inv.setItem(22, createItem(Material.BARRIER, "&c&lNo Gradients Available",
                    List.of("", "&cYou don't have permission", "&cto use any gradients")));
        } else {
            int start = page * GRADIENTS_PER_PAGE;
            int end = Math.min(start + GRADIENTS_PER_PAGE, permitted.size());

            for (int i = 0; i < GRADIENTS_PER_PAGE; i++) {
                int slot = 9 + i;
                int gradIndex = start + i;
                if (gradIndex < end) {
                    GradientDefinition grad = permitted.get(gradIndex);
                    setGradientItem(inv, slot, grad, current);
                } else {
                    setGlass(inv, slot);
                }
            }

            // Fill any remaining slots in rows 2-4
            for (int i = 9 + GRADIENTS_PER_PAGE; i <= 35; i++) {
                // 9+21=30, so slots 30-35 are filled
                if (inv.getItem(i) == null) {
                    setGlass(inv, i);
                }
            }
        }

        // Row 5: Custom gradient + glass
        if (player.hasPermission("awesomechat.chatcolor.custom")) {
            inv.setItem(36, createItem(Material.ANVIL, "&d&lCustom Gradient",
                    List.of("", "&7Create a custom gradient", "&7with up to 4 hex colors.", "", "&eClick to customize!")));
        } else {
            setGlass(inv, 36);
        }
        fillGlass(inv, 37, 44);

        // Row 6: Navigation
        inv.setItem(45, createItem(Material.SPECTRAL_ARROW, "&c&l\u2190 Colors",
                List.of("", "&7Back to solid colors")));

        if (page > 0) {
            inv.setItem(46, createItem(Material.ARROW, "&e&l\u2190 Previous",
                    List.of("", "&7Go to page " + page)));
        } else {
            setGlass(inv, 46);
        }

        fillGlass(inv, 47, 48);

        // Page indicator (slot 49)
        inv.setItem(49, createItem(Material.PAPER, "&7Page &f" + (page + 1) + "&7/&f" + totalPages,
                List.of()));

        if (page < totalPages - 1) {
            inv.setItem(50, createItem(Material.ARROW, "&e&lNext \u2192",
                    List.of("", "&7Go to page " + (page + 2))));
        } else {
            setGlass(inv, 50);
        }

        setGlass(inv, 51);
        inv.setItem(52, createItem(Material.LAVA_BUCKET, "&c&lReset Color",
                List.of("", "&7Remove your custom", "&7chat color and styles.")));
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
        lore.add("&7Example: &#" + stripHash(hex) + "Hello there!");
        lore.add("");
        if (isCurrentColor(current, "solid", List.of(hex))) {
            lore.add("&a&lCurrently Selected!");
        } else {
            lore.add("&eClick to select!");
        }
        inv.setItem(slot, createItem(material, name, lore));
    }

    private void setGradientItem(Inventory inv, int slot, GradientDefinition grad, ChatColorData current) {
        String gradientName = buildGradientText(grad.getName(), grad.getColors(), true);
        List<String> lore = new ArrayList<>();
        lore.add("&7Colors: &f" + String.join(" &7\u2192 &f", grad.getColors()));
        lore.add("&7Example: " + buildGradientText("Hello there!", grad.getColors(), false));
        lore.add("");
        if (isCurrentColor(current, "gradient", grad.getColors())) {
            lore.add("&a&lCurrently Selected!");
        } else {
            lore.add("&eClick to select!");
        }
        inv.setItem(slot, createItem(grad.getMaterial(), gradientName, lore));
    }

    private void setStyleItem(Inventory inv, int slot, Material material, String name,
                               String styleKey, ChatColorData current, Player player, String permission) {
        if (!player.hasPermission(permission)) {
            setGlass(inv, slot);
            return;
        }
        boolean active = isStyleActive(current, styleKey);
        String styleCode = getStyleCode(styleKey);
        List<String> lore = new ArrayList<>();
        lore.add(active ? "&a&lEnabled" : "&c&lDisabled");
        lore.add("&7Example: &f" + styleCode + "Hello there!");
        lore.add("");
        lore.add("&eClick to toggle!");
        inv.setItem(slot, createItem(material, name, lore));
    }

    private String getStyleCode(String styleKey) {
        return switch (styleKey) {
            case "bold" -> "&l";
            case "italic" -> "&o";
            case "underline" -> "&n";
            case "strikethrough" -> "&m";
            case "obfuscated" -> "&k";
            default -> "";
        };
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("").decoration(TextDecoration.ITALIC, false)
                    .append(deserializeLegacy(formatColors(name))));
            List<Component> loreComponents = new ArrayList<>();
            for (String line : lore) {
                loreComponents.add(Component.text("").decoration(TextDecoration.ITALIC, false)
                        .append(deserializeLegacy(formatColors(line))));
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

    // ========== Gradient Utilities ==========

    private String buildGradientText(String text, List<String> colors, boolean bold) {
        int len = text.length();
        if (len == 0) return "";

        int[][] rgb = new int[colors.size()][3];
        for (int i = 0; i < colors.size(); i++) {
            rgb[i] = hexToRgb(colors.get(i));
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < len; i++) {
            float ratio = len == 1 ? 0f : (float) i / (len - 1);
            int[] interpolated = interpolateMultiStop(rgb, ratio);
            String hex = String.format("%02X%02X%02X", interpolated[0], interpolated[1], interpolated[2]);
            result.append("&#").append(hex);
            if (bold) result.append("&l");
            result.append(text.charAt(i));
        }
        return result.toString();
    }

    private int[] hexToRgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return new int[]{
                Integer.parseInt(h.substring(0, 2), 16),
                Integer.parseInt(h.substring(2, 4), 16),
                Integer.parseInt(h.substring(4, 6), 16)
        };
    }

    private int[] interpolateMultiStop(int[][] stops, float ratio) {
        if (stops.length == 1) return stops[0];
        ratio = Math.max(0f, Math.min(1f, ratio));
        float segmentCount = stops.length - 1;
        float scaledRatio = ratio * segmentCount;
        int segmentIndex = (int) scaledRatio;
        if (segmentIndex >= stops.length - 1) return stops[stops.length - 1];
        float segmentRatio = scaledRatio - segmentIndex;
        int[] from = stops[segmentIndex];
        int[] to = stops[segmentIndex + 1];
        return new int[]{
                (int) (from[0] + (to[0] - from[0]) * segmentRatio),
                (int) (from[1] + (to[1] - from[1]) * segmentRatio),
                (int) (from[2] + (to[2] - from[2]) * segmentRatio)
        };
    }

    private String stripHash(String hex) {
        return hex.startsWith("#") ? hex.substring(1) : hex;
    }

    // ========== Click Handling: Page 1 (Solid Colors) ==========

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ChatColorHolder) {
            handleColorPageClick(event);
        } else if (event.getInventory().getHolder() instanceof GradientPageHolder) {
            handleGradientPageClick(event);
        }
    }

    private void handleColorPageClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        ChatColorManager manager = plugin.getChatColorManager();
        if (manager == null) return;
        int slot = event.getRawSlot();

        // Solid colors (slots 9-24)
        if (slot >= 9 && slot <= 24) {
            int colorIndex = (slot >= 18) ? (slot - 18 + 9) : (slot - 9);
            if (colorIndex >= 0 && colorIndex < SOLID_COLORS.size()) {
                SolidColor color = SOLID_COLORS.get(colorIndex);
                if (!player.hasPermission(color.getPermission())) return;

                ChatColorData data = getOrCreate(manager, player);
                data.setType("solid");
                data.setColors(List.of(color.hex));
                manager.setPlayerColor(player.getUniqueId(), data);
                player.sendMessage(deserializeLegacy(formatColors(
                        plugin.getChatPrefix() + "&aChat color set to &f" + color.hex + "&a!")));
                player.closeInventory();
                return;
            }
        }

        // Style toggles (slots 27-31)
        String style = getStyleForSlot(slot);
        if (style != null) {
            manager.toggleStyle(player.getUniqueId(), style);
            openGUI(player);
            return;
        }

        // Gradients button (slot 45)
        if (slot == 45) {
            openGradientPage(player, 0);
            return;
        }

        // Reset (slot 52)
        if (slot == 52) {
            manager.clearPlayerColor(player.getUniqueId());
            player.sendMessage(deserializeLegacy(formatColors(
                    plugin.getChatPrefix() + "&aChat color has been reset.")));
            player.closeInventory();
        }
    }

    // ========== Click Handling: Gradient Pages ==========

    private void handleGradientPageClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (event.getCurrentItem().getType() == Material.GRAY_STAINED_GLASS_PANE) return;

        ChatColorManager manager = plugin.getChatColorManager();
        if (manager == null) return;
        int slot = event.getRawSlot();
        int page = ((GradientPageHolder) event.getInventory().getHolder()).getPage();

        // Gradient items (slots 9-29)
        if (slot >= 9 && slot < 9 + GRADIENTS_PER_PAGE) {
            List<GradientDefinition> permitted = getPermittedGradients(player);
            int gradIndex = page * GRADIENTS_PER_PAGE + (slot - 9);
            if (gradIndex >= 0 && gradIndex < permitted.size()) {
                GradientDefinition grad = permitted.get(gradIndex);
                ChatColorData data = getOrCreate(manager, player);
                data.setType("gradient");
                data.setColors(grad.getColors());
                manager.setPlayerColor(player.getUniqueId(), data);
                player.sendMessage(deserializeLegacy(formatColors(
                        plugin.getChatPrefix() + "&aGradient color set to &f" + grad.getName() + "&a!")));
                player.closeInventory();
                return;
            }
        }

        // Custom gradient (slot 36)
        if (slot == 36) {
            if (!player.hasPermission("awesomechat.chatcolor.custom")) return;
            manager.setAwaitingCustomInput(player.getUniqueId(), true);
            player.closeInventory();
            player.sendMessage(deserializeLegacy(formatColors(
                    plugin.getChatPrefix() + "&ePlease enter 2-4 hex color codes separated by spaces.")));
            player.sendMessage(deserializeLegacy(formatColors(
                    plugin.getChatPrefix() + "&7Example: &f#FF0000 #00FF00 #0000FF")));
            player.sendMessage(deserializeLegacy(formatColors(
                    plugin.getChatPrefix() + "&7Type &ccancel &7to abort.")));
            return;
        }

        // Back to colors (slot 45)
        if (slot == 45) {
            openGUI(player);
            return;
        }

        // Previous page (slot 46)
        if (slot == 46 && page > 0) {
            openGradientPage(player, page - 1);
            return;
        }

        // Next page (slot 50)
        if (slot == 50) {
            List<GradientDefinition> permitted = getPermittedGradients(player);
            int totalPages = Math.max(1, (int) Math.ceil((double) permitted.size() / GRADIENTS_PER_PAGE));
            if (page < totalPages - 1) {
                openGradientPage(player, page + 1);
            }
            return;
        }

        // Reset (slot 52)
        if (slot == 52) {
            manager.clearPlayerColor(player.getUniqueId());
            player.sendMessage(deserializeLegacy(formatColors(
                    plugin.getChatPrefix() + "&aChat color has been reset.")));
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ChatColorHolder
                || event.getInventory().getHolder() instanceof GradientPageHolder) {
            event.setCancelled(true);
        }
    }

    // ========== Slot Mappings ==========

    private String getStyleForSlot(int slot) {
        return switch (slot) {
            case 27 -> "bold";
            case 28 -> "italic";
            case 29 -> "underline";
            case 30 -> "strikethrough";
            case 31 -> "obfuscated";
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

    private List<GradientDefinition> getPermittedGradients(Player player) {
        List<GradientDefinition> permitted = new ArrayList<>();
        for (GradientDefinition grad : allGradients) {
            if (player.hasPermission(grad.getPermission())) {
                permitted.add(grad);
            }
        }
        return permitted;
    }
}
