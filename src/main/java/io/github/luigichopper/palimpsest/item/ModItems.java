package io.github.luigichopper.palimpsest.item;

import io.github.luigichopper.palimpsest.Palimpsest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final Identifier DIFF_LENS_ID = Identifier.fromNamespaceAndPath(Palimpsest.MOD_ID, "diff_lens");
    public static final ResourceKey<Item> DIFF_LENS_KEY = ResourceKey.create(Registries.ITEM, DIFF_LENS_ID);

    public static final DiffLensItem DIFF_LENS = new DiffLensItem(
        new Item.Properties().setId(DIFF_LENS_KEY).stacksTo(1)
    );

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, DIFF_LENS_KEY, DIFF_LENS);
        Palimpsest.LOGGER.info("Registered Diff Lens item.");
    }
}
