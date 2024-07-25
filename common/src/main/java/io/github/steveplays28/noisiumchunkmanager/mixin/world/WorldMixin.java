package io.github.steveplays28.noisiumchunkmanager.mixin.world;

import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = World.class, priority = 200)
public abstract class WorldMixin {
	@Shadow
	public abstract boolean isClient();

	@Inject(method = "getLightingProvider", at = @At(value = "RETURN"), cancellable = true)
	private void noisiumchunkmanager$getServerWorldLightingProvider(@NotNull CallbackInfoReturnable<LightingProvider> cir) {
		if (this.isClient()) {
			return;
		}

		cir.setReturnValue(((ServerWorldExtension) this).noisiumchunkmanager$getServerWorldLightingProvider());
	}

	@Inject(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/Chunk;", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkFromNoisiumServerChunkManager(int chunkPositionX, int chunkPositionZ, @NotNull ChunkStatus leastStatus, boolean create, @NotNull CallbackInfoReturnable<Chunk> cir) {
		if (this.isClient()) {
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

	@Inject(method = "getBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getChunk(II)Lnet/minecraft/world/chunk/WorldChunk;"), cancellable = true)
	private void noisiumchunkmanager$getBlockStateGetChunkFromNoisiumServerChunkManager(@NotNull BlockPos blockPosition, @NotNull CallbackInfoReturnable<BlockState> cir) {
		if (this.isClient()) {
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

	@Inject(method = "getFluidState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getWorldChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/WorldChunk;"), cancellable = true)
	private void noisiumchunkmanager$getFluidStateGetChunkFromNoisiumServerChunkManager(@NotNull BlockPos blockPosition, @NotNull CallbackInfoReturnable<FluidState> cir) {
		if (this.isClient()) {
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
