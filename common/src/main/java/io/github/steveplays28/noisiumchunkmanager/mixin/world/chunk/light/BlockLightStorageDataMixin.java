package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk.light;

import io.github.steveplays28.noisiumchunkmanager.extension.world.chunk.light.ChunkToNibbleArrayMapExtension;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.BlockLightStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockLightStorage.Data.class)
public class BlockLightStorageDataMixin {
	@Redirect(method = "copy()Lnet/minecraft/world/chunk/light/BlockLightStorage$Data;", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;clone()Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;"))
	private @NotNull Long2ObjectOpenHashMap<ChunkNibbleArray> noisiumchunkmanager$cloneThreadSafe(@Nullable Long2ObjectOpenHashMap<ChunkNibbleArray> instance) {
		return new Long2ObjectOpenHashMap<>(((ChunkToNibbleArrayMapExtension) this).noisiumchunkmanager$getArrays());
	}
}
