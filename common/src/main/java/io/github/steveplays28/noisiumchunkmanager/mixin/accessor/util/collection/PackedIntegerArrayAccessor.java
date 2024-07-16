package io.github.steveplays28.noisiumchunkmanager.mixin.accessor.util.collection;

import net.minecraft.util.collection.PackedIntegerArray;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PackedIntegerArray.class)
public interface PackedIntegerArrayAccessor {
	@Accessor
	long getMaxValue();
}
