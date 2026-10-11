package io.github.luigichopper.palimpsest.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class PalimpsestConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean devMode = false;
    public boolean forceActivate = false;
    public String mode = "canon";
    public boolean metaLayerEnabled = true;
    public boolean exportsEnabled = true;
    public boolean logLinesEnabled = true;

    public static class DirectorConfig {
        public int maxTier = 2;
    }
    public DirectorConfig director = new DirectorConfig();

    public static class AccessibilityConfig {
        public boolean reduceFlashes = true;
        public boolean softAudioRamps = true;
    }
    public AccessibilityConfig accessibility = new AccessibilityConfig();

    public static class SchedulerConfig {
        public int msPerTick = 3;
    }
    public SchedulerConfig scheduler = new SchedulerConfig();

    public static class EchoConfig {
        public int radius = 32;
        public boolean recordPlayerChat = false;
    }
    public EchoConfig echo = new EchoConfig();

    public static class OverwriteConfig {
        public int blocksPerTick = 256;
    }
    public OverwriteConfig overwrite = new OverwriteConfig();

    private static PalimpsestConfig instance = new PalimpsestConfig();

    public static PalimpsestConfig get() {
        return instance;
    }

    public static PalimpsestConfig load(Path configDir) {
        Path configFile = configDir.resolve("palimpsest.json");
        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                PalimpsestConfig loaded = GSON.fromJson(reader, PalimpsestConfig.class);
                if (loaded != null) {
                    instance = loaded;
                    LOGGER.info("Loaded configuration from {}", configFile);
                    return instance;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to load palimpsest.json, falling back to defaults", e);
            }
        } else {
            try {
                Files.createDirectories(configDir);
                try (Writer writer = Files.newBufferedWriter(configFile)) {
                    GSON.toJson(instance, writer);
                    LOGGER.info("Created default configuration at {}", configFile);
                }
            } catch (IOException e) {
                LOGGER.error("Failed to create default palimpsest.json", e);
            }
        }
        return instance;
    }
}
