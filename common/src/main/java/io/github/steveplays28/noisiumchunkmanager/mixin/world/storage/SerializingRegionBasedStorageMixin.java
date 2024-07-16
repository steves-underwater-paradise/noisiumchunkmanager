package io.github.steveplays28.noisiumchunkmanager.mixin.world.storage;

import io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.storage.SerializingRegionBasedStorage;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Mixin(SerializingRegionBasedStorage.class)
public abstract class SerializingRegionBasedStorageMixin<R> {
	@Shadow
	protected abstract void onUpdate(long pos);

	@Shadow
	@Final
	private Function<Runnable, R> factory;
	/**
	 * The loaded elements of this {@link SerializingRegionBasedStorage}.
	 * Thread-safe.
	 */
	@Unique
	private final @NotNull Map<Long, Optional<R>> noisiumchunkmanager$loadedElements = new ConcurrentHashMap<>();

	@Redirect(method = {"getIfLoaded", "serialize", "onUpdate"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;"))
	private Object noisiumchunkmanager$getLoadedElementsThreadSafe(@NotNull Long2ObjectMap<Optional<R>> instance, long l) {
		return noisiumchunkmanager$loadedElements.get(l);
	}

	@SuppressWarnings("unchecked")
	@Redirect(method = {"getOrCreate", "update"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;"))
	private Object noisiumchunkmanager$putLoadedElementsThreadSafe(@NotNull Long2ObjectMap<Optional<R>> instance, long l, @NotNull Object object) {
		return noisiumchunkmanager$loadedElements.put(l, (Optional<R>) object);
	}

	@Inject(method = "getOrCreate", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;throwOrPause(Ljava/lang/Throwable;)Ljava/lang/Throwable;", shift = At.Shift.BEFORE), cancellable = true)
	private void noisiumchunkmanager$preventThrowingExceptionOnChunkSectionPositionOutOfBounds(long chunkSectionPosition, @NotNull CallbackInfoReturnable<R> cir) {
		NoisiumChunkManager.LOGGER.debug("Chunk section position ({}) was out of bounds.", chunkSectionPosition);
		R object = this.factory.apply(() -> this.onUpdate(chunkSectionPosition));
		this.noisiumchunkmanager$loadedElements.put(chunkSectionPosition, Optional.of(object));
		cir.setReturnValue(object);
	}
}
