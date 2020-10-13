/*
 * This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
 *
 * Copyright (c) OreCruncher
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.orecruncher.dsurround.client.renderer.weather;

import java.util.Random;

import org.orecruncher.dsurround.ModOptions;
import org.orecruncher.dsurround.capabilities.CapabilityDimensionInfo;
import org.orecruncher.dsurround.capabilities.CapabilitySeasonInfo;
import org.orecruncher.dsurround.capabilities.dimension.IDimensionInfo;
import org.orecruncher.dsurround.capabilities.season.ISeasonInfo;
import org.orecruncher.dsurround.capabilities.season.PrecipitationType;
import org.orecruncher.dsurround.client.fx.ParticleCollections;
import org.orecruncher.dsurround.client.handlers.SoundEffectHandler;
import org.orecruncher.dsurround.client.sound.SoundBuilder;
import org.orecruncher.dsurround.client.sound.SoundInstance;
import org.orecruncher.dsurround.client.weather.Weather;
import org.orecruncher.dsurround.client.weather.compat.RandomThings;
import org.orecruncher.lib.WorldUtils;
import org.orecruncher.lib.chunk.ClientChunkCache;
import org.orecruncher.lib.gfx.ParticleHelper;
import org.orecruncher.lib.random.XorShiftRandom;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class StormSplashRenderer {

	protected static final int PARTICLE_SOUND_CHANCE = 3;
	private static final Int2ObjectOpenHashMap<StormSplashRenderer> splashRenderers = new Int2ObjectOpenHashMap<>();
	private static final StormSplashRenderer DEFAULT = new StormSplashRenderer();

	static {
		splashRenderers.defaultReturnValue(DEFAULT);
		splashRenderers.put(-1, new NetherSplashRenderer());
		splashRenderers.put(1, new NullSplashRenderer());
	}

	public static void renderStormSplashes(final int dimensionId, final EntityRenderer renderer) {
		splashRenderers.get(dimensionId).addRainParticles(renderer);
	}

	protected final Random RANDOM = new XorShiftRandom();
	protected final NoiseGeneratorSimplex GENERATOR = new NoiseGeneratorSimplex(this.RANDOM);
	protected final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

	protected int rainSoundCounter = 0;

	protected float calculateRainSoundVolume(final World world) {
		final float currentVolume = Weather.getCurrentVolume();
		final float bounds = currentVolume * 0.25F;
		final float adjust = MathHelper.clamp(
				(float) (this.GENERATOR.getValue((world.getWorldTime() % 24000L) / 100.0D, 1) / 5.0F), -bounds, bounds);
		return MathHelper.clamp(currentVolume + adjust, 0, 1F);
	}

	protected void spawnBlockParticle(final IBlockState state, final boolean dust, final World world, final double x,
			final double y, final double z) {
		final Block block = state.getBlock();
		EnumParticleTypes particleType = null;

		if (dust || block == Blocks.SOUL_SAND) {
			return;
		} else if ((block == Blocks.NETHERRACK || block == Blocks.MAGMA) && ModOptions.rain.enableNetherrackMagmaSplashEffect && this.RANDOM.nextInt(20) == 0) {
			particleType = EnumParticleTypes.LAVA;
		} else if (state.getMaterial() == Material.LAVA) {
			particleType = EnumParticleTypes.SMOKE_NORMAL;
		} else if (WorldUtils.isFullWaterBlock(state)) {
			ParticleCollections.addWaterRipple(world, x, y, z);
		} else if (state.getMaterial() != Material.AIR) {
			ParticleCollections.addRainSplash(world, x, y, z);
		}

		if (particleType != null)
			ParticleHelper.spawnParticle(particleType, x, y, z);
	}

	protected SoundEvent getBlockSoundFX(final Block block, final PrecipitationType pt) {
		if (pt == PrecipitationType.DUST)
			return Weather.getWeatherProperties().getDustSound();
		if (block == Blocks.NETHERRACK)
			return SoundEvents.BLOCK_LAVA_POP;
		return Weather.getWeatherProperties().getStormSound();
	}

	protected BlockPos getPrecipitationHeight(final ISeasonInfo season, final int range, final BlockPos pos) {
		return season.getPrecipitationHeight(pos);
	}

	protected void playSplashSound(final ISeasonInfo season, final World world, final Entity player, double x, double y,
			double z) {

		this.pos.setPos(x, y - 1, z);
		final PrecipitationType pt = season.getPrecipitationType(this.pos, null);
		final Block block = ClientChunkCache.instance().getBlockState(this.pos).getBlock();
		final SoundEvent sound = getBlockSoundFX(block, pt);
		if (sound != null) {
			final float volume = calculateRainSoundVolume(world);
			float pitch = 1.0F;
			final int playerY = MathHelper.floor(player.posY);
			this.pos.setPos(player.posX, 0, player.posZ);
			if (y > player.posY + 1.0D && season.getPrecipitationHeight(this.pos).getY() > playerY)
				pitch = 0.5F;
			pitch -= (this.RANDOM.nextFloat() - this.RANDOM.nextFloat()) * 0.1F;
			this.pos.setPos(x, y, z);

			final SoundInstance fx = SoundBuilder.builder(sound, SoundCategory.WEATHER).setVolume(volume)
					.setPitch(pitch).setPosition(this.pos).build();
			SoundEffectHandler.INSTANCE.playSound(fx);
		}
	}

	public void addRainParticles(final EntityRenderer theThis) {
		final Minecraft mc = Minecraft.getMinecraft();
		if (mc.gameSettings.particleSetting == 2)
			return;

		final Entity entity = mc.getRenderViewEntity();
		if (entity == null)
			return;

		final World world = mc.world;
		IDimensionInfo info = CapabilityDimensionInfo.getCapability(world);
		if (info != null && !info.hasWeather())
			return;

		float rainStrengthFactor = Weather.getIntensityLevel();
		if (!mc.gameSettings.fancyGraphics)
			rainStrengthFactor /= 2.0F;

		if (rainStrengthFactor <= 0.0F)
			return;

		this.RANDOM.setSeed(RenderWeather.getRendererUpdateCount() * 312987231L);
		final int playerX = MathHelper.floor(entity.posX);
		final int playerY = MathHelper.floor(entity.posY);
		final int playerZ = MathHelper.floor(entity.posZ);
		double spawnX = 0.0D;
		double spawnY = 0.0D;
		double spawnZ = 0.0D;
		int particlesSpawned = 0;

		final int RANGE = Math.max((ModOptions.effects.specialEffectRange + 1) / 2, 10);
		final float rangeFactor = RANGE / 10.0F;
		int particleCount = (int) (100 * rainStrengthFactor * rainStrengthFactor * rangeFactor);

		if (mc.gameSettings.particleSetting == 1)
			particleCount >>= 1;

		final ISeasonInfo season = CapabilitySeasonInfo.getCapability(world);

		for (int j1 = 0; j1 < particleCount; ++j1) {
			final int locX = playerX + this.RANDOM.nextInt(RANGE) - this.RANDOM.nextInt(RANGE);
			final int locZ = playerZ + this.RANDOM.nextInt(RANGE) - this.RANDOM.nextInt(RANGE);
			this.pos.setPos(locX, 0, locZ);

			if (RandomThings.shouldRain(world, this.pos))
				continue;

			final BlockPos precipHeight = getPrecipitationHeight(season, RANGE / 2, this.pos);
			final PrecipitationType pt = season.getPrecipitationType(precipHeight, null);
			final boolean hasDust = pt == PrecipitationType.DUST;

			if ((hasDust || pt == PrecipitationType.RAIN) && precipHeight.getY() <= playerY + RANGE
					&& precipHeight.getY() >= playerY - RANGE) {

				final BlockPos blockPos = precipHeight.down();
				final IBlockState state = ClientChunkCache.instance().getBlockState(blockPos);
				final double posX = locX + this.RANDOM.nextFloat();
				final double posY = precipHeight.getY() + 0.1F - state.getBoundingBox(world, blockPos).minY;
				final double posZ = locZ + this.RANDOM.nextFloat();

				spawnBlockParticle(state, hasDust, world, posX, posY, posZ);

				if (this.RANDOM.nextInt(++particlesSpawned) == 0) {
					spawnX = posX;
					spawnY = posY;
					spawnZ = posZ;
				}
			}
		}

		if (particlesSpawned > 0 && this.RANDOM.nextInt(PARTICLE_SOUND_CHANCE) < this.rainSoundCounter++) {
			this.rainSoundCounter = 0;
			playSplashSound(season, world, entity, spawnX, spawnY, spawnZ);
		}
	}
}
