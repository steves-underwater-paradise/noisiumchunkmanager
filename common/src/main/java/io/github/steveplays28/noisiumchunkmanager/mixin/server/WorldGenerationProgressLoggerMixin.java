package io.github.steveplays28.noisiumchunkmanager.mixin.server;

import io.github.steveplays28.noisiumchunkmanager.server.world.chunk.event.ServerChunkEvent;
import net.minecraft.server.WorldGenerationProgressLogger;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldGenerationProgressLogger.class)
public abstract class WorldGenerationProgressLoggerMixin {
	@Shadow
	public abstract void setChunkStatus(@NotNull ChunkPos chunkPosition, @Nullable ChunkStatus chunkStatus);

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void noisiumchunkmanager$registerEventListeners(@NotNull CallbackInfo ci) {
		ServerChunkEvent.WORLD_CHUNK_GENERATED.register(worldChunk -> this.setChunkStatus(worldChunk.getPos(), worldChunk.getStatus()));
	}
}