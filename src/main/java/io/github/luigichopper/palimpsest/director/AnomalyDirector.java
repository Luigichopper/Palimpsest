package io.github.luigichopper.palimpsest.director;

import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import io.github.luigichopper.palimpsest.core.StoryState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AnomalyDirector {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/Director");
    private static final AnomalyDirector INSTANCE = new AnomalyDirector();

    private final Map<UUID, TensionTracker> trackers = new ConcurrentHashMap<>();
    private int tickCounter = 0;

    public static AnomalyDirector get() {
        return INSTANCE;
    }

    public TensionTracker getTracker(UUID playerId) {
        return trackers.computeIfAbsent(playerId, TensionTracker::new);
    }

    public static RandomSource createDeterministicRng(long worldSeed, UUID playerUuid, int eventIndex) {
        long seed = worldSeed ^ playerUuid.getMostSignificantBits() ^ (playerUuid.getLeastSignificantBits() * 31L) ^ ((long) eventIndex * 1000003L);
        return RandomSource.create(seed);
    }

    public void tick(MinecraftServer server) {
        tickCounter++;
        // Run director evaluation once every second (20 ticks)
        if (tickCounter % 20 != 0) {
            return;
        }

        int maxTier = PalimpsestConfig.get().director.maxTier;
        if (maxTier <= 0) {
            return; // Director disabled
        }

        long now = System.currentTimeMillis();
        long quietWindowMs = 120_000L; // 120s quiet window

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.level();
            StoryState storyState = StoryState.get(level);
            TensionTracker tracker = getTracker(player.getUUID());

            tracker.tick(level, player, storyState);

            if (tracker.canFireAnomaly(now, quietWindowMs) && tracker.getTension() >= 30.0f) {
                long worldSeed = level.getSeed();
                int eventIndex = tracker.getEventIndex();
                RandomSource random = createDeterministicRng(worldSeed, player.getUUID(), eventIndex);

                AnomalyContext ctx = new AnomalyContext(
                    level,
                    player,
                    tracker,
                    worldSeed,
                    eventIndex,
                    random,
                    storyState
                );

                List<Anomaly> candidates = AnomalyRegistry.get().getCandidates(maxTier, tracker.getTension(), ctx);
                if (!candidates.isEmpty()) {
                    Anomaly chosen = candidates.get(random.nextInt(candidates.size()));
                    try {
                        boolean fired = chosen.fire(ctx);
                        if (fired) {
                            tracker.recordAnomalyFired(chosen.cost(), now);
                            LOGGER.info("Director triggered anomaly '{}' (tier {}) for player {} [tension now: {}]",
                                chosen.id(), chosen.tier(), player.getGameProfile().name(), tracker.getTension());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error firing anomaly '{}' for player {}: {}", chosen.id(), player.getGameProfile().name(), e.getMessage());
                    }
                }
            }
        }
    }

    public boolean fireAnomalyDirectly(ServerPlayer player, String anomalyId) {
        Optional<Anomaly> anomalyOpt = AnomalyRegistry.get().get(anomalyId);
        if (anomalyOpt.isEmpty()) {
            return false;
        }

        ServerLevel level = player.level();
        StoryState storyState = StoryState.get(level);
        TensionTracker tracker = getTracker(player.getUUID());
        long now = System.currentTimeMillis();

        long worldSeed = level.getSeed();
        int eventIndex = tracker.getEventIndex();
        RandomSource random = createDeterministicRng(worldSeed, player.getUUID(), eventIndex);

        AnomalyContext ctx = new AnomalyContext(
            level,
            player,
            tracker,
            worldSeed,
            eventIndex,
            random,
            storyState
        );

        Anomaly anomaly = anomalyOpt.get();
        boolean fired = anomaly.fire(ctx);
        if (fired) {
            tracker.recordAnomalyFired(anomaly.cost(), now);
        }
        return fired;
    }
}
