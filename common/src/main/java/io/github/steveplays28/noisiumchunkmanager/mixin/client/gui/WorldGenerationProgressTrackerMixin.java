package io.github.steveplays28.noisiumchunkmanager.mixin.client.gui;

import io.github.steveplays28.noisiumchunkmanager.server.world.chunk.event.ServerChunkEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.WorldGenerationProgressTracker;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(WorldGenerationProgressTracker.class)
public abstract class WorldGenerationProgressTrackerMixin {
	@Shadow
	public abstract void setChunkStatus(@NotNull ChunkPos chunkPosition, @Nullable ChunkStatus chunkStatus);

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void noisiumchunkmanager$registerEventListeners(@NotNull CallbackInfo ci) {
		ServerChunkEvent.WORLD_CHUNK_GENERATED.register(worldChunk -> this.setChunkStatus(worldChunk.getPos(), worldChunk.getStatus()));
	}
}