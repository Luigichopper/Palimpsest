package io.github.luigichopper.palimpsest.net;

import io.github.luigichopper.palimpsest.Palimpsest;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TerminalOutputPayload(BlockPos pos, String output, boolean isError, boolean clearScreen) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TerminalOutputPayload> TYPE =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "terminal_output"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalOutputPayload> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TerminalOutputPayload::pos,
            ByteBufCodecs.STRING_UTF8,
            TerminalOutputPayload::output,
            ByteBufCodecs.BOOL,
            TerminalOutputPayload::isError,
            ByteBufCodecs.BOOL,
            TerminalOutputPayload::clearScreen,
            TerminalOutputPayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
