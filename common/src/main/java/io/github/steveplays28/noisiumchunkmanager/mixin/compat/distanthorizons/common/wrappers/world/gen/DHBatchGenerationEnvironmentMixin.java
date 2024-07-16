package io.github.steveplays28.noisiumchunkmanager.mixin.compat.distanthorizons.common.wrappers.world.gen;

import com.llamalad7.mixinextras.sugar.Local;
import loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.worldGeneration.BatchGenerationEnvironment;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.storage.StorageIoWorker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BatchGenerationEnvironment.class)
public class DHBatchGenerationEnvironmentMixin {
	@Redirect(method = "getChunkNbtData", at = @At(value = "FIELD", target = "Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;worker:Lnet/minecraft/world/storage/StorageIoWorker;", opcode = Opcodes.GETFIELD))
	private @NotNull StorageIoWorker noisiumchunkmanager$getIoWorkerFromNoisiumServerWorldChunkManager(@Nullable ThreadedAnvilChunkStorage instance, @Local(ordinal = 0) @NotNull ServerWorld serverWorld) {
		return (StorageIoWorker) ((io.github.steveplays28.noisiumchunkmanager.experimental.extension.world.server.ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager().getChunkIoWorker();
	}
}
