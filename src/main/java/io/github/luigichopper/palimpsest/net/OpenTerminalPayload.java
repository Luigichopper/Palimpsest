package io.github.luigichopper.palimpsest.net;

import io.github.luigichopper.palimpsest.Palimpsest;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenTerminalPayload(BlockPos pos) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenTerminalPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "open_terminal"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenTerminalPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            OpenTerminalPayload::pos,
            OpenTerminalPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
