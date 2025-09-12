package io.github.steveplays28.noisiumchunkmanager.mixin.world.gen.feature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.BitSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;

import static net.minecraft.world.level.levelgen.feature.OreFeature.canPlaceOre;

@Mixin(value = OreFeature.class, priority = 500)
public class OreFeatureMixin {
	@Inject(method = "doPlace", at = @At(value = "HEAD"), cancellable = true)
	public void noisiumchunkmanager$generateVeinPartWithoutChunkSectionCache(
			@NotNull WorldGenLevel world,
			@NotNull RandomSource random,
			@NotNull OreConfiguration config,
			double startX,
			double endX,
			double startZ,
			double endZ,
			double startY,
			double endY,
			int x,
			int y,
			int z,
			int horizontalSize,
			int verticalSize,
			CallbackInfoReturnable<Boolean> cir
	) {
		int i = 0;
		BitSet bitSet = new BitSet(horizontalSize * verticalSize * horizontalSize);
		BlockPos.MutableBlockPos mutableBlockPosition = new BlockPos.MutableBlockPos();
		int size = config.size;
		final double[] ds = new double[size * 4];

		for (int k = 0; k < size; ++k) {
			float f = (float) k / (float) size;
			double d = Mth.lerp(f, startX, endX);
			double e = Mth.lerp(f, startY, endY);
			double g = Mth.lerp(f, startZ, endZ);
			double h = random.nextDouble() * (double) size / 16.0;
			double l = ((double) (Mth.sin((float) Math.PI * f) + 1.0F) * h + 1.0) / 2.0;
			ds[k * 4] = d;
			ds[k * 4 + 1] = e;
			ds[k * 4 + 2] = g;
			ds[k * 4 + 3] = l;
		}

		for (int k = 0; k < size - 1; ++k) {
			if (!(ds[k * 4 + 3] <= 0.0)) {
				for (int m = k + 1; m < size; ++m) {
					if (!(ds[m * 4 + 3] <= 0.0)) {
						double d = ds[k * 4] - ds[m * 4];
						double e = ds[k * 4 + 1] - ds[m * 4 + 1];
						double g = ds[k * 4 + 2] - ds[m * 4 + 2];
						double h = ds[k * 4 + 3] - ds[m * 4 + 3];
						if (h * h > d * d + e * e + g * g) {
							if (h > 0.0) {
								ds[m * 4 + 3] = -1.0;
							} else {
								ds[k * 4 + 3] = -1.0;
							}
						}
					}
				}
			}
		}

		for (int m = 0; m < size; ++m) {
			double d = ds[m * 4 + 3];
			if (!(d < 0.0)) {
				double e = ds[m * 4];
				double g = ds[m * 4 + 1];
				double h = ds[m * 4 + 2];
				int n = Math.max(Mth.floor(e - d), x);
				int o = Math.max(Mth.floor(g - d), y);
				int p = Math.max(Mth.floor(h - d), z);
				int q = Math.max(Mth.floor(e + d), n);
				int r = Math.max(Mth.floor(g + d), o);
				int s = Math.max(Mth.floor(h + d), p);

				for (int t = n; t <= q; ++t) {
					double u = ((double) t + 0.5 - e) / d;
					if (u * u < 1.0) {
						for (int v = o; v <= r; ++v) {
							double w = ((double) v + 0.5 - g) / d;
							if (u * u + w * w < 1.0) {
								for (int aa = p; aa <= s; ++aa) {
									double ab = ((double) aa + 0.5 - h) / d;
									if (u * u + w * w + ab * ab < 1.0 && !world.isOutsideBuildHeight(v)) {
										int ac = t - x + (v - y) * horizontalSize + (aa - z) * horizontalSize * verticalSize;
										if (bitSet.get(ac)) {
											continue;
										}

										bitSet.set(ac);
										mutableBlockPosition.set(t, v, aa);
										if (!world.ensureCanWrite(mutableBlockPosition)) {
											continue;
										}

										@Nullable final var chunk = world.getChunk(mutableBlockPosition);
										if (chunk == null) {
											continue;
										}

										@Nullable final var blockState = chunk.getBlockState(mutableBlockPosition);
										if (blockState == null) {
											continue;
										}

										for (OreConfiguration.TargetBlockState target : config.targetStates) {
											if (canPlaceOre(
													blockState, chunk::getBlockState, random, config, target, mutableBlockPosition)) {
												chunk.setBlockState(mutableBlockPosition, target.state, false);
												++i;
												break;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		cir.setReturnValue(i > 0);
	}
}
