package io.github.steveplays28.noisiumchunkmanager.server.world.ticket;

import dev.architectury.event.events.common.TickEvent;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.ticket.ServerWorldTicketEvent;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ServerWorldTicketTracker {
	private final @NotNull ServerWorld serverWorld;
	private final @NotNull BiConsumer<ChunkPos, Integer> loadChunksInRadiusBiConsumer;
	private final @NotNull Consumer<ChunkPos> unloadChunkConsumer;
	private final @NotNull Map<ChunkPos, ServerWorldTicket> tickets;

	public ServerWorldTicketTracker(@NotNull ServerWorld serverWorld, @NotNull BiConsumer<ChunkPos, Integer> loadChunksInRadiusBiConsumer, @NotNull Consumer<ChunkPos> unloadChunkConsumer) {
		this.serverWorld = serverWorld;
		this.loadChunksInRadiusBiConsumer = loadChunksInRadiusBiConsumer;
		this.unloadChunkConsumer = unloadChunkConsumer;

		this.tickets = new HashMap<>();

		ServerWorldTicketEvent.TICKET_CREATED.register((ticketServerWorld, ticketType, chunkPosition, radius) -> {
			if (ticketServerWorld != serverWorld) {
				return;
			}

			onTicketCreated(ticketServerWorld, ticketType, chunkPosition, radius);
		});
		ServerWorldTicketEvent.TICKET_REMOVED.register((ticketServerWorld, chunkPosition) -> {
			if (ticketServerWorld != serverWorld) {
				return;
			}

			onTicketRemoved(chunkPosition);
		});
		TickEvent.SERVER_LEVEL_POST.register(instance -> {
			if (instance != serverWorld) {
				return;
			}

			tick();
		});
	}

	private void onTicketCreated(@NotNull ServerWorld serverWorld, @NotNull ChunkTicketType<?> ticketType, @NotNull ChunkPos chunkPosition, int radius) {
		if (tickets.containsKey(chunkPosition)) {
			return;
		}

		loadChunksInRadiusBiConsumer.accept(chunkPosition, radius);
		tickets.put(chunkPosition, new ServerWorldTicket(serverWorld.getTime(), ticketType.getExpiryTicks()));
	}

	private void onTicketRemoved(@NotNull ChunkPos chunkPosition) {
		if (!tickets.containsKey(chunkPosition)) {
			return;
		}

		unloadChunkConsumer.accept(chunkPosition);
		tickets.remove(chunkPosition);
	}

	private void tick() {
		for (@NotNull var ticketEntry : tickets.entrySet()) {
			@NotNull var ticket = ticketEntry.getValue();
			// TODO: Store endTick in the ServerWorldTicket instead of the startTick and duration
			if (ticket.startTick() + ticket.duration() > this.serverWorld.getTime()) {
				return;
			}

			@NotNull var ticketChunkPosition = ticketEntry.getKey();
			unloadChunkConsumer.accept(ticketChunkPosition);
			tickets.remove(ticketChunkPosition);
		}
	}
}
