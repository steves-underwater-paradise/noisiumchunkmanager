package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk;

import io.github.steveplays28.noisiumchunkmanager.extension.world.chunk.WorldChunkExtension;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import java.util.BitSet;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;

@Mixin(LevelChunk.class)
public class WorldChunkMixin implements WorldChunkExtension {
	@Shadow @Final Level level;

	@Unique private final BitSet noisiumchunkmanager$blockLightBits = new BitSet();
	@Unique private final BitSet noisiumchunkmanager$skyLightBits = new BitSet();

	@Override
	public @NotNull BitSet noisiumchunkmanager$getBlockLightBits() {
		return noisiumchunkmanager$blockLightBits;
	}

	@Override
	public @NotNull BitSet noisiumchunkmanager$getSkyLightBits() {
		return noisiumchunkmanager$skyLightBits;
	}

	@WrapOperation(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;updateSectionStatus(Lnet/minecraft/core/BlockPos;Z)V"))
	public void noisiumchunkmanager$updateSectionStatusInLevelLightEngine(LevelLightEngine instance, BlockPos blockPosition, boolean isQueueEmpty, Operation<Void> original) {
		this.level.getLightEngine().updateSectionStatus(blockPosition, isQueueEmpty);
	}

	@WrapOperation(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/lighting/LevelLightEngine;checkBlock(Lnet/minecraft/core/BlockPos;)V"))
	public void noisiumchunkmanager$checkBlockInLevelLightEngine(LevelLightEngine instance, BlockPos blockPosition, Operation<Void> original) {
		this.level.getLightEngine().checkBlock(blockPosition);
	}
}
