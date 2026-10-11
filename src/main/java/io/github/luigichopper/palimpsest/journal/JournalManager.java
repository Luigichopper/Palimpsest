package io.github.luigichopper.palimpsest.journal;

import io.github.luigichopper.palimpsest.core.StoryState;
import io.github.luigichopper.palimpsest.net.SyncJournalPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class JournalManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Palimpsest/Journal");
    private static final JournalManager INSTANCE = new JournalManager();

    // Server-side cache: playerId -> list of entries
    private final Map<UUID, List<JournalEntry>> playerEntries = new ConcurrentHashMap<>();

    // Client-side received entries
    public static final List<JournalEntry> CLIENT_ENTRIES = new ArrayList<>();

    private JournalManager() {}

    public static JournalManager get() {
        return INSTANCE;
    }

    public List<JournalEntry> getEntries(UUID playerId) {
        return playerEntries.computeIfAbsent(playerId, k -> new ArrayList<>());
    }

    public void recordDiscovery(ServerPlayer player, String id, String title, String content, String tag) {
        UUID pid = player.getUUID();
        List<JournalEntry> list = getEntries(pid);

        // Check if entry already recorded
        for (JournalEntry e : list) {
            if (e.id().equals(id)) {
                return;
            }
        }

        JournalEntry newEntry = new JournalEntry(id, title, content, tag, System.currentTimeMillis());
        list.add(newEntry);

        // Sync to client
        try {
            ServerPlayNetworking.send(player, new SyncJournalPayload(newEntry));
            player.sendSystemMessage(Component.literal("§a[Journal entry added: " + title + "]§r"));
        } catch (Exception e) {
            LOGGER.error("Failed to send SyncJournalPayload to player " + player.getScoreboardName(), e);
        }
    }
}
