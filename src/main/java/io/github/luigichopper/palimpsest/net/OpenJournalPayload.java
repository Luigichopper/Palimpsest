package io.github.luigichopper.palimpsest.net;

import io.github.luigichopper.palimpsest.Palimpsest;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenJournalPayload() implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenJournalPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "open_journal"));

    public static final StreamCodec<ByteBuf, OpenJournalPayload> STREAM_CODEC =
        StreamCodec.unit(new OpenJournalPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
