package io.github.steveplays28.noisiumchunkmanager.mixin.accessor;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerEntity.class)
public interface ServerEntityAccessor {
	@Accessor
	@NotNull Entity getEntity();
}
