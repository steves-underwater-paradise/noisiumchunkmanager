package io.github.steveplays28.noisiumchunkmanager.server.event.world.entity;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import dev.architectury.event.EventResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

public interface ServerEntityEvent {
	/**
	 * @see AfterAdded
	 */
	@NotNull Event<AfterAdded> AFTER_ADDED = EventFactory.createEventResult();
	/**
	 * @see AfterRemoved
	 */
	@NotNull Event<AfterRemoved> AFTER_REMOVED = EventFactory.createEventResult();

	@FunctionalInterface
	interface AfterAdded {
		/**
		 * Invoked after the server has added an entity.
		 */
		@NotNull
		EventResult afterServerEntityAdded(@NotNull ServerLevel serverLevel, @NotNull Entity entity);
	}

	@FunctionalInterface
	interface AfterRemoved {
		/**
		 * Invoked after the server has removed an entity.
		 */
		@NotNull
		EventResult afterServerEntityRemoved(@NotNull ServerLevel serverLevel, @NotNull Entity entity);
	}
}
