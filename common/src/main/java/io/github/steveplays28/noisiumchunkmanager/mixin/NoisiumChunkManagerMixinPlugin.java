package io.github.steveplays28.noisiumchunkmanager.mixin;

import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static io.github.steveplays28.noisiumchunkmanager.util.ModLoaderUtil.isModPresent;

public class NoisiumChunkManagerMixinPlugin implements IMixinConfigPlugin {
	private static final @NotNull Supplier<Boolean> TRUE = () -> true;
	private static final @NotNull String DISTANT_HORIZONS_MOD_ID = "distanthorizons";
	private static final @NotNull String MODERNFIX_MOD_ID = "modernfix";
	private static final @NotNull Map<String, Supplier<Boolean>> CONDITIONS = ImmutableMap.of(
			"io.github.steveplays28.noisiumchunkmanager.mixin.compat.distanthorizons.common.wrappers.world.gen.DHBatchGenerationEnvironmentMixin",
			() -> isModPresent(DISTANT_HORIZONS_MOD_ID),
			"io.github.steveplays28.noisiumchunkmanager.mixin.compat.modernfix.ModernFixMixin",
			() -> isModPresent(MODERNFIX_MOD_ID)
	);

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return CONDITIONS.getOrDefault(mixinClassName, TRUE).get();
	}

	@Override
	public void onLoad(String mixinPackage) {}

	@Override
	public @Nullable String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public @Nullable List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
