package io.github.luigichopper.palimpsest.journal;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record JournalEntry(
    String id,
    String title,
    String content,
    String tag,
    long timestamp
) {
    public static final StreamCodec<ByteBuf, JournalEntry> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, JournalEntry::id,
        ByteBufCodecs.STRING_UTF8, JournalEntry::title,
        ByteBufCodecs.STRING_UTF8, JournalEntry::content,
        ByteBufCodecs.STRING_UTF8, JournalEntry::tag,
        ByteBufCodecs.VAR_LONG, JournalEntry::timestamp,
        JournalEntry::new
    );
}
