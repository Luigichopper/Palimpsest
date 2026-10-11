package io.github.luigichopper.palimpsest.level;

import io.github.luigichopper.palimpsest.core.AnchorPos;
import io.github.luigichopper.palimpsest.core.StoryState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Evacuator {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/Evacuator");

    public static void recordAnchorPos(ServerPlayer player) {
        ServerLevel level = player.level();
        // Only record as anchor if in vanilla/anchor dimensions (e.g. overworld)
        if (level.dimension() == Level.OVERWORLD) {
            StoryState state = StoryState.get(level.getServer());
            AnchorPos pos = new AnchorPos(
                level.dimension().identifier(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
            );
            state.setLastAnchorPos(player.getUUID(), pos);
            LOGGER.debug("Recorded lastAnchorPos for player {}: {}", player.getScoreboardName(), pos);
        }
    }

    public static void evacuatePlayer(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        StoryState state = StoryState.get(server);
        AnchorPos anchor = state.getLastAnchorPos(player.getUUID());

        ServerLevel targetLevel = null;
        double targetX = 0, targetY = 64, targetZ = 0;
        float yaw = 0, pitch = 0;

        if (anchor != null) {
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, anchor.dimensionId());
            targetLevel = server.getLevel(key);
            if (targetLevel != null) {
                targetX = anchor.x();
                targetY = anchor.y();
                targetZ = anchor.z();
                yaw = anchor.yaw();
                pitch = anchor.pitch();
            }
        }

        if (targetLevel == null) {
            targetLevel = server.getLevel(Level.OVERWORLD);
            if (targetLevel != null) {
                BlockPos spawn = server.getRespawnData().pos();
                targetX = spawn.getX() + 0.5;
                targetY = spawn.getY();
                targetZ = spawn.getZ() + 0.5;
            }
        }

        if (targetLevel != null) {
            LOGGER.info("Evacuating player '{}' to level '{}' at ({}, {}, {})",
                player.getScoreboardName(), targetLevel.dimension().identifier(), targetX, targetY, targetZ);
            player.teleportTo(targetLevel, targetX, targetY, targetZ, Collections.emptySet(), yaw, pitch, true);
        }
    }

    public static void evacuateLevel(ServerLevel level) {
        List<ServerPlayer> players = new ArrayList<>(level.players());
        for (ServerPlayer player : players) {
            evacuatePlayer(player);
        }
    }

    public static void checkPlayerOnJoin(ServerPlayer player) {
        ResourceKey<Level> dimKey = player.level().dimension();
        Identifier dimId = dimKey.identifier();
        // If the player spawned into a temporary Palimpsest dimension that vanished
        if (dimId.getNamespace().equals("palimpsest") || dimId.getNamespace().equals("fantasy")) {
            if (player.level().getServer().getLevel(dimKey) == null) {
                LOGGER.warn("Player '{}' logged in inside vanished dimension '{}'; evacuating to anchor",
                    player.getScoreboardName(), dimId);
                evacuatePlayer(player);
            }
        }
    }
}
