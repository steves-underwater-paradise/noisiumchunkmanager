package io.github.steveplays28.noisiumchunkmanager.server.world;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class ServerLevelLightEngine extends LevelLightEngine {
	private @Nullable Executor blockLightEngineThreadPoolExecutor;
	private @Nullable Executor skyLightEngineThreadPoolExecutor;

	public ServerLevelLightEngine(@NotNull ServerLevel serverLevel, @NotNull LightChunkGetter lightChunkGetter, boolean enableBlockLightEngine, boolean enableSkyLightEngine) {
		super(lightChunkGetter, enableBlockLightEngine, enableSkyLightEngine);
		if (enableBlockLightEngine) {
			this.blockLightEngineThreadPoolExecutor = Executors.newFixedThreadPool(1,
					new ThreadFactoryBuilder().setNameFormat("Noisium Server World Chunk Manager Block Light Engine " + serverLevel.dimensionType().effectsLocation() + " %d").build());
		}
		if (enableSkyLightEngine) {
			this.skyLightEngineThreadPoolExecutor = Executors.newFixedThreadPool(1,
					new ThreadFactoryBuilder().setNameFormat("Noisium Server World Chunk Manager Sky Light Engine " + serverLevel.dimensionType().effectsLocation() + " %d").build());
		}
	}

	@Override
	public void checkBlock(BlockPos blockPosition) {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> this.blockEngine.checkBlock(blockPosition), blockLightEngineThreadPoolExecutor).join();
		}
		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.checkBlock(blockPosition), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public boolean hasLightWork() {
		return this.skyEngine != null && CompletableFuture.supplyAsync(() -> this.skyEngine.hasLightWork(), skyLightEngineThreadPoolExecutor).join() ? true
				: this.blockEngine != null && CompletableFuture.supplyAsync(() -> this.blockEngine.hasLightWork(), blockLightEngineThreadPoolExecutor).join();
	}

	@Override
	public int runLightUpdates() {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> {
				if (!this.blockEngine.hasLightWork()) {
					return;
				}

				this.blockEngine.runLightUpdates();
			}, blockLightEngineThreadPoolExecutor);
		}
		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> {
				if (!this.skyEngine.hasLightWork()) {
					return;
				}

				this.skyEngine.runLightUpdates();
			}, skyLightEngineThreadPoolExecutor);
		}
		return 0;
	}

	@Override
	public void updateSectionStatus(SectionPos sectionPosition, boolean bl) {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> this.blockEngine.updateSectionStatus(sectionPosition, bl), blockLightEngineThreadPoolExecutor).join();
		}

		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.updateSectionStatus(sectionPosition, bl), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public void setLightEnabled(ChunkPos chunkPosition, boolean lightEnabled) {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> this.blockEngine.setLightEnabled(chunkPosition, lightEnabled), blockLightEngineThreadPoolExecutor).join();
		}

		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.setLightEnabled(chunkPosition, lightEnabled), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public void propagateLightSources(ChunkPos chunkPosition) {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> this.blockEngine.propagateLightSources(chunkPosition), blockLightEngineThreadPoolExecutor).join();
		}

		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.propagateLightSources(chunkPosition), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public String getDebugData(LightLayer lightLayer, SectionPos sectionPosition) {
		if (lightLayer == LightLayer.BLOCK) {
			if (this.blockEngine != null) {
				return CompletableFuture.supplyAsync(() -> this.blockEngine.getDebugData(sectionPosition.asLong()), blockLightEngineThreadPoolExecutor).join();
			}
		} else if (this.skyEngine != null) {
			return CompletableFuture.supplyAsync(() -> this.skyEngine.getDebugData(sectionPosition.asLong()), skyLightEngineThreadPoolExecutor).join();
		}

		return "n/a";
	}

	@Override
	public LayerLightSectionStorage.SectionType getDebugSectionType(LightLayer lightLayer, SectionPos sectionPosition) {
		if (lightLayer == LightLayer.BLOCK) {
			if (this.blockEngine != null) {
				return CompletableFuture.supplyAsync(() -> this.blockEngine.getDebugSectionType(sectionPosition.asLong()), blockLightEngineThreadPoolExecutor).join();
			}
		} else if (this.skyEngine != null) {
			return CompletableFuture.supplyAsync(() -> this.skyEngine.getDebugSectionType(sectionPosition.asLong()), skyLightEngineThreadPoolExecutor).join();
		}

		return LayerLightSectionStorage.SectionType.EMPTY;
	}

	@Override
	public void queueSectionData(LightLayer lightLayer, SectionPos sectionPosition, @Nullable DataLayer dataLayer) {
		if (lightLayer == LightLayer.BLOCK) {
			if (this.blockEngine != null) {
				CompletableFuture.runAsync(() -> this.blockEngine.queueSectionData(sectionPosition.asLong(), dataLayer), blockLightEngineThreadPoolExecutor).join();
			}
		} else if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.queueSectionData(sectionPosition.asLong(), dataLayer), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public void retainData(ChunkPos chunkPosition, boolean retainData) {
		if (this.blockEngine != null) {
			CompletableFuture.runAsync(() -> this.blockEngine.retainData(chunkPosition, retainData), blockLightEngineThreadPoolExecutor).join();
		}

		if (this.skyEngine != null) {
			CompletableFuture.runAsync(() -> this.skyEngine.retainData(chunkPosition, retainData), skyLightEngineThreadPoolExecutor).join();
		}
	}

	@Override
	public int getRawBrightness(BlockPos blockPosition, int i) {
		int j = this.skyEngine == null ? 0 : CompletableFuture.supplyAsync(() -> this.skyEngine.getLightValue(blockPosition), skyLightEngineThreadPoolExecutor).join() - i;
		int k = this.blockEngine == null ? 0 : CompletableFuture.supplyAsync(() -> this.blockEngine.getLightValue(blockPosition), blockLightEngineThreadPoolExecutor).join();
		return Math.max(k, j);
	}

	@Override
	public boolean lightOnInSection(SectionPos sectionPosition) {
		long sectionPositionAsLong = sectionPosition.asLong();
		return this.blockEngine == null || CompletableFuture.supplyAsync(() -> this.blockEngine.storage.lightOnInSection(sectionPositionAsLong), blockLightEngineThreadPoolExecutor).join()
				&& (this.skyEngine == null || CompletableFuture.supplyAsync(() -> this.skyEngine.storage.lightOnInSection(sectionPositionAsLong), skyLightEngineThreadPoolExecutor).join());
	}
}
