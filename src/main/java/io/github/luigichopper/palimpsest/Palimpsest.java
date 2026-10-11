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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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

        // Register Blocks and Block Entities
        io.github.luigichopper.palimpsest.terminal.ModBlocks.register();

        // Register Networking Payloads
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
            io.github.luigichopper.palimpsest.net.OpenTerminalPayload.TYPE,
            io.github.luigichopper.palimpsest.net.OpenTerminalPayload.STREAM_CODEC
        );
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
            io.github.luigichopper.palimpsest.net.TerminalOutputPayload.TYPE,
            io.github.luigichopper.palimpsest.net.TerminalOutputPayload.STREAM_CODEC
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

        // Register TaskScheduler tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            TaskScheduler.get().tick(server);
        });

        // Player join events (Evacuator safety check and SeedGate notification)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            Evacuator.checkPlayerOnJoin(player);

            long seed = server.overworld().getSeed();
            boolean eligible = seedGate.isEligible(seed);
            if (!eligible) {
                StoryState state = StoryState.get(server);
                if (!state.hasWorldFlag("archive:notice_shown")) {
                    state.setWorldFlag("archive:notice_shown");
                    player.sendSystemMessage(Component.literal("§7[ARCHIVE: no matching backup found]§r"));
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
