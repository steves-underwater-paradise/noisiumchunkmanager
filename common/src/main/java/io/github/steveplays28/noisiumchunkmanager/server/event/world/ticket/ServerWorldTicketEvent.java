package io.github.steveplays28.noisiumchunkmanager.server.event.world.ticket;

import dev.architectury.event.Event;
import dev.architectury.event.EventFactory;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
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
		 * @param serverWorld   The {@link ServerWorld}.
		 * @param ticketType    The {@link ChunkTicketType}.
		 * @param chunkPosition The {@link ChunkPos}.
		 * @param radius        The radius, in chunks.
		 */
		void onTicketCreated(@NotNull ServerWorld serverWorld, @NotNull ChunkTicketType<?> ticketType, @NotNull ChunkPos chunkPosition, int radius);
	}

	@FunctionalInterface
	interface TicketRemoved {
		/**
		 * Invoked after a chunk ticket has been removed.
		 *
		 * @param serverWorld   The {@link ServerWorld}.
		 * @param chunkPosition The {@link ChunkPos}.
		 */
		void onTicketRemoved(@NotNull ServerWorld serverWorld, @NotNull ChunkPos chunkPosition);
	}
}
