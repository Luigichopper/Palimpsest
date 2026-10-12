package io.github.luigichopper.palimpsest.echo;

import io.github.luigichopper.palimpsest.config.PalimpsestConfig;
import io.github.luigichopper.palimpsest.core.AnchorPos;
import io.github.luigichopper.palimpsest.core.ScheduledTask;
import io.github.luigichopper.palimpsest.core.StoryState;
import io.github.luigichopper.palimpsest.core.TaskScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class EchoBuilder {

    public static BlockPos resolveBaseCenter(ServerPlayer player, MinecraftServer server) {
        if (player.getRespawnConfig() != null && player.getRespawnConfig().respawnData() != null) {
            return player.getRespawnConfig().respawnData().pos();
        }
        if (server.getRespawnData() != null && server.getRespawnData().pos() != null) {
            return server.getRespawnData().pos();
        }
        return new BlockPos(0, 64, 0);
    }

    public static void build(ServerPlayer player, ServerLevel echoWorld, StoryState storyState) {
        MinecraftServer server = player.level().getServer();
        ServerLevel anchorWorld = server.getLevel(Level.OVERWORLD);
        if (anchorWorld == null) {
            return;
        }

        BlockPos center = resolveBaseCenter(player, server);
        int radius = PalimpsestConfig.get().echo.radius;

        // Perform sliced copy via TaskScheduler to respect tick budget
        TaskScheduler.get().submit(new EchoCopyTask(player, anchorWorld, echoWorld, center, radius, storyState, true));
    }

    public static void copyDirect(ServerLevel anchorWorld, ServerLevel echoWorld, BlockPos center, int radius, boolean applyMutations, StoryState storyState, ServerPlayer player) {
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;

        int minY = Math.max(anchorWorld.getMinY(), center.getY() - 16);
        int maxY = Math.min(anchorWorld.getMaxY() - 1, center.getY() + 24);

        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    mut.set(x, y, z);
                    BlockState state = anchorWorld.getBlockState(mut);

                    if (applyMutations) {
                        // Mutation 1: remove doors
                        if (state.getBlock() instanceof DoorBlock) {
                            echoWorld.setBlock(mut, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
                            continue;
                        }
                        // Mutation 2: unlight torches
                        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
                            echoWorld.setBlock(mut, Blocks.REDSTONE_WALL_TORCH.defaultBlockState(), Block.UPDATE_NONE);
                            continue;
                        }
                    }

                    if (!state.isAir()) {
                        echoWorld.setBlock(mut, state, Block.UPDATE_NONE);

                        BlockEntity anchorBe = anchorWorld.getBlockEntity(mut);
                        if (anchorBe != null) {
                            try {
                                CompoundTag tag = anchorBe.saveWithFullMetadata(echoWorld.registryAccess());
                                BlockEntity echoBe = BlockEntity.loadStatic(mut, state, tag, echoWorld.registryAccess());
                                if (echoBe != null) {
                                    echoWorld.setBlockEntity(echoBe);

                                    // Mutation 4: replace chest contents with generated journal pages
                                    if (applyMutations && echoBe instanceof Container container) {
                                        container.clearContent();
                                        ItemStack page = createEchoJournalFragment();
                                        container.setItem(0, page);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }

        // Copy Item Frames with Mutation 3 (+1 rotation)
        AABB box = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        List<ItemFrame> frames = anchorWorld.getEntitiesOfClass(ItemFrame.class, box);
        for (ItemFrame frame : frames) {
            ItemFrame copy = new ItemFrame(echoWorld, frame.blockPosition(), frame.getDirection());
            copy.setItem(frame.getItem().copy());
            int rot = applyMutations ? (frame.getRotation() + 1) % 8 : frame.getRotation();
            copy.setRotation(rot);
            echoWorld.addFreshEntity(copy);
        }

        // Mutation 5: Place the Double at the player's most used spot
        if (applyMutations) {
            BlockPos doublePos = center;
            if (player != null && storyState != null) {
                AnchorPos anchorPos = storyState.getMostUsedSpot(player.getUUID());
                if (anchorPos != null) {
                    doublePos = new BlockPos((int) anchorPos.x(), (int) anchorPos.y(), (int) anchorPos.z());
                }
            }
            placeDoubleEntity(echoWorld, doublePos);
        }
    }

    private static void placeDoubleEntity(ServerLevel level, BlockPos pos) {
        Mannequin doubleEntity = EntityTypes.MANNEQUIN.create(level, EntitySpawnReason.TRIGGERED);
        if (doubleEntity != null) {
            doubleEntity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            doubleEntity.setCustomName(Component.literal("Reflection"));
            doubleEntity.setCustomNameVisible(false);
            doubleEntity.setPermanentlyInvulnerable(true);
            doubleEntity.setSilent(true);
            level.addFreshEntity(doubleEntity);
        }
    }

    private static ItemStack createEchoJournalFragment() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        Filterable<String> title = Filterable.passThrough("Shadow Fragment");
        List<Filterable<Component>> pages = List.of(
            Filterable.passThrough(Component.literal("§oA page torn from an older commit.\n\nThe double was observed sitting at your desk.\n\nEvery door was removed.\nEvery torch was cold.\nDo not look behind you.§r"))
        );
        WrittenBookContent content = new WrittenBookContent(title, "Reflection", 0, pages, true);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return book;
    }

    public static class EchoCopyTask implements ScheduledTask {
        private final ServerPlayer player;
        private final ServerLevel anchorWorld;
        private final ServerLevel echoWorld;
        private final BlockPos center;
        private final int radius;
        private final StoryState storyState;
        private final boolean applyMutations;

        private int currentX;
        private boolean finished = false;

        public EchoCopyTask(ServerPlayer player, ServerLevel anchorWorld, ServerLevel echoWorld, BlockPos center, int radius, StoryState storyState, boolean applyMutations) {
            this.player = player;
            this.anchorWorld = anchorWorld;
            this.echoWorld = echoWorld;
            this.center = center;
            this.radius = radius;
            this.storyState = storyState;
            this.applyMutations = applyMutations;
            this.currentX = center.getX() - radius;
        }

        @Override
        public String name() {
            return "echo_copy_" + (player != null ? player.getScoreboardName() : "test");
        }

        @Override
        public int priority() {
            return 80;
        }

        @Override
        public boolean tick(MinecraftServer server) {
            if (finished) {
                return true;
            }

            int endX = center.getX() + radius;
            int minZ = center.getZ() - radius;
            int maxZ = center.getZ() + radius;
            int minY = Math.max(anchorWorld.getMinY(), center.getY() - 16);
            int maxY = Math.min(anchorWorld.getMaxY() - 1, center.getY() + 24);

            long start = System.nanoTime();
            long maxNanos = 2_000_000L; // 2ms per tick slice

            BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

            while (currentX <= endX) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = minY; y <= maxY; y++) {
                        mut.set(currentX, y, z);
                        BlockState state = anchorWorld.getBlockState(mut);

                        if (applyMutations) {
                            if (state.getBlock() instanceof DoorBlock) {
                                echoWorld.setBlock(mut, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
                                continue;
                            }
                            if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) {
                                echoWorld.setBlock(mut, Blocks.REDSTONE_WALL_TORCH.defaultBlockState(), Block.UPDATE_NONE);
                                continue;
                            }
                        }

                        if (!state.isAir()) {
                            echoWorld.setBlock(mut, state, Block.UPDATE_NONE);
                            BlockEntity anchorBe = anchorWorld.getBlockEntity(mut);
                            if (anchorBe != null) {
                                try {
                                    CompoundTag tag = anchorBe.saveWithFullMetadata(echoWorld.registryAccess());
                                    BlockEntity echoBe = BlockEntity.loadStatic(mut, state, tag, echoWorld.registryAccess());
                                    if (echoBe != null) {
                                        echoWorld.setBlockEntity(echoBe);
                                        if (applyMutations && echoBe instanceof Container container) {
                                            container.clearContent();
                                            container.setItem(0, createEchoJournalFragment());
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
                currentX++;

                if (System.nanoTime() - start > maxNanos) {
                    return false; // Yield to next tick
                }
            }

            // Copy ItemFrames & Double once block copying is complete
            AABB box = new AABB(center.getX() - radius, minY, minZ, endX + 1, maxY + 1, maxZ + 1);
            List<ItemFrame> frames = anchorWorld.getEntitiesOfClass(ItemFrame.class, box);
            for (ItemFrame frame : frames) {
                ItemFrame copy = new ItemFrame(echoWorld, frame.blockPosition(), frame.getDirection());
                copy.setItem(frame.getItem().copy());
                int rot = applyMutations ? (frame.getRotation() + 1) % 8 : frame.getRotation();
                copy.setRotation(rot);
                echoWorld.addFreshEntity(copy);
            }

            if (applyMutations) {
                BlockPos doublePos = center;
                if (player != null && storyState != null) {
                    AnchorPos anchorPos = storyState.getMostUsedSpot(player.getUUID());
                    if (anchorPos != null) {
                        doublePos = new BlockPos((int) anchorPos.x(), (int) anchorPos.y(), (int) anchorPos.z());
                    }
                }
                placeDoubleEntity(echoWorld, doublePos);
            }

            finished = true;
            return true;
        }

        @Override
        public void cancel() {
            finished = true;
        }

        @Override
        public boolean isCancelled() {
            return finished;
        }

        @Override
        public float getProgress() {
            int total = (radius * 2) + 1;
            int done = currentX - (center.getX() - radius);
            return Math.clamp((float) done / total, 0.0f, 1.0f);
        }
    }
}
