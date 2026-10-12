package io.github.luigichopper.palimpsest.resonance;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.luigichopper.palimpsest.Palimpsest;
import io.github.luigichopper.palimpsest.core.StoryState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ResonanceBus {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/ResonanceBus");
    private static final ResonanceBus INSTANCE = new ResonanceBus();

    public static final int MAX_QUEUE_SIZE = 500;
    public static final int MAX_APPLY_PER_TICK = 32;

    public static final Identifier ANCHOR_DIM = Identifier.fromNamespaceAndPath("minecraft", "overworld");
    public static final Identifier MIRROR_DIM = Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "mirror");

    private final ThreadLocal<Boolean> suppressResonance = ThreadLocal.withInitial(() -> false);
    private final Map<Block, Block> invertMap = new HashMap<>();
    private int minDelay = 20;
    private int maxDelay = 120;

    private ResonanceBus() {
        loadRules();
    }

    public static ResonanceBus get() {
        return INSTANCE;
    }

    public void loadRules() {
        invertMap.clear();
        try (InputStream in = getClass().getResourceAsStream("/data/palimpsest/resonance/rules.json")) {
            if (in != null) {
                JsonObject json = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
                if (json.has("rules")) {
                    JsonArray rules = json.getAsJsonArray("rules");
                    for (JsonElement el : rules) {
                        JsonObject obj = el.getAsJsonObject();
                        String type = obj.get("type").getAsString();
                        if ("INVERT".equalsIgnoreCase(type)) {
                            Identifier fromId = Identifier.tryParse(obj.get("from").getAsString());
                            Identifier toId = Identifier.tryParse(obj.get("to").getAsString());
                            if (fromId != null && toId != null) {
                                Block fromBlock = BuiltInRegistries.BLOCK.getValue(fromId);
                                Block toBlock = BuiltInRegistries.BLOCK.getValue(toId);
                                if (fromBlock != null && toBlock != null) {
                                    invertMap.put(fromBlock, toBlock);
                                }
                            }
                        } else if ("DELAY".equalsIgnoreCase(type)) {
                            if (obj.has("minTicks")) minDelay = obj.get("minTicks").getAsInt();
                            if (obj.has("maxTicks")) maxDelay = obj.get("maxTicks").getAsInt();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load resonance rules, falling back to defaults: {}", e.getMessage());
        }

        // Always ensure fallback inverts
        if (invertMap.isEmpty()) {
            invertMap.put(Blocks.TORCH, Blocks.SOUL_TORCH);
            invertMap.put(Blocks.WALL_TORCH, Blocks.SOUL_WALL_TORCH);
            invertMap.put(Blocks.LANTERN, Blocks.SOUL_LANTERN);
            invertMap.put(Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE);
            invertMap.put(Blocks.OAK_PLANKS, Blocks.DARK_OAK_PLANKS);
            invertMap.put(Blocks.GLASS, Blocks.TINTED_GLASS);
        }
    }

    public boolean isSuppressed() {
        return suppressResonance.get();
    }

    public void setSuppressed(boolean suppressed) {
        suppressResonance.set(suppressed);
    }

    public static BlockPos reflectCoordinate(BlockPos pos) {
        return new BlockPos(-pos.getX() - 1, pos.getY(), pos.getZ());
    }

    public void onBlockChanged(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState, StoryState storyState) {
        if (level == null) return;
        onBlockChanged(level.dimension().identifier(), level.getGameTime(), pos, oldState, newState, storyState);
    }

    public void onBlockChanged(Identifier dim, long gameTime, BlockPos pos, BlockState oldState, BlockState newState, StoryState storyState) {
        if (suppressResonance.get()) {
            return;
        }

        Identifier targetDim;
        if (dim.equals(ANCHOR_DIM)) {
            targetDim = MIRROR_DIM;
        } else if (dim.equals(MIRROR_DIM)) {
            targetDim = ANCHOR_DIM;
        } else {
            return; // Not an Anchor <-> Mirror event
        }

        BlockPos targetPos = reflectCoordinate(pos);
        BlockState targetState;

        if (newState.isAir()) {
            targetState = Blocks.AIR.defaultBlockState();
        } else {
            Block block = newState.getBlock();
            if (invertMap.containsKey(block)) {
                targetState = invertMap.get(block).defaultBlockState().mirror(Mirror.FRONT_BACK);
            } else {
                targetState = newState.mirror(Mirror.FRONT_BACK);
            }
        }

        // Bounded queue check: discard if exceeding MAX_QUEUE_SIZE
        if (storyState.getPendingResonanceCount() >= MAX_QUEUE_SIZE) {
            LOGGER.warn("ResonanceBus queue capacity reached ({}), dropping resonance at {}", MAX_QUEUE_SIZE, targetPos);
            return;
        }

        int delay = minDelay;
        if (maxDelay > minDelay) {
            delay = minDelay + Math.abs((pos.hashCode() + (int) gameTime) % (maxDelay - minDelay + 1));
        }

        long executeTick = gameTime + delay;
        storyState.addPendingResonance(new PendingResonance(targetDim, targetPos, targetState, executeTick));
    }

    public void tick(MinecraftServer server) {
        if (server == null) return;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        StoryState storyState = StoryState.get(server);
        long currentTick = overworld.getGameTime();

        List<PendingResonance> due = storyState.pollDueResonances(currentTick, MAX_APPLY_PER_TICK);
        if (due.isEmpty()) return;

        for (PendingResonance res : due) {
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, res.dimensionId());
            ServerLevel targetLevel = server.getLevel(dimKey);
            if (targetLevel != null && targetLevel.isLoaded(res.pos())) {
                suppressResonance.set(true);
                try {
                    targetLevel.setBlock(res.pos(), res.state(), 3);
                } catch (Exception e) {
                    LOGGER.warn("Failed to apply resonance block at {}: {}", res.pos(), e.getMessage());
                } finally {
                    suppressResonance.set(false);
                }
            }
        }
    }
}
