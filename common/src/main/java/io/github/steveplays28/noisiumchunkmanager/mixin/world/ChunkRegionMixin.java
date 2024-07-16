package io.github.steveplays28.noisiumchunkmanager.mixin.world;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ChunkRegion.class, priority = 200)
public abstract class ChunkRegionMixin implements StructureWorldAccess {
	@Shadow
	public abstract boolean isChunkLoaded(int chunkX, int chunkZ);

	@Shadow
	public abstract int getSeaLevel();

	@Shadow
	@Final
	private @NotNull ChunkPos lowerCorner;

	@Shadow
	@Final
	private @NotNull List<Chunk> chunks;

	@Shadow
	@Final
	private int width;

	@Shadow
	public abstract @NotNull DynamicRegistryManager getRegistryManager();

	@Shadow
	@Final
	private @NotNull ServerWorld world;

	@Inject(method = "needsBlending", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelBlending(@NotNull ChunkPos chunkPosition, int checkRadius, @NotNull CallbackInfoReturnable<Boolean> cir) {
		// TODO: Reimplement chunk blending
		cir.setReturnValue(false);
	}

	@Inject(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getIoWorldChunkIfChunkIsUnloaded(int chunkPositionX, int chunkPositionZ, @NotNull ChunkStatus leastStatus, boolean create, @NotNull CallbackInfoReturnable<Chunk> cir) {
		if (!this.isChunkLoaded(chunkPositionX, chunkPositionZ)) {
			cir.setReturnValue(((io.github.steveplays28.noisiumchunkmanager.experimental.extension.world.server.ServerWorldExtension) this.world).noisiumchunkmanager$getServerWorldChunkManager().getIoWorldChunk(
					new ChunkPos(chunkPositionX, chunkPositionZ)));
			return;
		}

		cir.setReturnValue(this.chunks.get(chunkPositionX - this.lowerCorner.x + (chunkPositionZ - this.lowerCorner.z) * this.width));
	}

	@Inject(method = "getBlockState", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getBlockStateFromIoWorldChunkIfChunkIsUnloaded(@NotNull BlockPos blockPos, @NotNull CallbackInfoReturnable<BlockState> cir) {
		if (!this.isChunkLoaded(ChunkSectionPos.getSectionCoord(blockPos.getX()), ChunkSectionPos.getSectionCoord(blockPos.getZ()))) {
			cir.setReturnValue(Blocks.AIR.getDefaultState());
		}
	}

	@Inject(method = "setBlockState", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$setBlockStateToIoWorldChunkIfChunkIsUnloaded(@NotNull BlockPos blockPos, @NotNull BlockState blockState, int flags, int maxUpdateDepth, @NotNull CallbackInfoReturnable<Boolean> cir) {
		if (!this.isChunkLoaded(ChunkSectionPos.getSectionCoord(blockPos.getX()), ChunkSectionPos.getSectionCoord(blockPos.getZ()))) {
			((io.github.steveplays28.noisiumchunkmanager.experimental.extension.world.server.ServerWorldExtension) this.world).noisiumchunkmanager$getServerWorldChunkManager().getIoWorldChunk(
					new ChunkPos(blockPos)).setBlockState(blockPos, blockState, false);
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getFluidState", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getFluidStateFromIoWorldChunkIfChunkIsUnloaded(@NotNull BlockPos blockPos, @NotNull CallbackInfoReturnable<FluidState> cir) {
		if (!this.isChunkLoaded(ChunkSectionPos.getSectionCoord(blockPos.getX()), ChunkSectionPos.getSectionCoord(blockPos.getZ()))) {
			cir.setReturnValue(((io.github.steveplays28.noisiumchunkmanager.experimental.extension.world.server.ServerWorldExtension) this.world).noisiumchunkmanager$getServerWorldChunkManager().getIoWorldChunk(
					new ChunkPos(blockPos)).getFluidState(blockPos));
		}
	}

	@Inject(method = "getTopY", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getTopYFromIoWorldChunkIfChunkIsUnloaded(@NotNull Heightmap.Type heightmap, int blockPositionX, int blockPositionZ, @NotNull CallbackInfoReturnable<Integer> cir) {
		if (!this.isChunkLoaded(ChunkSectionPos.getSectionCoord(blockPositionX), ChunkSectionPos.getSectionCoord(blockPositionZ))) {
			cir.setReturnValue(((io.github.steveplays28.noisiumchunkmanager.experimental.extension.world.server.ServerWorldExtension) this.world).noisiumchunkmanager$getServerWorldChunkManager().getIoWorldChunk(
					new ChunkPos(
							ChunkSectionPos.getSectionCoord(blockPositionX),
							ChunkSectionPos.getSectionCoord(blockPositionZ)
					)).getTopY());
		}
	}
}
