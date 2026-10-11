package io.github.luigichopper.palimpsest.landmark;

import io.github.luigichopper.palimpsest.core.StoryState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.Filterable;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public class CabinBuilder {

    public static void build(ServerLevel level, BlockPos anchor, RandomSource random, StoryState storyState) {
        // Foundation & Floor (7x7)
        BlockPos floorMin = anchor.offset(-3, 0, -3);
        BlockPos floorMax = anchor.offset(3, 0, 3);
        ProceduralDsl.fill(level, floorMin.below(), floorMax.below(), Blocks.COBBLESTONE.defaultBlockState());
        ProceduralDsl.fill(level, floorMin, floorMax, Blocks.SPRUCE_PLANKS.defaultBlockState());

        // Box Walls (7x4x7)
        BlockPos wallMin = anchor.offset(-3, 1, -3);
        BlockPos wallMax = anchor.offset(3, 4, 3);
        ProceduralDsl.box(level, wallMin, wallMax, Blocks.DARK_OAK_PLANKS.defaultBlockState(), Blocks.AIR.defaultBlockState());

        // Corner Posts (Spruce Logs)
        for (int dx : new int[]{-3, 3}) {
            for (int dz : new int[]{-3, 3}) {
                ProceduralDsl.fill(level, anchor.offset(dx, 1, dz), anchor.offset(dx, 4, dz), Blocks.SPRUCE_LOG.defaultBlockState());
            }
        }

        // Ceiling / Flat Roof
        ProceduralDsl.fill(level, anchor.offset(-3, 5, -3), anchor.offset(3, 5, 3), Blocks.SPRUCE_SLAB.defaultBlockState());

        // Doorway on South wall (Z = 3)
        BlockPos doorPos = anchor.offset(0, 1, 3);
        level.setBlock(doorPos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(doorPos.above(), Blocks.AIR.defaultBlockState(), 3);
        ProceduralDsl.setDoor(level, doorPos, Blocks.SPRUCE_DOOR, Direction.SOUTH);

        // Windows (Glass panes)
        level.setBlock(anchor.offset(-3, 2, 0), Blocks.GLASS_PANE.defaultBlockState(), 3);
        level.setBlock(anchor.offset(3, 2, 0), Blocks.GLASS_PANE.defaultBlockState(), 3);
        level.setBlock(anchor.offset(0, 2, -3), Blocks.GLASS_PANE.defaultBlockState(), 3);

        // Interior Lighting
        level.setBlock(anchor.offset(0, 4, 0), Blocks.LANTERN.defaultBlockState(), 3);

        // Work area: Crafting table, Furnace, Table
        level.setBlock(anchor.offset(-2, 1, -2), Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
        level.setBlock(anchor.offset(-1, 1, -2), Blocks.FURNACE.defaultBlockState(), 3);

        // Terminal Console Block on desk
        BlockPos deskPos = anchor.offset(2, 1, -2);
        level.setBlock(deskPos.below(), Blocks.POLISHED_ANDESITE.defaultBlockState(), 3);
        ProceduralDsl.setTerminal(level, deskPos, "term0_cabin");

        // Lectern with README Written Book
        BlockPos lecternPos = anchor.offset(1, 1, 1);
        ItemStack readmeBook = createReadmeBook();
        ProceduralDsl.setLectern(level, lecternPos, Direction.NORTH, readmeBook);

        // Atmospheric touches: Cobwebs, fallen leaves
        level.setBlock(anchor.offset(-2, 4, -2), Blocks.COBWEB.defaultBlockState(), 3);
        level.setBlock(anchor.offset(2, 4, 2), Blocks.COBWEB.defaultBlockState(), 3);
    }

    public static ItemStack createReadmeBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        Filterable<String> title = Filterable.passThrough("README.txt");

        // 7 shuffled pages labeled with SGA letters & Roman numerals
        // Ordering by Roman numeral I-VII yields: E - P - I - T - A - P - H
        List<Filterable<Component>> pages = List.of(
            Filterable.passThrough(Component.literal("§l[IV] §r§n[T]§r\n\nThe logs were severed at midnight. The backup node flickered, but the connection dropped.")),
            Filterable.passThrough(Component.literal("§l[I] §r§n[E]§r\n\nEchoes in the partition. If you are reading this, the restoration script failed.")),
            Filterable.passThrough(Component.literal("§l[VII] §r§n[H]§r\n\nHold onto the anchor coordinate. Without it, you cannot return from the echo.")),
            Filterable.passThrough(Component.literal("§l[II] §r§n[P]§r\n\nPreserve the mirror memory. The system only remembers what you have witnessed.")),
            Filterable.passThrough(Component.literal("§l[V] §r§n[A]§r\n\nArchive integrity degraded. Check /etc/archive_manifest.txt on the console.")),
            Filterable.passThrough(Component.literal("§l[VI] §r§n[P]§r\n\nPast commits cannot be undone. We tried to rollback, but the block hashes diverged.")),
            Filterable.passThrough(Component.literal("§l[III] §r§n[I]§r\n\nIn the deep strata beneath the bunker, the resonance frequency begins to align."))
        );

        WrittenBookContent content = new WrittenBookContent(title, "Odette", 0, pages, true);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }
}
