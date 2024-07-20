package io.github.steveplays28.noisiumchunkmanager.server.world.ticket;

/**
 * Stores specific data of a {@link net.minecraft.server.world.ChunkTicketType}.
 *
 * @param startTick     The start tick.
 * @param duration      The duration, in ticks.
 */
public record ServerWorldTicket(long startTick, long duration) {
	// NO-OP
}
