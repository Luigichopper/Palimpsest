package io.github.luigichopper.palimpsest.diff;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record DiffEntry(
    BlockPos pos,
    DiffCategory category,
    double distanceSq
) {
    public static final StreamCodec<ByteBuf, DiffEntry> STREAM_CODEC = StreamCodec.of(
        (buf, entry) -> {
            BlockPos.STREAM_CODEC.encode(buf, entry.pos());
            buf.writeByte(entry.category().getId());
            buf.writeDouble(entry.distanceSq());
        },
        buf -> {
            BlockPos pos = BlockPos.STREAM_CODEC.decode(buf);
            byte catId = buf.readByte();
            double dist = buf.readDouble();
            return new DiffEntry(pos, DiffCategory.fromId(catId), dist);
        }
    );
}
