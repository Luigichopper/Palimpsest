package io.github.luigichopper.palimpsest.director.anomalies;

import io.github.luigichopper.palimpsest.director.Anomaly;
import io.github.luigichopper.palimpsest.director.AnomalyContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignTextSlot;

import java.util.List;

public class SignChangeAnomaly implements Anomaly {
    private static final List<String> HAUNTING_MESSAGES = List.of(
        "I STILL REMEMBER",
        "WREN?",
        "THE COPY WRITES BACK",
        "DONT TRUST THE MIRROR",
        "ARCHIVE DESYNC"
    );

    @Override
    public String id() {
        return "sign_change";
    }

    @Override
    public int tier() {
        return 2;
    }

    @Override
    public float cost() {
        return 30.0f;
    }

    @Override
    public long cooldownSeconds() {
        return 360;
    }

    @Override
    public boolean canFire(AnomalyContext ctx) {
        return findNearbySign(ctx.level(), ctx.player().blockPosition()) != null;
    }

    @Override
    public boolean fire(AnomalyContext ctx) {
        ServerLevel level = ctx.level();
        SignBlockEntity sign = findNearbySign(level, ctx.player().blockPosition());
        if (sign == null) {
            return false;
        }

        String msg = HAUNTING_MESSAGES.get(ctx.random().nextInt(HAUNTING_MESSAGES.size()));
        sign.updateText(text -> text.asMutable().setLine(1, Component.literal(msg)).asImmutable(), SignTextSlot.FRONT);
        sign.setChanged();
        level.sendBlockUpdated(sign.getBlockPos(), sign.getBlockState(), sign.getBlockState(), 3);
        return true;
    }

    private SignBlockEntity findNearbySign(ServerLevel level, BlockPos origin) {
        int radius = 10;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos p = origin.offset(x, y, z);
                    BlockEntity be = level.getBlockEntity(p);
                    if (be instanceof SignBlockEntity sign) {
                        return sign;
                    }
                }
            }
        }
        return null;
    }
}
