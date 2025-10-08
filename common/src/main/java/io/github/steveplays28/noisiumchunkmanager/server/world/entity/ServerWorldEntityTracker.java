package io.github.steveplays28.noisiumchunkmanager.server.world.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Iterables;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import io.github.steveplays28.noisiumchunkmanager.mixin.accessor.ServerEntityAccessor;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.ServerTickEvent;
import io.github.steveplays28.noisiumchunkmanager.server.event.world.entity.ServerEntityEvent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.phys.AABB;

// TODO: Reimplement vanilla's ServerEntityManager's save method
public class ServerWorldEntityTracker {
	private static final @NotNull Logger LOGGER = LoggerFactory.getLogger("Noisium Server World Entity Tracker");

	private final @NotNull ServerLevel serverLevel;
	private final @NotNull EntityPersistentStorage<Entity> entityPersistentStorage;
	private final @NotNull Consumer<Packet<?>> packetSendConsumer;
	private final @NotNull Map<Integer, ServerEntity> entityTrackerEntriesById;
	private final @NotNull Map<UUID, ServerEntity> entityTrackerEntriesByUUID;
	private final @NotNull LevelEntityGetter<Entity> levelEntityGetter;

	public ServerWorldEntityTracker(@NotNull ServerLevel serverLevel, @NotNull EntityPersistentStorage<Entity> entityPersistentStorage, @NotNull Consumer<Packet<?>> packetSendConsumer) {
		this.serverLevel = serverLevel;
		this.entityPersistentStorage = entityPersistentStorage;
		this.packetSendConsumer = packetSendConsumer;

		this.entityTrackerEntriesById = new HashMap<>();
		this.entityTrackerEntriesByUUID = new HashMap<>();
		this.levelEntityGetter = new LevelEntityGetter<Entity>() {
			@Override
			public @Nullable Entity get(int index) {
				@Nullable var serverEntity = entityTrackerEntriesById.get(index);
				if (serverEntity == null) {
					return null;
				}

				return ((ServerEntityAccessor) serverEntity).getEntity();
			}

			@Override
			public @Nullable Entity get(@NotNull UUID uuid) {
				@Nullable var serverEntity = entityTrackerEntriesByUUID.get(uuid);
				if (serverEntity == null) {
					return null;
				}

				return ((ServerEntityAccessor) serverEntity).getEntity();
			}

			@Override
			public <U extends Entity> void get(@NotNull EntityTypeTest<Entity, U> entityTypeTest, @NotNull AbortableIterationConsumer<U> abortableIterationConsumer) {
				for (ServerEntity serverEntity : entityTrackerEntriesById.values()) {
					U serverEntityWithType = (U) entityTypeTest.tryCast(((ServerEntityAccessor) serverEntity).getEntity());
					if (serverEntityWithType != null && abortableIterationConsumer.accept(serverEntityWithType).shouldAbort()) {
						return;
					}
				}
			}

			@Override
			public void get(@NotNull AABB axisAlignedBoundingBox, @NotNull Consumer<Entity> consumer) {
				// TODO
				// entitySectionStorage.getEntities(axisAlignedBoundingBox, AbortableIterationConsumer.forConsumer(consumer));
			}

			@Override
			public <U extends Entity> void get(@NotNull EntityTypeTest<Entity, U> entityTypeTest, @NotNull AABB axisAlignedBoundingBox,
					@NotNull AbortableIterationConsumer<U> abortableIterationConsumer) {
				// TODO
				// entitySectionStorage.getEntities(entityTypeTest, axisAlignedBoundingBox, abortableIterationConsumer);
			}

			@Override
			public @NotNull Iterable<Entity> getAll() {
				return Iterables.unmodifiableIterable(entityTrackerEntriesById.values().stream().map(serverEntity -> ((ServerEntityAccessor) serverEntity).getEntity()).toList());
			}
		};

		EntityEvent.ADD.register((@NotNull Entity entity, @NotNull Level level) -> {
			if (!level.equals(serverLevel)) {
				return EventResult.pass();
			}

			return onEntityAdded(serverLevel, entity);
		});
		PlayerEvent.PLAYER_JOIN.register(player -> {
			if (!player.serverLevel().equals(serverLevel)) {
				return;
			}

			EntityEvent.ADD.invoker().add(player, serverLevel);
		});
		ServerEntityEvent.AFTER_REMOVED.register(this::onEntityRemoved);
		PlayerEvent.PLAYER_QUIT.register(player -> {
			if (!player.serverLevel().equals(serverLevel)) {
				return;
			}

			ServerEntityEvent.AFTER_REMOVED.invoker().afterServerEntityRemoved(serverLevel, player);
		});
		ServerTickEvent.SERVER_ENTITY_MOVEMENT_TICK.register(this::onTick);
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private EventResult onEntityAdded(@NotNull ServerLevel serverLevel, @NotNull Entity entity) {
		var entityType = entity.getType();
		var entityTrackerEntry = new ServerEntity(serverLevel, entity, entityType.updateInterval(), entityType.trackDeltas(), packetSendConsumer);
		entityTrackerEntriesById.put(entity.getId(), entityTrackerEntry);
		entityTrackerEntriesByUUID.put(entity.getUUID(), entityTrackerEntry);

		var players = serverLevel.players();
		for (int i = 0; i < players.size(); i++) {
			entityTrackerEntry.addPairing(players.get(i));
		}
		ServerEntityEvent.AFTER_ADDED.invoker().afterServerEntityAdded(serverLevel, entity);
		return EventResult.interruptTrue();
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private EventResult onEntityRemoved(@NotNull ServerLevel serverLevel, @NotNull Entity entity) {
		var entityId = entity.getId();
		var entityTrackerEntry = entityTrackerEntriesById.get(entityId);
		if (entityTrackerEntry == null) {
			LOGGER.error("Tried removing an entity's entity tracker, but it was null. Entity: {}", entity);
			return EventResult.interruptDefault();
		}

		var players = ((ServerLevel) serverLevel).players();
		for (int i = 0; i < players.size(); i++) {
			entityTrackerEntry.removePairing(players.get(i));
		}
		entityTrackerEntriesById.remove(entityId);
		entityTrackerEntriesByUUID.remove(entity.getUUID());
		ServerEntityEvent.AFTER_REMOVED.invoker().afterServerEntityRemoved(serverLevel, entity);
		return EventResult.interruptDefault();
	}

	private void onTick() {
		for (var entityTrackerEntry : entityTrackerEntriesById.values()) {
			entityTrackerEntry.sendChanges();
		}
	}

	public @NotNull LevelEntityGetter<Entity> getEntities() {
		return levelEntityGetter;
	}
}
