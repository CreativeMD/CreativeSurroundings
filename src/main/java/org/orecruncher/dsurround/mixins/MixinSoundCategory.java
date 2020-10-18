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

package org.orecruncher.dsurround.mixins;

import java.lang.reflect.Field;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;

@Mixin(SoundCategory.class)
public abstract class MixinSoundCategory {

	static {
		// Hacky way to get this done, but it works. Longer term need to get rid of this.
		// Not sure of the decent usable way to do this will be. SoundCategory is an enum,
		// and we have our own categories.
		if (SoundCategory.getByName("ds_footsteps") == null) {
			// Add our new sound categories
			final Class<?>[] parms = new Class<?>[] { String.class };
			final SoundCategory fs = EnumHelper.addEnum(SoundCategory.class, "DS_FOOTSTEPS", parms, "ds_footsteps");
			final SoundCategory b = EnumHelper.addEnum(SoundCategory.class, "DS_BIOME", parms, "ds_biome");

			// Update the internal cached list
			try {
				final Field f = findField();
				@SuppressWarnings("unchecked")
				final Map<String, SoundCategory> theMap = (Map<String, SoundCategory>) f.get(null);
				theMap.put(fs.getName(), fs);
				theMap.put(b.getName(), b);
			} catch (@Nonnull final Throwable ignore) {
			}
		}
	}
	
    @Nonnull
    private static Field findField() throws NoSuchFieldException, SecurityException
    {
        Preconditions.checkNotNull((Class<?>) SoundCategory.class);
        Preconditions.checkArgument(StringUtils.isNotEmpty("SOUND_CATEGORIES"), "Field name cannot be empty");

        final String nameToFind = FMLLaunchHandler.isDeobfuscatedEnvironment() ? "SOUND_CATEGORIES" : MoreObjects.firstNonNull("field_187961_k", "SOUND_CATEGORIES");
        final Field f = SoundCategory.class.getDeclaredField(nameToFind);
        f.setAccessible(true);
        return f;
    }

}
