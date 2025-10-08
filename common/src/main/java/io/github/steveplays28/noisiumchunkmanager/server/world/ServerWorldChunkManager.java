package io.github.steveplays28.noisiumchunkmanager.server.world;

import static io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager.MOD_NAME;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.DataFixer;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager;
import io.github.steveplays28.noisiumchunkmanager.config.NoisiumChunkManagerConfig;
import io.github.steveplays28.noisiumchunkmanager.extension.world.level.chunk.storage.SectionStorageExtension;
import io.github.steveplays28.noisiumchunkmanager.mixin.accessor.util.collection.PackedIntegerArrayAccessor;
import io.github.steveplays28.noisiumchunkmanager.mixin.accessor.world.gen.chunk.NoiseChunkGeneratorAccessor;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.chunk.ServerChunkEvent;
import io.github.steveplays28.noisiumchunkmanager.world.chunk.IoWorldChunk;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A chunk manager for {@link ServerLevel}s. This class cannot extend {@link net.minecraft.server.level.ServerChunkCache} or {@link ChunkSource} due to {@link ServerLevel}s requiring an implementation
 * of {@link net.minecraft.server.level.ServerChunkCache}, which would slow the chunk manager down.
 */
// TODO: Save all chunks when save event is called
public class ServerWorldChunkManager {
	private final ServerLevel serverWorld;
	private final ChunkGenerator chunkGenerator;
	private final RandomState noiseConfig;
	private final Consumer<Runnable> syncRunnableConsumer;
	private final DimensionDataStorage persistentStateManager;
	private final PoiManager pointOfInterestStorage;
	private final ChunkStorage versionedChunkStorage;
	private final Executor threadPoolExecutor;
	private final ConcurrentMap<ChunkPos, CompletableFuture<LevelChunk>> loadingWorldChunks;
	private final Queue<ChunkPos> unloadingWorldChunks;
	private final ConcurrentMap<ChunkPos, IoWorldChunk> ioWorldChunks;
	private final Map<ChunkPos, LevelChunk> loadedWorldChunks;

	private boolean isStopping;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public ServerWorldChunkManager(@NotNull ServerLevel serverWorld, @NotNull ChunkGenerator chunkGenerator, @NotNull RandomState noiseConfig, @NotNull Consumer<Runnable> syncRunnableConsumer,
			@NotNull Path worldDirectoryPath, @NotNull DataFixer dataFixer) {
		this.serverWorld = serverWorld;
		this.chunkGenerator = chunkGenerator;
		this.noiseConfig = noiseConfig;
		this.syncRunnableConsumer = syncRunnableConsumer;

		var worldDataFile = worldDirectoryPath.resolve("data").toFile();
		worldDataFile.mkdirs();
		this.persistentStateManager = new DimensionDataStorage(worldDataFile, dataFixer);
		this.pointOfInterestStorage = new PoiManager(worldDirectoryPath.resolve("poi"), dataFixer, false, serverWorld.registryAccess(), serverWorld);
		this.versionedChunkStorage = new ChunkStorage(worldDirectoryPath.resolve("region"), dataFixer, false);
		this.threadPoolExecutor = Executors.newFixedThreadPool(NoisiumChunkManagerConfig.HANDLER.instance().serverWorldChunkManagerThreads,
				new ThreadFactoryBuilder().setNameFormat("Noisium Server World Chunk Manager " + serverWorld.dimensionType().effectsLocation() + " %d").build());
		this.loadingWorldChunks = new ConcurrentHashMap<>();
		this.unloadingWorldChunks = new ConcurrentLinkedQueue<>();
		this.ioWorldChunks = new ConcurrentHashMap<>();
		this.loadedWorldChunks = new HashMap<>();

		TickEvent.SERVER_LEVEL_POST.register(instance -> {
			if (!instance.equals(serverWorld) || instance.players().isEmpty()) {
				return;
			}

			unloadingWorldChunks.removeIf((@NotNull ChunkPos chunkPosition) -> {
				var removeUnloadingWorldChunkCondition = !loadingWorldChunks.containsKey(chunkPosition);
				if (removeUnloadingWorldChunkCondition) {
					loadedWorldChunks.remove(chunkPosition);
				}

				return removeUnloadingWorldChunkCondition;
			});
			serverWorld.getLightEngine().runLightUpdates();
			pointOfInterestStorage.tick(() -> true);
			NoisiumChunkManager.LOGGER.info("Loading {} chunks, unloading {} chunks and loaded {} chunks.", loadingWorldChunks.size(), unloadingWorldChunks.size(), loadedWorldChunks.size());
		});
		LifecycleEvent.SERVER_STOPPING.register(instance -> {
			this.isStopping = true;
			for (var loadingWorldChunkCompletableFuture : loadingWorldChunks.values()) {
				loadingWorldChunkCompletableFuture.cancel(true);
			}
			loadingWorldChunks.clear();
			for (var loadedWorldChunk : loadedWorldChunks.values()) {
				@NotNull var chunkPosition = loadedWorldChunk.getPos();
				pointOfInterestStorage.flush(chunkPosition);
				loadedWorldChunk.setUnsaved(false);
				versionedChunkStorage.write(chunkPosition, ChunkSerializer.write(serverWorld, loadedWorldChunk));
			}
			loadedWorldChunks.clear();
		});
	}

	/**
	 * Loads the chunk at the specified position, returning the loaded chunk when done. Returns the chunk from the {@link ServerWorldChunkManager#loadedWorldChunks} cache if available. This method is
	 * ran asynchronously.
	 *
	 * @param chunkPos The position at which to load the chunk.
	 * @return The loaded chunk.
	 */
	public @NotNull CompletableFuture<LevelChunk> getChunkAsync(ChunkPos chunkPos) {
		if (isStopping) {
			return CompletableFuture.failedFuture(new IllegalStateException(String.format("Can't get chunk because %s Server World Chunk Manager is stopping.", MOD_NAME)));
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
				return new LevelChunk(serverWorld, generateChunk(chunkPos, this::getIoWorldChunk, ioWorldChunks::remove), null);
			}

			fetchedNbtData = versionedChunkStorage.upgradeChunkTag(serverWorld.dimension(), () -> persistentStateManager, fetchedNbtData, this.chunkGenerator.getTypeNameForDataFixer());
			var fetchedChunk = ChunkSerializer.read(serverWorld, pointOfInterestStorage, chunkPos, fetchedNbtData);
			return new LevelChunk(serverWorld, fetchedChunk, chunkToAddEntitiesTo -> serverWorld.addWorldGenChunkEntities(EntityType.loadEntitiesRecursive(fetchedChunk.getEntities(), serverWorld)));
		}, threadPoolExecutor).whenComplete((fetchedWorldChunk, throwable) -> {
			if (throwable != null) {
				NoisiumChunkManager.LOGGER.error("Exception thrown while getting a chunk asynchronously:\n{}", ExceptionUtils.getStackTrace(throwable));
				loadingWorldChunks.remove(chunkPos);
				return;
			}


			syncRunnableConsumer.accept(() -> {
				// fetchedWorldChunk.postProcessGeneration();
				fetchedWorldChunk.registerTickContainerInLevel(serverWorld);
				// serverWorld.startTickingChunk(fetchedWorldChunk);
			});

			loadingWorldChunks.remove(chunkPos);
			if (!unloadingWorldChunks.contains(chunkPos)) {
				loadedWorldChunks.put(chunkPos, fetchedWorldChunk);
				fetchedWorldChunk.setLoaded(true);
			}
			// TODO: Run `serverLevel.getProfiler().incrementCounter("chunkLoad");` on the ServerChunkEvent.WORLD_CHUNK_LOADED event
			syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_LOADED.invoker().onWorldChunkLoaded(serverWorld, fetchedWorldChunk));
			if (unloadingWorldChunks.remove(chunkPos)) {
				syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_UNLOADED.invoker().onWorldChunkUnloaded(serverWorld, chunkPos));
			}
		});
		loadingWorldChunks.put(chunkPos, worldChunkCompletableFuture);
		return worldChunkCompletableFuture;
	}

	/**
	 * Loads the chunk at the specified position, returning the loaded {@link LevelChunk} when done. Returns the chunk from the {@link ServerWorldChunkManager#loadedWorldChunks} cache if available.
	 * WARNING: This method blocks the server thread. Prefer using {@link ServerWorldChunkManager#getChunkAsync} instead.
	 *
	 * @param chunkPos The position at which to load the {@link LevelChunk}.
	 * @return The loaded {@link LevelChunk}.
	 */
	public @NotNull LevelChunk getChunk(@NotNull ChunkPos chunkPos) {
		if (isStopping) {
			throw new IllegalStateException(String.format("Can't get chunk because %s Server World Chunk Manager is stopping.", MOD_NAME));
		}

		// NoisiumChunkManager.LOGGER.info("Getting chunk at ({}, {}) in dimension {}.", chunkPos.x, chunkPos.z, this.serverWorld.dimensionType().effectsLocation());
		// NoisiumChunkManager.LOGGER.info("", new Throwable());

		if (loadedWorldChunks.containsKey(chunkPos)) {
			return loadedWorldChunks.get(chunkPos);
		} else if (loadingWorldChunks.containsKey(chunkPos)) {
			return loadingWorldChunks.get(chunkPos).join();
		}

		var fetchedNbtData = getNbtDataAtChunkPosition(chunkPos);
		if (fetchedNbtData == null) {
			// TODO: Schedule ProtoChunk worldgen and update loadedWorldChunks incrementally during worldgen steps
			var fetchedWorldChunk = new LevelChunk(serverWorld, generateChunk(chunkPos, this::getIoWorldChunk, ioWorldChunks::remove), null);
			syncRunnableConsumer.accept(() -> {
				// fetchedWorldChunk.postProcessGeneration();
				fetchedWorldChunk.registerTickContainerInLevel(serverWorld);
				// serverWorld.startTickingChunk(fetchedWorldChunk);
			});

			if (!unloadingWorldChunks.contains(chunkPos)) {
				loadedWorldChunks.put(chunkPos, fetchedWorldChunk);
			}
			syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_LOADED.invoker().onWorldChunkLoaded(serverWorld, fetchedWorldChunk));
			if (unloadingWorldChunks.remove(chunkPos)) {
				syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_UNLOADED.invoker().onWorldChunkUnloaded(serverWorld, chunkPos));
			}
			return fetchedWorldChunk;
		}

		fetchedNbtData = versionedChunkStorage.upgradeChunkTag(serverWorld.dimension(), () -> persistentStateManager, fetchedNbtData, this.chunkGenerator.getTypeNameForDataFixer());
		var fetchedChunk = ChunkSerializer.read(serverWorld, pointOfInterestStorage, chunkPos, fetchedNbtData);
		var fetchedWorldChunk =
				new LevelChunk(serverWorld, fetchedChunk, chunkToAddEntitiesTo -> serverWorld.addWorldGenChunkEntities(EntityType.loadEntitiesRecursive(fetchedChunk.getEntities(), serverWorld)));
		syncRunnableConsumer.accept(() -> {
			// fetchedWorldChunk.postProcessGeneration();
			fetchedWorldChunk.registerTickContainerInLevel(serverWorld);
			// serverWorld.startTickingChunk(fetchedWorldChunk);
		});

		if (!unloadingWorldChunks.contains(chunkPos)) {
			loadedWorldChunks.put(chunkPos, fetchedWorldChunk);
		}
		// TODO: Run `serverLevel.getProfiler().incrementCounter("chunkLoad");` on the ServerChunkEvent.WORLD_CHUNK_LOADED event
		syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_LOADED.invoker().onWorldChunkLoaded(serverWorld, fetchedWorldChunk));
		if (unloadingWorldChunks.remove(chunkPos)) {
			syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_UNLOADED.invoker().onWorldChunkUnloaded(serverWorld, chunkPos));
		}
		return fetchedWorldChunk;
	}

	private void lightProtoChunk(@NotNull ProtoChunk protoChunk) {
		@NotNull var protoChunkPosition = protoChunk.getPos();
		@NotNull var levelLightEngine = serverWorld.getLightEngine();
		for (int i = 0; i < protoChunk.getSectionsCount(); i++) {
			levelLightEngine.updateSectionStatus(SectionPos.of(protoChunkPosition, protoChunk.getSectionYFromSectionIndex(i)), false);
		}
		levelLightEngine.setLightEnabled(protoChunkPosition, protoChunk.isLightCorrect());
		levelLightEngine.retainData(protoChunkPosition, false);
		protoChunk.setLightCorrect(false);
		if (!protoChunk.isLightCorrect()) {
			levelLightEngine.propagateLightSources(protoChunkPosition);
		}
		protoChunk.setLightCorrect(true);
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
	 * Gets all {@link LevelChunk}s around the specified chunk, using a square radius. This method is ran asynchronously.
	 *
	 * @param chunkPos The center {@link ChunkPos}.
	 * @param radius A square radius of chunks.
	 * @return All the {@link LevelChunk}s around the specified chunk, using a square radius.
	 */
	public @NotNull Map<ChunkPos, CompletableFuture<LevelChunk>> getChunksInRadiusAsync(@NotNull ChunkPos chunkPos, int radius) {
		@NotNull var chunks = new HashMap<ChunkPos, CompletableFuture<LevelChunk>>();
		for (int chunkPosX = chunkPos.x - radius; chunkPosX < chunkPos.x + radius; chunkPosX++) {
			for (int chunkPosZ = chunkPos.z - radius; chunkPosZ < chunkPos.z + radius; chunkPosZ++) {
				var chunkPosThatShouldBeLoaded = new ChunkPos(chunkPosX, chunkPosZ);
				chunks.put(chunkPosThatShouldBeLoaded, getChunkAsync(chunkPosThatShouldBeLoaded));
			}
		}

		return chunks;
	}

	/**
	 * Gets all {@link LevelChunk}s around the specified chunk, using a square radius. WARNING: This method blocks the server thread. Prefer using
	 * {@link ServerWorldChunkManager#getChunksInRadiusAsync(ChunkPos, int)} instead.
	 *
	 * @param chunkPos The center {@link ChunkPos}.
	 * @param radius A square radius of chunks.
	 * @return All the {@link LevelChunk}s around the specified chunk, using a square radius.
	 */
	public @NotNull Map<@NotNull ChunkPos, @Nullable LevelChunk> getChunksInRadius(@NotNull ChunkPos chunkPos, int radius) {
		var chunks = new HashMap<@NotNull ChunkPos, @Nullable LevelChunk>();

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

		@Nullable var loadedWorldChunk = loadedWorldChunks.get(chunkPosition);
		if (loadedWorldChunk == null) {
			return;
		}

		saveChunk(loadedWorldChunk);
		serverWorld.unload(loadedWorldChunk);
		loadedWorldChunks.remove(chunkPosition);
		syncRunnableConsumer.accept(() -> ServerChunkEvent.WORLD_CHUNK_UNLOADED.invoker().onWorldChunkUnloaded(serverWorld, chunkPosition));
	}

	public boolean isChunkLoaded(ChunkPos chunkPos) {
		return this.loadedWorldChunks.containsKey(chunkPos);
	}

	public @NotNull ChunkScanAccess getChunkIoWorker() {
		return versionedChunkStorage.chunkScanner();
	}

	public @NotNull DimensionDataStorage getPersistentStateManager() {
		return persistentStateManager;
	}

	public @NotNull PoiManager getPointOfInterestManager() {
		return pointOfInterestStorage;
	}

	private @Nullable CompoundTag getNbtDataAtChunkPosition(ChunkPos chunkPos) {
		try {
			var fetchedNbtCompoundOptionalFuture = versionedChunkStorage.read(chunkPos).join();
			if (fetchedNbtCompoundOptionalFuture.isPresent()) {
				return fetchedNbtCompoundOptionalFuture.get();
			}
		} catch (Exception ex) {
			NoisiumChunkManager.LOGGER.error("Error occurred while fetching NBT data for chunk at {}", chunkPos);
		}

		return null;
	}

	private void saveChunk(@NotNull ChunkAccess chunkAccess) {
		@NotNull var chunkPosition = chunkAccess.getPos();
		syncRunnableConsumer.accept(() -> {
			pointOfInterestStorage.flush(chunkPosition);
			chunkAccess.setUnsaved(false);
			versionedChunkStorage.write(chunkPosition, ChunkSerializer.write(serverWorld, chunkAccess));
		});
	}

	// TODO: Move this into the constructor as a Supplier<ChunkPos, ProtoChunk>
	private @NotNull ProtoChunk generateChunk(@NotNull ChunkPos chunkPos, @NotNull Function<ChunkPos, IoWorldChunk> ioWorldChunkGetFunction,
			@NotNull Function<ChunkPos, IoWorldChunk> ioWorldChunkRemoveFunction) {
		// var serverLightingProvider = serverWorld.getLightEngine();
		var protoChunk = new ProtoChunk(chunkPos, UpgradeData.EMPTY, serverWorld, serverWorld.registryAccess().registryOrThrow(Registries.BIOME), null);
		List<ChunkAccess> chunkRegionChunks = List.of(protoChunk);
		var chunkRegion = new WorldGenRegion(serverWorld, chunkRegionChunks, ChunkStatus.FULL, 1);
		var blender = Blender.of(chunkRegion);
		var chunkRegionStructureAccessor = serverWorld.structureManager().forWorldGenRegion(chunkRegion);
		for (int sectionYPosition = protoChunk.getMinSection(); sectionYPosition < protoChunk.getMaxSection(); sectionYPosition++) {
			((SectionStorageExtension) pointOfInterestStorage).noisiumchunkmanager$createPointOfInterestSection(SectionPos.asLong(chunkPos.x, sectionYPosition, chunkPos.z));
		}

		protoChunk.setStatus(ChunkStatus.STRUCTURE_STARTS);
		// TODO: Move the structure placement calculator into NoisiumServerWorldChunkManager
		// TODO: Pass the structure template manager into NoisiumServerWorldChunkManager
		// TODO: Pass the shouldGenerateStructures boolean into NoisiumServerWorldChunkManager
		if (serverWorld.getServer().getWorldData().worldGenOptions().generateStructures()) {
			chunkGenerator.createStructures(serverWorld.registryAccess(), serverWorld.getChunkSource().getGeneratorState(), chunkRegionStructureAccessor, protoChunk,
					serverWorld.getServer().getStructureManager());
		}
		serverWorld.onStructureStartsAvailable(protoChunk);

		protoChunk.setStatus(ChunkStatus.STRUCTURE_REFERENCES);
		chunkGenerator.createReferences(chunkRegion, chunkRegionStructureAccessor, protoChunk);

		protoChunk.setStatus(ChunkStatus.BIOMES);
		protoChunk.fillBiomesFromNoise(chunkGenerator.getBiomeSource(), noiseConfig.sampler());

		protoChunk.setStatus(ChunkStatus.NOISE);
		NoiseSettings generationShapeConfig = ((NoiseChunkGeneratorAccessor) chunkGenerator).getSettings().value().noiseSettings().clampToHeightAccessor(protoChunk);
		int minimumYCellHeightRemainder = Math.floorDiv(generationShapeConfig.minY(), generationShapeConfig.getCellHeight());
		int chunkHeightCellHeightRemainder = Math.floorDiv(generationShapeConfig.height(), generationShapeConfig.getCellHeight());
		protoChunk = (ProtoChunk) ((NoiseChunkGeneratorAccessor) chunkGenerator).invokeDoFill(blender, chunkRegionStructureAccessor, noiseConfig, protoChunk, minimumYCellHeightRemainder,
				chunkHeightCellHeightRemainder);

		protoChunk.setStatus(ChunkStatus.SURFACE);
		chunkGenerator.buildSurface(chunkRegion, chunkRegionStructureAccessor, noiseConfig, protoChunk);

		protoChunk.setStatus(ChunkStatus.CARVERS);
		chunkGenerator.applyCarvers(chunkRegion, chunkRegion.getSeed(), noiseConfig, chunkRegion.getBiomeManager(), chunkRegionStructureAccessor, protoChunk, GenerationStep.Carving.AIR);

		protoChunk.setStatus(ChunkStatus.FEATURES);
		Heightmap.primeHeightmaps(protoChunk, EnumSet.of(Heightmap.Types.MOTION_BLOCKING, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Heightmap.Types.OCEAN_FLOOR, Heightmap.Types.WORLD_SURFACE));
		chunkGenerator.applyBiomeDecoration(chunkRegion, protoChunk, chunkRegionStructureAccessor);
		Blender.generateBorderTicks(chunkRegion, protoChunk);

		@NotNull var ioWorldChunkSectionArray = ioWorldChunkGetFunction.apply(chunkPos).getSections();
		for (int chunkSectionIndex = 0; chunkSectionIndex < ioWorldChunkSectionArray.length; chunkSectionIndex++) {
			@NotNull var ioWorldChunkSection = ioWorldChunkSectionArray[chunkSectionIndex];
			if (ioWorldChunkSection.hasOnlyAir()) {
				continue;
			}

			@NotNull var ioWorldChunkSectionBlockStateContainer = ioWorldChunkSection.getStates();
			ioWorldChunkSectionBlockStateContainer.acquire();

			@NotNull var protoChunkSectionBlockStateContainer = protoChunk.getSections()[chunkSectionIndex].getStates();
			protoChunkSectionBlockStateContainer.acquire();

			@NotNull var protoChunkPalettedContainerData = protoChunkSectionBlockStateContainer.data;
			@NotNull var protoChunkPaletteStorage = protoChunkPalettedContainerData.storage();
			@NotNull var ioWorldChunkPalettedContainerData = ioWorldChunkSectionBlockStateContainer.data;
			@NotNull var ioWorldChunkPaletteStorage = ioWorldChunkPalettedContainerData.storage();
			var ioWorldChunkPaletteStorageSize = ioWorldChunkPaletteStorage.getSize();
			if (protoChunkPaletteStorage.getRaw().length == 0) {
				protoChunkPaletteStorage = new SimpleBitStorage(ioWorldChunkPaletteStorage.getBits(), ioWorldChunkPaletteStorageSize);
			}

			for (int blockIndex = 0; blockIndex < ioWorldChunkPaletteStorageSize; blockIndex++) {
				@NotNull var blockState = ioWorldChunkPalettedContainerData.palette().valueFor(ioWorldChunkPaletteStorage.get(blockIndex));
				var blockStatePaletteValue = protoChunkPalettedContainerData.palette().idFor(blockState);
				if (blockState.equals(Blocks.AIR.defaultBlockState()) || blockStatePaletteValue > ((PackedIntegerArrayAccessor) protoChunkPaletteStorage).getMask()) {
					continue;
				}

				protoChunkPaletteStorage.getAndSet(blockIndex, blockStatePaletteValue);
			}

			ioWorldChunkSectionBlockStateContainer.release();
			protoChunkSectionBlockStateContainer.release();
		}

		ioWorldChunkRemoveFunction.apply(chunkPos);

		protoChunk.setStatus(ChunkStatus.INITIALIZE_LIGHT);
		protoChunk.initializeLightSources();

		protoChunk.setStatus(ChunkStatus.LIGHT);
		lightProtoChunk(protoChunk);

		protoChunk.setStatus(ChunkStatus.SPAWN);
		// chunkGenerator.spawnOriginalMobs(chunkRegion);

		protoChunk.setStatus(ChunkStatus.FULL);
		saveChunk(protoChunk);
		// TODO: Add a (Neo)Forge ChunkDataEvent.Save invoker
		// Also add a Fabric/Architectury chunk save event invoker
		// and run `serverLevel.getProfiler().incrementCounter("chunkSave");` on the chunk save event
		return protoChunk;
	}
}
