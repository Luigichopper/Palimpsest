package io.github.luigichopper.palimpsest.annex;

import io.github.luigichopper.palimpsest.Palimpsest;
import io.github.luigichopper.palimpsest.core.ScheduledTask;
import io.github.luigichopper.palimpsest.core.StoryState;
import io.github.luigichopper.palimpsest.core.TaskScheduler;
import io.github.luigichopper.palimpsest.level.Evacuator;
import io.github.luigichopper.palimpsest.level.LevelManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import xyz.nucleoid.fantasy.RuntimeLevelHandle;

import java.util.UUID;

public class AnnexManager {
    private static final AnnexManager INSTANCE = new AnnexManager();

    private AnnexManager() {}

    public static AnnexManager get() {
        return INSTANCE;
    }

    public void completeAnnex(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        StoryState storyState = StoryState.get(server);
        UUID playerId = player.getUUID();

        storyState.setPlayerFlag(playerId, "ch0.complete");
        storyState.setWorldFlag("ch0.complete");

        player.sendSystemMessage(Component.literal("§b[ARCHIVE: Partition 0xff-ch0 resolved. Dismantling temporary space...]§r"));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_DEACTIVATE, SoundSource.AMBIENT, 1.0f, 0.8f);

        // Schedule delayed evacuation (3 seconds / 60 ticks)
        Identifier annexId = Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "annex_" + playerId.toString().replace("-", ""));

        TaskScheduler.get().submit(new ScheduledTask() {
            private int elapsed = 0;
            private boolean done = false;

            @Override
            public String name() {
                return "annex_evacuate_" + playerId;
            }

            @Override
            public int priority() {
                return 100;
            }

            @Override
            public boolean tick(MinecraftServer s) {
                elapsed++;
                if (elapsed >= 60 && !done) {
                    done = true;
                    Evacuator.evacuatePlayer(player);

                    LevelManager lm = LevelManager.get();
                    if (lm != null) {
                        RuntimeLevelHandle handle = lm.getHandle(annexId);
                        if (handle != null) {
                            lm.closeAndDelete(handle);
                        }
                    }
                    player.sendSystemMessage(Component.literal("§a[ARCHIVE: Returned to Anchor safely.]§r"));
                    return true;
                }
                return false;
            }

            @Override
            public void cancel() {
                done = true;
            }

            @Override
            public boolean isCancelled() {
                return done;
            }

            @Override
            public float getProgress() {
                return Math.min(1.0f, elapsed / 60.0f);
            }
        });
    }
}
