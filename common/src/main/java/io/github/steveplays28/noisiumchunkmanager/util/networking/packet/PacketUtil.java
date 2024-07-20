package io.github.steveplays28.noisiumchunkmanager.util.networking.packet;

import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PacketUtil {
	/**
	 * Sends a packet to a {@link List} of players.
	 *
	 * @param players The {@link List} of players.
	 * @param packet  The {@link Packet} that should be sent to the {@link List} of players.
	 */
	@SuppressWarnings("ForLoopReplaceableByForEach")
	public static void sendPacketToPlayers(@NotNull List<ServerPlayerEntity> players, @NotNull Packet<?> packet) {
		for (int i = 0; i < players.size(); i++) {
			players.get(i).networkHandler.sendPacket(packet);
		}
	}
}
