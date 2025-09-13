package io.github.steveplays28.noisiumchunkmanager.mixin.world.storage;

import io.github.steveplays28.noisiumchunkmanager.NoisiumChunkManager;
import io.github.steveplays28.noisiumchunkmanager.extension.world.level.chunk.storage.SectionStorageExtension;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
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
import net.minecraft.world.level.chunk.storage.SectionStorage;

@Mixin(SectionStorage.class)
public abstract class SectionStorageMixin<R> implements SectionStorageExtension {
	@Shadow
	@Final
	private Function<Runnable, R> factory;
	
	@Shadow
	protected abstract void setDirty(long pos);
	
	/**
	 * The loaded elements of this {@link SectionStorage}.
	 * Thread-safe.
	 */
	@Unique
	private final @NotNull Map<Long, Optional<R>> noisiumchunkmanager$loadedElements = new ConcurrentHashMap<>();

	@Redirect(method = {"get", "writeColumn", "setDirty"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;get(J)Ljava/lang/Object;", remap = false))
	private Object noisiumchunkmanager$getLoadedElementsThreadSafe(@NotNull Long2ObjectMap<Optional<R>> instance, long chunkSectionPosition) {
		return noisiumchunkmanager$loadedElements.get(chunkSectionPosition);
	}

	@SuppressWarnings("unchecked")
	@Redirect(method = {"getOrCreate", "readColumn"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;", remap = false))
	private Object noisiumchunkmanager$putLoadedElementsThreadSafe(@NotNull Long2ObjectMap<Optional<R>> instance, long chunkSectionPosition, @NotNull Object object) {
		return noisiumchunkmanager$loadedElements.put(chunkSectionPosition, (Optional<R>) object);
	}

	@Inject(method = "getOrCreate", at = @At(value = "INVOKE", target = "Lnet/minecraft/Util;pauseInIde(Ljava/lang/Throwable;)Ljava/lang/Throwable;", shift = At.Shift.BEFORE), cancellable = true)
	private void noisiumchunkmanager$preventThrowingExceptionOnChunkSectionPositionOutOfBounds(long chunkSectionPosition, @NotNull CallbackInfoReturnable<R> cir) {
		NoisiumChunkManager.LOGGER.debug("Chunk section position ({}) was out of bounds.", chunkSectionPosition);
		R object = this.factory.apply(() -> this.setDirty(chunkSectionPosition));
		noisiumchunkmanager$loadedElements.put(chunkSectionPosition, Optional.of(object));
		cir.setReturnValue(object);
	}
	
	@Override
	public void noisiumchunkmanager$createPointOfInterestSection(long chunkSectionPosition) {
		noisiumchunkmanager$loadedElements.put(chunkSectionPosition, Optional.of(this.factory.apply(() -> this.setDirty(chunkSectionPosition))));
	}
}
