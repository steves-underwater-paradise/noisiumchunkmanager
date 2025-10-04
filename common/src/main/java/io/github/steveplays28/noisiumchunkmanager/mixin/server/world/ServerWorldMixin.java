package io.github.steveplays28.noisiumchunkmanager.mixin.server.world;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DataFixer;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import io.github.steveplays28.noisiumchunkmanager.server.world.ServerLevelLightEngine;
import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import io.github.steveplays28.noisiumchunkmanager.server.world.chunk.tick.ServerWorldChunkTicker;
import io.github.steveplays28.noisiumchunkmanager.server.world.ticket.ServerWorldTicketTracker;
import io.github.steveplays28.noisiumchunkmanager.util.networking.packet.PacketUtil;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import io.github.steveplays28.noisiumchunkmanager.server.world.entity.ServerWorldEntityTracker;
import io.github.steveplays28.noisiumchunkmanager.server.world.entity.player.ServerWorldPlayerChunkLoader;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk.ServerChunkEvent;

import java.util.List;
import java.util.concurrent.Executor;

@Debug(export = true)
@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin extends Level implements ServerWorldExtension {
	@Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;

	@Shadow
	public abstract boolean areEntitiesLoaded(long chunkPos);

	@Shadow
	public abstract void tickNonPassenger(Entity entity);

	@Shadow
	public abstract @NotNull MinecraftServer getServer();

	@Shadow
	public abstract @NotNull List<ServerPlayer> players();

	@Unique private RandomState noisiumchunkmanager$noiseConfig;
	@Unique private LevelLightEngine noisiumchunkmanager$lightEngine;
	/**
	 * Keeps a reference to this {@link ServerLevel}'s {@link ServerWorldChunkManager}, to make sure it doesn't get garbage collected until the object is no longer necessary.
	 */
	@Unique private ServerWorldChunkManager noisiumchunkmanager$serverWorldChunkManager;
	/**
	 * Keeps a reference to this {@link ServerLevel}'s {@link ServerWorldTicketTracker}, to make sure it doesn't get garbage collected until the object is no longer necessary.
	 */
	@SuppressWarnings("unused") @Unique private ServerWorldTicketTracker noisiumchunkmanager$serverWorldTicketTracker;
	/**
	 * Keeps a reference to this {@link ServerLevel}'s {@link ServerWorldChunkTicker}, to make sure it doesn't get garbage collected until the object is no longer necessary.
	 */
	@SuppressWarnings("unused") @Unique private ServerWorldChunkTicker noisiumchunkmanager$serverWorldChunkTicker;
	/**
	 * Keeps a reference to this {@link ServerLevel}'s {@link ServerWorldEntityTracker}, to make sure it doesn't get garbage collected until the object is no longer necessary.
	 */
	@SuppressWarnings("unused") @Unique private ServerWorldEntityTracker noisiumchunkmanager$serverWorldEntityManager;
	/**
	 * Keeps a reference to this {@link ServerLevel}'s {@link ServerWorldPlayerChunkLoader}, to make sure it doesn't get garbage collected until the object is no longer necessary.
	 */
	@SuppressWarnings("unused") @Unique private ServerWorldPlayerChunkLoader noisiumchunkmanager$serverWorldPlayerChunkLoader;

	public ServerWorldMixin(MinecraftServer minecraftServer, Executor executor, LevelStorageSource.LevelStorageAccess levelStorageAccess, ServerLevelData serverLevelData,
			ResourceKey<Level> resourceKey, LevelStem levelStem, ChunkProgressListener chunkProgressListener, boolean bl, long l, List<CustomSpawner> list, boolean bl2,
			@Nullable RandomSequences randomSequences) {
		super(serverLevelData, resourceKey, minecraftServer.registryAccess(), levelStem.type(), minecraftServer::getProfiler, false, bl, l, minecraftServer.getMaxChainedNeighborUpdates());
	}

	@Inject(method = "<init>", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/server/MinecraftServer;getFixerUpper()Lcom/mojang/datafixers/DataFixer;", shift = At.Shift.AFTER))
	private void noisiumchunkmanager$constructorCreateServerWorldChunkManager(@NotNull MinecraftServer server, Executor workerExecutor, @NotNull LevelStorageSource.LevelStorageAccess session,
			@NotNull ServerLevelData serverWorldProperties, @NotNull ResourceKey<Level> worldKey, @NotNull LevelStem dimensionOptions, ChunkProgressListener worldGenerationProgressListener,
			boolean debugWorld, long seed, List<?> spawners, boolean shouldTickTime, RandomSequences randomSequencesState, @NotNull CallbackInfo ci, @Local @NotNull DataFixer dataFixer) {
		@SuppressWarnings("DataFlowIssue") var serverWorld = ((ServerLevel) (Object) this);
		@NotNull ChunkGenerator chunkGenerator = dimensionOptions.generator();
		@NotNull NoiseGeneratorSettings chunkGeneratorSettings;
		if (chunkGenerator instanceof NoiseBasedChunkGenerator noiseChunkGenerator) {
			chunkGeneratorSettings = noiseChunkGenerator.generatorSettings().value();
		} else {
			chunkGeneratorSettings = NoiseGeneratorSettings.dummy();
		}
		noisiumchunkmanager$noiseConfig = RandomState.create(chunkGeneratorSettings, serverWorld.registryAccess().lookupOrThrow(Registries.NOISE), serverWorld.getSeed());
		noisiumchunkmanager$lightEngine = new ServerLevelLightEngine(serverWorld, new LightChunkGetter() {
			@Override
			public @NotNull BlockGetter getLevel() {
				return serverWorld;
			}

			@Override
			public @Nullable LightChunk getChunkForLighting(int chunkX, int chunkZ) {
				var noisiumServerWorldChunkManager = ((ServerWorldExtension) serverWorld).noisiumchunkmanager$getServerWorldChunkManager();
				var chunkPosition = new ChunkPos(chunkX, chunkZ);
				if (!noisiumServerWorldChunkManager.isChunkLoaded(chunkPosition)) {
					return null;
				}

				return noisiumServerWorldChunkManager.getChunk(chunkPosition);
			}

			@Override
			public void onLightUpdate(LightLayer lightLayer, SectionPos chunkSectionPosition) {
				ServerChunkEvent.LIGHT_UPDATE.invoker().onLightUpdate(serverWorld, lightLayer, chunkSectionPosition);
			}
		}, true, true);
		noisiumchunkmanager$serverWorldChunkManager =
				new ServerWorldChunkManager(serverWorld, chunkGenerator, noisiumchunkmanager$noiseConfig, this.getServer()::executeIfPossible, session.getDimensionPath(worldKey), dataFixer);
		noisiumchunkmanager$serverWorldTicketTracker =
				new ServerWorldTicketTracker(serverWorld, noisiumchunkmanager$serverWorldChunkManager::getChunksInRadiusAsync, noisiumchunkmanager$serverWorldChunkManager::unloadChunk);
		noisiumchunkmanager$serverWorldChunkTicker = new ServerWorldChunkTicker(serverWorld);
		noisiumchunkmanager$serverWorldEntityManager = new ServerWorldEntityTracker(packet -> PacketUtil.sendPacketToPlayers(serverWorld.players(), packet));
		noisiumchunkmanager$serverWorldPlayerChunkLoader = new ServerWorldPlayerChunkLoader(serverWorld, noisiumchunkmanager$serverWorldChunkManager::getChunksInRadiusAsync,
				noisiumchunkmanager$serverWorldChunkManager::getChunkAsync, noisiumchunkmanager$serverWorldChunkManager::unloadChunk, server.getPlayerList()::getViewDistance);

		// TODO: Redo the server entity manager entirely, in an event-based way
		// Also remove this line when that's done, since this doesn't belong here
		PlayerEvent.PLAYER_JOIN.register(player -> {
			if (!player.level().equals(serverWorld)) {
				return;
			}

			this.entityManager.addNewEntity(player);
		});

		// TODO: Move this event listener registration to ServerEntityManagerMixin
		// or (when it's finished and able to completely replace the vanilla class) to NoisiumServerWorldEntityTracker
		// More efficient methods can be used when registering the event listener directly in the server entity manager
		ServerChunkEvent.WORLD_CHUNK_LOADED.register((instance, worldChunk) -> {
			if (instance != serverWorld) {
				return;
			}

			server.executeIfPossible(() -> this.entityManager.updateChunkStatus(worldChunk.getPos(), FullChunkStatus.ENTITY_TICKING));
		});
		LifecycleEvent.SERVER_STOPPED.register(instance -> {
			noisiumchunkmanager$serverWorldPlayerChunkLoader = null;
			noisiumchunkmanager$serverWorldEntityManager = null;
			noisiumchunkmanager$serverWorldChunkTicker = null;
			noisiumchunkmanager$serverWorldTicketTracker = null;
			noisiumchunkmanager$serverWorldChunkManager = null;
			noisiumchunkmanager$noiseConfig = null;
		});
	}

	@Inject(method = "getDataStorage", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getPersistentStateManagerFromNoisiumServerWorldChunkManager(@NotNull CallbackInfoReturnable<DimensionDataStorage> cir) {
		cir.setReturnValue(((ServerWorldExtension) this).noisiumchunkmanager$getServerWorldChunkManager().getPersistentStateManager());
	}

	@Inject(method = "getPoiManager", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$getPointOfInterestManagerFromNoisiumServerWorldChunkManager(@NotNull CallbackInfoReturnable<PoiManager> cir) {
		cir.setReturnValue(((ServerWorldExtension) this).noisiumchunkmanager$getServerWorldChunkManager().getPointOfInterestManager());
	}

	// TODO: Move into LevelMixin using `instanceof ServerLevel`
	@Override
	public LevelLightEngine getLightEngine() {
		return noisiumchunkmanager$lightEngine;
	}

	// @Inject(method = "getLightEngine", at = @At(value = "HEAD"), cancellable = true)
	// private void noisiumchunkmanager$getLightEngineFromServerChunkManager(@NotNull CallbackInfoReturnable<LevelLightEngine> cir) {
	// cir.setReturnValue(noisiumchunkmanager$lightEngine);
	// }

	@Inject(method = "isPositionTickingWithEntitiesLoaded", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$checkIfTickingFutureIsReadyByCheckingIfTheChunkIsLoaded(long chunkPos, @NotNull CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(this.areEntitiesLoaded(chunkPos));
	}

	@Inject(method = {"lambda$tick$6(Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/world/entity/Entity;)V", "a", "method_31420"},
			at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerChunkCache;chunkMap:Lnet/minecraft/server/level/ChunkMap;", opcode = Opcodes.GETFIELD), cancellable = true)
	private void noisiumchunkmanager$redirectShouldTickEntities(@NotNull ProfilerFiller profiler, @NotNull Entity entity, @NotNull CallbackInfo ci) {
		if (!this.entityManager.canPositionTick(entity.chunkPosition())) {
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
		this.tickNonPassenger(entity);
		profiler.pop();
		ci.cancel();
	}

	@Override
	public ServerWorldChunkManager noisiumchunkmanager$getServerWorldChunkManager() {
		return noisiumchunkmanager$serverWorldChunkManager;
	}

	@Override
	public RandomState noisiumchunkmanager$getNoiseConfig() {
		return noisiumchunkmanager$noiseConfig;
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Override
	public void noisiumchunkmanager$sendPacketToNearbyPlayers(@NotNull Packet<?> packet) {
		@NotNull var players = this.players();
		for (int i = 0; i < players.size(); i++) {
			players.get(i).connection.send(packet);
		}
	}
}
