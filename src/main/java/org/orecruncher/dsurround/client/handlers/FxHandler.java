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
package org.orecruncher.dsurround.client.handlers;

import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;

import org.orecruncher.dsurround.ModOptions;
import org.orecruncher.dsurround.capabilities.CapabilityEntityFXData;
import org.orecruncher.dsurround.capabilities.entityfx.IEntityFX;
import org.orecruncher.dsurround.client.effects.EntityEffectHandler;
import org.orecruncher.dsurround.client.effects.EntityEffectLibrary;
import org.orecruncher.dsurround.client.effects.EventEffectLibrary;
import org.orecruncher.dsurround.client.effects.IParticleHelper;
import org.orecruncher.dsurround.client.effects.ISoundHelper;
import org.orecruncher.dsurround.client.handlers.EnvironStateHandler.EnvironState;
import org.orecruncher.dsurround.client.handlers.effects.BreathEffect;
import org.orecruncher.dsurround.client.handlers.effects.CraftingSoundEffect;
import org.orecruncher.dsurround.client.handlers.effects.EntityBowSoundEffect;
import org.orecruncher.dsurround.client.handlers.effects.EntityChatEffect;
import org.orecruncher.dsurround.client.handlers.effects.EntityFootprintEffect;
import org.orecruncher.dsurround.client.handlers.effects.EntityHealthPopoffEffect;
import org.orecruncher.dsurround.client.handlers.effects.EntitySwingEffect;
import org.orecruncher.dsurround.client.handlers.effects.PlayerToolBarSoundEffect;
import org.orecruncher.dsurround.client.handlers.effects.VillagerChatEffect;
import org.orecruncher.dsurround.client.sound.ISoundInstance;
import org.orecruncher.dsurround.event.DiagnosticEvent;
import org.orecruncher.dsurround.lib.OutOfBandTimerEMA;
import org.orecruncher.dsurround.registry.RegistryDataEvent;
import org.orecruncher.dsurround.registry.effect.EffectRegistry;
import org.orecruncher.lib.gfx.ParticleHelper;
import org.orecruncher.lib.math.TimerEMA;

import com.google.common.collect.ImmutableList;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class FxHandler extends EffectHandlerBase {

	private static final IParticleHelper PARTICLE_HELPER = ParticleHelper::addParticle;
	private static final ISoundHelper SOUND_HELPER = new ISoundHelper() {
		@Override
		public boolean playSound(@Nonnull final ISoundInstance sound) {
			return SoundEffectHandler.INSTANCE.playSound(sound);
		}

		@Override
		public void stopSound(@Nonnull final ISoundInstance sound) {
			SoundEffectHandler.INSTANCE.stopSound(sound);
		}
	};

	private static final EntityEffectLibrary library = new EntityEffectLibrary(PARTICLE_HELPER, SOUND_HELPER);

	static {
		library.register(BreathEffect.DEFAULT_FILTER, new BreathEffect.Factory());
		library.register(EntityChatEffect.DEFAULT_FILTER, new EntityChatEffect.Factory());
		library.register(VillagerChatEffect.DEFAULT_FILTER, new VillagerChatEffect.Factory());
		library.register(PlayerToolBarSoundEffect.DEFAULT_FILTER, new PlayerToolBarSoundEffect.Factory());
		library.register(EntityFootprintEffect.DEFAULT_FILTER, new EntityFootprintEffect.Factory());
		library.register(EntitySwingEffect.DEFAULT_FILTER, new EntitySwingEffect.Factory());
		library.register(EntityBowSoundEffect.DEFAULT_FILTER, new EntityBowSoundEffect.Factory());
		library.register(EntityHealthPopoffEffect.DEFAULT_FILTER, new EntityHealthPopoffEffect.Factory());
	}

	private final EventEffectLibrary eventLibrary = new EventEffectLibrary(PARTICLE_HELPER, SOUND_HELPER);

	private final TimerEMA compute = new OutOfBandTimerEMA("Entity Effect Updates");
	private long nanos;

	public FxHandler() {
		super("Special Effects");
	}

	@Override
	public void process(@Nonnull final EntityPlayer player) {
		this.compute.update(this.nanos);
		this.nanos = 0;
	}

	/**
	 * Used for diagnostics to get data about an Entity.
	 *
	 * @param entity Entity to get information on
	 * @return A list of EntityEffects, if any
	 */
	public List<String> getEffects(@Nonnull final Entity entity) {
		final IEntityFX caps = CapabilityEntityFXData.getCapability(entity);
		if (caps != null) {
			final EntityEffectHandler eh = caps.get();
			if (eh != null)
				return eh.getAttachedEffects();
		}
		return ImmutableList.of();
	}

	/**
	 * Whenever an Entity updates make sure we have an appropriate handler, and
	 * update it's state if necessary.
	 */
	@SubscribeEvent(receiveCanceled = true)
	public void onLivingUpdate(@Nonnull final LivingUpdateEvent event) {
		final Entity entity = event.getEntity();
		if (entity == null || !entity.getEntityWorld().isRemote)
			return;

		final long start = System.nanoTime();
		final IEntityFX cap = CapabilityEntityFXData.getCapability(entity);
		if (cap != null) {
			final double distanceThreshold = ModOptions.effects.specialEffectRange
					* ModOptions.effects.specialEffectRange;
			final boolean inRange = entity.getDistanceSq(EnvironState.getPlayer()) <= distanceThreshold;
			final EntityEffectHandler handler = cap.get();
			if (handler != null && !inRange) {
				cap.clear();
			} else if (handler == null && inRange && entity.isEntityAlive()) {
				library.create(entity).ifPresent(cap::set);
			} else if (handler != null) {
				handler.update();
			}
		}

		this.nanos += (System.nanoTime() - start);
	}

	protected void clearHandlers() {
		//@formatter:off
		EnvironState.getWorld().getLoadedEntityList().stream()
			.map(CapabilityEntityFXData::getCapability)
			.filter(Objects::nonNull)
			.forEach(IEntityFX::clear);
		//@formatter:on
	}

	/**
	 * Check if the player joining the world is the one sitting at the keyboard. If
	 * so we need to wipe out the existing handler list because the dimension
	 * changed.
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onEntityJoin(@Nonnull final EntityJoinWorldEvent event) {
		if (event.getWorld().isRemote && event.getEntity() instanceof EntityPlayerSP)
			clearHandlers();
	}

	/**
	 * Wipe out the effect handlers on a registry reload. Possible something changed
	 * in the effect configuration.
	 */
	@SubscribeEvent
	public void registryReload(@Nonnull final RegistryDataEvent.Reload event) {
		if (event.reg instanceof EffectRegistry)
			clearHandlers();
	}

	@Override
	public void onConnect() {
		this.eventLibrary.register(new CraftingSoundEffect());
		((DiagnosticHandler) EffectManager.instance().lookupService(DiagnosticHandler.class)).addTimer(this.compute);
	}

	@Override
	public void onDisconnect() {
		this.eventLibrary.cleanup();
	}

	@SubscribeEvent
	public void diagnostic(@Nonnull final DiagnosticEvent.Gather event) {
		final double range = ModOptions.effects.specialEffectRange;
		final BlockPos min = EnvironState.getPlayerPosition().add(-range, -range, -range);
		final BlockPos max = EnvironState.getPlayerPosition().add(range, range, range);
		final AxisAlignedBB box = new AxisAlignedBB(min, max);

		final int count = EnvironState.getWorld().getEntitiesWithinAABBExcludingEntity(EnvironState.getPlayer(), box)
				.size() + 1;
		event.output.add("Effected Entities: " + count);
	}

}
