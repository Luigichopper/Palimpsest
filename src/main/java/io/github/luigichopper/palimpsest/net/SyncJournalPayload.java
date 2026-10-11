package io.github.luigichopper.palimpsest.net;

import io.github.luigichopper.palimpsest.Palimpsest;
import io.github.luigichopper.palimpsest.journal.JournalEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncJournalPayload(JournalEntry entry) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncJournalPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "sync_journal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncJournalPayload> STREAM_CODEC =
        StreamCodec.composite(
            JournalEntry.STREAM_CODEC,
            SyncJournalPayload::entry,
            SyncJournalPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
