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

package org.blockartistry.lib.script;

import javax.annotation.Nonnull;

public abstract class Variant implements Comparable<Variant>, Expression.LazyVariant {

	protected final String name;
	
	public Variant() {
		this("<ANON>");
	}
	
	public Variant(@Nonnull final String name) {
		this.name = name;
	}
	
	@Nonnull
	public String getName() {
		return this.name;
	}
	
	public abstract float asNumber();

	@Nonnull
	public abstract String asString();
	
	public abstract boolean asBoolean();

	// Operator support in case of strings
	@Nonnull
	public abstract Variant add(@Nonnull final Variant term);
	
	@Override
	@Nonnull
	public final String toString() {
		return asString();
	}

	@Override
	@Nonnull
	public final Variant eval() {
		return this;
	}
}