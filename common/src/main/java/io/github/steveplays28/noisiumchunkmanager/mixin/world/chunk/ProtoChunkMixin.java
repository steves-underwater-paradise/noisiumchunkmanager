package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ProtoChunk.class)
public abstract class ProtoChunkMixin {
	@Shadow
	public abstract @NotNull HeightLimitView getHeightLimitView();

	@WrapOperation(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;setSectionStatus(Lnet/minecraft/util/math/BlockPos;Z)V"))
	private void noisiumchunkmanager$redirectSetSectionStatusToServerWorldLightingProvider(@Nullable LightingProvider instance, @NotNull BlockPos blockPosition, boolean notReady, @NotNull Operation<Void> original) {
		if (getHeightLimitView() instanceof ServerWorld serverWorld) {
			((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldLightingProvider().setSectionStatus(
					blockPosition, notReady);
			return;
		}

		original.call(instance, blockPosition, notReady);
	}

	@WrapOperation(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/light/LightingProvider;checkBlock(Lnet/minecraft/util/math/BlockPos;)V"))
	private void noisiumchunkmanager$redirectCheckBlockToServerWorldLightingProvider(@Nullable LightingProvider instance, @NotNull BlockPos blockPosition, @NotNull Operation<Void> original) {
		if (getHeightLimitView() instanceof ServerWorld serverWorld) {
			((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldLightingProvider().checkBlock(blockPosition);
			return;
		}

		original.call(instance, blockPosition);
	}
}
