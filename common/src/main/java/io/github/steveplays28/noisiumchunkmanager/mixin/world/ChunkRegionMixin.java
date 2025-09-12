package io.github.steveplays28.noisiumchunkmanager.mixin.world;

import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;

@Mixin(value = WorldGenRegion.class, priority = 200)
public abstract class ChunkRegionMixin implements WorldGenLevel {
	@Shadow
	public abstract boolean hasChunk(int chunkX, int chunkZ);

	@Shadow
	public abstract int getSeaLevel();

	@Shadow
	@Final
	private @NotNull ChunkPos firstPos;

	@Shadow
	@Final
	private @NotNull List<ChunkAccess> cache;

	@Shadow
	@Final
	private int size;

	@Shadow
	public abstract @NotNull RegistryAccess registryAccess();

	@Shadow
	@Final
	private @NotNull ServerLevel level;

	@Inject(method = "isOldChunkAround", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelBlending(@NotNull ChunkPos chunkPosition, int checkRadius, @NotNull CallbackInfoReturnable<Boolean> cir) {
		// TODO: Reimplement chunk blending
		cir.setReturnValue(false);
	}

	@Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getIoWorldChunkIfChunkIsUnloaded(int chunkPositionX, int chunkPositionZ, @NotNull ChunkStatus leastStatus, boolean create, @NotNull CallbackInfoReturnable<ChunkAccess> cir) {
		if (!this.hasChunk(chunkPositionX, chunkPositionZ)) {
			cir.setReturnValue(((ServerWorldExtension) this.level).noisiumchunkmanager$getServerWorldChunkManager().getIoWorldChunk(
					new ChunkPos(chunkPositionX, chunkPositionZ)));
			return;
		}

		cir.setReturnValue(this.cache.get(chunkPositionX - this.firstPos.x + (chunkPositionZ - this.firstPos.z) * this.size));
	}

	@Inject(method = "getBlockState", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getBlockStateFromIoWorldChunkIfChunkIsUnloaded(@NotNull BlockPos blockPos, @NotNull CallbackInfoReturnable<BlockState> cir) {
		if (!this.hasChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()))) {
			cir.setReturnValue(Blocks.AIR.defaultBlockState());
		}
	}

	@Inject(method = "setBlock", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$setBlockStateToIoWorldChunkIfChunkIsUnloaded(@NotNull BlockPos blockPos, @NotNull BlockState blockState, int flags, int maxUpdateDepth, @NotNull CallbackInfoReturnable<Boolean> cir) {
		if (!this.hasChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()))) {
			((ServerWorldExtension) this.level).noisiumchunkmanager$getServerWorldChunkManager().getIoWorldChunk(
					new ChunkPos(blockPos)).setBlockState(blockPos, blockState, false);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getFluidState", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getFluidStateFromIoWorldChunkIfChunkIsUnloaded(@NotNull BlockPos blockPos, @NotNull CallbackInfoReturnable<FluidState> cir) {
		if (!this.hasChunk(SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()))) {
			cir.setReturnValue(((ServerWorldExtension) this.level).noisiumchunkmanager$getServerWorldChunkManager().getIoWorldChunk(
					new ChunkPos(blockPos)).getFluidState(blockPos));
		}
	}

	@Inject(method = "getHeight", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getTopYFromIoWorldChunkIfChunkIsUnloaded(@NotNull Heightmap.Types heightmap, int blockPositionX, int blockPositionZ, @NotNull CallbackInfoReturnable<Integer> cir) {
		if (!this.hasChunk(SectionPos.blockToSectionCoord(blockPositionX), SectionPos.blockToSectionCoord(blockPositionZ))) {
			cir.setReturnValue(((ServerWorldExtension) this.level).noisiumchunkmanager$getServerWorldChunkManager().getIoWorldChunk(
					new ChunkPos(
							SectionPos.blockToSectionCoord(blockPositionX),
							SectionPos.blockToSectionCoord(blockPositionZ)
					)).getMaxBuildHeight());
		}
	}
}
