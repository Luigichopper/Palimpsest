package io.github.luigichopper.palimpsest.reflection;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReflectionManager {
    private static final ReflectionManager INSTANCE = new ReflectionManager();

    private final Map<UUID, MovementReplayer> activeReplayers = new ConcurrentHashMap<>();

    private ReflectionManager() {
    }

    public static ReflectionManager get() {
        return INSTANCE;
    }

    public void tick(MinecraftServer server) {
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            long tick = player.level().getGameTime();
            MovementRecorder.get().record(player, tick);

            MovementReplayer replayer = activeReplayers.get(player.getUUID());
            if (replayer != null) {
                if (replayer.isActive()) {
                    replayer.tick(player.level(), player);
                } else {
                    activeReplayers.remove(player.getUUID());
                }
            }
        }
    }

    public MovementReplayer startReplay(ServerPlayer player, ReplayMode mode) {
        stopReplay(player.getUUID());

        List<RecordedFrame> frames = MovementRecorder.get().getFrames(player.getUUID());
        if (frames.isEmpty()) {
            return null;
        }

        MovementReplayer replayer = new MovementReplayer(player.getUUID(), frames, mode);
        replayer.spawn(player.level(), player);
        activeReplayers.put(player.getUUID(), replayer);
        return replayer;
    }

    public void stopReplay(UUID playerId) {
        MovementReplayer replayer = activeReplayers.remove(playerId);
        if (replayer != null) {
            replayer.cleanup();
        }
    }

    public void cleanupLevel(ServerLevel level) {
        Iterator<Map.Entry<UUID, MovementReplayer>> it = activeReplayers.entrySet().iterator();
        while (it.hasNext()) {
            MovementReplayer replayer = it.next().getValue();
            if (replayer.getEntity() != null && replayer.getEntity().level().equals(level)) {
                replayer.cleanup();
                it.remove();
            }
        }
    }

    public void clearAll() {
        for (MovementReplayer replayer : activeReplayers.values()) {
            replayer.cleanup();
        }
        activeReplayers.clear();
    }
}
