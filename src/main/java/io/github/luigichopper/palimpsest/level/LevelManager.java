package io.github.luigichopper.palimpsest.level;

import io.github.luigichopper.palimpsest.Palimpsest;
import io.github.luigichopper.palimpsest.core.StoryState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LevelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/LevelManager");
    private static LevelManager instance;

    private final MinecraftServer server;
    private final Fantasy fantasy;
    private final Map<Identifier, RuntimeLevelHandle> activeHandles = new ConcurrentHashMap<>();

    private LevelManager(MinecraftServer server) {
        this.server = server;
        this.fantasy = Fantasy.get(server);
    }

    public static LevelManager get() {
        return instance;
    }

    public static void initialize(MinecraftServer server) {
        instance = new LevelManager(server);
        instance.reopenPersistentLevels();
    }

    public MinecraftServer getServer() {
        return server;
    }

    public Fantasy getFantasy() {
        return fantasy;
    }

    public Map<Identifier, RuntimeLevelHandle> getActiveHandles() {
        return activeHandles;
    }

    public RuntimeLevelHandle getHandle(Identifier id) {
        return activeHandles.get(id);
    }

    public void reopenPersistentLevels() {
        LOGGER.info("Checking registered persistent levels for restoration...");
        StoryState state = StoryState.get(server);
        Map<Identifier, LevelRegistryEntry> registered = state.getRegisteredLevels();

        for (Map.Entry<Identifier, LevelRegistryEntry> entry : registered.entrySet()) {
            Identifier id = entry.getKey();
            LevelRegistryEntry reg = entry.getValue();
            try {
                LOGGER.info("Restoring persistent level '{}' (kind: {})...", id, reg.kind());
                RuntimeLevelConfig config = buildConfig(reg);
                RuntimeLevelHandle handle = fantasy.getOrOpenPersistentLevel(id, config);
                activeHandles.put(id, handle);
                LOGGER.info("Successfully restored persistent level '{}'.", id);
            } catch (Exception e) {
                LOGGER.error("Failed to restore registered persistent level '{}', skipping to prevent crash", id, e);
            }
        }
    }

    public RuntimeLevelConfig buildConfig(LevelRegistryEntry reg) {
        RuntimeLevelConfig config = new RuntimeLevelConfig();
        ResourceKey<DimensionType> dimTypeKey = ResourceKey.create(Registries.DIMENSION_TYPE, reg.dimensionTypeId());
        config.setDimensionType(dimTypeKey);
        config.setSeed(reg.seed());

        Difficulty diff = Difficulty.byName(reg.difficulty());
        if (diff != null) {
            config.setDifficulty(diff);
        }

        config.setMirrorOverworldClocks(reg.mirrorClocks());

        // Select generator
        if ("vanilla".equalsIgnoreCase(reg.generatorKind())) {
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld != null) {
                config.setGenerator(overworld.getChunkSource().getGenerator());
            } else {
                config.setGenerator(new VoidChunkGenerator());
            }
        } else {
            config.setGenerator(new VoidChunkGenerator());
        }

        return config;
    }

    public RuntimeLevelHandle openAnnex(ServerPlayer player) {
        return openAnnex(player, io.github.luigichopper.palimpsest.annex.AnnexTemplate.CH0_FRAME_ROOM);
    }

    public RuntimeLevelHandle openAnnex(ServerPlayer player, io.github.luigichopper.palimpsest.annex.AnnexTemplate template) {
        Evacuator.recordAnchorPos(player);
        Identifier annexId = Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "annex_" + player.getUUID().toString().replace("-", ""));

        RuntimeLevelConfig config = new RuntimeLevelConfig();
        config.setDimensionType(ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "annex")));
        config.setGenerator(new VoidChunkGenerator());
        config.setSeed(server.overworld().getSeed());
        config.setDifficulty(Difficulty.NORMAL);
        config.setMirrorOverworldClocks(false);

        RuntimeLevelHandle handle = fantasy.openTemporaryLevel(annexId, config);
        activeHandles.put(annexId, handle);

        ServerLevel annexWorld = server.getLevel(handle.getRegistryKey());
        if (annexWorld != null) {
            StoryState storyState = StoryState.get(server);
            io.github.luigichopper.palimpsest.annex.AnnexBuilder.build(annexWorld, template, storyState);
            player.teleportTo(annexWorld, 0.5, 65.0, 2.5, java.util.Collections.emptySet(), 0.0f, 0.0f, true);
        }

        return handle;
    }

    public RuntimeLevelHandle openEcho(ServerPlayer player) {
        Evacuator.recordAnchorPos(player);
        Identifier echoId = Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "echo_" + player.getUUID().toString().replace("-", ""));

        RuntimeLevelConfig config = new RuntimeLevelConfig();
        config.setDimensionType(ResourceKey.create(Registries.DIMENSION_TYPE, Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "echo")));
        config.setGenerator(new VoidChunkGenerator());
        config.setSeed(server.overworld().getSeed());
        config.setDifficulty(Difficulty.NORMAL);
        config.setMirrorOverworldClocks(false);

        RuntimeLevelHandle handle = fantasy.openTemporaryLevel(echoId, config);
        activeHandles.put(echoId, handle);
        return handle;
    }

    public RuntimeLevelHandle ensureMirror() {
        Identifier mirrorId = Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "mirror");
        RuntimeLevelHandle existing = activeHandles.get(mirrorId);
        if (existing != null) {
            return existing;
        }

        LevelRegistryEntry reg = new LevelRegistryEntry(
            mirrorId,
            LevelKind.MIRROR,
            Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "mirror"),
            server.overworld().getSeed(),
            "NORMAL",
            false,
            "mirror"
        );

        StoryState state = StoryState.get(server);
        state.registerLevel(reg);

        RuntimeLevelConfig config = buildConfig(reg);
        RuntimeLevelHandle handle = fantasy.getOrOpenPersistentLevel(mirrorId, config);
        activeHandles.put(mirrorId, handle);
        return handle;
    }

    public RuntimeLevelHandle openBackup(String backupId) {
        Identifier id = Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "backup_" + backupId.toLowerCase());
        RuntimeLevelHandle existing = activeHandles.get(id);
        if (existing != null) {
            return existing;
        }

        LevelRegistryEntry reg = new LevelRegistryEntry(
            id,
            LevelKind.BACKUP,
            Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "backup"),
            server.overworld().getSeed(),
            "NORMAL",
            true,
            "vanilla"
        );

        StoryState state = StoryState.get(server);
        state.registerLevel(reg);

        RuntimeLevelConfig config = buildConfig(reg);
        RuntimeLevelHandle handle = fantasy.getOrOpenPersistentLevel(id, config);
        activeHandles.put(id, handle);
        return handle;
    }

    public void closeBackup(String backupId) {
        Identifier id = Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "backup_" + backupId.toLowerCase());
        RuntimeLevelHandle handle = activeHandles.remove(id);
        StoryState state = StoryState.get(server);
        state.unregisterLevel(id);

        if (handle != null) {
            closeAndDelete(handle);
        }
    }

    public void closeAndDelete(RuntimeLevelHandle handle) {
        if (handle == null) return;
        ServerLevel level = handle.asLevel();
        Identifier id = handle.getRegistryKey().identifier();

        LOGGER.info("Evacuating players before deleting level '{}'...", id);
        Evacuator.evacuateLevel(level);

        activeHandles.remove(id);
        StoryState state = StoryState.get(server);
        state.unregisterLevel(id);

        handle.delete();
        LOGGER.info("Enqueued deletion for level '{}'.", id);
    }
}
