package io.github.steveplays28.noisiumchunkmanager.mixin.server.world;

import io.github.steveplays28.noisiumchunkmanager.server.world.entity.event.ServerEntityEvent;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.world.entity.EntityLike;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerEntityManager.class, priority = 500)
public class ServerEntityManagerMixin<T extends EntityLike> {
	@Inject(method = "stopTracking", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$entityRemoveEventImplementation(T entity, CallbackInfo ci) {
		if (entity instanceof Entity entityInWorld) {
			if (ServerEntityEvent.REMOVE.invoker().onServerEntityRemove(
					entityInWorld, entityInWorld.getWorld()).interruptsFurtherEvaluation()) {
				ci.cancel();
			}
		}
	}

	@Inject(method = "save", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelSave(@NotNull CallbackInfo ci) {
		// TODO: Invoke an entity manager save event that's used in NoisiumServerWorldEntityTracker
		ci.cancel();
	}

	@Inject(method = "flush", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelFlush(@NotNull CallbackInfo ci) {
		ci.cancel();
	}
}
