package io.github.luigichopper.palimpsest.landmark;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.luigichopper.palimpsest.core.StoryState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LandmarkPlacer {
    private static final Logger LOGGER = LoggerFactory.getLogger(LandmarkPlacer.class);
    private static final Gson GSON = new Gson();

    private final List<LandmarkDef> definitions = new ArrayList<>();

    public LandmarkPlacer() {
        loadDefinitions();
    }

    public List<LandmarkDef> getDefinitions() {
        return Collections.unmodifiableList(definitions);
    }

    public void loadDefinitions() {
        definitions.clear();
        try (InputStream stream = getClass().getResourceAsStream("/data/palimpsest/landmarks.json")) {
            if (stream == null) {
                LOGGER.warn("landmarks.json not found in resources");
                return;
            }
            JsonObject root = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
            if (root.has("landmarks")) {
                JsonArray array = root.getAsJsonArray("landmarks");
                for (JsonElement elem : array) {
                    JsonObject obj = elem.getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    int x = obj.get("anchorX").getAsInt();
                    int z = obj.get("anchorZ").getAsInt();
                    String builder = obj.get("builder").getAsString();
                    Optional<String> reqFlag = obj.has("requiresFlag") ? Optional.of(obj.get("requiresFlag").getAsString()) : Optional.empty();
                    String note = obj.has("note") ? obj.get("note").getAsString() : "";
                    definitions.add(new LandmarkDef(id, x, z, builder, reqFlag, note));
                }
            }
            LOGGER.info("Loaded {} landmark definitions", definitions.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load landmarks.json", e);
        }
    }

    public void placeAllIfEligible(MinecraftServer server, StoryState storyState) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }

        RandomSource random = RandomSource.create(overworld.getSeed());

        for (LandmarkDef def : definitions) {
            if (storyState.isLandmarkPlaced(def.id()) || storyState.hasWorldFlag("landmark.placed:" + def.id())) {
                continue;
            }

            if (def.requiresFlag().isPresent() && !storyState.hasWorldFlag(def.requiresFlag().get())) {
                continue;
            }

            placeLandmark(overworld, def, random, storyState);
        }
    }

    public void placeLandmark(ServerLevel level, LandmarkDef def, RandomSource random, StoryState storyState) {
        int chunkX = def.anchorX() >> 4;
        int chunkZ = def.anchorZ() >> 4;

        try {
            // Force load the chunk temporarily
            level.setChunkForced(chunkX, chunkZ, true);

            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, def.anchorX(), def.anchorZ());
            if (surfaceY <= level.getMinY()) {
                surfaceY = 64; // fallback
            }

            BlockPos anchorPos = new BlockPos(def.anchorX(), surfaceY, def.anchorZ());
            LOGGER.info("Placing landmark '{}' with builder '{}' at {}", def.id(), def.builder(), anchorPos);

            switch (def.builder().toLowerCase(Locale.ROOT)) {
                case "cabin" -> CabinBuilder.build(level, anchorPos, random, storyState);
                case "bunker" -> BunkerBuilder.build(level, anchorPos, random, storyState);
                default -> LOGGER.warn("Unknown landmark builder: {}", def.builder());
            }

            storyState.recordLandmarkPlaced(def.id(), anchorPos.getX(), anchorPos.getY(), anchorPos.getZ());
            storyState.setWorldFlag("landmark.placed:" + def.id());
        } catch (Exception e) {
            LOGGER.error("Failed to place landmark: " + def.id(), e);
        } finally {
            level.setChunkForced(chunkX, chunkZ, false);
        }
    }
}
