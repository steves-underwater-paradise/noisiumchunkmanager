package io.github.steveplays28.noisiumchunkmanager.server;

import dev.architectury.event.events.common.LifecycleEvent;
import io.github.steveplays28.noisiumchunkmanager.server.player.ServerPlayerBlockUpdater;

public class NoisiumChunkManagerServerInitialiser {
	/**
	 * Keeps a reference to the {@link ServerPlayerBlockUpdater}, to make sure it doesn't get garbage collected until the object is no longer necessary.
	 */
	@SuppressWarnings("unused")
	private static ServerPlayerBlockUpdater serverPlayerBlockUpdater;

	public static void initialise() {
		LifecycleEvent.SERVER_STARTED.register(instance -> serverPlayerBlockUpdater = new ServerPlayerBlockUpdater());
		LifecycleEvent.SERVER_STOPPING.register(instance -> serverPlayerBlockUpdater = null);
	}
}
