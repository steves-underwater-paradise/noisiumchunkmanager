package io.github.steveplays28.noisiumchunkmanager.mixin.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.ChunkSectionCache;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.chunk.ChunkSection;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkSectionCache.class)
public class ChunkSectionCacheMixin {
	@Shadow
	@Final
	private @NotNull WorldAccess world;

	@Shadow
	@Final
	private Long2ObjectMap<ChunkSection> cache;

	@Inject(method = "getSection", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$returnEmptyChunkSectionIfChunkIsUnloaded(@NotNull BlockPos blockPos, @NotNull CallbackInfoReturnable<ChunkSection> cir) {
		if (!world.isChunkLoaded(ChunkSectionPos.getSectionCoord(blockPos.getX()), ChunkSectionPos.getSectionCoord(blockPos.getZ()))) {
			// TODO: Get an IoWorldChunk from NoisiumServerWorldChunkManager instead
			//  Get a World by checking if (world instanceof World worldCasted)
			cir.setReturnValue(this.cache.computeIfAbsent(new ChunkPos(blockPos).toLong(),
					l -> new ChunkSection(world.getRegistryManager().get(RegistryKeys.BIOME))
			));
		}
	}
}
