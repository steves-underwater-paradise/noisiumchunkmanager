package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
	@WrapOperation(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;retainData(Lnet/minecraft/world/level/ChunkPos;Z)V"))
	private static void noisiumchunkmanager$retainDataInLevelLightEngine(LevelLightEngine instance, @NotNull ChunkPos chunkPosition, boolean retainData, Operation<Void> original,
			@Local(argsOnly = true) @NotNull ServerLevel serverLevel) {
		serverLevel.getLightEngine().retainData(chunkPosition, retainData);
	}

	@WrapOperation(method = "read", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;queueSectionData(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/chunk/DataLayer;)V"))
	private static void noisiumchunkmanager$queueSectionDataInLevelLightEngine(LevelLightEngine instance, @NotNull LightLayer type, @NotNull SectionPos sectionPosition, @NotNull DataLayer dataLayer,
			Operation<Void> original, @Local(argsOnly = true) @NotNull ServerLevel serverLevel) {
		serverLevel.getLightEngine().queueSectionData(type, sectionPosition, dataLayer);
	}

	@WrapOperation(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ProtoChunk;setLightEngine(Lnet/minecraft/world/level/lighting/LevelLightEngine;)V"))
	private static void noisiumchunkmanager$setProtoChunkLightEngineToLevelLightEngine(@NotNull ProtoChunk instance, LevelLightEngine levelLightEngine, Operation<Void> original,
			@Local(argsOnly = true) @NotNull ServerLevel serverLevel) {
		instance.setLightEngine(serverLevel.getLightEngine());
	}

	@WrapOperation(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;getMinLightSection()I"))
	private static int noisiumchunkmanager$getMinLightSectionFromLevelLightEngine(LevelLightEngine instance, Operation<Integer> original, @Local(argsOnly = true) @NotNull ServerLevel serverLevel) {
		return serverLevel.getLightEngine().getMinLightSection();
	}

	@WrapOperation(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;getMaxLightSection()I"))
	private static int noisiumchunkmanager$getMaxLightSectionFromLevelLightEngine(LevelLightEngine instance, Operation<Integer> original, @Local(argsOnly = true) @NotNull ServerLevel serverLevel) {
		return serverLevel.getLightEngine().getMaxLightSection();
	}

	@WrapOperation(method = "write", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;getLayerListener(Lnet/minecraft/world/level/LightLayer;)Lnet/minecraft/world/level/lighting/LayerLightEventListener;"))
	private static @NotNull LayerLightEventListener noisiumchunkmanager$getLayerLightEventListenerFromLevelLightEngine(LevelLightEngine instance, @NotNull LightLayer type,
			Operation<LayerLightEventListener> original, @Local(argsOnly = true) @NotNull ServerLevel serverLevel) {
		return serverLevel.getLightEngine().getLayerListener(type);
	}
}
