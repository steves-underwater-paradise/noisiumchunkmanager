package io.github.steveplays28.noisiumchunkmanager.server.world;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixer;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager;
import io.github.steveplays28.noisiumchunkmanager.config.NoisiumChunkManagerConfig;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk.ServerChunkEvent;
import io.github.steveplays28.noisiumchunkmanager.mixin.accessor.util.collection.PackedIntegerArrayAccessor;
import io.github.steveplays28.noisiumchunkmanager.mixin.accessor.world.gen.chunk.ChunkGeneratorAccessor;
import io.github.steveplays28.noisiumchunkmanager.world.chunk.IoWorldChunk;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.*;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import net.minecraft.world.poi.PointOfInterestTypes;
import net.minecraft.world.storage.NbtScannable;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager.MOD_NAME;

/**
 * A chunk manager for {@link ServerWorld}s.
 * This class cannot extend {@link net.minecraft.server.world.ServerChunkManager} or {@link ChunkManager} due to {@link ServerWorld}s requiring an implementation of {@link net.minecraft.server.world.ServerChunkManager}, which would slow the chunk manager down.
 */
// TODO: Fix canTickBlockEntities() check
//  The check needs to be changed to point to the server world's isChunkLoaded() method
// TODO: Implement chunk ticking
// TODO: Save all chunks when save event is called
public class ServerWorldChunkManager {
	private final ServerWorld serverWorld;
	private final ChunkGenerator chunkGenerator;
	private final NoiseConfig noiseConfig;
	private final Consumer<Runnable> syncRunnableConsumer;
	private final BiFunction<Chunk, Boolean, CompletableFuture<Chunk>> initializeChunkLightingBiFunction;
	private final BiFunction<Chunk, Boolean, CompletableFuture<Chunk>> lightChunkBiFunction;
	private final PersistentStateManager persistentStateManager;
	private final PointOfInterestStorage pointOfInterestStorage;
	private final VersionedChunkStorage versionedChunkStorage;
	private final Executor threadPoolExecutor;
	private final Executor noisePopulationThreadPoolExecutor;
	private final ConcurrentMap<ChunkPos, CompletableFuture<WorldChunk>> loadingWorldChunks;
	private final Queue<ChunkPos> unloadingWorldChunks;
	private final ConcurrentMap<ChunkPos, IoWorldChunk> ioWorldChunks;
	private final Map<ChunkPos, WorldChunk> loadedWorldChunks;

	private boolean isStopping;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public ServerWorldChunkManager(@NotNull ServerWorld serverWorld, @NotNull ChunkGenerator chunkGenerator, @NotNull NoiseConfig noiseConfig, @NotNull Consumer<Runnable> syncRunnableConsumer, @NotNull BiFunction<Chunk, Boolean, CompletableFuture<Chunk>> initializeChunkLightingBiFunction, @NotNull BiFunction<Chunk, Boolean, CompletableFuture<Chunk>> lightChunkBiFunction, @NotNull Path worldDirectoryPath, @NotNull DataFixer dataFixer) {
		this.serverWorld = serverWorld;
		this.chunkGenerator = chunkGenerator;
		this.noiseConfig = noiseConfig;
		this.syncRunnableConsumer = syncRunnableConsumer;
		this.initializeChunkLightingBiFunction = initializeChunkLightingBiFunction;
		this.lightChunkBiFunction = lightChunkBiFunction;

		var worldDataFile = worldDirectoryPath.resolve("data").toFile();
		worldDataFile.mkdirs();
		this.persistentStateManager = new PersistentStateManager(worldDataFile, dataFixer);
		this.pointOfInterestStorage = new PointOfInterestStorage(
				worldDirectoryPath.resolve("poi"), dataFixer, false, serverWorld.getRegistryManager(), serverWorld);
		this.versionedChunkStorage = new VersionedChunkStorage(worldDirectoryPath.resolve("region"), dataFixer, false);
		this.threadPoolExecutor = Executors.newFixedThreadPool(
				NoisiumChunkManagerConfig.HANDLER.instance().serverWorldChunkManagerThreads, new ThreadFactoryBuilder().setNameFormat(
						"Noisium Server World Chunk Manager " + serverWorld.getDimension().effects() + " %d").build());
		this.noisePopulationThreadPoolExecutor = Executors.newFixedThreadPool(
				NoisiumChunkManagerConfig.HANDLER.instance().serverWorldChunkManagerThreads, new ThreadFactoryBuilder().setNameFormat(
						"Noisium Server World Chunk Manager Noise Population " + serverWorld.getDimension().effects() + " %d").build());
		this.loadingWorldChunks = new ConcurrentHashMap<>();
		this.unloadingWorldChunks = new ConcurrentLinkedQueue<>();
		this.ioWorldChunks = new ConcurrentHashMap<>();
		this.loadedWorldChunks = new HashMap<>();

		ServerChunkEvent.BLOCK_CHANGE.register(this::onBlockChange);
		TickEvent.SERVER_LEVEL_POST.register(instance -> {
			if (!instance.equals(serverWorld) || instance.getPlayers().isEmpty()) {
				return;
			}

			pointOfInterestStorage.tick(() -> true);
		});
		LifecycleEvent.SERVER_STOPPING.register(instance -> {
			this.isStopping = true;
			for (var loadingWorldChunkCompletableFuture : loadingWorldChunks.values()) {
				loadingWorldChunkCompletableFuture.cancel(true);
			}

			loadingWorldChunks.clear();
			loadedWorldChunks.clear();
		});
	}

	/**
	 * Loads the chunk at the specified position, returning the loaded chunk when done.
	 * Returns the chunk from the {@link ServerWorldChunkManager#loadedWorldChunks} cache if available.
	 * This method is ran asynchronously.
	 *
	 * @param chunkPos The position at which to load the chunk.
	 * @return The loaded chunk.
	 */
	public @NotNull CompletableFuture<WorldChunk> getChunkAsync(ChunkPos chunkPos) {
		if (isStopping) {
			return CompletableFuture.failedFuture(new IllegalStateException(
					String.format("Can't get chunk because %s Server World Chunk Manager is stopping.", MOD_NAME)));
		}

		if (loadedWorldChunks.containsKey(chunkPos)) {
			return CompletableFuture.completedFuture(loadedWorldChunks.get(chunkPos));
		} else if (loadingWorldChunks.containsKey(chunkPos)) {
			return loadingWorldChunks.get(chunkPos);
		}

		var worldChunkCompletableFuture = CompletableFuture.supplyAsync(() -> {
			var fetchedNbtData = getNbtDataAtChunkPosition(chunkPos);
			if (fetchedNbtData == null) {
				// TODO: Schedule ProtoChunk worldgen and update loadedWorldChunks incrementally during worldgen steps
				return new WorldChunk(
						serverWorld,
						generateChunk(
								chunkPos, this::getIoWorldChunk, ioWorldChunks::remove,
								initializeChunkLightingBiFunction, lightChunkBiFunction
						),
						null
				);
			}

			versionedChunkStorage.updateChunkNbt(
					serverWorld.getRegistryKey(), () -> persistentStateManager, fetchedNbtData, this.chunkGenerator.getCodecKey());
			var fetchedChunk = ChunkSerializer.deserialize(serverWorld, pointOfInterestStorage, chunkPos, fetchedNbtData);
			return new WorldChunk(serverWorld, fetchedChunk,
					chunkToAddEntitiesTo -> serverWorld.addEntities(EntityType.streamFromNbt(fetchedChunk.getEntities(), serverWorld))
			);
		}, threadPoolExecutor).whenComplete((fetchedWorldChunk, throwable) -> {
			if (throwable != null) {
				NoisiumChunkManager.LOGGER.error(
						"Exception thrown while getting a chunk asynchronously:\n{}", ExceptionUtils.getStackTrace(throwable));
				loadingWorldChunks.remove(chunkPos);
				return;
			}

			syncRunnableConsumer.accept(() -> fetchedWorldChunk.addChunkTickSchedulers(serverWorld));
			fetchedWorldChunk.loadEntities();
			loadingWorldChunks.remove(chunkPos);

			if (!unloadingWorldChunks.contains(chunkPos)) {
				loadedWorldChunks.put(chunkPos, fetchedWorldChunk);
			}

			syncRunnableConsumer.accept(
					() -> ServerChunkEvent.WORLD_CHUNK_LOADED.invoker().onWorldChunkLoaded(serverWorld, fetchedWorldChunk));
			unloadingWorldChunks.remove(chunkPos);
			syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_UNLOADED.invoker().onWorldChunkUnloaded(serverWorld, chunkPos));
		});
		loadingWorldChunks.put(chunkPos, worldChunkCompletableFuture);
		return worldChunkCompletableFuture;
	}

	/**
	 * Loads the chunk at the specified position, returning the loaded {@link WorldChunk} when done.
	 * Returns the chunk from the {@link ServerWorldChunkManager#loadedWorldChunks} cache if available.
	 * WARNING: This method blocks the server thread. Prefer using {@link ServerWorldChunkManager#getChunkAsync} instead.
	 *
	 * @param chunkPos The position at which to load the {@link WorldChunk}.
	 * @return The loaded {@link WorldChunk}.
	 */
	public @NotNull WorldChunk getChunk(@NotNull ChunkPos chunkPos) {
		if (isStopping) {
			throw new IllegalStateException(String.format("Can't get chunk because %s Server World Chunk Manager is stopping.", MOD_NAME));
		}

		if (loadedWorldChunks.containsKey(chunkPos)) {
			return loadedWorldChunks.get(chunkPos);
		} else if (loadingWorldChunks.containsKey(chunkPos)) {
			return loadingWorldChunks.get(chunkPos).join();
		}

		var fetchedNbtData = getNbtDataAtChunkPosition(chunkPos);
		if (fetchedNbtData == null) {
			// TODO: Schedule ProtoChunk worldgen and update loadedWorldChunks incrementally during worldgen steps
			var fetchedWorldChunk = new WorldChunk(
					serverWorld,
					generateChunk(
							chunkPos, this::getIoWorldChunk, ioWorldChunks::remove,
							initializeChunkLightingBiFunction, lightChunkBiFunction
					),
					null
			);
			if (!unloadingWorldChunks.contains(chunkPos)) {
				loadedWorldChunks.put(chunkPos, fetchedWorldChunk);
			}

			syncRunnableConsumer.accept(
					() -> ServerChunkEvent.WORLD_CHUNK_LOADED.invoker().onWorldChunkLoaded(serverWorld, fetchedWorldChunk));
			unloadingWorldChunks.remove(chunkPos);
			syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_UNLOADED.invoker().onWorldChunkUnloaded(serverWorld, chunkPos));
			return fetchedWorldChunk;
		}

		var fetchedChunk = ChunkSerializer.deserialize(serverWorld, pointOfInterestStorage, chunkPos, fetchedNbtData);
		var fetchedWorldChunk = new WorldChunk(serverWorld, fetchedChunk,
				chunkToAddEntitiesTo -> serverWorld.addEntities(EntityType.streamFromNbt(fetchedChunk.getEntities(), serverWorld))
		);
		syncRunnableConsumer.accept(() -> fetchedWorldChunk.addChunkTickSchedulers(serverWorld));
		fetchedWorldChunk.loadEntities();

		if (!unloadingWorldChunks.contains(chunkPos)) {
			loadedWorldChunks.put(chunkPos, fetchedWorldChunk);
		}

		syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_LOADED.invoker().onWorldChunkLoaded(serverWorld, fetchedWorldChunk));
		unloadingWorldChunks.remove(chunkPos);
		syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_UNLOADED.invoker().onWorldChunkUnloaded(serverWorld, chunkPos));
		return fetchedWorldChunk;
	}

	public @NotNull IoWorldChunk getIoWorldChunk(@NotNull ChunkPos chunkPos) {
		if (ioWorldChunks.containsKey(chunkPos)) {
			return ioWorldChunks.get(chunkPos);
		}

		@NotNull var ioWorldChunk = new IoWorldChunk(serverWorld, chunkPos);
		ioWorldChunks.put(chunkPos, ioWorldChunk);
		return ioWorldChunk;
	}

	/**
	 * Gets all {@link WorldChunk}s around the specified chunk, using a square radius.
	 * This method is ran asynchronously.
	 *
	 * @param chunkPos The center {@link ChunkPos}.
	 * @param radius   A square radius of chunks.
	 * @return All the {@link WorldChunk}s around the specified chunk, using a square radius.
	 */
	public @NotNull Map<ChunkPos, CompletableFuture<WorldChunk>> getChunksInRadiusAsync(@NotNull ChunkPos chunkPos, int radius) {
		@NotNull var chunks = new HashMap<ChunkPos, CompletableFuture<WorldChunk>>();
		for (int chunkPosX = chunkPos.x - radius; chunkPosX < chunkPos.x + radius; chunkPosX++) {
			for (int chunkPosZ = chunkPos.z - radius; chunkPosZ < chunkPos.z + radius; chunkPosZ++) {
				var chunkPosThatShouldBeLoaded = new ChunkPos(chunkPosX, chunkPosZ);
				chunks.put(chunkPosThatShouldBeLoaded, getChunkAsync(chunkPosThatShouldBeLoaded));
			}
		}

		return chunks;
	}

	/**
	 * Gets all {@link WorldChunk}s around the specified chunk, using a square radius.
	 * WARNING: This method blocks the server thread. Prefer using {@link ServerWorldChunkManager#getChunksInRadiusAsync(ChunkPos, int)} instead.
	 *
	 * @param chunkPos The center {@link ChunkPos}.
	 * @param radius   A square radius of chunks.
	 * @return All the {@link WorldChunk}s around the specified chunk, using a square radius.
	 */
	public @NotNull Map<@NotNull ChunkPos, @Nullable WorldChunk> getChunksInRadius(@NotNull ChunkPos chunkPos, int radius) {
		var chunks = new HashMap<@NotNull ChunkPos, @Nullable WorldChunk>();

		for (int chunkPosX = chunkPos.x - radius; chunkPosX < chunkPos.x + radius; chunkPosX++) {
			for (int chunkPosZ = chunkPos.z - radius; chunkPosZ < chunkPos.z + radius; chunkPosZ++) {
				var chunkPosThatShouldBeLoaded = new ChunkPos(chunkPosX, chunkPosZ);
				chunks.put(chunkPosThatShouldBeLoaded, getChunk(chunkPosThatShouldBeLoaded));
			}
		}

		return chunks;
	}

	public void unloadChunk(@NotNull ChunkPos chunkPosition) {
		if (loadingWorldChunks.containsKey(chunkPosition)) {
			unloadingWorldChunks.add(chunkPosition);
			return;
		}

		loadedWorldChunks.remove(chunkPosition);
		syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_UNLOADED.invoker().onWorldChunkUnloaded(serverWorld, chunkPosition));
	}

	public boolean isChunkLoaded(ChunkPos chunkPos) {
		return this.loadedWorldChunks.containsKey(chunkPos);
	}

	public @NotNull NbtScannable getChunkIoWorker() {
		return versionedChunkStorage.getWorker();
	}

	public @NotNull PersistentStateManager getPersistentStateManager() {
		return persistentStateManager;
	}

	// TODO: Check if this can be ran asynchronously
	@SuppressWarnings("OptionalIsPresent")
	private void onBlockChange(@NotNull BlockPos blockPos, @NotNull BlockState oldBlockState, @NotNull BlockState newBlockState) {
		Optional<RegistryEntry<PointOfInterestType>> oldBlockStatePointOfInterestTypeOptional = PointOfInterestTypes.getTypeForState(
				oldBlockState);
		Optional<RegistryEntry<PointOfInterestType>> newBlockStatePointOfInterestTypeOptional = PointOfInterestTypes.getTypeForState(
				newBlockState);
		if (oldBlockStatePointOfInterestTypeOptional.equals(newBlockStatePointOfInterestTypeOptional)) {
			return;
		}

		BlockPos immutableBlockPos = blockPos.toImmutable();
		if (oldBlockStatePointOfInterestTypeOptional.isPresent()) {
			pointOfInterestStorage.remove(immutableBlockPos);
			// TODO: Add sendPoiRemoval method call into DebugInfoSenderMixin using an event
		}
		if (newBlockStatePointOfInterestTypeOptional.isPresent()) {
			pointOfInterestStorage.add(immutableBlockPos, newBlockStatePointOfInterestTypeOptional.get());
			// TODO: Add sendPoiRemoval method call into DebugInfoSenderMixin using an event
		}
	}

	private @Nullable NbtCompound getNbtDataAtChunkPosition(ChunkPos chunkPos) {
		try {
			var fetchedNbtCompoundOptionalFuture = versionedChunkStorage.getNbt(chunkPos).get();
			if (fetchedNbtCompoundOptionalFuture.isPresent()) {
				return fetchedNbtCompoundOptionalFuture.get();
			}
		} catch (Exception ex) {
			NoisiumChunkManager.LOGGER.error("Error occurred while fetching NBT data for chunk at {}", chunkPos);
		}

		return null;
	}

	// TODO: Move this into the constructor as a Supplier<ChunkPos, ProtoChunk>
	private @NotNull ProtoChunk generateChunk(@NotNull ChunkPos chunkPos, @NotNull Function<ChunkPos, IoWorldChunk> ioWorldChunkGetFunction, @NotNull Function<ChunkPos, IoWorldChunk> ioWorldChunkRemoveFunction, @NotNull BiFunction<Chunk, Boolean, CompletableFuture<Chunk>> initializeChunkLightingBiConsumer, @NotNull BiFunction<Chunk, Boolean, CompletableFuture<Chunk>> lightChunkBiConsumer) {
		var protoChunk = new ProtoChunk(chunkPos, UpgradeData.NO_UPGRADE_DATA, serverWorld,
				serverWorld.getRegistryManager().get(RegistryKeys.BIOME), null
		);
		List<Chunk> chunkRegionChunks = List.of(protoChunk);
		var chunkRegion = new ChunkRegion(serverWorld, chunkRegionChunks, ChunkStatus.FULL, 1);
		var blender = Blender.getBlender(chunkRegion);
		var chunkRegionStructureAccessor = serverWorld.getStructureAccessor().forRegion(chunkRegion);

		protoChunk.setStatus(ChunkStatus.STRUCTURE_STARTS);
		// TODO: Move the structure placement calculator into NoisiumServerWorldChunkManager
		// TODO: Pass the structure template manager into NoisiumServerWorldChunkManager
		// TODO: Pass the shouldGenerateStructures boolean into NoisiumServerWorldChunkManager
		if (serverWorld.getServer().getSaveProperties().getGeneratorOptions().shouldGenerateStructures()) {
			chunkGenerator.setStructureStarts(
					serverWorld.getRegistryManager(), serverWorld.getChunkManager().getStructurePlacementCalculator(),
					chunkRegionStructureAccessor, protoChunk, serverWorld.getServer().getStructureTemplateManager()
			);
		}
		serverWorld.cacheStructures(protoChunk);

		protoChunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
		chunkGenerator.addStructureReferences(chunkRegion, chunkRegionStructureAccessor, protoChunk);

		protoChunk.setStatus(ChunkStatus.BIOMES);
		protoChunk.populateBiomes(chunkGenerator.getBiomeSource(), noiseConfig.getMultiNoiseSampler());

		protoChunk.setStatus(ChunkStatus.NOISE);
		protoChunk = (ProtoChunk) ((ChunkGeneratorAccessor) chunkGenerator).invokePopulateNoise(
				noisePopulationThreadPoolExecutor, blender, noiseConfig, chunkRegionStructureAccessor, protoChunk).join();

		protoChunk.setStatus(ChunkStatus.SURFACE);
		chunkGenerator.buildSurface(chunkRegion, chunkRegionStructureAccessor, noiseConfig, protoChunk);

		protoChunk.setStatus(ChunkStatus.CARVERS);
		chunkGenerator.carve(
				chunkRegion, chunkRegion.getSeed(), noiseConfig, chunkRegion.getBiomeAccess(), chunkRegionStructureAccessor, protoChunk,
				GenerationStep.Carver.AIR
		);

		protoChunk.setStatus(ChunkStatus.FEATURES);
		Heightmap.populateHeightmaps(
				protoChunk,
				EnumSet.of(
						Heightmap.Type.MOTION_BLOCKING, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
						Heightmap.Type.OCEAN_FLOOR, Heightmap.Type.WORLD_SURFACE
				)
		);
		chunkGenerator.generateFeatures(chunkRegion, protoChunk, chunkRegionStructureAccessor);
		Blender.tickLeavesAndFluids(chunkRegion, protoChunk);

		@NotNull var ioWorldChunkSectionArray = ioWorldChunkGetFunction.apply(chunkPos).getSectionArray();
		for (int chunkSectionIndex = 0; chunkSectionIndex < ioWorldChunkSectionArray.length; chunkSectionIndex++) {
			@NotNull var ioWorldChunkSection = ioWorldChunkSectionArray[chunkSectionIndex];
			if (ioWorldChunkSection.isEmpty()) {
				continue;
			}

			@NotNull var ioWorldChunkSectionBlockStateContainer = ioWorldChunkSection.getBlockStateContainer();
			ioWorldChunkSectionBlockStateContainer.lock();

			@NotNull var protoChunkSectionBlockStateContainer = protoChunk.getSectionArray()[chunkSectionIndex].getBlockStateContainer();
			protoChunkSectionBlockStateContainer.lock();

			@NotNull var protoChunkPalettedContainerData = protoChunkSectionBlockStateContainer.data;
			@NotNull var protoChunkPaletteStorage = protoChunkPalettedContainerData.storage();
			@NotNull var ioWorldChunkPalettedContainerData = ioWorldChunkSectionBlockStateContainer.data;
			@NotNull var ioWorldChunkPaletteStorage = ioWorldChunkPalettedContainerData.storage();
			var ioWorldChunkPaletteStorageSize = ioWorldChunkPaletteStorage.getSize();
			if (protoChunkPaletteStorage.getData().length == 0) {
				protoChunkPaletteStorage = new PackedIntegerArray(
						ioWorldChunkPaletteStorage.getElementBits(), ioWorldChunkPaletteStorageSize);
			}

			for (int blockIndex = 0; blockIndex < ioWorldChunkPaletteStorageSize; blockIndex++) {
				@NotNull var blockState = ioWorldChunkPalettedContainerData.palette().get(ioWorldChunkPaletteStorage.get(blockIndex));
				var blockStatePaletteValue = protoChunkPalettedContainerData.palette.index(blockState);
				if (blockState.equals(Blocks.AIR.getDefaultState())
						|| blockStatePaletteValue > ((PackedIntegerArrayAccessor) protoChunkPaletteStorage).getMaxValue()) {
					continue;
				}

				protoChunkPaletteStorage.set(blockIndex, blockStatePaletteValue);
			}

			ioWorldChunkSectionBlockStateContainer.unlock();
			protoChunkSectionBlockStateContainer.unlock();
		}

		ioWorldChunkRemoveFunction.apply(chunkPos);

		protoChunk.setStatus(ChunkStatus.INITIALIZE_LIGHT);
		protoChunk.refreshSurfaceY();
		initializeChunkLightingBiConsumer.apply(protoChunk, protoChunk.isLightOn()).join();

		protoChunk.setStatus(ChunkStatus.LIGHT);
		lightChunkBiConsumer.apply(protoChunk, protoChunk.isLightOn()).join();

		protoChunk.setStatus(ChunkStatus.SPAWN);
		chunkGenerator.populateEntities(chunkRegion);

		protoChunk.setStatus(ChunkStatus.FULL);
		pointOfInterestStorage.saveChunk(chunkPos);
		versionedChunkStorage.setNbt(chunkPos, ChunkSerializer.serialize(serverWorld, protoChunk));
		// TODO: Add a (Neo)Forge ChunkDataEvent.Save invoker
		//  Also add a Fabric/Architectury chunk save event invoker
		return protoChunk;
	}
}
