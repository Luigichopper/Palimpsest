package io.github.luigichopper.palimpsest.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.luigichopper.palimpsest.level.LevelRegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StoryState extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/StoryState");

    private final Set<String> worldFlags = new HashSet<>();
    private final Map<UUID, Set<String>> playerFlags = new HashMap<>();
    private final Map<UUID, AnchorPos> lastAnchorPositions = new HashMap<>();
    private final Set<String> placedLandmarks = new HashSet<>();
    private final Map<String, Long> puzzleSolves = new HashMap<>();
    private final Map<Identifier, LevelRegistryEntry> levelRegistry = new HashMap<>();
    private final Map<UUID, AnchorPos> mostUsedSpots = new HashMap<>();

    public static final Codec<Set<String>> SET_CODEC = Codec.STRING.listOf().<Set<String>>xmap(HashSet::new, ArrayList::new);

    public static final Codec<StoryState> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            SET_CODEC.optionalFieldOf("world_flags", Collections.emptySet())
                .forGetter(s -> s.worldFlags),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, SET_CODEC)
                .optionalFieldOf("player_flags", Collections.emptyMap())
                .forGetter(s -> s.playerFlags),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, AnchorPos.CODEC)
                .optionalFieldOf("last_anchors", Collections.emptyMap())
                .forGetter(s -> s.lastAnchorPositions),
            SET_CODEC.optionalFieldOf("landmarks", Collections.emptySet())
                .forGetter(s -> s.placedLandmarks),
            Codec.unboundedMap(Codec.STRING, Codec.LONG)
                .optionalFieldOf("puzzle_solves", Collections.emptyMap())
                .forGetter(s -> s.puzzleSolves),
            Codec.unboundedMap(Identifier.CODEC, LevelRegistryEntry.CODEC)
                .optionalFieldOf("levels", Collections.emptyMap())
                .forGetter(s -> s.levelRegistry),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, AnchorPos.CODEC)
                .optionalFieldOf("most_used_spots", Collections.emptyMap())
                .forGetter(s -> s.mostUsedSpots)
        ).apply(instance, StoryState::new)
    );

    public static final SavedDataType<StoryState> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("palimpsest", "story_state"),
        StoryState::new,
        CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    public StoryState() {
    }

    public StoryState(
        Set<String> worldFlags,
        Map<UUID, Set<String>> playerFlags,
        Map<UUID, AnchorPos> lastAnchorPositions,
        Set<String> placedLandmarks,
        Map<String, Long> puzzleSolves,
        Map<Identifier, LevelRegistryEntry> levelRegistry,
        Map<UUID, AnchorPos> mostUsedSpots
    ) {
        this.worldFlags.addAll(worldFlags);
        this.playerFlags.putAll(playerFlags);
        this.lastAnchorPositions.putAll(lastAnchorPositions);
        this.placedLandmarks.addAll(placedLandmarks);
        this.puzzleSolves.putAll(puzzleSolves);
        this.levelRegistry.putAll(levelRegistry);
        this.mostUsedSpots.putAll(mostUsedSpots);
    }

    public void recordStationInteraction(UUID playerId, BlockPos pos, Identifier dim) {
        mostUsedSpots.put(playerId, new AnchorPos(dim, pos.getX(), pos.getY(), pos.getZ(), 0, 0));
        setDirty();
    }

    public AnchorPos getMostUsedSpot(UUID playerId) {
        return mostUsedSpots.get(playerId);
    }

    public static StoryState get(ServerLevel level) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        ServerLevel target = overworld != null ? overworld : level;
        return target.getDataStorage().computeIfAbsent(TYPE);
    }

    public static StoryState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null) {
            return overworld.getDataStorage().computeIfAbsent(TYPE);
        }
        ServerLevel first = server.getAllLevels().iterator().next();
        return first.getDataStorage().computeIfAbsent(TYPE);
    }

    // World flags
    public boolean hasWorldFlag(String flag) {
        return worldFlags.contains(flag);
    }

    public void setWorldFlag(String flag) {
        if (worldFlags.add(flag)) {
            setDirty();
        }
    }

    public void clearWorldFlag(String flag) {
        if (worldFlags.remove(flag)) {
            setDirty();
        }
    }

    public Set<String> getWorldFlags() {
        return Collections.unmodifiableSet(worldFlags);
    }

    // Player flags
    public boolean hasPlayerFlag(UUID playerId, String flag) {
        Set<String> flags = playerFlags.get(playerId);
        return flags != null && flags.contains(flag);
    }

    public void setPlayerFlag(UUID playerId, String flag) {
        playerFlags.computeIfAbsent(playerId, k -> new HashSet<>()).add(flag);
        setDirty();
    }

    public void clearPlayerFlag(UUID playerId, String flag) {
        Set<String> flags = playerFlags.get(playerId);
        if (flags != null && flags.remove(flag)) {
            setDirty();
        }
    }

    public Set<String> getPlayerFlags(UUID playerId) {
        Set<String> flags = playerFlags.get(playerId);
        return flags != null ? Collections.unmodifiableSet(flags) : Collections.emptySet();
    }

    // Last Anchor Position
    public AnchorPos getLastAnchorPos(UUID playerId) {
        return lastAnchorPositions.get(playerId);
    }

    public void setLastAnchorPos(UUID playerId, AnchorPos pos) {
        lastAnchorPositions.put(playerId, pos);
        setDirty();
    }

    // Landmarks
    public boolean isLandmarkPlaced(String landmarkId) {
        return placedLandmarks.contains(landmarkId);
    }

    public void markLandmarkPlaced(String landmarkId) {
        if (placedLandmarks.add(landmarkId)) {
            setDirty();
        }
    }

    public void recordLandmarkPlaced(String landmarkId, int x, int y, int z) {
        markLandmarkPlaced(landmarkId);
    }

    // Puzzles
    public boolean isPuzzleSolved(String puzzleId) {
        return puzzleSolves.containsKey(puzzleId);
    }

    public void markPuzzleSolved(String puzzleId) {
        puzzleSolves.put(puzzleId, System.currentTimeMillis());
        setDirty();
    }

    // Level Registry
    public Map<Identifier, LevelRegistryEntry> getRegisteredLevels() {
        return Collections.unmodifiableMap(levelRegistry);
    }

    public void registerLevel(LevelRegistryEntry entry) {
        levelRegistry.put(entry.id(), entry);
        setDirty();
        LOGGER.info("Registered persistent runtime level '{}' (kind: {})", entry.id(), entry.kind());
    }

    public void unregisterLevel(Identifier id) {
        if (levelRegistry.remove(id) != null) {
            setDirty();
            LOGGER.info("Unregistered persistent runtime level '{}'", id);
        }
    }

    public boolean isLevelRegistered(Identifier id) {
        return levelRegistry.containsKey(id);
    }
}
