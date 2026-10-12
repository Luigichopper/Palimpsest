package io.github.luigichopper.palimpsest.reflection;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class MovementReplayer {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/MovementReplayer");

    private final UUID targetPlayerId;
    private final List<RecordedFrame> frames;
    private final ReplayMode mode;

    private Mannequin entity;
    private int currentPlaybackIndex = 0;
    private int sustainedEyeContactTicks = 0;
    private boolean active = false;

    public MovementReplayer(UUID targetPlayerId, List<RecordedFrame> frames, ReplayMode mode) {
        this.targetPlayerId = targetPlayerId;
        this.frames = frames;
        this.mode = mode;
    }

    public boolean isActive() {
        return active && entity != null && entity.isAlive();
    }

    public Mannequin getEntity() {
        return entity;
    }

    public int getCurrentPlaybackIndex() {
        return currentPlaybackIndex;
    }

    public RecordedFrame advance() {
        if (currentPlaybackIndex < frames.size()) {
            RecordedFrame frame = frames.get(currentPlaybackIndex);
            currentPlaybackIndex++;
            return frame;
        }
        return null;
    }

    public void spawn(ServerLevel level, ServerPlayer player) {
        if (frames.isEmpty()) {
            return;
        }

        RecordedFrame first = frames.get(0);
        entity = EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.TRIGGERED);
        if (entity == null) {
            return;
        }

        entity.setPos(first.x(), first.y(), first.z());
        entity.setYRot(first.yaw());
        entity.setXRot(first.pitch());
        entity.setCustomName(Component.literal("Reflection"));
        entity.setCustomNameVisible(false);
        entity.setPermanentlyInvulnerable(true);
        entity.setSilent(true);

        level.addFreshEntity(entity);
        active = true;
        currentPlaybackIndex = 0;
    }

    public void tick(ServerLevel level, ServerPlayer targetPlayer) {
        if (!isActive() || frames.isEmpty()) {
            return;
        }

        // 1. Eye contact detection: despawn on sustained eye contact (> 0.95 view dot product for 20 ticks)
        if (targetPlayer != null) {
            Vec3 playerEye = targetPlayer.getEyePosition();
            Vec3 entityEye = entity.position().add(0, 1.6, 0);
            Vec3 toEntity = entityEye.subtract(playerEye).normalize();
            Vec3 view = targetPlayer.getViewVector(1.0f);

            double dot = view.dot(toEntity);
            if (dot > 0.95 && playerEye.distanceTo(entityEye) < 48.0) {
                sustainedEyeContactTicks++;
                if (sustainedEyeContactTicks >= 20) {
                    cleanup();
                    return;
                }
            } else {
                sustainedEyeContactTicks = 0;
            }
        }

        // 2. Playback based on mode
        switch (mode.type()) {
            case FAITHFUL -> {
                if (currentPlaybackIndex < frames.size()) {
                    applyFrame(frames.get(currentPlaybackIndex));
                    currentPlaybackIndex++;
                }
            }
            case DELAYED -> {
                int frameIndex = currentPlaybackIndex - mode.parameter();
                if (frameIndex >= 0 && frameIndex < frames.size()) {
                    applyFrame(frames.get(frameIndex));
                }
                currentPlaybackIndex++;
            }
            case DIVERGE -> {
                int divergeTick = mode.parameter();
                if (currentPlaybackIndex < divergeTick && currentPlaybackIndex < frames.size()) {
                    applyFrame(frames.get(currentPlaybackIndex));
                    currentPlaybackIndex++;
                } else {
                    // Diverged: stop moving along path, stare directly at player
                    if (targetPlayer != null) {
                        lookAt(targetPlayer.getEyePosition());
                    }
                    currentPlaybackIndex++;
                }
            }
        }
    }

    public void applyFrame(RecordedFrame frame) {
        if (entity == null) return;
        entity.setPos(frame.x(), frame.y(), frame.z());
        entity.setYRot(frame.yaw());
        entity.setXRot(frame.pitch());
        entity.setPose(frame.getPose());
        entity.setItemSlot(EquipmentSlot.MAINHAND, frame.getHeldItemStack());
    }

    public void lookAt(Vec3 target) {
        if (entity == null) return;
        double dx = target.x - entity.getX();
        double dy = target.y - (entity.getY() + 1.6);
        double dz = target.z - entity.getZ();
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = (float) (Math.toDegrees(-Math.atan2(dy, distXZ)));

        entity.setYRot(yaw);
        entity.setXRot(pitch);
    }

    public void cleanup() {
        if (entity != null) {
            entity.discard();
            entity = null;
        }
        active = false;
        sustainedEyeContactTicks = 0;
    }
}
