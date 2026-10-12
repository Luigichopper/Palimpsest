package io.github.luigichopper.palimpsest.reflection;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public record RecordedFrame(
    double x,
    double y,
    double z,
    float yaw,
    float pitch,
    String poseName,
    Identifier heldItemId,
    long tick
) {
    public static final Codec<RecordedFrame> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(RecordedFrame::x),
            Codec.DOUBLE.fieldOf("y").forGetter(RecordedFrame::y),
            Codec.DOUBLE.fieldOf("z").forGetter(RecordedFrame::z),
            Codec.FLOAT.fieldOf("yaw").forGetter(RecordedFrame::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(RecordedFrame::pitch),
            Codec.STRING.optionalFieldOf("pose", "STANDING").forGetter(RecordedFrame::poseName),
            Identifier.CODEC.optionalFieldOf("held_item", Identifier.fromNamespaceAndPath("minecraft", "air")).forGetter(RecordedFrame::heldItemId),
            Codec.LONG.fieldOf("tick").forGetter(RecordedFrame::tick)
        ).apply(instance, RecordedFrame::new)
    );

    public Pose getPose() {
        try {
            return Pose.valueOf(poseName);
        } catch (Exception e) {
            return Pose.STANDING;
        }
    }

    public ItemStack getHeldItemStack() {
        Item item = BuiltInRegistries.ITEM.getValue(heldItemId);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }
}
