package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.lighting.LayerLightEventListener;
import net.minecraft.world.level.lighting.LevelLightEngine;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
	@WrapOperation(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;getMinLightSection()I"))
	private static int noisiumchunkmanager$getMinLightSectionFromLevelLightEngine(LevelLightEngine instance, Operation<Integer> original, @Local @NotNull ServerLevel serverLevel) {
		return serverLevel.getLightEngine().getMinLightSection();
	}

	@WrapOperation(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;getMaxLightSection()I"))
	private static int noisiumchunkmanager$getMaxLightSectionFromLevelLightEngine(LevelLightEngine instance, Operation<Integer> original, @Local @NotNull ServerLevel serverLevel) {
		return serverLevel.getLightEngine().getMaxLightSection();
	}

	@WrapOperation(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;getLayerListener(Lnet/minecraft/world/level/LightLayer;)Lnet/minecraft/world/level/lighting/LayerLightEventListener;"))
	private static @NotNull LayerLightEventListener noisiumchunkmanager$getLayerLightEventListenerFromLevelLightEngine(LevelLightEngine instance, @NotNull LightLayer type, Operation<LayerLightEventListener> original, @Local @NotNull ServerLevel serverLevel) {
		return serverLevel.getLightEngine().getLayerListener(type);
	}
}
