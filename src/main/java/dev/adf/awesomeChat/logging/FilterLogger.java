package dev.adf.awesomeChat.logging;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FilterLogger {

    private static final Path LOGS_FOLDER = Paths.get("plugins/AwesomeChat/logs");

    static {
        try {
            if (!Files.exists(LOGS_FOLDER)) {
                Files.createDirectories(LOGS_FOLDER);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void log(String message) {
        String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Path file = LOGS_FOLDER.resolve("log-violations-" + date + ".log");

        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
        String logLine = "[" + timestamp + "] " + message;

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(logLine);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
