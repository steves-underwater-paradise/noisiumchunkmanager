package io.github.steveplays28.noisiumchunkmanager.mixin.accessor;

import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LightStorage.class)
public interface LightStorageAccessor {
	@Invoker
	void invokeUpdateLight(ChunkLightProvider<?, ?> lightProvider);

	@Invoker
	void invokeNotifyChanges();
}
