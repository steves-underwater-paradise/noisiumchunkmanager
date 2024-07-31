package io.github.steveplays28.noisiumchunkmanager.mixin.client.gui.hud;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
	@Shadow
	private @Nullable ChunkPos pos;

	@Shadow
	protected abstract @Nullable ServerWorld getServerWorld();

	@Inject(method = "getChunk", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getChunkFromNoisiumServerWorldChunkManager(@NotNull CallbackInfoReturnable<WorldChunk> cir) {
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

	@Inject(method = "getLeftText", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/DebugHud;getChunk()Lnet/minecraft/world/chunk/WorldChunk;"))
	private void e(@NotNull CallbackInfoReturnable<List<String>> cir, @Local @NotNull List<String> leftText, @Local @NotNull BlockPos clientCameraBlockPosition) {
		@Nullable var serverWorld = this.getServerWorld();
		if (serverWorld == null) {
			return;
		}

		leftText.add(String.format(
				"Server Light: %s (%s sky, %s block)",
				serverWorld.getLightingProvider().getLight(clientCameraBlockPosition, 0),
				serverWorld.getLightLevel(LightType.SKY, clientCameraBlockPosition),
				serverWorld.getLightLevel(LightType.BLOCK, clientCameraBlockPosition)
		));
	}
}
