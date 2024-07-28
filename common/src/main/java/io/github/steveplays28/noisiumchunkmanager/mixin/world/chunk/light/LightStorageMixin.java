package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk.light;

import io.github.steveplays28.noisiumchunkmanager.extension.world.chunk.light.LightStorageExtension;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@Mixin(LightStorage.class)
public abstract class LightStorageMixin implements LightStorageExtension {
	@Shadow
	@Final
	protected @NotNull ChunkToNibbleArrayMap<?> storage;
	@Shadow
	@Final
	protected @NotNull ChunkProvider chunkProvider;
	@Shadow
	@Final
	@Mutable
	protected @Nullable Long2ByteMap sectionPropagations;
	@Shadow
	@Final
	@Mutable
	protected @Nullable LongSet dirtySections;
	@Shadow
	@Final
	@Mutable
	protected @Nullable LongSet notifySections;
	@Shadow
	@Final
	@Mutable
	protected @Nullable Long2ObjectMap<ChunkNibbleArray> queuedSections;

	@Shadow
	@Final
	private LightType lightType;
	@Shadow
	@Final
	@Mutable
	private @Nullable LongSet enabledColumns;
	@Shadow
	@Final
	@Mutable
	private @Nullable LongSet columnsToRetain;
	@Shadow
	@Final
	@Mutable
	private @Nullable LongSet sectionsToRemove;

	@Shadow
	protected abstract boolean hasSection(long sectionPos);

	@Shadow
	protected abstract @Nullable ChunkNibbleArray getLightSection(long sectionPos, boolean cached);

	@Shadow
	protected abstract void onUnloadSection(long sectionPos);

	@Unique
	private Map<Long, Byte> noisiumchunkmanager$sectionPropagations;
	@Unique
	private Set<Long> noisiumchunkmanager$dirtySections;
	@Unique
	private Set<Long> noisiumchunkmanager$notifySections;
	@Unique
	private Map<Long, Object> noisiumchunkmanager$queuedSections;
	@Unique
	private Set<Long> noisiumchunkmanager$enabledColumns;
	@Unique
	private Set<Long> noisiumchunkmanager$columnsToRetain;
	@Unique
	private Set<Long> noisiumchunkmanager$sectionsToRemove;

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void noisiumchunkmanager$replaceSets(@NotNull CallbackInfo ci) {
		noisiumchunkmanager$sectionPropagations = new ConcurrentHashMap<>();
		noisiumchunkmanager$dirtySections = new ConcurrentSkipListSet<>();
		noisiumchunkmanager$notifySections = new ConcurrentSkipListSet<>();
		noisiumchunkmanager$queuedSections = new ConcurrentHashMap<>();
		noisiumchunkmanager$enabledColumns = new ConcurrentSkipListSet<>();
		noisiumchunkmanager$columnsToRetain = new ConcurrentSkipListSet<>();
		noisiumchunkmanager$sectionsToRemove = new ConcurrentSkipListSet<>();

		this.sectionPropagations = null;
		this.dirtySections = null;
		this.notifySections = null;
		this.queuedSections = null;
		this.enabledColumns = null;
		this.columnsToRetain = null;
		this.sectionsToRemove = null;
	}

	/**
	 * @author Steveplays28
	 * @reason Add a {@code null} check.
	 */
	@Overwrite
	public int get(long blockPosition) {
		@Nullable var chunkNibbleArray = this.getLightSection(ChunkSectionPos.fromBlockPos(blockPosition), true);
		if (chunkNibbleArray == null) {
			return 0;
		}

		return chunkNibbleArray.get(
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongX(blockPosition)),
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongY(blockPosition)),
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongZ(blockPosition))
		);
	}

	/**
	 * @author Steveplays28
	 * @reason Add thread-safety and add a {@code null} check.
	 */
	@Overwrite
	public void set(long blockPosition, int value) {
		long chunkSectionPosition = ChunkSectionPos.fromBlockPos(blockPosition);
		@Nullable var chunkNibbleArray = noisiumchunkmanager$dirtySections.add(chunkSectionPosition) ? this.storage.replaceWithCopy(
				chunkSectionPosition) : this.getLightSection(chunkSectionPosition, true);
		if (chunkNibbleArray == null) {
			return;
		}

		chunkNibbleArray.set(
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongX(blockPosition)),
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongY(blockPosition)),
				ChunkSectionPos.getLocalCoord(BlockPos.unpackLongZ(blockPosition)), value
		);
		ChunkSectionPos.forEachChunkSectionAround(blockPosition, noisiumchunkmanager$notifySections::add);
	}

	// noisiumchunkmanager$sectionPropagations
	@Redirect(method = {"setSectionStatus", "getStatus"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ByteMap;get(J)B"))
	private byte noisiumchunkmanager$getSectionPropagationFromSectionPropagationsThreadSafe(@Nullable Long2ByteMap instance, long sectionPosition) {
		return noisiumchunkmanager$sectionPropagations.getOrDefault(sectionPosition, (byte) 0);
	}

	@Redirect(method = "setSectionPropagation", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ByteMap;put(JB)B"))
	private byte noisiumchunkmanager$addSectionPropagationToSectionPropagationsThreadSafe(@Nullable Long2ByteMap instance, long sectionPosition, byte sectionPropagation) {
		@Nullable var addedSectionPropagation = noisiumchunkmanager$sectionPropagations.put(sectionPosition, sectionPropagation);
		if (addedSectionPropagation == null) {
			return 0;
		}

		return addedSectionPropagation;
	}

	@Redirect(method = "setSectionPropagation", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ByteMap;remove(J)B"))
	private byte noisiumchunkmanager$removeSectionPropagationFromSectionPropagationsThreadSafe(@Nullable Long2ByteMap instance, long sectionPosition) {
		@Nullable var removedSectionPropagation = noisiumchunkmanager$sectionPropagations.remove(sectionPosition);
		if (removedSectionPropagation == null) {
			return 0;
		}

		return removedSectionPropagation;
	}

	// noisiumchunkmanager$dirtySections
	@Redirect(method = {"method_51547", "updateLight", "queueForUpdate"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;add(J)Z"))
	private boolean noisiumchunkmanager$addDirtySectionToDirtySectionsThreadSafe(@Nullable LongSet instance, long section) {
		return noisiumchunkmanager$dirtySections.add(section);
	}

	@Redirect(method = "notifyChanges", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;isEmpty()Z", ordinal = 0))
	private boolean noisiumchunkmanager$checkIfDirtySectionsIsEmptyThreadSafe(@Nullable LongSet instance) {
		return noisiumchunkmanager$dirtySections.isEmpty();
	}

	@Redirect(method = "notifyChanges", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;clear()V", ordinal = 0))
	private void noisiumchunkmanager$clearDirtySectionsThreadSafe(@Nullable LongSet instance) {
		noisiumchunkmanager$dirtySections.clear();
	}

	// noisiumchunkmanager$notifySections
	@Redirect(method = "addNotifySections", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;add(J)Z"))
	private boolean noisiumchunkmanager$addSectionToNotifySectionsThreadSafe(@Nullable LongSet instance, long section) {
		return noisiumchunkmanager$notifySections.add(section);
	}

	@Redirect(method = "notifyChanges", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;isEmpty()Z", ordinal = 1))
	private boolean noisiumchunkmanager$checkIfNotifySectionsIsEmptyThreadSafe(@Nullable LongSet instance) {
		return noisiumchunkmanager$notifySections.isEmpty();
	}

	@Redirect(method = "notifyChanges", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;clear()V", ordinal = 1))
	private void noisiumchunkmanager$clearNotifySectionsThreadSafe(@Nullable LongSet instance) {
		noisiumchunkmanager$notifySections.clear();
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Inject(method = "notifyChanges", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;iterator()Lit/unimi/dsi/fastutil/longs/LongIterator;"), cancellable = true)
	private void noisiumchunkmanager$iterateOverNotifySectionsThreadSafe(@NotNull CallbackInfo ci) {
		for (@NotNull var notifySectionsIterator = noisiumchunkmanager$notifySections.iterator(); notifySectionsIterator.hasNext(); ) {
			@Nullable var notifySectionPosition = notifySectionsIterator.next();
			if (notifySectionPosition == null) {
				continue;
			}

			this.chunkProvider.onLightUpdate(this.lightType, ChunkSectionPos.from(notifySectionPosition));
		}

		noisiumchunkmanager$notifySections.clear();
		ci.cancel();
	}

	// noisiumchunkmanager$queuedSections
	@Redirect(method = {"getLightSection(J)Lnet/minecraft/world/chunk/ChunkNibbleArray;", "createSection"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"))
	private @Nullable Object noisiumchunkmanager$getSectionFromQueuedSectionsThreadSafe(@Nullable Long2ObjectMap<ChunkNibbleArray> instance, long section) {
		return noisiumchunkmanager$queuedSections.get(section);
	}

	@Redirect(method = {"updateLight", "enqueueSectionData"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;"))
	private @Nullable Object noisiumchunkmanager$addSectionToQueuedSectionsThreadSafe(@Nullable Long2ObjectMap<ChunkNibbleArray> instance, long section, Object chunkNibbleArray) {
		return noisiumchunkmanager$queuedSections.put(section, chunkNibbleArray);
	}

	@Redirect(method = {"updateLight", "enqueueSectionData"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;remove(J)Ljava/lang/Object;"))
	private @Nullable Object noisiumchunkmanager$removeSectionFromQueuedSectionsThreadSafe(@Nullable Long2ObjectMap<ChunkNibbleArray> instance, long section) {
		return noisiumchunkmanager$queuedSections.remove(section);
	}

	@Redirect(method = "updateLight", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMaps;fastIterator(Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;)Lit/unimi/dsi/fastutil/objects/ObjectIterator;"))
	private @NotNull ObjectIterator<Long2ObjectMap.Entry<ChunkNibbleArray>> noisiumchunkmanager$preventIteratingOverQueuedSectionsThreadUnsafely(@Nullable Long2ObjectMap<ChunkNibbleArray> instance) {
		return new ObjectIterator<>() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public @Nullable Long2ObjectMap.Entry<ChunkNibbleArray> next() {
				return null;
			}
		};
	}

	@Inject(method = "updateLight", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkToNibbleArrayMap;clearCache()V", ordinal = 1, shift = At.Shift.BEFORE))
	private void noisiumchunkmanager$iterateOverQueuedSectionsThreadSafe(@NotNull ChunkLightProvider<?, ?> chunkLightProvider, @NotNull CallbackInfo ci) {
		for (var queuedSectionsIterator = noisiumchunkmanager$queuedSections.entrySet().iterator(); queuedSectionsIterator.hasNext(); ) {
			var queuedSectionEntry = queuedSectionsIterator.next();

			long queuedSectionPosition = queuedSectionEntry.getKey();
			if (!this.hasSection(queuedSectionPosition)) {
				continue;
			}

			var chunkNibbleArray = queuedSectionEntry.getValue();
			if (this.storage.get(queuedSectionPosition) != chunkNibbleArray) {
				this.storage.put(queuedSectionPosition, (ChunkNibbleArray) chunkNibbleArray);
				noisiumchunkmanager$dirtySections.add(queuedSectionPosition);
			}

			queuedSectionsIterator.remove();
		}
	}

	// noisiumchunkmanager$enabledColumns
	@Redirect(method = "setColumnEnabled", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;add(J)Z"))
	private boolean noisiumchunkmanager$addColumnToEnabledColumnsThreadSafe(@Nullable LongSet instance, long section) {
		return noisiumchunkmanager$enabledColumns.add(section);
	}

	@Redirect(method = "setColumnEnabled", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;remove(J)Z"))
	private boolean noisiumchunkmanager$removeColumnFromEnabledColumnsThreadSafe(@Nullable LongSet instance, long section) {
		return noisiumchunkmanager$enabledColumns.remove(section);
	}

	@Redirect(method = "isSectionInEnabledColumn", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;contains(J)Z"))
	private boolean noisiumchunkmanager$checkIfEnabledColumnsContainsColumnThreadSafe(@Nullable LongSet instance, long section) {
		return noisiumchunkmanager$enabledColumns.contains(section);
	}

	// noisiumchunkmanager$columnsToRetain
	@Redirect(method = "setRetainColumn", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;add(J)Z"))
	private boolean noisiumchunkmanager$addColumnToColumnsToRetainThreadSafe(@Nullable LongSet instance, long section) {
		return noisiumchunkmanager$columnsToRetain.add(section);
	}

	@Redirect(method = "setRetainColumn", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;remove(J)Z"))
	private boolean noisiumchunkmanager$removeColumnFromColumnsToRetainThreadSafe(@Nullable LongSet instance, long section) {
		return noisiumchunkmanager$columnsToRetain.remove(section);
	}

	@Redirect(method = "updateLight", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;contains(J)Z"))
	private boolean noisiumchunkmanager$checkIfColumnsToRetainContainsColumnThreadSafe(@Nullable LongSet instance, long section) {
		return noisiumchunkmanager$columnsToRetain.contains(section);
	}

	// noisiumchunkmanager$sectionsToRemove
	@Redirect(method = "queueForRemoval", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;add(J)Z"))
	private boolean noisiumchunkmanager$addSectionToSectionsToRemoveThreadSafe(@Nullable LongSet instance, long section) {
		return noisiumchunkmanager$sectionsToRemove.add(section);
	}

	@Redirect(method = "queueForUpdate", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;remove(J)Z"))
	private boolean noisiumchunkmanager$removeColumnFromSectionsToRemoveThreadSafe(@Nullable LongSet instance, long section) {
		return noisiumchunkmanager$sectionsToRemove.remove(section);
	}

	@Redirect(method = "updateLight", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;iterator()Lit/unimi/dsi/fastutil/longs/LongIterator;"))
	private @NotNull LongIterator noisiumchunkmanager$preventIteratingOverSectionsToRemoveThreadUnsafely(@Nullable LongSet instance) {
		return new LongIterator() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public long nextLong() {
				return 0;
			}
		};
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Inject(method = "updateLight", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkToNibbleArrayMap;clearCache()V", ordinal = 0))
	private void noisiumchunkmanager$iterateOverSectionsToRemoveThreadSafe(@NotNull ChunkLightProvider<?, ?> lightProvider, @NotNull CallbackInfo ci) {
		for (var sectionsToRemoveIterator = noisiumchunkmanager$sectionsToRemove.iterator(); sectionsToRemoveIterator.hasNext(); ) {
			var sectionToRemove = sectionsToRemoveIterator.next();
			var queuedChunkNibbleArray = noisiumchunkmanager$getQueuedSections().remove(sectionToRemove);
			var chunkNibbleArray = this.storage.removeChunk(sectionToRemove);
			if (!noisiumchunkmanager$columnsToRetain.contains(ChunkSectionPos.withZeroY(sectionToRemove))) {
				continue;
			}
			if (queuedChunkNibbleArray != null) {
				noisiumchunkmanager$getQueuedSections().put(sectionToRemove, queuedChunkNibbleArray);
				continue;
			}
			if (chunkNibbleArray == null) {
				continue;
			}

			noisiumchunkmanager$getQueuedSections().put(sectionToRemove, chunkNibbleArray);
		}
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Inject(method = "updateLight", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/ChunkToNibbleArrayMap;clearCache()V", ordinal = 0, shift = At.Shift.AFTER))
	private void noisiumchunkmanager$unloadSectionsToRemoveThreadSafe(@NotNull ChunkLightProvider<?, ?> lightProvider, @NotNull CallbackInfo ci) {
		for (var sectionsToRemoveIterator = noisiumchunkmanager$sectionsToRemove.iterator(); sectionsToRemoveIterator.hasNext(); ) {
			var sectionToRemove = sectionsToRemoveIterator.next();
			this.onUnloadSection(sectionToRemove);
			noisiumchunkmanager$dirtySections.add(sectionToRemove);
		}
	}

	@Redirect(method = "updateLight", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSet;clear()V"))
	private void noisiumchunkmanager$clearSectionsToRemoveThreadSafe(@Nullable LongSet instance) {
		noisiumchunkmanager$sectionsToRemove.clear();
	}

	@Override
	public Map<Long, Object> noisiumchunkmanager$getQueuedSections() {
		return noisiumchunkmanager$queuedSections;
	}
}
