/* This file is part of Dynamic Surroundings, licensed under the MIT License (MIT).
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

package org.orecruncher.dsurround.client.footsteps.facade;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/*
import codechicken.microblock.BlockMicroMaterial;
import codechicken.microblock.Microblock;
import codechicken.multipart.BlockMultipart;
import codechicken.multipart.PartRayTraceResult;
import codechicken.multipart.TileMultipart;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class ForgeMultiPartCBE implements IFacadeAccessor {

	public ForgeMultiPartCBE() {
	}

	@Nonnull
	@Override
	public String getName() {
		return "ForgeMultiPartCBE";
	}

	@Override
	public boolean instanceOf(@Nonnull final Block block) {
		return isValid() && block instanceof BlockMultipart;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public IBlockState getBlockState(@Nonnull final EntityLivingBase entity, @Nonnull final IBlockState state,
			@Nonnull final IBlockAccess world, @Nonnull final Vec3d vec, @Nullable final EnumFacing side) {
		try {
			final BlockPos pos = new BlockPos(vec);
			final TileEntity te = world.getTileEntity(pos);
			if (te instanceof TileMultipart) {
				final TileMultipart tm = (TileMultipart) te;
				final Vec3d end = vec.addVector(0, -1, 0);
				final PartRayTraceResult result = tm.collisionRayTrace(vec, end);
				if (result != null && result.typeOfHit == Type.BLOCK) {
					final Microblock micro = (Microblock) tm.partList().apply(result.partIndex());
					if (micro != null) {
						final BlockMicroMaterial bmm = (BlockMicroMaterial) micro.getIMaterial();
						if (bmm != null) {
							final IBlockState mmState = bmm.state();
							if (mmState != null)
								return mmState;
						}

					}
				}
			}
		} catch (@Nonnull final Throwable ignore) {
		}
		return state;
	}

}
*/
