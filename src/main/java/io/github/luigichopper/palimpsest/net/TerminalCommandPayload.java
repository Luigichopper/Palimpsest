package io.github.luigichopper.palimpsest.net;

import io.github.luigichopper.palimpsest.Palimpsest;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TerminalCommandPayload(BlockPos pos, String commandLine) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TerminalCommandPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "terminal_command"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalCommandPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TerminalCommandPayload::pos,
            ByteBufCodecs.STRING_UTF8,
            TerminalCommandPayload::commandLine,
            TerminalCommandPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
