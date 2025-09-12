package io.github.steveplays28.noisiumchunkmanager.mixin.world;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BulkSectionAccess.class)
public class ChunkSectionCacheMixin {
	@Shadow
	@Final
	private @NotNull LevelAccessor level;

	@Shadow
	@Final
	private Long2ObjectMap<LevelChunkSection> acquiredSections;

	@Inject(method = "getSection", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$returnEmptyChunkSectionIfChunkIsUnloaded(@NotNull BlockPos blockPos, @NotNull CallbackInfoReturnable<LevelChunkSection> cir) {
		if (!level.hasChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()))) {
			// TODO: Get an IoWorldChunk from NoisiumServerWorldChunkManager instead
			//  Get a World by checking if (world instanceof World worldCasted)
			cir.setReturnValue(this.acquiredSections.computeIfAbsent(new ChunkPos(blockPos).toLong(),
					l -> new LevelChunkSection(level.registryAccess().registryOrThrow(Registries.BIOME))
			));
		}
	}
}
