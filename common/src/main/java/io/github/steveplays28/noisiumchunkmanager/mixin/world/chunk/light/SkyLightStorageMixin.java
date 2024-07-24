package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk.light;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.steveplays28.noisiumchunkmanager.extension.world.chunk.light.ChunkToNibbleArrayMapExtension;
import io.github.steveplays28.noisiumchunkmanager.extension.world.chunk.light.LightStorageExtension;
import io.github.steveplays28.noisiumchunkmanager.extension.world.chunk.light.SkyLightStorageDataExtension;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.light.LightStorage;
import net.minecraft.world.chunk.light.SkyLightStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(SkyLightStorage.class)
public abstract class SkyLightStorageMixin extends LightStorage<SkyLightStorage.Data> {
	protected SkyLightStorageMixin(LightType lightType, ChunkProvider chunkProvider, SkyLightStorage.Data lightData) {
		super(lightType, chunkProvider, lightData);
	}

	@Redirect(method = "getLight(JZ)I", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2IntOpenHashMap;get(J)I"))
	private int noisiumchunkmanager$getFromColumnToTopSectionThreadSafe(@Nullable Long2IntOpenHashMap instance, long column, @Local(ordinal = 0) @NotNull SkyLightStorage.Data data) {
		@NotNull var skyLightStorageDataExtension = (SkyLightStorageDataExtension) (Object) data;
		return skyLightStorageDataExtension.noisiumchunkmanager$getColumnToTopSection().getOrDefault(
				column, skyLightStorageDataExtension.noisiumchunkmanager$getColumnToTopSectionDefaultReturnValue());
	}

	@Redirect(method = {"onLoadSection", "onUnloadSection", "createSection", "isAtOrAboveTopmostSection", "getTopSectionForColumn"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2IntOpenHashMap;get(J)I"))
	private int noisiumchunkmanager$getFromColumnToTopSectionThreadSafe(@Nullable Long2IntOpenHashMap instance, long column) {
		@Nullable var skyLightStorageDataExtension = (SkyLightStorageDataExtension) (Object) this.storage;
		if (skyLightStorageDataExtension == null) {
			return 0;
		}

		return skyLightStorageDataExtension.noisiumchunkmanager$getColumnToTopSection().getOrDefault(
				column, skyLightStorageDataExtension.noisiumchunkmanager$getColumnToTopSectionDefaultReturnValue());
	}

	@Redirect(method = {"onLoadSection", "onUnloadSection"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2IntOpenHashMap;put(JI)I"))
	private int noisiumchunkmanager$putToColumnToTopSectionThreadSafe(@Nullable Long2IntOpenHashMap instance, long column, int topSection) {
		@NotNull var columnTopSection = ((SkyLightStorageDataExtension) (Object) this.storage).noisiumchunkmanager$getColumnToTopSection();
		@Nullable var topSectionInMap = columnTopSection.put(column, topSection);
		if (topSectionInMap == null) {
			return columnTopSection.size();
		}

		return topSectionInMap;
	}

	@Redirect(method = {"onLoadSection", "onUnloadSection"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2IntOpenHashMap;remove(J)I"))
	private int noisiumchunkmanager$removeFromColumnToTopSectionThreadSafe(@Nullable Long2IntOpenHashMap instance, long column) {
		return ((SkyLightStorageDataExtension) (Object) this.storage).noisiumchunkmanager$getColumnToTopSection().remove(column);
	}

	@Redirect(method = "onLoadSection", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2IntOpenHashMap;defaultReturnValue(I)V"))
	private void noisiumchunkmanager$setColumnToTopSectionDefaultReturnValueThreadSafe(@Nullable Long2IntOpenHashMap instance, int topSection) {
		((SkyLightStorageDataExtension) (Object) this.storage).noisiumchunkmanager$setColumnToTopSectionDefaultReturnValue(topSection);
	}

	@Redirect(method = "createSection", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"))
	private @Nullable Object noisiumchunkmanager$getSectionFromQueuedSectionsThreadSafe(@Nullable Long2ObjectMap<ChunkNibbleArray> instance, long section) {
		return ((LightStorageExtension) this).noisiumchunkmanager$getQueuedSections().get(section);
	}

	@Mixin(SkyLightStorage.Data.class)
	public static class DataMixin implements SkyLightStorageDataExtension {
		@Shadow
		@Final
		@Mutable
		@Nullable Long2IntOpenHashMap columnToTopSection;

		@Unique
		private Map<Long, Integer> noisiumchunkmanager$columnToTopSection;
		@Unique
		private int noisiumchunkmanager$columnToTopSectionDefaultReturnValue;

		@Inject(method = "<init>", at = @At(value = "TAIL"))
		private void noisiumchunkmanager$replaceSets(@NotNull Long2ObjectOpenHashMap<Object> arrays, @Nullable Long2IntOpenHashMap columnToTopSection, int minSectionY, @NotNull CallbackInfo ci) {
			noisiumchunkmanager$columnToTopSection = new ConcurrentHashMap<>();
			noisiumchunkmanager$columnToTopSectionDefaultReturnValue = minSectionY;

			this.columnToTopSection = null;
		}

		@Redirect(method = "copy()Lnet/minecraft/world/chunk/light/SkyLightStorage$Data;", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;clone()Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;"))
		private @NotNull Long2ObjectOpenHashMap<ChunkNibbleArray> noisiumchunkmanager$cloneThreadSafe(@Nullable Long2ObjectOpenHashMap<ChunkNibbleArray> instance) {
			return new Long2ObjectOpenHashMap<>(((ChunkToNibbleArrayMapExtension) this).noisiumchunkmanager$getArrays());
		}

		@Redirect(method = "copy()Lnet/minecraft/world/chunk/light/SkyLightStorage$Data;", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2IntOpenHashMap;clone()Lit/unimi/dsi/fastutil/longs/Long2IntOpenHashMap;"))
		private @NotNull Long2IntOpenHashMap noisiumchunkmanager$cloneThreadSafe(@Nullable Long2IntOpenHashMap instance) {
			return new Long2IntOpenHashMap(noisiumchunkmanager$columnToTopSection);
		}

		@Override
		public Map<Long, Integer> noisiumchunkmanager$getColumnToTopSection() {
			return noisiumchunkmanager$columnToTopSection;
		}

		@Override
		public int noisiumchunkmanager$getColumnToTopSectionDefaultReturnValue() {
			return noisiumchunkmanager$columnToTopSectionDefaultReturnValue;
		}

		@Override
		public void noisiumchunkmanager$setColumnToTopSectionDefaultReturnValue(int defaultReturnValue) {
			noisiumchunkmanager$columnToTopSectionDefaultReturnValue = defaultReturnValue;
		}
	}
}
