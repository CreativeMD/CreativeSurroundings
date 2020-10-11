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
package org.orecruncher.dsurround.client.handlers.scanners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.orecruncher.dsurround.client.handlers.EnvironStateHandler.EnvironState;
import org.orecruncher.lib.chunk.ClientChunkCache;
import org.orecruncher.lib.math.MathStuff;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Performs area scanning around the player to determine area ceiling coverage.
 * Used to determine if the player is "inside" or "outside".
 */
@SideOnly(Side.CLIENT)
public final class CeilingCoverage implements ITickable {

	private static final int SURVEY_INTERVAL = 4;
	private static final int INSIDE_SURVEY_RANGE = 3;
	private static final float INSIDE_THRESHOLD = 1.0F - 65.0F / 176.0F;
	private static final Cell[] cells;
	private static final float TOTAL_POINTS;

	static {

		final List<Cell> cellList = new ArrayList<>();
		// Build our cell map
		for (int x = -INSIDE_SURVEY_RANGE; x <= INSIDE_SURVEY_RANGE; x++)
			for (int z = -INSIDE_SURVEY_RANGE; z <= INSIDE_SURVEY_RANGE; z++)
				cellList.add(new Cell(new Vec3i(x, 0, z), INSIDE_SURVEY_RANGE));

		// Sort so the highest score cells are first
		Collections.sort(cellList);
		cells = cellList.toArray(new Cell[0]);

		float totalPoints = 0.0F;
		for (final Cell c : cellList)
			totalPoints += c.potentialPoints();
		TOTAL_POINTS = totalPoints;
	}

	private boolean reallyInside = false;

	@Override
	public void update() {
		if (EnvironState.getTickCounter() % SURVEY_INTERVAL == 0) {
			float ceilingCoverageRatio;
			if (EnvironState.getDimensionId() == -1 || EnvironState.getDimensionInfo().alwaysOutside()) {
				this.reallyInside = false;
			} else {
				final BlockPos pos = EnvironState.getPlayerPosition();
				float score = 0.0F;
				for (Cell cell : cells) score += cell.score(pos);
				ceilingCoverageRatio = 1.0F - (score / TOTAL_POINTS);
				this.reallyInside = ceilingCoverageRatio > INSIDE_THRESHOLD;
			}
		}
	}

	public boolean isReallyInside() {
		return this.reallyInside;
	}

	private static final class Cell implements Comparable<Cell> {

		private final Vec3i offset;
		private final float points;
		private final BlockPos.MutableBlockPos working;

		public Cell(@Nonnull final Vec3i offset, final int range) {
			this.offset = offset;
			final float xV = range - MathStuff.abs(offset.getX()) + 1;
			final float zV = range - MathStuff.abs(offset.getZ()) + 1;
			final float candidate = Math.min(xV, zV);
			this.points = candidate * candidate;
			this.working = new BlockPos.MutableBlockPos();
		}

		public float potentialPoints() {
			return this.points;
		}

		public float score(@Nonnull final BlockPos playerPos) {
			//@formatter:off
			this.working.setPos(
					playerPos.getX() + this.offset.getX(),
					playerPos.getY() + this.offset.getY(),
					playerPos.getZ() + this.offset.getZ()
				);
			//@formatter:on

			final World world = EnvironState.getWorld();
			final int playerHeight = Math.max(playerPos.getY() + 1, 0);

			// Get the precipitation height
			this.working.setPos(ClientChunkCache.instance().getPrecipitationHeight(this.working));

			// Scan down looking for blocks that are considered "cover"
			while (this.working.getY() > playerHeight) {

				final IBlockState state = ClientChunkCache.instance().getBlockState(this.working);

				//@formatter:off
	            if (state.getMaterial().blocksMovement()
	            	&& !state.getBlock().isLeaves(state, world, this.working)
	            	&& !state.getBlock().isFoliage(world, this.working)) {
	            	// Cover block - no points for you!
	                return 0;
	            }
	            //@formatter:on

				this.working.setY(this.working.getY() - 1);
			}

			// Scanned down to the players head and found nothing. So give the points.
			return this.points;
		}

		@Override
		public int compareTo(@Nonnull final Cell cell) {
			// Want big scores first in the list
			return -Float.compare(potentialPoints(), cell.potentialPoints());
		}

		@Override
		@Nonnull
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append(this.offset.toString());
			builder.append(" points: ").append(this.points);
			return builder.toString();
		}

	}

}
