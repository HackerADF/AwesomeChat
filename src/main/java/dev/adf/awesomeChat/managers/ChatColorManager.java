package dev.adf.awesomeChat.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ChatColorManager {

    private final JavaPlugin plugin;
    private final Path dataFolder;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<UUID, ChatColorData> colorMap = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> awaitingCustomInput = new ConcurrentHashMap<>();

    private static final Pattern MANUAL_COLOR_CODES = Pattern.compile(
            "(?i)(&[0-9a-fk-or])|(&#[A-Fa-f0-9]{6})|(<#[A-Fa-f0-9]{6}>)|(<(?:black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|dark_gray|blue|green|aqua|red|light_purple|yellow|white)>)"
    );

    public ChatColorManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder().toPath().resolve("data").resolve("chatcolors");

        try {
            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create chatcolors directory: " + e.getMessage());
        }

        loadAll();
    }

    // ========== Data Model ==========

    public static class ChatColorData {
        private String type;
        private List<String> colors;
        private boolean bold;
        private boolean italic;
        private boolean underline;
        private boolean strikethrough;
        private boolean obfuscated;

        public ChatColorData() {
            this.type = "solid";
            this.colors = new ArrayList<>();
            this.bold = false;
            this.italic = false;
            this.underline = false;
            this.strikethrough = false;
            this.obfuscated = false;
        }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public List<String> getColors() { return colors; }
        public void setColors(List<String> colors) { this.colors = new ArrayList<>(colors); }
        public boolean isBold() { return bold; }
        public void setBold(boolean bold) { this.bold = bold; }
        public boolean isItalic() { return italic; }
        public void setItalic(boolean italic) { this.italic = italic; }
        public boolean isUnderline() { return underline; }
        public void setUnderline(boolean underline) { this.underline = underline; }
        public boolean isStrikethrough() { return strikethrough; }
        public void setStrikethrough(boolean strikethrough) { this.strikethrough = strikethrough; }
        public boolean isObfuscated() { return obfuscated; }
        public void setObfuscated(boolean obfuscated) { this.obfuscated = obfuscated; }
    }

    // ========== Persistence ==========

    private void loadAll() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataFolder, "*.json")) {
            for (Path file : stream) {
                String filename = file.getFileName().toString();
                String uuidStr = filename.replace(".json", "");
                try {
                    UUID playerId = UUID.fromString(uuidStr);
                    ChatColorData data = loadPlayer(file);
                    if (data != null) {
                        colorMap.put(playerId, data);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid chatcolor file: " + filename);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load chatcolor data: " + e.getMessage());
        }
    }

    private ChatColorData loadPlayer(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            return gson.fromJson(reader, ChatColorData.class);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to load chatcolor file: " + file.getFileName());
            return null;
        }
    }

    private void savePlayer(UUID playerId) {
        Path file = dataFolder.resolve(playerId.toString() + ".json");
        ChatColorData data = colorMap.get(playerId);

        if (data == null) {
            try { Files.deleteIfExists(file); } catch (IOException ignored) {}
            return;
        }

        try (Writer writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            gson.toJson(data, writer);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save chatcolor data for " + playerId + ": " + e.getMessage());
        }
    }

    // ========== Public API ==========

    public ChatColorData getPlayerColor(UUID playerId) {
        return colorMap.get(playerId);
    }

    public void setPlayerColor(UUID playerId, ChatColorData data) {
        if (data == null) {
            colorMap.remove(playerId);
        } else {
            colorMap.put(playerId, data);
        }
        savePlayer(playerId);
    }

    public void clearPlayerColor(UUID playerId) {
        colorMap.remove(playerId);
        savePlayer(playerId);
    }

    public void toggleStyle(UUID playerId, String style) {
        ChatColorData data = colorMap.get(playerId);
        if (data == null) {
            data = new ChatColorData();
            data.setType("solid");
            data.setColors(List.of("#FFFFFF"));
        }
        switch (style.toLowerCase()) {
            case "bold"          -> data.setBold(!data.isBold());
            case "italic"        -> data.setItalic(!data.isItalic());
            case "underline"     -> data.setUnderline(!data.isUnderline());
            case "strikethrough" -> data.setStrikethrough(!data.isStrikethrough());
            case "obfuscated"    -> data.setObfuscated(!data.isObfuscated());
        }
        colorMap.put(playerId, data);
        savePlayer(playerId);
    }

    public boolean isAwaitingCustomInput(UUID playerId) {
        return awaitingCustomInput.getOrDefault(playerId, false);
    }

    public void setAwaitingCustomInput(UUID playerId, boolean awaiting) {
        if (awaiting) {
            awaitingCustomInput.put(playerId, true);
        } else {
            awaitingCustomInput.remove(playerId);
        }
    }

    // ========== Color Application ==========

    public String applyColor(UUID playerId, String message, boolean useMiniMessage) {
        ChatColorData data = colorMap.get(playerId);
        if (data == null || data.getColors() == null || data.getColors().isEmpty()) return message;

        String stylePrefix = buildStylePrefix(data, useMiniMessage);

        if ("solid".equals(data.getType()) || data.getColors().size() == 1) {
            String color = data.getColors().get(0);
            if (useMiniMessage) {
                return stylePrefix + "<" + color + ">" + message;
            } else {
                return stylePrefix + "&#" + stripHash(color) + message;
            }
        }

        // Gradient
        boolean hasManualCodes = MANUAL_COLOR_CODES.matcher(message).find();

        if (hasManualCodes) {
            String fallbackColor = data.getColors().get(0);
            if (useMiniMessage) {
                return stylePrefix + "<" + fallbackColor + ">" + message;
            } else {
                return stylePrefix + "&#" + stripHash(fallbackColor) + message;
            }
        }

        if (useMiniMessage) {
            return applyGradientMiniMessage(data, message, stylePrefix);
        } else {
            return applyGradientLegacy(data, message, stylePrefix);
        }
    }

    private String buildStylePrefix(ChatColorData data, boolean useMiniMessage) {
        StringBuilder sb = new StringBuilder();
        if (data.isBold())          sb.append(useMiniMessage ? "<bold>" : "&l");
        if (data.isItalic())        sb.append(useMiniMessage ? "<italic>" : "&o");
        if (data.isUnderline())     sb.append(useMiniMessage ? "<underlined>" : "&n");
        if (data.isStrikethrough()) sb.append(useMiniMessage ? "<strikethrough>" : "&m");
        if (data.isObfuscated())    sb.append(useMiniMessage ? "<obfuscated>" : "&k");
        return sb.toString();
    }

    private String applyGradientMiniMessage(ChatColorData data, String message, String stylePrefix) {
        StringBuilder tag = new StringBuilder("<gradient");
        for (String color : data.getColors()) {
            tag.append(":").append(color);
        }
        tag.append(">");
        return tag + stylePrefix + message + "</gradient>";
    }

    private String applyGradientLegacy(ChatColorData data, String message, String stylePrefix) {
        List<String> colors = data.getColors();
        int len = message.length();
        if (len == 0) return message;
        if (len == 1) {
            return "&#" + stripHash(colors.get(0)) + stylePrefix + message;
        }

        int[][] rgb = new int[colors.size()][3];
        for (int i = 0; i < colors.size(); i++) {
            rgb[i] = hexToRgb(colors.get(i));
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < len; i++) {
            float ratio = (float) i / (len - 1);
            int[] interpolated = interpolateMultiStop(rgb, ratio);
            String hex = String.format("%02X%02X%02X", interpolated[0], interpolated[1], interpolated[2]);
            result.append("&#").append(hex);
            if (data.isBold())          result.append("&l");
            if (data.isItalic())        result.append("&o");
            if (data.isUnderline())     result.append("&n");
            if (data.isStrikethrough()) result.append("&m");
            if (data.isObfuscated())    result.append("&k");
            result.append(message.charAt(i));
        }
        return result.toString();
    }

    // ========== Gradient Math ==========

    private int[] interpolateMultiStop(int[][] stops, float ratio) {
        if (stops.length == 1) return stops[0];

        ratio = Math.max(0f, Math.min(1f, ratio));

        float segmentCount = stops.length - 1;
        float scaledRatio = ratio * segmentCount;
        int segmentIndex = (int) scaledRatio;

        if (segmentIndex >= stops.length - 1) {
            return stops[stops.length - 1];
        }

        float segmentRatio = scaledRatio - segmentIndex;
        int[] from = stops[segmentIndex];
        int[] to = stops[segmentIndex + 1];

        return new int[] {
                (int) (from[0] + (to[0] - from[0]) * segmentRatio),
                (int) (from[1] + (to[1] - from[1]) * segmentRatio),
                (int) (from[2] + (to[2] - from[2]) * segmentRatio)
        };
    }

    private int[] hexToRgb(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        return new int[] {
                Integer.parseInt(h.substring(0, 2), 16),
                Integer.parseInt(h.substring(2, 4), 16),
                Integer.parseInt(h.substring(4, 6), 16)
        };
    }

    private String stripHash(String hex) {
        return hex.startsWith("#") ? hex.substring(1) : hex;
    }
}
