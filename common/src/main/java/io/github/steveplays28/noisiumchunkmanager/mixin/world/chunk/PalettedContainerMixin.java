package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk;

import net.minecraft.util.thread.LockHelper;
import net.minecraft.world.chunk.PalettedContainer;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(PalettedContainer.class)
public class PalettedContainerMixin {
	@Shadow
	@Final
	@Mutable
	private LockHelper lockHelper;

	@Unique
	private final Lock noisiumchunkmanager$lock = new ReentrantLock();

	@Inject(
			method = {
					"<init>(Lnet/minecraft/util/collection/IndexedIterable;Ljava/lang/Object;Lnet/minecraft/world/chunk/PalettedContainer$PaletteProvider;)V",
					"<init>(Lnet/minecraft/util/collection/IndexedIterable;Lnet/minecraft/world/chunk/PalettedContainer$PaletteProvider;Lnet/minecraft/world/chunk/PalettedContainer$Data;)V",
					"<init>(Lnet/minecraft/util/collection/IndexedIterable;Lnet/minecraft/world/chunk/PalettedContainer$PaletteProvider;Lnet/minecraft/world/chunk/PalettedContainer$DataProvider;Lnet/minecraft/util/collection/PaletteStorage;Ljava/util/List;)V",
			},
			at = @At("TAIL")
	)
	public void removeLockHelper(@NotNull CallbackInfo ci) {
		this.lockHelper = null;
	}

	/**
	 * @reason Optimise locking.
	 * @author Steveplays28
	 */
	@Overwrite
	public void lock() {
		noisiumchunkmanager$lock.lock();
	}

	/**
	 * @reason Optimise unlocking.
	 * @author Steveplays28
	 */
	@Overwrite
	public void unlock() {
		noisiumchunkmanager$lock.unlock();
	}
}
