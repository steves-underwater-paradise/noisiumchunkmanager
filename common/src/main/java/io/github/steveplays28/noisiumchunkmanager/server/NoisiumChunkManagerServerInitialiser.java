package io.github.steveplays28.noisiumchunkmanager.server;

import dev.architectury.event.events.common.LifecycleEvent;

public class NoisiumChunkManagerServerInitialiser {
	/**
	 * Keeps a reference to the {@link io.github.steveplays28.noisiumchunkmanager.experimental.server.player.ServerPlayerBlockUpdater}, to make sure it doesn't get garbage collected until the object is no longer necessary.
	 */
	@SuppressWarnings("unused")
	private static io.github.steveplays28.noisiumchunkmanager.experimental.server.player.ServerPlayerBlockUpdater serverPlayerBlockUpdater;

	public static void initialise() {
		LifecycleEvent.SERVER_STARTED.register(instance -> serverPlayerBlockUpdater = new io.github.steveplays28.noisiumchunkmanager.experimental.server.player.ServerPlayerBlockUpdater());
		LifecycleEvent.SERVER_STOPPING.register(instance -> serverPlayerBlockUpdater = null);
	}
}
