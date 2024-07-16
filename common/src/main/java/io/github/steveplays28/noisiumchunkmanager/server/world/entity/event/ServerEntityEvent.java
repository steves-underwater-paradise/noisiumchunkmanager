package io.github.steveplays28.noisiumchunkmanager.server.world.entity.event;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public interface ServerEntityEvent {
	/**
	 * @see Remove
	 */
	@NotNull Event<Remove> REMOVE = EventFactory.createEventResult();

	@FunctionalInterface
	interface Remove {
		/**
		 * Invoked when the server is about to remove an entity.
		 */
		@NotNull EventResult onServerEntityRemove(@NotNull Entity entity, @NotNull World world);
	}
}
