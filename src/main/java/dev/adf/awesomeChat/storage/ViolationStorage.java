package dev.adf.awesomeChat.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ViolationStorage {

    private static final Path DATA_FOLDER = Paths.get("plugins/AwesomeChat/data");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static {
        try {
            if (!Files.exists(DATA_FOLDER)) {
                Files.createDirectories(DATA_FOLDER);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Represents a violation record
    public static class ViolationRecord {
        public String ruleName;
        public long timestamp;

        public ViolationRecord(String ruleName, long timestamp) {
            this.ruleName = ruleName;
            this.timestamp = timestamp;
        }
    }

    private static Path getFile(UUID playerId) {
        return DATA_FOLDER.resolve(playerId.toString() + ".json");
    }

    /** Ensures the JSON file exists and is a valid empty array if new */
    private static void ensureFileExists(Path file) {
        if (!Files.exists(file)) {
            try {
                Files.createFile(file);
                try (Writer writer = Files.newBufferedWriter(file)) {
                    writer.write("[]"); // initialize empty array
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addViolation(UUID playerId, String ruleName) {
        List<ViolationRecord> violations = getViolations(playerId);
        violations.add(new ViolationRecord(ruleName, System.currentTimeMillis()));
        saveViolations(playerId, violations);
    }

    public static List<ViolationRecord> getViolations(UUID playerId) {
        Path file = getFile(playerId);
        ensureFileExists(file);

        try (Reader reader = Files.newBufferedReader(file)) {
            ViolationRecord[] records = GSON.fromJson(reader, ViolationRecord[].class);
            if (records != null) {
                return new ArrayList<>(Arrays.asList(records));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (com.google.gson.JsonSyntaxException e) {
            // File corrupted? Reset to empty array
            saveViolations(playerId, new ArrayList<>());
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    private static void saveViolations(UUID playerId, List<ViolationRecord> violations) {
        Path file = getFile(playerId);
        ensureFileExists(file);

        try (Writer writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(violations, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
