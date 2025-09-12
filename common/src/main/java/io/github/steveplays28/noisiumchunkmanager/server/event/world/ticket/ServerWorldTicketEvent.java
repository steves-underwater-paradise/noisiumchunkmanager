package io.github.steveplays28.noisiumchunkmanager.server.event.world.ticket;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

public interface ServerWorldTicketEvent {
	/**
	 * @see TicketCreated
	 */
	Event<TicketCreated> TICKET_CREATED = EventFactory.createLoop();
	/**
	 * @see TicketCreated
	 */
	Event<TicketRemoved> TICKET_REMOVED = EventFactory.createLoop();

	@FunctionalInterface
	interface TicketCreated {
		/**
		 * Invoked after a chunk ticket has been created.
		 *
		 * @param serverWorld   The {@link ServerLevel}.
		 * @param ticketType    The {@link TicketType}.
		 * @param chunkPosition The {@link ChunkPos}.
		 * @param radius        The radius, in chunks.
		 */
		void onTicketCreated(@NotNull ServerLevel serverWorld, @NotNull TicketType<?> ticketType, @NotNull ChunkPos chunkPosition, int radius);
	}

	@FunctionalInterface
	interface TicketRemoved {
		/**
		 * Invoked after a chunk ticket has been removed.
		 *
		 * @param serverWorld   The {@link ServerLevel}.
		 * @param chunkPosition The {@link ChunkPos}.
		 */
		void onTicketRemoved(@NotNull ServerLevel serverWorld, @NotNull ChunkPos chunkPosition);
	}
}
