package io.github.steveplays28.noisiumchunkmanager.mixin.world;

import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Level.class, priority = 200)
public abstract class WorldMixin {
	@Shadow
	public abstract boolean isClientSide();

	@Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkFromNoisiumServerChunkManager(int chunkPositionX, int chunkPositionZ, @NotNull ChunkStatus leastStatus, boolean create, @NotNull CallbackInfoReturnable<ChunkAccess> cir) {
		if (this.isClientSide()) {
			return;
		}

		var noisiumServerWorldChunkManager = ((ServerWorldExtension) this).noisiumchunkmanager$getServerWorldChunkManager();
		var chunkPosition = new ChunkPos(chunkPositionX, chunkPositionZ);
		if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
			cir.setReturnValue(noisiumServerWorldChunkManager.getIoWorldChunk(chunkPosition));
			return;
		}

		cir.setReturnValue(noisiumServerWorldChunkManager.getChunk(chunkPosition));
	}

	@Inject(method = "getBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;"), cancellable = true)
	private void noisiumchunkmanager$getBlockStateGetChunkFromNoisiumServerChunkManager(@NotNull BlockPos blockPosition, @NotNull CallbackInfoReturnable<BlockState> cir) {
		if (this.isClientSide()) {
			return;
		}

		var noisiumServerWorldChunkManager = ((ServerWorldExtension) this).noisiumchunkmanager$getServerWorldChunkManager();
		var chunkPosition = new ChunkPos(blockPosition);
		if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
			cir.setReturnValue(noisiumServerWorldChunkManager.getIoWorldChunk(chunkPosition).getBlockState(blockPosition));
			return;
		}

		cir.setReturnValue(noisiumServerWorldChunkManager.getChunk(chunkPosition).getBlockState(blockPosition));
	}

	@Inject(method = "getFluidState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getChunkAt(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/chunk/LevelChunk;"), cancellable = true)
	private void noisiumchunkmanager$getFluidStateGetChunkFromNoisiumServerChunkManager(@NotNull BlockPos blockPosition, @NotNull CallbackInfoReturnable<FluidState> cir) {
		if (this.isClientSide()) {
			return;
		}

		var noisiumServerWorldChunkManager = ((ServerWorldExtension) this).noisiumchunkmanager$getServerWorldChunkManager();
		var chunkPosition = new ChunkPos(blockPosition);
		if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
			cir.setReturnValue(noisiumServerWorldChunkManager.getIoWorldChunk(chunkPosition).getFluidState(blockPosition));
			return;
		}

		cir.setReturnValue(noisiumServerWorldChunkManager.getChunk(chunkPosition).getFluidState(blockPosition));
	}
}
