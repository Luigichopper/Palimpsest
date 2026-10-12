package io.github.luigichopper.palimpsest.reflection;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MovementRecorder {
    private static final MovementRecorder INSTANCE = new MovementRecorder();

    public static final int MAX_FRAMES = 18000; // ~30 minutes at 10 samples/sec (every 2 ticks)

    private final Map<UUID, List<RecordedFrame>> playerBuffers = new ConcurrentHashMap<>();

    private MovementRecorder() {
    }

    public static MovementRecorder get() {
        return INSTANCE;
    }

    public void record(ServerPlayer player, long tick) {
        if (tick % 2 != 0) {
            return;
        }

        UUID uuid = player.getUUID();
        List<RecordedFrame> buffer = playerBuffers.computeIfAbsent(uuid, id -> new ArrayList<>());

        ItemStack mainHand = player.getMainHandItem();
        Identifier itemId = mainHand.isEmpty() ? Identifier.fromNamespaceAndPath("minecraft", "air") : BuiltInRegistries.ITEM.getKey(mainHand.getItem());

        RecordedFrame frame = new RecordedFrame(
            player.getX(),
            player.getY(),
            player.getZ(),
            player.getYRot(),
            player.getXRot(),
            player.getPose().name(),
            itemId,
            tick
        );

        synchronized (buffer) {
            if (buffer.size() >= MAX_FRAMES) {
                downsample(buffer);
            }
            buffer.add(frame);
        }
    }

    public void recordFrameDirect(UUID playerId, RecordedFrame frame) {
        List<RecordedFrame> buffer = playerBuffers.computeIfAbsent(playerId, id -> new ArrayList<>());
        synchronized (buffer) {
            if (buffer.size() >= MAX_FRAMES) {
                downsample(buffer);
            }
            buffer.add(frame);
        }
    }

    private void downsample(List<RecordedFrame> buffer) {
        // Drop every second frame in the first half of history to compress older movement
        int half = buffer.size() / 2;
        List<RecordedFrame> compressed = new ArrayList<>(buffer.size() - (half / 2));
        for (int i = 0; i < half; i++) {
            if (i % 2 == 0) {
                compressed.add(buffer.get(i));
            }
        }
        for (int i = half; i < buffer.size(); i++) {
            compressed.add(buffer.get(i));
        }
        buffer.clear();
        buffer.addAll(compressed);
    }

    public List<RecordedFrame> getFrames(UUID playerId) {
        List<RecordedFrame> buffer = playerBuffers.get(playerId);
        if (buffer == null) {
            return Collections.emptyList();
        }
        synchronized (buffer) {
            return new ArrayList<>(buffer);
        }
    }

    public int getFrameCount(UUID playerId) {
        List<RecordedFrame> buffer = playerBuffers.get(playerId);
        if (buffer == null) return 0;
        synchronized (buffer) {
            return buffer.size();
        }
    }

    public void clear(UUID playerId) {
        playerBuffers.remove(playerId);
    }

    public void clearAll() {
        playerBuffers.clear();
    }
}
