package io.github.steveplays28.noisiumchunkmanager.mixin.world;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.light.ChunkLightingView;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
	@Redirect(method = "serialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;getBottomY()I"))
	private static int noisiumchunkmanager$getBottomYFromServerWorld(@Nullable LightingProvider instance, @Local(ordinal = 0, argsOnly = true) @NotNull ServerWorld serverWorld) {
		return serverWorld.getBottomY() - 1;
	}

	@Redirect(method = "serialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;getTopY()I"))
	private static int noisiumchunkmanager$getTopYFromServerWorld(@Nullable LightingProvider instance, @Local(ordinal = 0, argsOnly = true) @NotNull ServerWorld serverWorld) {
		return serverWorld.countVerticalSections() + 2;
	}

	@Redirect(method = "serialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;get(Lnet/minecraft/world/LightType;)Lnet/minecraft/world/chunk/light/ChunkLightingView;"))
	private static @NotNull ChunkLightingView noisiumchunkmanager$getChunkLightingViewFromServerWorldLightingProvider(@Nullable LightingProvider instance, @NotNull LightType lightType, @Local(ordinal = 0, argsOnly = true) @NotNull ServerWorld serverWorld) {
		return ((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldLightingProvider().getChunkLightingView(lightType);
	}

	@Redirect(method = "deserialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;setRetainData(Lnet/minecraft/util/math/ChunkPos;Z)V"))
	private static void noisiumchunkmanager$redirectRetainLightingDataToServerWorldLightingProvider(@Nullable LightingProvider instance, @NotNull ChunkPos chunkPosition, boolean retainLightingData, @Local(ordinal = 0, argsOnly = true) @NotNull ServerWorld serverWorld) {
		((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldLightingProvider().setRetainData(
				chunkPosition, retainLightingData);
	}

	@Redirect(method = "deserialize", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;enqueueSectionData(Lnet/minecraft/world/LightType;Lnet/minecraft/util/math/ChunkSectionPos;Lnet/minecraft/world/chunk/ChunkNibbleArray;)V"))
	private static void noisiumchunkmanager$redirectEnqueueSectionDataToServerWorldLightingProvider(@Nullable LightingProvider instance, @NotNull LightType lightType, @NotNull ChunkSectionPos chunkSectionPosition, @NotNull ChunkNibbleArray chunkLightingDataNibbleArray, @Local(ordinal = 0, argsOnly = true) @NotNull ServerWorld serverWorld) {
		((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldLightingProvider().enqueueSectionData(
				lightType, chunkSectionPosition, chunkLightingDataNibbleArray);
	}
}
