package io.github.luigichopper.palimpsest.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PalimpsestClient implements ClientModInitializer {
    public static final Logger CLIENT_LOGGER = LoggerFactory.getLogger("Palimpsest/Client");

    @Override
    public void onInitializeClient() {
        CLIENT_LOGGER.info("Palimpsest client initialization active.");
    }
}
