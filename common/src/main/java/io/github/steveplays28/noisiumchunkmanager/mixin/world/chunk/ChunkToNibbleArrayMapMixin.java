package io.github.steveplays28.noisiumchunkmanager.mixin.world.chunk;

import io.github.steveplays28.noisiumchunkmanager.extension.world.chunk.light.ChunkToNibbleArrayMapExtension;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkToNibbleArrayMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ChunkToNibbleArrayMap.class)
public class ChunkToNibbleArrayMapMixin implements ChunkToNibbleArrayMapExtension {
	@Shadow
	@Final
	@Mutable
	protected @Nullable Long2ObjectOpenHashMap<ChunkNibbleArray> arrays;

	@Unique
	private Map<Long, ChunkNibbleArray> noisiumchunkmanager$arrays;

	@Inject(method = "<init>", at = @At(value = "TAIL"))
	private void noisiumchunkmanager$replaceMap(@NotNull CallbackInfo ci) {
		noisiumchunkmanager$arrays = new ConcurrentHashMap<>();

		if (this.arrays == null) {
			return;
		}

		noisiumchunkmanager$arrays.putAll(this.arrays);
		this.arrays = null;
	}

	@Redirect(method = {"replaceWithCopy", "get"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;get(J)Ljava/lang/Object;"))
	private @Nullable Object noisiumchunkmanager$getThreadSafe(@Nullable Long2ObjectOpenHashMap<ChunkNibbleArray> instance, long chunkPosition) {
		return noisiumchunkmanager$arrays.get(chunkPosition);
	}

	@Redirect(method = {"replaceWithCopy", "put"}, at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;put(JLjava/lang/Object;)Ljava/lang/Object;"))
	private @Nullable Object noisiumchunkmanager$putThreadSafe(@Nullable Long2ObjectOpenHashMap<ChunkNibbleArray> instance, long chunkPosition, @NotNull Object chunkNibbleArray) {
		return noisiumchunkmanager$arrays.put(chunkPosition, (ChunkNibbleArray) chunkNibbleArray);
	}

	@Redirect(method = "removeChunk", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;remove(J)Ljava/lang/Object;"))
	private @Nullable Object noisiumchunkmanager$removeThreadSafe(@Nullable Long2ObjectOpenHashMap<ChunkNibbleArray> instance, long chunkPosition) {
		return noisiumchunkmanager$arrays.remove(chunkPosition);
	}

	@Redirect(method = "containsKey", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectOpenHashMap;containsKey(J)Z"))
	private boolean noisiumchunkmanager$containsKeyThreadSafe(@Nullable Long2ObjectOpenHashMap<ChunkNibbleArray> instance, long chunkPosition) {
		return noisiumchunkmanager$arrays.containsKey(chunkPosition);
	}

	@Override
	public Map<Long, ChunkNibbleArray> noisiumchunkmanager$getArrays() {
		return noisiumchunkmanager$arrays;
	}
}
