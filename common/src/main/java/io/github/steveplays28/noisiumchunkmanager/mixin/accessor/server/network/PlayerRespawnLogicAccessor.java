package io.github.steveplays28.noisiumchunkmanager.mixin.accessor.server.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerRespawnLogic.class)
public interface PlayerRespawnLogicAccessor {
	@Invoker
	static @Nullable BlockPos invokeGetOverworldRespawnPos(@NotNull ServerLevel serverWorld, int searchPositionX, int searchPositionZ) {
		throw new AssertionError();
	}
}
