package io.github.luigichopper.palimpsest.director.anomalies;

import io.github.luigichopper.palimpsest.director.Anomaly;
import io.github.luigichopper.palimpsest.director.AnomalyContext;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ItemFrame;

import java.util.List;

public class ItemFrameTurnAnomaly implements Anomaly {
    @Override
    public String id() {
        return "item_frame_turn";
    }

    @Override
    public int tier() {
        return 1;
    }

    @Override
    public float cost() {
        return 15.0f;
    }

    @Override
    public long cooldownSeconds() {
        return 200;
    }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        List<ItemFrame> frames = ctx.level().getEntitiesOfClass(
            ItemFrame.class,
            ctx.player().getBoundingBox().inflate(12.0)
        );
        return !frames.isEmpty();
    }

    @Override
    public boolean fire(AnomalyContext ctx) {
        List<ItemFrame> frames = ctx.level().getEntitiesOfClass(
            ItemFrame.class,
            ctx.player().getBoundingBox().inflate(12.0)
        );
        if (frames.isEmpty()) {
            return false;
        }

        ItemFrame frame = frames.get(ctx.random().nextInt(frames.size()));
        int nextRot = (frame.getRotation() + 1) % 8;
        frame.setRotation(nextRot);
        ctx.level().playSound(null, frame.blockPosition(), SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.4f, 1.0f);
        return true;
    }
}
