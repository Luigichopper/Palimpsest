package io.github.luigichopper.palimpsest.director;

import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import io.github.luigichopper.palimpsest.core.StoryState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.List;
import java.util.UUID;

public class TensionTracker {
    private final UUID playerId;
    private float tension = 0.0f;

    private long lastAnomalyTimestamp = 0;
    private long lastDamageTimestamp = 0;
    private long lastCombatTimestamp = 0;
    private long lastSleepAttemptTimestamp = 0;
    private int eventIndex = 0;

    private SoundEvent lastPlayerSound = null;

    public TensionTracker(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public float getTension() {
        return tension;
    }

    public void setTension(float tension) {
        this.tension = Math.clamp(tension, 0.0f, 100.0f);
    }

    public int getEventIndex() {
        return eventIndex;
    }

    public void setEventIndex(int eventIndex) {
        this.eventIndex = eventIndex;
    }

    public SoundEvent getLastPlayerSound() {
        return lastPlayerSound;
    }

    public void setLastPlayerSound(SoundEvent lastPlayerSound) {
        this.lastPlayerSound = lastPlayerSound;
    }

    public void recordDamage(long timestamp) {
        this.lastDamageTimestamp = timestamp;
    }

    public void recordCombat(long timestamp) {
        this.lastCombatTimestamp = timestamp;
    }

    public void recordSleepAttempt(long timestamp) {
        this.lastSleepAttemptTimestamp = timestamp;
        this.tension = Math.min(100.0f, this.tension + 6.0f);
    }

    public boolean canFireAnomaly(long currentTimeMs, long quietWindowMs) {
        // Enforce quiet window (default 120s)
        if (currentTimeMs - lastAnomalyTimestamp < quietWindowMs) {
            return false;
        }

        // No events during combat or within 10s of damage/combat
        if (currentTimeMs - lastDamageTimestamp < 10_000L) {
            return false;
        }
        if (currentTimeMs - lastCombatTimestamp < 10_000L) {
            return false;
        }

        return true;
    }

    public void recordAnomalyFired(float cost, long timestamp) {
        this.tension = Math.max(0.0f, this.tension - cost);
        this.lastAnomalyTimestamp = timestamp;
        this.eventIndex++;
    }

    public void tick(ServerLevel level, ServerPlayer player, StoryState storyState) {
        long now = System.currentTimeMillis();

        // If in combat or recently damaged, tension decays fast and anomalies are suppressed
        if (now - lastCombatTimestamp < 10_000L || now - lastDamageTimestamp < 10_000L) {
            this.tension = Math.max(0.0f, this.tension - 0.2f);
            return;
        }

        BlockPos pos = player.blockPosition();

        // 1. Light Level input: <= 4 increases tension, > 8 decays
        int light = level.getMaxLocalRawBrightness(pos);
        if (light <= 4) {
            this.tension += 0.05f;
        } else if (light >= 8) {
            this.tension -= 0.04f;
        }

        // 2. Underground input (Y < 55 or no sky view)
        if (pos.getY() < 55 || !level.canSeeSky(pos)) {
            this.tension += 0.03f;
        }

        // 3. Time alone input (no other players within 48 blocks)
        boolean alone = true;
        for (ServerPlayer other : level.players()) {
            if (other != player && other.distanceToSqr(player) < 48 * 48) {
                alone = false;
                break;
            }
        }
        if (alone) {
            this.tension += 0.02f;
        }

        // 4. Safe signal: villagers nearby (within 16 blocks)
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(16.0));
        if (!villagers.isEmpty()) {
            this.tension -= 0.08f;
        }

        // 5. Landmark proximity input (within 48 blocks of placed landmarks)
        if (storyState != null && storyState.isLandmarkPlaced("L0") || storyState != null && storyState.isLandmarkPlaced("L1")) {
            this.tension += 0.03f;
        }

        // Clamp
        this.tension = Math.clamp(this.tension, 0.0f, 100.0f);
    }
}
