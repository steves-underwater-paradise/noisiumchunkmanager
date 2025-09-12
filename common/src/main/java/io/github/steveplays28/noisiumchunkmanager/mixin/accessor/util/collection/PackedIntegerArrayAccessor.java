package io.github.steveplays28.noisiumchunkmanager.mixin.accessor.util.collection;

import net.minecraft.util.SimpleBitStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleBitStorage.class)
public interface PackedIntegerArrayAccessor {
	@Accessor
	long getMask();
}
