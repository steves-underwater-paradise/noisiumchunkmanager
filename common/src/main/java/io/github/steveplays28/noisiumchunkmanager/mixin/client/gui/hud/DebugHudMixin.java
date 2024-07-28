package io.github.steveplays28.noisiumchunkmanager.mixin.client.gui.hud;

import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
	@Shadow
	protected abstract @Nullable ServerWorld getServerWorld();

	@Shadow
	private @Nullable ChunkPos pos;

	@Inject(method = "getChunk", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkFromNoisiumServerWorldChunkManager(CallbackInfoReturnable<WorldChunk> cir) {
		@Nullable var serverWorld = this.getServerWorld();
		@Nullable var playerChunkPosition = this.pos;
		if (serverWorld == null || playerChunkPosition == null) {
			cir.setReturnValue(null);
			return;
		}

		@NotNull var noisiumServerWorldChunkManager = ((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager();
		if (!noisiumServerWorldChunkManager.isChunkLoaded(playerChunkPosition)) {
			cir.setReturnValue(null);
			return;
		}

		cir.setReturnValue(noisiumServerWorldChunkManager.getChunk(playerChunkPosition));
	}
}
