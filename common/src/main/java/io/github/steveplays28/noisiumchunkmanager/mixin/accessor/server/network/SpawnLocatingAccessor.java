package io.github.steveplays28.noisiumchunkmanager.mixin.accessor.server.network;

import net.minecraft.server.network.SpawnLocating;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpawnLocating.class)
public interface SpawnLocatingAccessor {
	@Invoker
	static @Nullable BlockPos invokeFindOverworldSpawn(@NotNull ServerWorld serverWorld, int searchPositionX, int searchPositionZ) {
		throw new AssertionError();
	}
}
