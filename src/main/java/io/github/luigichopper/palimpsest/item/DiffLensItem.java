package io.github.luigichopper.palimpsest.item;

import io.github.luigichopper.palimpsest.Palimpsest;
import io.github.luigichopper.palimpsest.diff.DiffEntry;
import io.github.luigichopper.palimpsest.diff.DiffTransform;
import io.github.luigichopper.palimpsest.diff.LevelDiffService;
import io.github.luigichopper.palimpsest.level.LevelManager;
import io.github.luigichopper.palimpsest.net.DiffOverlayPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.util.List;

public class DiffLensItem extends Item {

    public DiffLensItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        MinecraftServer server = level.getServer();
        if (server == null) {
            return InteractionResult.FAIL;
        }

        ServerLevel currentLevel = (ServerLevel) level;
        ServerLevel targetLevel = null;
        DiffTransform transform = DiffTransform.IDENTITY;

        String currentDim = currentLevel.dimension().identifier().toString();

        if (currentDim.contains(":echo_")) {
            // Echo layer: compare with Anchor Overworld
            targetLevel = server.getLevel(Level.OVERWORLD);
            transform = DiffTransform.IDENTITY;
        } else if (currentDim.endsWith(":mirror")) {
            // Mirror layer: compare with Anchor Overworld with reflection
            targetLevel = server.getLevel(Level.OVERWORLD);
            transform = DiffTransform.REFLECT;
        } else if (currentLevel.dimension() == Level.OVERWORLD) {
            // Overworld: check if player has active Echo
            LevelManager lm = LevelManager.get();
            if (lm != null) {
                Identifier echoId = Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "echo_" + sp.getUUID().toString().replace("-", ""));
                RuntimeLevelHandle echoHandle = lm.getHandle(echoId);
                if (echoHandle != null) {
                    targetLevel = server.getLevel(echoHandle.getRegistryKey());
                    transform = DiffTransform.IDENTITY;
                }
            }
        }

        if (targetLevel == null) {
            sp.sendSystemMessage(Component.literal("§7[DIFF LENS: No alternate layer active to compare against.]§r"));
            return InteractionResult.FAIL;
        }

        // Execute scan (radius 32, max 2048 results)
        List<DiffEntry> diffs = LevelDiffService.compare(
            currentLevel,
            targetLevel,
            sp.blockPosition(),
            32,
            transform,
            2048
        );

        ServerPlayNetworking.send(sp, new DiffOverlayPayload(diffs));

        sp.sendSystemMessage(Component.literal(String.format(
            "§b[DIFF LENS: Scan complete across '%s' vs '%s'. %d anomalies detected.]§r",
            currentLevel.dimension().identifier().getPath(),
            targetLevel.dimension().identifier().getPath(),
            diffs.size()
        )));

        level.playSound(
            null,
            sp.getX(), sp.getY(), sp.getZ(),
            SoundEvents.BEACON_ACTIVATE,
            SoundSource.PLAYERS,
            0.8f,
            1.6f
        );

        return InteractionResult.SUCCESS;
    }
}
