package io.github.luigichopper.palimpsest.terminal;

import io.github.luigichopper.palimpsest.Palimpsest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.Set;

public class ModBlocks {
    public static final Identifier TERMINAL_ID = Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "terminal");

    public static final ResourceKey<Block> TERMINAL_BLOCK_KEY = ResourceKey.create(Registries.BLOCK, TERMINAL_ID);
    public static final ResourceKey<Item> TERMINAL_ITEM_KEY = ResourceKey.create(Registries.ITEM, TERMINAL_ID);

    public static final TerminalBlock TERMINAL_BLOCK = new TerminalBlock(
        BlockBehaviour.Properties.of()
            .setId(TERMINAL_BLOCK_KEY)
            .strength(2.5f)
            .sound(SoundType.METAL)
            .requiresCorrectToolForDrops()
    );

    public static final BlockItem TERMINAL_ITEM = new BlockItem(
        TERMINAL_BLOCK,
        new Item.Properties().setId(TERMINAL_ITEM_KEY)
    );

    public static final BlockEntityType<TerminalBlockEntity> TERMINAL_BLOCK_ENTITY_TYPE = new BlockEntityType<>(
        TerminalBlockEntity::new,
        Set.of(TERMINAL_BLOCK)
    );

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, TERMINAL_BLOCK_KEY, TERMINAL_BLOCK);
        Registry.register(BuiltInRegistries.ITEM, TERMINAL_ITEM_KEY, TERMINAL_ITEM);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, TERMINAL_ID, TERMINAL_BLOCK_ENTITY_TYPE);
        Palimpsest.LOGGER.info("Registered Terminal block, item, and block entity type.");
    }
}
