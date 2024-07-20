package io.github.steveplays28.noisiumchunkmanager.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.steveplays28.noisiumchunkmanager.config.NoisiumChunkManagerConfig;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin<T> {
	@Shadow
	@Final
	private static Logger LOGGER;
	@Shadow
	@Final
	public static int START_TICKET_CHUNK_RADIUS;

	@Shadow
	private long timeReference;

	@Shadow
	protected abstract void updateMobSpawnOptions();

	@Shadow
	public abstract ServerWorld getOverworld();

	@Shadow
	protected abstract void runTasksTillTickEnd();

	/**
	 * @author Steveplays28
	 * @reason Wait for the overworld's spawn chunk to be loaded.
	 */
	@Overwrite
	private void prepareStartRegion(@NotNull WorldGenerationProgressListener worldGenerationProgressListener) {
		@NotNull var overworld = this.getOverworld();
		LOGGER.info("Preparing start region for dimension {}", overworld.getRegistryKey().getValue());
		worldGenerationProgressListener.start();
		this.timeReference = Util.getMeasuringTimeMs();

		@NotNull var noisiumServerWorldChunkManager = ((ServerWorldExtension) overworld).noisiumchunkmanager$getServerWorldChunkManager();
		@NotNull var overworldSpawnChunkPosition = new ChunkPos(overworld.getSpawnPos());
		if (NoisiumChunkManagerConfig.HANDLER.instance().loadSpawnChunks) {
			overworld.getChunkManager().addTicket(
					ChunkTicketType.START, overworldSpawnChunkPosition, START_TICKET_CHUNK_RADIUS, Unit.INSTANCE);
		} else {
			noisiumServerWorldChunkManager.getChunkAsync(overworldSpawnChunkPosition);
		}

		while (!noisiumServerWorldChunkManager.isChunkLoaded(overworldSpawnChunkPosition)) {
			this.timeReference = Util.getMeasuringTimeMs() + 10L;
			this.runTasksTillTickEnd();
		}

		worldGenerationProgressListener.stop();
		this.updateMobSpawnOptions();
	}

	@Redirect(method = "shutdown", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;anyMatch(Ljava/util/function/Predicate;)Z"))
	private boolean noisiumchunkmanager$shutdownRedirectThreadedAnvilChunkStorageShouldDelayShutdown(@NotNull Stream<ServerWorld> instance, @NotNull Predicate<? super T> predicate) {
		return false;
	}

	@Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorlds()Ljava/lang/Iterable;", shift = At.Shift.BEFORE), cancellable = true)
	private void noisiumchunkmanager$saveCancelThreadedAnvilChunkStorageLogging(boolean suppressLogs, boolean flush, boolean force, @NotNull CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 3) boolean bl) {
		cir.setReturnValue(bl);
	}
}
