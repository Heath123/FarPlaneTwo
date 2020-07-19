/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2020 DaPorkchop_
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * Any persons and/or organizations using this software must include the above copyright notice and this permission notice,
 * provide sufficient credit to the original authors of the project (IE: DaPorkchop_), as well as provide a link to the original project.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package net.daporkchop.fp2.strategy.heightmap.gen.vanilla;

import lombok.NonNull;
import net.daporkchop.fp2.strategy.heightmap.HeightmapGenerator;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.util.threading.CachedBlockAccess;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class VanillaHeightmapGenerator implements HeightmapGenerator {
    @Override
    public void init(@NonNull WorldServer world) {
    }

    @Override
    public void generateRough(@NonNull CachedBlockAccess world, @NonNull HeightmapPiece piece) {
        this.generateExact(world, piece);
    }

    @Override
    public void generateExact(@NonNull CachedBlockAccess world, @NonNull HeightmapPiece piece) {
        int pieceX = piece.x();
        int pieceZ = piece.z();
        world.prefetch(new AxisAlignedBB(
                pieceX * HEIGHTMAP_VOXELS, 0, pieceZ * HEIGHTMAP_VOXELS,
                (pieceX + 1) * HEIGHTMAP_VOXELS - 1, 0, (pieceZ + 1) * HEIGHTMAP_VOXELS - 1), true);
        for (int x = 0; x < HEIGHTMAP_VOXELS; x++) {
            for (int z = 0; z < HEIGHTMAP_VOXELS; z++) {
                int height = world.getTopBlockY(pieceX * HEIGHTMAP_VOXELS + x, pieceZ * HEIGHTMAP_VOXELS + z);
                BlockPos pos = new BlockPos(pieceX * HEIGHTMAP_VOXELS + x, height, pieceZ * HEIGHTMAP_VOXELS + z);
                IBlockState state = world.getBlockState(pos);

                while (height <= 63 && state.getMaterial() == Material.WATER) {
                    pos = new BlockPos(pos.getX(), --height, pos.getZ());
                    state = world.getBlockState(pos);
                }

                Biome biome = world.getBiome(pos);
                piece.set(x, z,
                        height,
                        Block.getStateId(state),
                        Biome.getIdForBiome(biome),
                        packCombinedLight(world.getCombinedLight(pos.add(0, 1, 0), 0)));
            }
        }
    }
}