package io.github.steveplays28.noisiumchunkmanager.server.world.entity;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import io.github.steveplays28.noisiumchunkmanager.server.world.entity.event.ServerEntityEvent;
import io.github.steveplays28.noisiumchunkmanager.server.world.event.ServerTickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

// TODO: Reimplement the rest of ServerEntityManager and replace it
// TODO: Reimplement vanilla's ServerEntityManager's save method
public class ServerWorldEntityTracker {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger("Noisium Server World Entity Tracker");

	private final @NotNull Consumer<Packet<?>> packetSendConsumer;
	private final @NotNull Map<Integer, EntityTrackerEntry> entityTrackerEntries;

	public ServerWorldEntityTracker(@NotNull Consumer<Packet<?>> packetSendConsumer) {
		this.packetSendConsumer = packetSendConsumer;

		this.entityTrackerEntries = new HashMap<>();

		EntityEvent.ADD.register(this::onEntityAdded);
		ServerEntityEvent.REMOVE.register(this::onEntityRemoved);
		ServerTickEvent.SERVER_ENTITY_MOVEMENT_TICK.register(this::onTick);
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private EventResult onEntityAdded(@NotNull Entity entity, @NotNull World world) {
		if (world.isClient()) {
			return EventResult.pass();
		}

		var entityType = entity.getType();
		var entityTrackerEntry = new EntityTrackerEntry(
				(ServerWorld) world, entity, entityType.getTrackTickInterval(), entityType.alwaysUpdateVelocity(),
				packetSendConsumer
		);
		entityTrackerEntries.put(entity.getId(), entityTrackerEntry);

		var players = ((ServerWorld) world).getPlayers();
		for (int i = 0; i < players.size(); i++) {
			entityTrackerEntry.startTracking(players.get(i));
		}
		return EventResult.interruptTrue();
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private EventResult onEntityRemoved(@NotNull Entity entity, @NotNull World world) {
		var entityId = entity.getId();
		var entityTrackerEntry = entityTrackerEntries.get(entityId);
		if (entityTrackerEntry == null) {
			LOGGER.error("Tried removing an entity's entity tracker, but it was null. Entity: {}", entity);
			return EventResult.interruptDefault();
		}

		var players = ((ServerWorld) world).getPlayers();
		for (int i = 0; i < players.size(); i++) {
			entityTrackerEntry.stopTracking(players.get(i));
		}
		entityTrackerEntries.remove(entityId);
		return EventResult.interruptDefault();
	}

	private void onTick() {
		for (var entityTrackerEntry : entityTrackerEntries.values()) {
			entityTrackerEntry.tick();
		}
	}
}
