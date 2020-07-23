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

package net.daporkchop.fp2.strategy.heightmap.gen.cwg;

import io.github.opencubicchunks.cubicchunks.cubicgen.common.biome.IBiomeBlockReplacer;
import lombok.NonNull;
import net.daporkchop.fp2.strategy.heightmap.HeightmapPiece;
import net.daporkchop.fp2.strategy.heightmap.gen.HeightmapGenerator;
import net.daporkchop.fp2.strategy.heightmap.gen.block.CCHeightmapGenerator;
import net.daporkchop.fp2.util.cwg.CWGContext;
import net.daporkchop.fp2.util.cwg.CWGUtil;
import net.daporkchop.fp2.util.threading.CachedBlockAccess;
import net.daporkchop.lib.common.ref.Ref;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;

import java.util.List;

import static java.lang.Math.*;
import static net.daporkchop.fp2.strategy.heightmap.HeightmapConstants.*;
import static net.daporkchop.fp2.util.Constants.*;

/**
 * @author DaPorkchop_
 */
public class CWGHeightmapGenerator implements HeightmapGenerator {
    protected Ref<CWGContext> ctx;
    protected int seaLevel;

    @Override
    public void init(@NonNull WorldServer world) {
        this.ctx = CWGUtil.tlCWGCtx(world);
        this.seaLevel = world.getSeaLevel();
    }

    @Override
    public void generate(@NonNull CachedBlockAccess world, @NonNull HeightmapPiece piece) {
        int pieceX = piece.x();
        int pieceZ = piece.z();

        CWGContext ctx = this.ctx.get();
        Biome[] biomes = ctx.biomeCache
                = ctx.biomeProvider().getBiomes(ctx.biomeCache, pieceX * HEIGHTMAP_VOXELS - 1, pieceZ * HEIGHTMAP_VOXELS - 1, HEIGHTMAP_VOXELS + 2, HEIGHTMAP_VOXELS + 2, false);

        for (int x = 0; x < HEIGHTMAP_VOXELS; x++) {
            for (int z = 0; z < HEIGHTMAP_VOXELS; z++) {
                int blockX = pieceX * HEIGHTMAP_VOXELS + x;
                int blockZ = pieceZ * HEIGHTMAP_VOXELS + z;

                int height = CWGUtil.getHeight(ctx.terrainBuilder(), blockX, blockZ);
                double density = ctx.terrainBuilder().get(blockX, height, blockZ);

                double dx = ctx.terrainBuilder().get(blockX + 1, height, blockZ) - density;
                double dy = ctx.terrainBuilder().get(blockX, height + 1, blockZ) - density;
                double dz = ctx.terrainBuilder().get(blockX, height, blockZ + 1) - density;

                //Biome biome = ctx.biomeSource().getBiome(blockX, height, blockZ).getBiome();
                Biome biome = biomes[(z + 1) * (HEIGHTMAP_VOXELS + 2) + x + 1];

                IBlockState state = Blocks.AIR.getDefaultState();
                List<IBiomeBlockReplacer> replacers = ctx.biomeBlockReplacers().get(biome);
                for (int i = 0, size = replacers.size(); i < size; i++) {
                    state = replacers.get(i).getReplacedBlock(state, blockX, height, blockZ, dx, dy, dz, density);
                }

                piece.set(x, z, height, state, packCombinedLight((height < this.seaLevel ? max(15 - (this.seaLevel - height) * 3, 0) : 15) << 20));
            }
        }

        for (int x = 0; x < HEIGHTMAP_VOXELS + 2; x++) {
            for (int z = 0; z < HEIGHTMAP_VOXELS + 2; z++) {
                piece.setBiome(x - 1, z - 1, biomes[z * (HEIGHTMAP_VOXELS + 2) + x]);
            }
        }
    }
}
