package io.github.steveplays28.noisiumchunkmanager.mixin.client.gui.hud;

import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(DebugScreenOverlay.class)
public abstract class DebugHudMixin {
	@Shadow
	@Nullable
	protected abstract ServerLevel getServerLevel();

	@Shadow
	@Nullable
	private ChunkPos lastPos;

	@Inject(method = "getServerChunk", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkFromNoisiumServerWorldChunkManager(CallbackInfoReturnable<LevelChunk> cir) {
		@Nullable var serverWorld = this.getServerLevel();
		@Nullable var playerChunkPosition = this.lastPos;
		if (serverWorld == null || playerChunkPosition == null) {
			cir.setReturnValue(null);
			return;
		}

		@NotNull var noisiumServerWorldChunkManager = ((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager();
		if (!noisiumServerWorldChunkManager.isChunkLoaded(playerChunkPosition)) {
			cir.setReturnValue(null);
			return;
		}

		noisiumServerWorldChunkManager.getChunk(playerChunkPosition);
	}
}
