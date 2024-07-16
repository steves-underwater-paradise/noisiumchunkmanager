package io.github.steveplays28.noisiumchunkmanager.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin<T> {
//	@Shadow
//	public abstract ServerWorld getOverworld();
//
//	@Redirect(method = "prepareStartRegion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;addTicket(Lnet/minecraft/server/world/ChunkTicketType;Lnet/minecraft/util/math/ChunkPos;ILjava/lang/Object;)V"))
//	private void noisiumchunkmanager$prepareStartRegionAddTicketForSpawnChunksToNoisiumServerWorldChunkManager(ServerChunkManager instance, ChunkTicketType<T> ticketType, ChunkPos chunkPos, int radius, T argument) {
//		((NoisiumServerWorldExtension) getOverworld()).noisiumchunkmanager$getServerWorldChunkManager().getChunksInRadius(chunkPos, radius);
//	}
//
//	@Redirect(method = "prepareStartRegion", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;getTotalChunksLoadedCount()I"))
//	private int noisiumchunkmanager$prepareStartRegionRedirectTotalLoadedChunksToNoisiumServerWorldChunkManager(ServerChunkManager instance) {
//		// TODO: Make the loadedWorldChunks field private again and remove this whole vanilla Minecraft system
//		return ((NoisiumServerWorldExtension) getOverworld()).noisiumchunkmanager$getServerWorldChunkManager().loadedWorldChunks.size();
//	}
//
//	@ModifyConstant(method = "prepareStartRegion", constant = @Constant(intValue = 441))
//	private int noisiumchunkmanager$prepareStartRegionChangeTheAmountOfSpawnChunks(int original) {
//		return 484;
//	}

	@Redirect(method = "shutdown", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;anyMatch(Ljava/util/function/Predicate;)Z"))
	private boolean noisiumchunkmanager$shutdownRedirectThreadedAnvilChunkStorageShouldDelayShutdown(@NotNull Stream<ServerWorld> instance, @NotNull Predicate<? super T> predicate) {
		return false;
	}

	@Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorlds()Ljava/lang/Iterable;", shift = At.Shift.BEFORE), cancellable = true)
	private void noisiumchunkmanager$saveCancelThreadedAnvilChunkStorageLogging(boolean suppressLogs, boolean flush, boolean force, @NotNull CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 3) boolean bl) {
		cir.setReturnValue(bl);
	}
}
