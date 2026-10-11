package io.github.luigichopper.palimpsest.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PalimpsestClient implements ClientModInitializer {
    public static final Logger CLIENT_LOGGER = LoggerFactory.getLogger("Palimpsest/Client");

    @Override
    public void onInitializeClient() {
        CLIENT_LOGGER.info("Palimpsest client initialization active.");

        // Register OpenTerminalPayload receiver to open TerminalScreen
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
            io.github.luigichopper.palimpsest.net.OpenTerminalPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    net.minecraft.client.Minecraft.getInstance().gui.setScreen(new TerminalScreen(payload.pos()));
                });
            }
        );

        // Register TerminalOutputPayload receiver to append output to active screen
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
            io.github.luigichopper.palimpsest.net.TerminalOutputPayload.TYPE,
            (payload, context) -> {
                context.client().execute(() -> {
                    if (net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof TerminalScreen termScreen) {
                        if (termScreen.getTerminalPos().equals(payload.pos())) {
                            termScreen.appendOutput(payload.output(), payload.isError(), payload.clearScreen());
                        }
                    }
                });
            }
        );
    }
}
