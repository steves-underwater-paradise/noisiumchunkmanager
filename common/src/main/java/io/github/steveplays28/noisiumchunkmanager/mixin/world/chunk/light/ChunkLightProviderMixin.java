package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk.light;

import io.github.steveplays28.noisiumchunkmanager.mixin.accessor.LightStorageAccessor;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.chunk.light.ChunkLightProvider;
import net.minecraft.world.chunk.light.LightStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Deque;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListSet;

@Mixin(ChunkLightProvider.class)
public abstract class ChunkLightProviderMixin {
	@Shadow
	@Final
	protected @NotNull LightStorage<?> lightStorage;

	@Shadow
	@Final
	@Mutable
	private @Nullable LongOpenHashSet blockPositionsToCheck;
	@Shadow
	@Final
	@Mutable
	private @Nullable LongArrayFIFOQueue field_44734;
	@Shadow
	@Final
	@Mutable
	private @Nullable LongArrayFIFOQueue field_44735;

	@Shadow
	protected abstract void method_51529(long var1);

	@Shadow
	protected abstract int method_51570();

	@Shadow
	protected abstract void clearChunkCache();

	@Shadow
	protected abstract int method_51567();

	@Unique
	private Set<Long> noisiumchunkmanager$blockPositionsToCheck;
	@Unique
	private Deque<Long> noisiumchunkmanager$field_44734;
	@Unique
	private Deque<Long> noisiumchunkmanager$field_44735;

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void noisiumchunkmanager$replaceSets(@NotNull CallbackInfo ci) {
		noisiumchunkmanager$blockPositionsToCheck = new ConcurrentSkipListSet<>();
		noisiumchunkmanager$field_44734 = new ConcurrentLinkedDeque<>();
		noisiumchunkmanager$field_44735 = new ConcurrentLinkedDeque<>();

		this.blockPositionsToCheck = null;
		this.field_44734 = null;
		this.field_44735 = null;
	}

	@Redirect(method = "checkBlock", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;add(J)Z"))
	private boolean noisiumchunkmanager$addBlockPositionToBlockPositionsToCheckThreadSafe(@Nullable LongOpenHashSet instance, long blockPosition) {
		return noisiumchunkmanager$blockPositionsToCheck.add(blockPosition);
	}

	@Redirect(method = "hasUpdates", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;isEmpty()Z"))
	private boolean noisiumchunkmanager$checkIfBlockPositionsToCheckIsEmptyThreadSafe(@Nullable LongOpenHashSet instance) {
		return noisiumchunkmanager$blockPositionsToCheck.isEmpty();
	}

	/**
	 * @author Steveplays28
	 * @reason Add thread-safety.
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	@Overwrite
	public int doLightUpdates() {
		for (@NotNull var blockPositionsToCheckIterator = noisiumchunkmanager$blockPositionsToCheck.iterator(); blockPositionsToCheckIterator.hasNext(); ) {
			@Nullable var blockPositionToCheck = blockPositionsToCheckIterator.next();
			if (blockPositionToCheck == null) {
				continue;
			}

			this.method_51529(blockPositionToCheck);
		}

		noisiumchunkmanager$blockPositionsToCheck.clear();
		var i = this.method_51570();
		this.clearChunkCache();

		@NotNull var castedLightStorage = (LightStorageAccessor) this.lightStorage;
		castedLightStorage.invokeUpdateLight((ChunkLightProvider<?, ?>) (Object) this);
		castedLightStorage.invokeNotifyChanges();
		return i + this.method_51567();
	}

	// noisiumchunkmanager$field_44734
	@Redirect(method = "method_51570", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongArrayFIFOQueue;isEmpty()Z"))
	private boolean noisiumchunkmanager$checkIfField_44734IsEmptyThreadSafe(@Nullable LongArrayFIFOQueue instance) {
		return noisiumchunkmanager$field_44734.isEmpty();
	}

	@Redirect(method = "method_51570", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongArrayFIFOQueue;dequeueLong()J"))
	private long noisiumchunkmanager$removeFirstFromField_44734ThreadSafe(@Nullable LongArrayFIFOQueue instance) {
		return noisiumchunkmanager$field_44734.removeFirst();
	}

	@Redirect(method = "method_51565", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongArrayFIFOQueue;enqueue(J)V"))
	private void noisiumchunkmanager$AddFlagsToField_44734ThreadSafe(@Nullable LongArrayFIFOQueue instance, long value) {
		noisiumchunkmanager$field_44734.add(value);
	}

	@Redirect(method = "hasUpdates", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongArrayFIFOQueue;isEmpty()Z", ordinal = 0))
	private boolean noisiumchunkmanager$checkIfField_44734IsEmptyThreadSafe2(@Nullable LongArrayFIFOQueue instance) {
		return noisiumchunkmanager$field_44734.isEmpty();
	}

	// noisiumchunkmanager$field_44735
	@Redirect(method = "method_51567", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongArrayFIFOQueue;isEmpty()Z"))
	private boolean noisiumchunkmanager$checkIfField_44735IsEmptyThreadSafe(@Nullable LongArrayFIFOQueue instance) {
		return noisiumchunkmanager$field_44735.isEmpty();
	}

	@Redirect(method = "method_51567", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongArrayFIFOQueue;dequeueLong()J"))
	private long noisiumchunkmanager$removeFirstFromField_44735ThreadSafe(@Nullable LongArrayFIFOQueue instance) {
		return noisiumchunkmanager$field_44735.removeFirst();
	}

	@Redirect(method = "method_51566", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongArrayFIFOQueue;enqueue(J)V"))
	private void noisiumchunkmanager$AddFlagsToField_44735ThreadSafe(@Nullable LongArrayFIFOQueue instance, long value) {
		noisiumchunkmanager$field_44735.add(value);
	}

	@Redirect(method = "hasUpdates", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongArrayFIFOQueue;isEmpty()Z", ordinal = 1))
	private boolean noisiumchunkmanager$checkIfField_44735IsEmptyThreadSafe2(@Nullable LongArrayFIFOQueue instance) {
		return noisiumchunkmanager$field_44735.isEmpty();
	}
}
