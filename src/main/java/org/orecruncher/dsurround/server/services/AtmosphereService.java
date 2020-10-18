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

package org.orecruncher.dsurround.server.services;

import javax.annotation.Nonnull;

import org.orecruncher.dsurround.ModBase;
import org.orecruncher.dsurround.ModOptions;
import org.orecruncher.dsurround.capabilities.CapabilityDimensionInfo;
import org.orecruncher.dsurround.capabilities.dimension.IDimensionInfo;
import org.orecruncher.dsurround.lib.compat.ModEnvironment;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.relauncher.Side;

public final class AtmosphereService extends Service {

	AtmosphereService() {
		super("AtmosphereService");
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void tickEvent(@Nonnull final TickEvent.WorldTickEvent event) {
		if (event.side == Side.SERVER && event.phase == Phase.END)
			getGenerator(event.world).update();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onWorldLoad(final WorldEvent.Load e) {
		final World world = e.getWorld();
		if (world.isRemote)
			return;

		getGenerator(world);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onWorldUnload(final WorldEvent.Unload e) {
		final World world = e.getWorld();
		if (world.isRemote)
			return;

		final int dimId = world.provider.getDimension();
		this.generators.remove(dimId);
	}

	private final Int2ObjectOpenHashMap<WeatherGenerator> generators = new Int2ObjectOpenHashMap<>();

	private WeatherGenerator getGenerator(@Nonnull final World world) {
		final int dimId = world.provider.getDimension();
		WeatherGenerator result = this.generators.get(dimId);
		if (result == null) {
			this.generators.put(dimId, result = createGenerator(world));
		}
		return result;
	}

	private boolean doVanillaRain() {
		return ModOptions.rain.doVanillaRain || ModEnvironment.Weather2.isLoaded();
	}

	private WeatherGenerator createGenerator(@Nonnull final World world) {
		WeatherGenerator result = null;
		IDimensionInfo info = CapabilityDimensionInfo.getCapability(world);
		if (info != null && info.hasWeather()) {
			final int dimId = world.provider.getDimension();
			if (doVanillaRain()) {
				if (dimId != -1) {
					result = new WeatherGeneratorVanilla(world);
				}
			} else if (dimId == -1) {
				result = new WeatherGeneratorNether(world);
			} else {
				result = new WeatherGenerator(world);
			}
		}

		if (result == null) {
			result = new WeatherGeneratorNone(world);
		}

		ModBase.log().info("Creating %s weather generator for dimension [%s]", result.name(),
				world.provider.getDimensionType().getName());

		return result;
	}

}
