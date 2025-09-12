package io.github.steveplays28.noisiumchunkmanager.mixin.server;

import io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk.ServerChunkEvent;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoggerChunkProgressListener.class)
public abstract class WorldGenerationProgressLoggerMixin {
	@Shadow
	public abstract void onStatusChange(@NotNull ChunkPos chunkPosition, @Nullable ChunkStatus chunkStatus);

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void noisiumchunkmanager$registerEventListeners(@NotNull CallbackInfo ci) {
		ServerChunkEvent.WORLD_CHUNK_LOADED.register((instance, worldChunk) -> {
			if (!instance.dimensionType().effectsLocation().equals(BuiltinDimensionTypes.OVERWORLD_EFFECTS)) {
				return;
			}

			this.onStatusChange(worldChunk.getPos(), worldChunk.getStatus());
		});
	}
}
