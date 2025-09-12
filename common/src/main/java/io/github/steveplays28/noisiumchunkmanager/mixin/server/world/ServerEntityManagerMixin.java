package io.github.steveplays28.noisiumchunkmanager.mixin.server.world;

import io.github.steveplays28.noisiumchunkmanager.server.event.world.entity.ServerEntityEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PersistentEntitySectionManager.class, priority = 500)
public class ServerEntityManagerMixin<T extends EntityAccess> {
	@Inject(method = "stopTracking", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$entityRemoveEventImplementation(T entity, CallbackInfo ci) {
		if (entity instanceof Entity entityInWorld) {
			if (ServerEntityEvent.REMOVE.invoker().onServerEntityRemove(
					entityInWorld, entityInWorld.level()).interruptsFurtherEvaluation()) {
				ci.cancel();
			}
		}
	}

	@Inject(method = "autoSave", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelSave(@NotNull CallbackInfo ci) {
		// TODO: Invoke an entity manager save event that's used in NoisiumServerWorldEntityTracker
		ci.cancel();
	}

	@Inject(method = "saveAll", at = @At(value = "HEAD"), cancellable = true)
	private void noisiumchunkmanager$cancelFlush(@NotNull CallbackInfo ci) {
		ci.cancel();
	}
}
