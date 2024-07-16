package io.github.steveplays28.noisiumchunkmanager.mixin.server.world;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DataFixer;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import io.github.steveplays28.noisiumchunkmanager.util.world.chunk.networking.packet.PacketUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ChunkLevelType;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.steveplays28.noisiumchunkmanager.extension.world.server.ServerWorldExtension;
import io.github.steveplays28.noisiumchunkmanager.server.world.entity.ServerWorldEntityTracker;
import io.github.steveplays28.noisiumchunkmanager.server.world.entity.player.ServerWorldPlayerChunkLoader;
import io.github.steveplays28.noisiumchunkmanager.server.world.chunk.event.ServerChunkEvent;

import java.util.List;
import java.util.concurrent.Executor;

@Debug(export = true)
@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin implements ServerWorldExtension {
	@Shadow
	@Final
	private ServerEntityManager<Entity> entityManager;

	@Shadow
	public abstract boolean isChunkLoaded(long chunkPos);

	@Shadow
	public abstract void tickEntity(Entity entity);

	@Shadow
	public abstract @NotNull MinecraftServer getServer();

	@Unique
	private NoiseConfig noisiumchunkmanager$noiseConfig;
	/**
	 * Keeps a reference to this {@link ServerWorld}'s {@link ServerWorldChunkManager}, to make sure it doesn't get garbage collected until the object is no longer necessary.
	 */
	@Unique
	private ServerWorldChunkManager noisiumchunkmanager$serverWorldChunkManager;
	/**
	 * Keeps a reference to this {@link ServerWorld}'s {@link ServerWorldEntityTracker}, to make sure it doesn't get garbage collected until the object is no longer necessary.
	 */
	@SuppressWarnings({"unused", "FieldCanBeLocal"})
	@Unique
	private ServerWorldEntityTracker noisiumchunkmanager$serverWorldEntityManager;
	/**
	 * Keeps a reference to this {@link ServerWorld}'s {@link ServerWorldPlayerChunkLoader}, to make sure it doesn't get garbage collected until the object is no longer necessary.
	 */
	@SuppressWarnings({"unused", "FieldCanBeLocal"})
	@Unique
	private ServerWorldPlayerChunkLoader noisiumchunkmanager$serverWorldPlayerChunkLoader;

	@Inject(method = "<init>", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/server/MinecraftServer;getDataFixer()Lcom/mojang/datafixers/DataFixer;", shift = At.Shift.AFTER))
	private void noisiumchunkmanager$constructorCreateServerWorldChunkManager(@NotNull MinecraftServer server, Executor workerExecutor, @NotNull LevelStorage.Session session, @NotNull ServerWorldProperties serverWorldProperties, @NotNull RegistryKey<World> worldKey, @NotNull DimensionOptions dimensionOptions, WorldGenerationProgressListener worldGenerationProgressListener, boolean debugWorld, long seed, List<?> spawners, boolean shouldTickTime, RandomSequencesState randomSequencesState, @NotNull CallbackInfo ci, @Local @NotNull DataFixer dataFixer) {
		@SuppressWarnings("DataFlowIssue")
		var serverWorld = ((ServerWorld) (Object) this);
		@NotNull ChunkGenerator chunkGenerator = dimensionOptions.chunkGenerator();
		@NotNull ChunkGeneratorSettings chunkGeneratorSettings;
		if (chunkGenerator instanceof NoiseChunkGenerator noiseChunkGenerator) {
			chunkGeneratorSettings = noiseChunkGenerator.getSettings().value();
		} else {
			chunkGeneratorSettings = ChunkGeneratorSettings.createMissingSettings();
		}
		noisiumchunkmanager$noiseConfig = NoiseConfig.create(
				chunkGeneratorSettings, serverWorld.getRegistryManager().getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS),
				serverWorld.getSeed()
		);
		noisiumchunkmanager$serverWorldChunkManager = new ServerWorldChunkManager(
				serverWorld, chunkGenerator, noisiumchunkmanager$noiseConfig, session.getWorldDirectory(worldKey), dataFixer);
		noisiumchunkmanager$serverWorldEntityManager = new ServerWorldEntityTracker(
				packet -> PacketUtil.sendPacketToPlayers(serverWorld.getPlayers(), packet));
		noisiumchunkmanager$serverWorldPlayerChunkLoader = new ServerWorldPlayerChunkLoader(
				serverWorld, noisiumchunkmanager$serverWorldChunkManager::getChunksInRadiusAsync,
				noisiumchunkmanager$serverWorldChunkManager::getChunkAsync,
				noisiumchunkmanager$serverWorldChunkManager::unloadChunk, server.getPlayerManager()::getViewDistance
		);

		// TODO: Redo the server entity manager entirely, in an event-based way
		//  Also remove this line when that's done, since this doesn't belong here
		PlayerEvent.PLAYER_JOIN.register(player -> {
			if (!player.getWorld().equals(serverWorld)) {
				return;
			}

			this.entityManager.addEntity(player);
		});

		// TODO: Move this event listener registration to ServerEntityManagerMixin
		//  or (when it's finished and able to completely replace the vanilla class) to NoisiumServerWorldEntityTracker
		//  More efficient methods can be used when registering the event listener directly in the server entity manager
		ServerChunkEvent.WORLD_CHUNK_GENERATED.register(worldChunk -> server.executeSync(
				() -> this.entityManager.updateTrackingStatus(worldChunk.getPos(), ChunkLevelType.ENTITY_TICKING)));
		LifecycleEvent.SERVER_STOPPED.register(instance -> {
			noisiumchunkmanager$serverWorldPlayerChunkLoader = null;
			noisiumchunkmanager$serverWorldEntityManager = null;
			noisiumchunkmanager$serverWorldChunkManager = null;
			noisiumchunkmanager$noiseConfig = null;
		});
	}

	@Inject(method = "getPersistentStateManager", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getPersistentStateManagerFromNoisiumServerWorldChunkManager(@NotNull CallbackInfoReturnable<PersistentStateManager> cir) {
		cir.setReturnValue(((ServerWorldExtension) this).noisiumchunkmanager$getServerWorldChunkManager().getPersistentStateManager());
	}

	@Inject(method = "isTickingFutureReady", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$checkIfTickingFutureIsReadyByCheckingIfTheChunkIsLoaded(long chunkPos, @NotNull CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(this.isChunkLoaded(chunkPos));
	}

	@Inject(method = "method_31420", at = @At(value = "FIELD", target = "Lnet/minecraft/server/world/ServerChunkManager;threadedAnvilChunkStorage:Lnet/minecraft/server/world/ThreadedAnvilChunkStorage;", opcode = Opcodes.GETFIELD), cancellable = true)
	private void noisiumchunkmanager$redirectShouldTickEntities(@NotNull Profiler profiler, @NotNull Entity entity, @NotNull CallbackInfo ci) {
		if (!this.entityManager.shouldTick(entity.getChunkPos())) {
			ci.cancel();
			return;
		}

		var vehicleEntity = entity.getVehicle();
		if (vehicleEntity != null) {
			if (!vehicleEntity.isRemoved() && vehicleEntity.hasPassenger(entity)) {
				ci.cancel();
				return;
			}

			entity.stopRiding();
		}

		profiler.push("tick");
		this.tickEntity(entity);
		profiler.pop();
		ci.cancel();
	}

	@Inject(method = "onBlockChanged", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$redirectOnBlockChangedToNoisiumServerWorldChunkManager(BlockPos blockPos, BlockState oldBlockState, BlockState newBlockState, CallbackInfo ci) {
		ServerChunkEvent.BLOCK_CHANGE.invoker().onBlockChange(blockPos, oldBlockState, newBlockState);
		ci.cancel();
	}

	@Override
	public ServerWorldChunkManager noisiumchunkmanager$getServerWorldChunkManager() {
		return noisiumchunkmanager$serverWorldChunkManager;
	}

	@Override
	public NoiseConfig noisiumchunkmanager$getNoiseConfig() {
		return noisiumchunkmanager$noiseConfig;
	}
}
