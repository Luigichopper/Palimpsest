package io.github.luigichopper.palimpsest;

import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import io.github.luigichopper.palimpsest.core.SeedGate;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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

        // Initialize SeedGate
        seedGate = new SeedGate();
        LOGGER.info("SeedGate initialized (salt loaded: {})", !seedGate.getExpectedSalt().isEmpty());

        // Register /palimpsest ping command
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
        });

        LOGGER.info("Palimpsest initialization complete. Ping command registered.");
    }

    public static SeedGate getSeedGate() {
        return seedGate;
    }
}
