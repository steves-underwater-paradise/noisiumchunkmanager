package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Semaphore;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.world.level.chunk.PalettedContainer;

@Mixin(PalettedContainer.class)
public class PalettedContainerMixin {
	@Shadow
	@Final
	@Mutable
	private ThreadingDetector threadingDetector;

	@Unique
	private final Semaphore noisiumchunkmanager$lock = new Semaphore(1);

	@Inject(method = {
			"<init>(Lnet/minecraft/core/IdMap;Ljava/lang/Object;Lnet/minecraft/world/level/chunk/PalettedContainer$Strategy;)V",
			"<init>(Lnet/minecraft/core/IdMap;Lnet/minecraft/world/level/chunk/PalettedContainer$Strategy;Lnet/minecraft/world/level/chunk/PalettedContainer$Data;)V",
			"<init>(Lnet/minecraft/core/IdMap;Lnet/minecraft/world/level/chunk/PalettedContainer$Strategy;Lnet/minecraft/world/level/chunk/PalettedContainer$Configuration;Lnet/minecraft/util/BitStorage;Ljava/util/List;)V",
	}, at = @At("TAIL"))
	public void removeLockHelper(@NotNull CallbackInfo ci) {
		this.threadingDetector = null;
	}

	/**
	 * @reason Optimise locking.
	 * @author Steveplays28
	 */
	@Overwrite
	public void acquire() {
		noisiumchunkmanager$lock.acquireUninterruptibly();
	}

	/**
	 * @reason Optimise unlocking.
	 * @author Steveplays28
	 */
	@Overwrite
	public void release() {
		noisiumchunkmanager$lock.release();
	}
}
