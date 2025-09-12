package io.github.steveplays28.noisiumchunkmanager.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.steveplays28.noisiumchunkmanager.config.NoisiumChunkManagerConfig;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.Unit;
import net.minecraft.world.level.ChunkPos;
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
	public static int START_CHUNK_RADIUS;

	@Shadow
	private long nextTickTime;

	@Shadow
	protected abstract void updateMobSpawningFlags();

	@Shadow
	public abstract ServerLevel overworld();

	@Shadow
	protected abstract void waitUntilNextTick();

	/**
	 * @author Steveplays28
	 * @reason Wait for the overworld's spawn chunk to be loaded.
	 */
	@Overwrite
	private void prepareLevels(@NotNull ChunkProgressListener worldGenerationProgressListener) {
		@NotNull var overworld = this.overworld();
		LOGGER.info("Preparing start region for dimension {}", overworld.dimension().location());
		worldGenerationProgressListener.start();
		this.nextTickTime = Util.getMillis();

		@NotNull var noisiumServerWorldChunkManager = ((ServerWorldExtension) overworld).noisiumchunkmanager$getServerWorldChunkManager();
		@NotNull var overworldSpawnChunkPosition = new ChunkPos(overworld.getSharedSpawnPos());
		if (NoisiumChunkManagerConfig.HANDLER.instance().loadSpawnChunks) {
			overworld.getChunkSource().addRegionTicket(
					TicketType.START, overworldSpawnChunkPosition, START_CHUNK_RADIUS, Unit.INSTANCE);
		} else {
			noisiumServerWorldChunkManager.getChunkAsync(overworldSpawnChunkPosition);
		}

		while (!noisiumServerWorldChunkManager.isChunkLoaded(overworldSpawnChunkPosition)) {
			this.nextTickTime = Util.getMillis() + 10L;
			this.waitUntilNextTick();
		}

		worldGenerationProgressListener.stop();
		this.updateMobSpawningFlags();
	}

	@Redirect(method = "stopServer", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;anyMatch(Ljava/util/function/Predicate;)Z"))
	private boolean noisiumchunkmanager$shutdownRedirectThreadedAnvilChunkStorageShouldDelayShutdown(@NotNull Stream<ServerLevel> instance, @NotNull Predicate<? super T> predicate) {
		return false;
	}

	@Inject(method = "saveAllChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getAllLevels()Ljava/lang/Iterable;", shift = At.Shift.BEFORE), cancellable = true)
	private void noisiumchunkmanager$saveCancelThreadedAnvilChunkStorageLogging(boolean suppressLogs, boolean flush, boolean force, @NotNull CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 3) boolean bl) {
		cir.setReturnValue(bl);
	}
}
