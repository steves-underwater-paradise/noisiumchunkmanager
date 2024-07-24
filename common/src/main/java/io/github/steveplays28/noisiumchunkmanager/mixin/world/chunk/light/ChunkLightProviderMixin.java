package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk.light;

import io.github.steveplays28.noisiumchunkmanager.mixin.accessor.LightStorageAccessor;
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

import java.util.Set;
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
	protected abstract void method_51529(long var1);

	@Shadow
	protected abstract int method_51570();

	@Shadow
	protected abstract void clearChunkCache();

	@Shadow
	protected abstract int method_51567();

	@Unique
	private Set<Long> noisiumchunkmanager$blockPositionsToCheck;

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void noisiumchunkmanager$replaceSets(@NotNull CallbackInfo ci) {
		noisiumchunkmanager$blockPositionsToCheck = new ConcurrentSkipListSet<>();

		this.blockPositionsToCheck = null;
	}

	@Redirect(method = "checkBlock", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;add(J)Z"))
	private boolean noisiumchunkmanager$addBlockPositionToBlockPositionsToCheckThreadSafe(@Nullable LongOpenHashSet instance, long blockPosition) {
		noisiumchunkmanager$blockPositionsToCheck.add(blockPosition);
		return true;
	}

	@Redirect(method = "hasUpdates", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;isEmpty()Z"))
	private boolean noisiumchunkmanager$checkIfBlockPositionsToCheckIsEmptyThreadSafe(@Nullable LongOpenHashSet instance) {
		return !noisiumchunkmanager$blockPositionsToCheck.isEmpty();
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
}
