package io.github.luigichopper.palimpsest;

import io.github.luigichopper.palimpsest.command.DebugCommands;
import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import io.github.luigichopper.palimpsest.core.SeedGate;
import io.github.luigichopper.palimpsest.core.StoryState;
import io.github.luigichopper.palimpsest.core.TaskScheduler;
import io.github.luigichopper.palimpsest.level.Evacuator;
import io.github.luigichopper.palimpsest.level.LevelManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Palimpsest implements ModInitializer {
    public static final String MOD_ID = "palimpsest";
    public static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest");

    private static SeedGate seedGate;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Palimpsest ARG mod...");

        // Load configuration
        PalimpsestConfig.load(FabricLoader.getInstance().getConfigDir());

        // Register Blocks and Items
        io.github.luigichopper.palimpsest.terminal.ModBlocks.register();
        io.github.luigichopper.palimpsest.item.ModItems.register();

        // Register Networking Payloads
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
            io.github.luigichopper.palimpsest.net.OpenTerminalPayload.TYPE,
            io.github.luigichopper.palimpsest.net.OpenTerminalPayload.STREAM_CODEC
        );
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
            io.github.luigichopper.palimpsest.net.TerminalOutputPayload.TYPE,
            io.github.luigichopper.palimpsest.net.TerminalOutputPayload.STREAM_CODEC
        );
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
            io.github.luigichopper.palimpsest.net.SyncJournalPayload.TYPE,
            io.github.luigichopper.palimpsest.net.SyncJournalPayload.STREAM_CODEC
        );
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
            io.github.luigichopper.palimpsest.net.OpenJournalPayload.TYPE,
            io.github.luigichopper.palimpsest.net.OpenJournalPayload.STREAM_CODEC
        );
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
            io.github.luigichopper.palimpsest.net.DiffOverlayPayload.TYPE,
            io.github.luigichopper.palimpsest.net.DiffOverlayPayload.STREAM_CODEC
        );
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.serverboundPlay().register(
            io.github.luigichopper.palimpsest.net.TerminalCommandPayload.TYPE,
            io.github.luigichopper.palimpsest.net.TerminalCommandPayload.STREAM_CODEC
        );

        // Register Server-side Packet Handlers
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.registerGlobalReceiver(
            io.github.luigichopper.palimpsest.net.TerminalCommandPayload.TYPE,
            (payload, context) -> {
                context.server().execute(() -> {
                    io.github.luigichopper.palimpsest.terminal.TerminalManager.get().handleCommand(
                        context.player(),
                        payload.pos(),
                        payload.commandLine()
                    );
                });
            }
        );

        // Register ChapterPackLoader
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
            .registerReloadListener(new io.github.luigichopper.palimpsest.chapter.ChapterPackLoader());

        // Initialize SeedGate
        seedGate = new SeedGate();
        LOGGER.info("SeedGate initialized (salt loaded: {})", !seedGate.getExpectedSalt().isEmpty());

        // Register Server Lifecycle Events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Server started; initializing Palimpsest LevelManager...");
            LevelManager.initialize(server);
        });

        // Register TaskScheduler, AnomalyDirector, ResonanceBus, and ReflectionManager tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TaskScheduler.get().tick(server);
            io.github.luigichopper.palimpsest.director.AnomalyDirector.get().tick(server);
            io.github.luigichopper.palimpsest.resonance.ResonanceBus.get().tick(server);
            io.github.luigichopper.palimpsest.reflection.ReflectionManager.get().tick(server);
        });

        // Director event listeners: combat and damage tracking
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            long now = System.currentTimeMillis();
            if (entity instanceof ServerPlayer player) {
                io.github.luigichopper.palimpsest.director.AnomalyDirector.get().getTracker(player.getUUID()).recordDamage(now);
            }
            if (source.getEntity() instanceof ServerPlayer attacker) {
                io.github.luigichopper.palimpsest.director.AnomalyDirector.get().getTracker(attacker.getUUID()).recordCombat(now);
            }
            return true;
        });

        // Director sleep attempt tracking
        net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents.START_SLEEPING.register((entity, pos) -> {
            if (entity instanceof ServerPlayer player) {
                io.github.luigichopper.palimpsest.director.AnomalyDirector.get().getTracker(player.getUUID()).recordSleepAttempt(System.currentTimeMillis());
            }
        });

        // Director sound recording and ResonanceBus on block break
        net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, be) -> {
            if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel sl) {
                io.github.luigichopper.palimpsest.director.AnomalyDirector.get().getTracker(serverPlayer.getUUID())
                    .setLastPlayerSound(state.getSoundType().getBreakSound());

                StoryState ss = StoryState.get(sl);
                io.github.luigichopper.palimpsest.resonance.ResonanceBus.get().onBlockChanged(
                    sl, pos, state, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), ss
                );
            }
        });

        // Player station interaction and ResonanceBus block placement tracking
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() && hand == net.minecraft.world.InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp && world instanceof ServerLevel sl) {
                var state = world.getBlockState(hitResult.getBlockPos());
                if (state.is(net.minecraft.tags.BlockTags.BEDS) || state.is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE) || state.is(net.minecraft.world.level.block.Blocks.FURNACE) || state.is(net.minecraft.world.level.block.Blocks.BLAST_FURNACE) || state.is(net.minecraft.world.level.block.Blocks.SMOKER)) {
                    StoryState.get(sp.level()).recordStationInteraction(sp.getUUID(), hitResult.getBlockPos(), world.dimension().identifier());
                }

                net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
                if (held.getItem() instanceof net.minecraft.world.item.BlockItem) {
                    BlockPos placedPos = hitResult.getBlockPos().relative(hitResult.getDirection());
                    sl.getServer().execute(() -> {
                        BlockState placedState = sl.getBlockState(placedPos);
                        if (!placedState.isAir()) {
                            io.github.luigichopper.palimpsest.resonance.ResonanceBus.get().onBlockChanged(
                                sl, placedPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), placedState, StoryState.get(sl)
                            );
                        }
                    });
                }
            }
            return net.minecraft.world.InteractionResult.PASS;
        });

        // Player join events (Evacuator safety check and SeedGate notification)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            Evacuator.checkPlayerOnJoin(player);

            long seed = server.overworld().getSeed();
            boolean eligible = seedGate.isEligible(seed) || PalimpsestConfig.get().devMode;
            StoryState state = StoryState.get(server);
            if (!eligible) {
                if (!state.hasWorldFlag("archive:notice_shown")) {
                    state.setWorldFlag("archive:notice_shown");
                    player.sendSystemMessage(Component.literal("§7[ARCHIVE: no matching backup found]§r"));
                }
            } else {
                io.github.luigichopper.palimpsest.landmark.LandmarkPlacer placer = new io.github.luigichopper.palimpsest.landmark.LandmarkPlacer();
                placer.placeAllIfEligible(server, state);

                if (!state.hasWorldFlag("ch0.first_spawn_tp")) {
                    state.setWorldFlag("ch0.first_spawn_tp");
                    int y = server.overworld().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, 16, 16);
                    player.teleportTo(server.overworld(), 16.5, Math.max(64, y) + 1, 19.5, java.util.Collections.emptySet(), 180.0f, 0.0f, true);
                    player.sendSystemMessage(Component.literal("§6[PALIMPSEST: Wake at Cabin L0]§r"));
                }
            }
        });

        // Register Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> {
            dispatcher.register(
                Commands.literal("palimpsest")
                    .then(Commands.literal("ping")
                        .executes(context -> {
                            context.getSource().sendSuccess(
                                () -> Component.literal("Palimpsest: pong! Archive connection active."),
                                false
                            );
                            return 1;
                        })
                    )
                    .then(Commands.literal("journal")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                                player,
                                new io.github.luigichopper.palimpsest.net.OpenJournalPayload()
                            );
                            return 1;
                        })
                    )
            );

            // Register dev mode debug commands
            DebugCommands.register(dispatcher);
        });

        LOGGER.info("Palimpsest initialization complete. Commands and lifecycle listeners registered.");
    }

    public static SeedGate getSeedGate() {
        return seedGate;
    }
}
