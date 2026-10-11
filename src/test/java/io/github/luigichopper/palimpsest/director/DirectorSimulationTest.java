package io.github.luigichopper.palimpsest.director;

import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class DirectorSimulationTest {
    private AnomalyRegistry registry;

    @BeforeEach
    void setUp() {
        registry = AnomalyRegistry.get();
    }

    @Test
    void testAllEightAnomaliesRegistered() {
        Collection<Anomaly> all = registry.getAll();
        assertEquals(8, all.size(), "Should have exactly 8 initial anomalies registered");

        Set<String> expectedIds = Set.of(
            "door_ajar",
            "torch_out",
            "footsteps_behind",
            "sound_of_you",
            "item_frame_turn",
            "sign_change",
            "watcher",
            "wrong_hour"
        );

        for (String id : expectedIds) {
            assertTrue(registry.get(id).isPresent(), "Missing expected anomaly: " + id);
        }
    }

    @Test
    void testMaxTierZeroNothingFires() {
        UUID playerId = UUID.randomUUID();
        TensionTracker tracker = new TensionTracker(playerId);
        tracker.setTension(100.0f); // Maximum tension

        int maxTier = 0; // Disabled
        long now = 1_000_000L;
        long quietWindowMs = 120_000L;

        // Candidate check
        List<Anomaly> candidates = registry.getCandidates(maxTier, tracker.getTension(), null);
        assertTrue(candidates.isEmpty(), "With maxTier=0, candidate list must be empty");

        // Simulate 3600 seconds (1 hour)
        int firedCount = 0;
        for (int sec = 0; sec < 3600; sec++) {
            long currentMs = now + sec * 1000L;
            if (tracker.canFireAnomaly(currentMs, quietWindowMs)) {
                List<Anomaly> c = registry.getCandidates(maxTier, tracker.getTension(), null);
                if (!c.isEmpty()) {
                    firedCount++;
                }
            }
        }
        assertEquals(0, firedCount, "With maxTier=0, zero anomalies should fire across 1 hour");
    }

    @Test
    void testSimulatedOneHourSessionDensityAndQuietWindows() {
        UUID playerId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        long worldSeed = 9876543210L;
        TensionTracker tracker = new TensionTracker(playerId);

        long startTime = 0L;
        long quietWindowMs = 120_000L; // 120 seconds
        int maxTier = 2;

        List<Long> eventTimestamps = new ArrayList<>();
        List<String> eventIds = new ArrayList<>();

        // Simulate 3,600 seconds (1 hour)
        for (int sec = 0; sec < 3600; sec++) {
            long currentMs = startTime + sec * 1000L;

            // Simulate eerie conditions: tension rises smoothly (~0.5 per second in dark underground)
            tracker.setTension(tracker.getTension() + 0.45f);

            // Director evaluation at 1Hz
            if (tracker.canFireAnomaly(currentMs, quietWindowMs) && tracker.getTension() >= 30.0f) {
                int eventIdx = tracker.getEventIndex();
                RandomSource rng = AnomalyDirector.createDeterministicRng(worldSeed, playerId, eventIdx);

                // Filter candidates by tier and cost (mocking context check for simulation)
                List<Anomaly> candidates = new ArrayList<>();
                for (Anomaly a : registry.getAll()) {
                    if (a.tier() <= maxTier && a.cost() <= tracker.getTension()) {
                        candidates.add(a);
                    }
                }

                if (!candidates.isEmpty()) {
                    Anomaly chosen = candidates.get(rng.nextInt(candidates.size()));
                    tracker.recordAnomalyFired(chosen.cost(), currentMs);
                    eventTimestamps.add(currentMs);
                    eventIds.add(chosen.id());
                }
            }
        }

        // 1. Verify density: in 1 hour with 120s quiet window, maximum possible is 30 events.
        // Expect a realistic eerie density between 10 and 30 events.
        int count = eventTimestamps.size();
        assertTrue(count >= 10 && count <= 30, "Simulated 1-hour session should fire 10-30 events, fired: " + count);

        // 2. Verify strict quiet window enforcement between every consecutive event
        for (int i = 1; i < eventTimestamps.size(); i++) {
            long prev = eventTimestamps.get(i - 1);
            long curr = eventTimestamps.get(i);
            long gap = curr - prev;
            assertTrue(gap >= quietWindowMs, "Quiet window violated! Gap between events " + (i - 1) + " and " + i + " was only " + (gap / 1000) + "s");
        }
    }

    @Test
    void testCombatAndDamageSuppression() {
        UUID playerId = UUID.randomUUID();
        TensionTracker tracker = new TensionTracker(playerId);
        tracker.setTension(80.0f);

        long now = 300_000L;
        long quietWindowMs = 120_000L;

        // Player took damage 5 seconds ago
        tracker.recordDamage(now - 5_000L);
        assertFalse(tracker.canFireAnomaly(now, quietWindowMs), "Must suppress anomalies within 10s of damage");

        // Player took damage 11 seconds ago -> safe
        tracker.recordDamage(now - 11_000L);
        assertTrue(tracker.canFireAnomaly(now, quietWindowMs), "Allowed 11s after damage");

        // Player was in combat 4 seconds ago
        tracker.recordCombat(now - 4_000L);
        assertFalse(tracker.canFireAnomaly(now, quietWindowMs), "Must suppress anomalies during combat (within 10s)");
    }

    @Test
    void testDeterministicReplay() {
        UUID playerId = UUID.fromString("deadbeef-1234-5678-9abc-def012345678");
        long worldSeed = 4242424242L;

        List<Integer> run1Picks = new ArrayList<>();
        List<Integer> run2Picks = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            RandomSource r1 = AnomalyDirector.createDeterministicRng(worldSeed, playerId, i);
            run1Picks.add(r1.nextInt(100));
        }

        for (int i = 0; i < 50; i++) {
            RandomSource r2 = AnomalyDirector.createDeterministicRng(worldSeed, playerId, i);
            run2Picks.add(r2.nextInt(100));
        }

        assertEquals(run1Picks, run2Picks, "RNG output must be 100% deterministic across replays");
    }
}
