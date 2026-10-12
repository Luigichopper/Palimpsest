package io.github.luigichopper.palimpsest.net;

import io.github.luigichopper.palimpsest.Palimpsest;
import io.github.luigichopper.palimpsest.diff.DiffEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record DiffOverlayPayload(List<DiffEntry> entries) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DiffOverlayPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "diff_overlay"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiffOverlayPayload> STREAM_CODEC =
        StreamCodec.composite(
            DiffEntry.STREAM_CODEC.apply(ByteBufCodecs.list(2048)),
            DiffOverlayPayload::entries,
            DiffOverlayPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
