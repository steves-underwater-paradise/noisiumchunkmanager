package io.github.steveplays28.noisiumchunkmanager.mixin.server.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DataFixer;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager;
import io.github.steveplays28.noisiumchunkmanager.server.world.ServerLevelLightEngine;
import io.github.steveplays28.noisiumchunkmanager.server.world.ServerWorldChunkManager;
import io.github.steveplays28.noisiumchunkmanager.server.world.chunk.tick.ServerWorldChunkTicker;
import io.github.steveplays28.noisiumchunkmanager.server.world.ticket.ServerWorldTicketTracker;
import io.github.steveplays28.noisiumchunkmanager.util.networking.packet.PacketUtil;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.Util;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
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
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import io.github.steveplays28.noisiumchunkmanager.server.extension.world.ServerWorldExtension;
import io.github.steveplays28.noisiumchunkmanager.server.world.entity.ServerWorldEntityTracker;
import io.github.steveplays28.noisiumchunkmanager.server.world.entity.player.ServerWorldPlayerChunkLoader;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk.ServerChunkEvent;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.entity.ServerEntityEvent;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Debug(export = true)
@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin extends Level implements ServerWorldExtension {
	@Shadow @Final @Mutable private @Nullable PersistentEntitySectionManager<Entity> entityManager;
	@Shadow @Final @NotNull List<ServerPlayer> players;
	@Shadow @Final @NotNull EntityTickList entityTickList;
	@Shadow @Final @NotNull Set<Mob> navigatingMobs;
	@Shadow volatile boolean isUpdatingNavigations;
	@Shadow @Final @NotNull Int2ObjectMap<EnderDragonPart> dragonParts;

	@Shadow
	public abstract @NotNull MinecraftServer getServer();

	@Shadow
	public abstract @NotNull List<ServerPlayer> players();

	@Shadow
	public abstract boolean areEntitiesLoaded(long chunkPos);

	@Shadow
	public abstract void tickNonPassenger(Entity entity);

	@Shadow
	public abstract void updateSleepingPlayerList();

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

	@WrapOperation(method = "<init>",
			at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD, target = "Lnet/minecraft/server/level/ServerLevel;entityManager:Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;"))
	private void noisiumchunkmanager$preventCreatingPersistentEntitySectionManager(ServerLevel instance, PersistentEntitySectionManager<?> newValue, Operation<Void> original,
			MinecraftServer minecraftServer, Executor executor, LevelStorageSource.LevelStorageAccess levelStorageAccess, ServerLevelData serverLevelData, ResourceKey<Level> resourceKey,
			LevelStem levelStem, ChunkProgressListener chunkProgressListener, boolean bl, long l, List<CustomSpawner> list, boolean bl2, @Nullable RandomSequences randomSequences) {
		return;
	}

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Objects;requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 1))
	private Object noisiumchunkmanager$preventPersistentEntitySectionManagerNonNullCheck(Object value, Operation<PersistentEntitySectionManager<?>> original) {
		return null;
	}

	// TODO: Reimplement ServerLevel#EntityCallbacks
	@Inject(method = "<init>", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/chunk/storage/EntityStorage;<init>(Lnet/minecraft/server/level/ServerLevel;Ljava/nio/file/Path;Lcom/mojang/datafixers/DataFixer;ZLjava/util/concurrent/Executor;)V",
			shift = At.Shift.BY, by = 3))
	private void noisiumchunkmanager$constructorCreateServerWorldChunkManager(@NotNull MinecraftServer server, Executor workerExecutor, @NotNull LevelStorageSource.LevelStorageAccess session,
			@NotNull ServerLevelData serverWorldProperties, @NotNull ResourceKey<Level> worldKey, @NotNull LevelStem dimensionOptions, ChunkProgressListener worldGenerationProgressListener,
			boolean debugWorld, long seed, List<?> spawners, boolean shouldTickTime, RandomSequences randomSequencesState, @NotNull CallbackInfo ci, @Local @NotNull DataFixer dataFixer,
			@Local @NotNull EntityPersistentStorage<Entity> entityPersistentStorage) {
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
		noisiumchunkmanager$serverWorldEntityManager = new ServerWorldEntityTracker(serverWorld, entityPersistentStorage, packet -> PacketUtil.sendPacketToPlayers(serverWorld.players(), packet));
		noisiumchunkmanager$serverWorldPlayerChunkLoader = new ServerWorldPlayerChunkLoader(serverWorld, noisiumchunkmanager$serverWorldChunkManager::getChunksInRadiusAsync,
				noisiumchunkmanager$serverWorldChunkManager::getChunkAsync, noisiumchunkmanager$serverWorldChunkManager::unloadChunk, server.getPlayerList()::getViewDistance);

		// TODO: Reimplement ServerLevel.EntityCallbacks#onSectionChange
		ServerEntityEvent.AFTER_ADDED.register((ServerLevel serverLevel, Entity entity) -> {
			if (!serverWorld.equals(serverLevel)) {
				return EventResult.pass();
			}

			if (entity instanceof ServerPlayer serverPlayer) {
				this.players.add(serverPlayer);
				this.updateSleepingPlayerList();
			}

			if (entity instanceof Mob mob) {
				if (this.isUpdatingNavigations) {
					@NotNull String message = "onTrackingStart called during navigation iteration";
					Util.logAndPauseIfInIde(message, new IllegalStateException(message));
				}

				this.navigatingMobs.add(mob);
			}

			if (entity instanceof EnderDragon enderDragon) {
				for (EnderDragonPart enderDragonPart : enderDragon.getSubEntities()) {
					this.dragonParts.put(enderDragonPart.getId(), enderDragonPart);
				}
			}

			entity.updateDynamicGameEventListener(DynamicGameEventListener::add);
			this.entityTickList.add(entity);
			return EventResult.pass();
		});
		ServerEntityEvent.AFTER_REMOVED.register((ServerLevel serverLevel, Entity entity) -> {
			if (!serverWorld.equals(serverLevel)) {
				return EventResult.pass();
			}

			if (entity instanceof ServerPlayer serverPlayer) {
				this.players.remove(serverPlayer);
				this.updateSleepingPlayerList();
			}

			if (entity instanceof Mob mob) {
				if (this.isUpdatingNavigations) {
					@NotNull String message = "onTrackingEnd called during navigation iteration";
					Util.logAndPauseIfInIde(message, new IllegalStateException(message));
				}

				this.navigatingMobs.remove(mob);
			}

			if (entity instanceof EnderDragon enderDragon) {
				for (EnderDragonPart enderDragonPart : enderDragon.getSubEntities()) {
					this.dragonParts.remove(enderDragonPart.getId());
				}
			}

			entity.updateDynamicGameEventListener(DynamicGameEventListener::remove);
			this.entityTickList.remove(entity);
			this.getScoreboard().entityRemoved(entity);
			return EventResult.pass();
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

	@Inject(method = "isPositionTickingWithEntitiesLoaded", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$checkIfTickingFutureIsReadyByCheckingIfTheChunkIsLoaded(long chunkPos, @NotNull CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(true);
	}

	@Inject(method = {"lambda$tick$6(Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/world/entity/Entity;)V", "a", "method_31420"},
			at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerChunkCache;chunkMap:Lnet/minecraft/server/level/ChunkMap;", opcode = Opcodes.GETFIELD), cancellable = true)
	private void noisiumchunkmanager$redirectShouldTickEntities(@NotNull ProfilerFiller profiler, @NotNull Entity entity, @NotNull CallbackInfo ci) {
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

	@WrapOperation(method = "Lnet/minecraft/server/level/ServerLevel;tick(Ljava/util/function/BooleanSupplier;)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;tick()V"))
	private void noisiumchunkmanager$redirectPersistentEntitySectionManagerTickToServerLevelEntityTracker(@Nullable PersistentEntitySectionManager<Entity> instance, Operation<Void> original) {
		// TODO: Invoke a server level entity tracker tick event
	}

	@Inject(method = "getEntities()Lnet/minecraft/world/level/entity/LevelEntityGetter;", at = @At(value = "HEAD"), cancellable = true)
	protected void noisiumchunkmanager$getEntitiesFromServerLevelEntityTracker(@NotNull CallbackInfoReturnable<LevelEntityGetter<Entity>> cir) {
		cir.setReturnValue(this.noisiumchunkmanager$serverWorldEntityManager.getEntities());
	}

	@Inject(method = "addPlayer", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelAddPlayer(@NotNull CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "addEntity", at = @At(value = "HEAD"), cancellable = true, order = 0)
	private void noisiumchunkmanager$cancelAddEntity(Entity entity, @NotNull CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(EntityEvent.ADD.invoker().add(entity, (ServerLevel) (Object) this).isFalse());
	}

	@ModifyArg(method = "tryAddFreshEntityWithPassengers", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;anyMatch(Ljava/util/function/Predicate;)Z"))
	private @NotNull Predicate<UUID> noisiumchunkmanager$redirectPersistentEntitySectionManagerIsEntityLoadedToServerLevelEntityTracker(@Nullable Predicate<UUID> original) {
		NoisiumChunkManager.LOGGER.info("tryAddFreshEntityWithPassengers");
		return (UUID uuid) -> noisiumchunkmanager$serverWorldEntityManager.getEntities().get(uuid) != null;
	}

	@WrapOperation(method = "tryAddFreshEntityWithPassengers", at = @At(value = "INVOKE", target = "Ljava/util/Objects;requireNonNull(Ljava/lang/Object;)Ljava/lang/Object;"))
	private Object noisiumchunkmanager$tryAddFreshEntityWithPassengersPreventPersistentEntitySectionManagerNonNullCheck(Object value, Operation<PersistentEntitySectionManager<?>> original) {
		return null;
	}

	@WrapOperation(method = "close", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;close()V"))
	private void noisiumchunkmanager$cancelPersistentEntitySectionManagerClose(@Nullable PersistentEntitySectionManager<Entity> instance, Operation<Void> original) {
		// NO-OP
	}

	@WrapOperation(method = "gatherChunkSourceStats", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;gatherStats()Ljava/lang/String;"))
	private @NotNull String noisiumchunkmanager$gatherStatisticsFromServerLevelEntityTracker(@Nullable PersistentEntitySectionManager<Entity> instance, Operation<String> original) {
		// TODO
		return String.format("%s/%s", "TODO", "TODO");
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
