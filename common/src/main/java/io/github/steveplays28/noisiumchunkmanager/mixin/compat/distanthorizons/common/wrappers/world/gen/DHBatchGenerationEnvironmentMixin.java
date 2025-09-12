package io.github.steveplays28.noisiumchunkmanager.mixin.compat.distanthorizons.common.wrappers.world.gen;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BatchGenerationEnvironment.class)
public class DHBatchGenerationEnvironmentMixin {
	@Redirect(method = "getChunkNbtData", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ChunkMap;worker:Lnet/minecraft/world/level/chunk/storage/IOWorker;", opcode = Opcodes.GETFIELD))
	private @NotNull IOWorker noisiumchunkmanager$getIoWorkerFromNoisiumServerWorldChunkManager(@Nullable ChunkMap instance, @Local(ordinal = 0) @NotNull ServerLevel serverWorld) {
		return (IOWorker) ((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager().getChunkIoWorker();
	}
}
