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
package org.orecruncher.dsurround.client.handlers.effects;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.orecruncher.dsurround.ModBase;
import org.orecruncher.dsurround.ModOptions;
import org.orecruncher.dsurround.capabilities.CapabilitySpeechData;
import org.orecruncher.dsurround.capabilities.speech.ISpeechData;
import org.orecruncher.dsurround.client.effects.EntityEffect;
import org.orecruncher.dsurround.client.effects.IEntityEffectFactory;
import org.orecruncher.dsurround.client.effects.IEntityEffectFactoryFilter;
import org.orecruncher.dsurround.registry.effect.EntityEffectInfo;
import org.orecruncher.lib.Translations;
import org.orecruncher.lib.WeightTable;
import org.orecruncher.lib.collections.ObjectArray;
import org.orecruncher.lib.compat.EntityUtil;
import org.orecruncher.lib.random.XorShiftRandom;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EntityChatEffect extends EntityEffect {

	private static final String SPLASH_TOKEN = "$MINECRAFT$";
	private static final ResourceLocation SPLASH_TEXT = new ResourceLocation("texts/splashes.txt");
	private static final Translations xlate = new Translations();
	private static final ObjectArray<String> minecraftSplashText = new ObjectArray<>();
	private static final Map<String, EntityChatData> messages = new Object2ObjectOpenHashMap<>();

	static {
		xlate.load("/assets/dsurround/dsurround/data/chat/");
		xlate.forAll(new WeightTableBuilder());
		xlate.transform(new Stripper());

		setTimers(EntitySquid.class, 600, EntityChatData.DEFAULT_RANDOM);

		try (final IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(SPLASH_TEXT)) {
			
			final InputStreamReader input = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8);
			final BufferedReader bufferedreader = new BufferedReader(input);
			
			String s;

			while ((s = bufferedreader.readLine()) != null) {
				s = s.trim();

				if (!s.isEmpty()) {
					minecraftSplashText.add(s);
				}
			}

		} catch (final Throwable ignore) {
		}

	}

	static class EntityChatData {
		public static final int DEFAULT_INTERVAL = 400;
		public static final int DEFAULT_RANDOM = 1200;

		public int baseInterval = DEFAULT_INTERVAL;
		public int baseRandom = DEFAULT_RANDOM;

		public final WeightTable<String> table = new WeightTable<>();
	}

	private static class WeightTableBuilder implements Predicate<Entry<String, String>> {

		private final Pattern TYPE_PATTERN = Pattern.compile("chat\\.([a-zA-Z.]*)\\.[0-9]*$");
		private final Pattern WEIGHT_PATTERN = Pattern.compile("^([0-9]*),(.*)");

		public WeightTableBuilder() {
		}

		@Override
		public boolean apply(@Nullable final Entry<String, String> input) {
			if (input == null)
				return true;

			final Matcher matcher1 = this.TYPE_PATTERN.matcher(input.getKey());
			if (matcher1.matches()) {
				final String key = matcher1.group(1).toLowerCase();
				final Matcher matcher2 = this.WEIGHT_PATTERN.matcher(input.getValue());
				if (matcher2.matches()) {
					EntityChatData data = messages.get(key);
					if (data == null)
						messages.put(key, data = new EntityChatData());
					final int weight = Integer.parseInt(matcher2.group(1));
					final String item = matcher2.group(2);
					data.table.add(new WeightTable.IItem<String>() {
						@Override
						public int getWeight() {
							return weight;
						}

						@Override
						public String getItem() {
							return item;
						}
					});
				} else {
					ModBase.log().warn("Invalid value in language file: %s", input.getValue());
				}
			} else {
				ModBase.log().warn("Invalid key in language file: %s", input.getKey());
			}

			return true;
		}

	}

	private static class Stripper implements Function<Entry<String, String>, String> {

		private final Pattern WEIGHT_PATTERN = Pattern.compile("^([0-9]*),(.*)");

		@Override
		public String apply(@Nullable final Entry<String, String> input) {
			if (input == null)
				return "NOT SET";
			final Matcher matcher = this.WEIGHT_PATTERN.matcher(input.getValue());
			return matcher.matches() ? matcher.group(2) : input.getValue();
		}

	}

	protected static void setTimers(@Nonnull final Class<? extends Entity> entity, final int base, final int random) {
		setTimers(EntityUtil.getClassName(entity), base, random);
	}

	static void setTimers(@Nonnull final String entity, final int base, final int random) {
		final EntityChatData data = messages.get(entity);
		if (data != null) {
			data.baseInterval = base;
			data.baseRandom = random;
		}
	}

	protected static boolean hasMessages(@Nonnull final Entity entity) {
		return !(entity instanceof EntityPlayer) && messages.get(EntityUtil.getClassName(entity.getClass())) != null;
	}

	private String getSpeechFormatted(@Nonnull final String message) {
		String xlated = xlate.loadString(message);
		if (minecraftSplashText.size() > 0 && SPLASH_TOKEN.equals(xlated)) {
			xlated = minecraftSplashText.get(random.nextInt(minecraftSplashText.size()));
		}
		return xlated;
	}

	protected static final Random random = XorShiftRandom.current();
	
	protected final EntityChatData data;
	protected long nextChat;

	public EntityChatEffect(@Nonnull final Entity entity) {
		this(entity, null);
	}

	public EntityChatEffect(@Nonnull final Entity entity, @Nullable final String entityName) {
		final String theName = StringUtils.isEmpty(entityName) ? EntityUtil.getClassName(entity.getClass())
				: entityName;
		this.data = messages.get(theName);
		this.nextChat = getWorldTicks(entity) + getNextChatTime();
	}

	@Nonnull
	@Override
	public String name() {
		return "Entity Chat";
	}

	protected int getBase() {
		return this.data.baseInterval;
	}

	protected int getRandom() {
		return this.data.baseRandom;
	}

	protected long getWorldTicks(final Entity e) {
		return e.getEntityWorld().getTotalWorldTime();
	}

	protected String getChatMessage() {
		String txt = this.data.table.next();
		return txt != null ? xlate.loadString(getSpeechFormatted(txt)) : "NO TEXT";
	}

	protected int getNextChatTime() {
		return getBase() + random.nextInt(getRandom());
	}

	@Override
	public void update(@Nonnull final Entity subject) {
		if (!ModOptions.speechbubbles.enableEntityChat)
			return;

		final long ticks = getWorldTicks(subject);
		final long delta = this.nextChat - ticks;
		if (delta <= 0) {
			final ISpeechData data = CapabilitySpeechData.getCapability(subject);
			if (data != null) {
				final int expiry = (int) (ModOptions.speechbubbles.speechBubbleDuration * 20F);
				data.addMessage(getChatMessage(), expiry);
			}
			genNextChatTime();
		}

	}

	public void genNextChatTime() {
		getState().subject().ifPresent(e -> this.nextChat = getWorldTicks(e) + getNextChatTime());
	}

	public static final IEntityEffectFactoryFilter DEFAULT_FILTER = (@Nonnull final Entity e,
			@Nonnull final EntityEffectInfo eei) -> eei.effects.contains("chat") && !(e instanceof EntityVillager)
					&& EntityChatEffect.hasMessages(e);

	public static class Factory implements IEntityEffectFactory {

		@Nonnull
		@Override
		public List<EntityEffect> create(@Nonnull final Entity entity) {
			return ImmutableList.of(new EntityChatEffect(entity));
		}
	}

}
