/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 DaPorkchop_
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

package net.daporkchop.fp2.compat.vanilla.biome.layer.java;

import lombok.NonNull;
import net.daporkchop.fp2.compat.vanilla.biome.layer.AbstractFastLayerWithRiverSource;
import net.daporkchop.fp2.util.alloc.IntArrayAllocator;
import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayerHills;

import static net.daporkchop.fp2.compat.vanilla.biome.BiomeHelper.*;

/**
 * @author DaPorkchop_
 * @see GenLayerHills
 */
public class FastLayerHills extends AbstractFastLayerWithRiverSource {
    public FastLayerHills(long seed) {
        super(seed);
    }

    @Override
    public int getSingle(@NonNull IntArrayAllocator alloc, int x, int z) {
        int center, v0, v1, v2, v3;

        int[] arr = alloc.get(3 * 3);
        try {
            this.child.getGrid(alloc, x - 1, z - 1, 3, 3, arr);

            v0 = arr[1];
            v2 = arr[3];
            center = arr[4];
            v1 = arr[5];
            v3 = arr[7];
        } finally {
            alloc.release(arr);
        }

        int river = this.childRiver.getSingle(alloc, x, z);
        int riverSubMod = (river - 2) % 29;

        Biome centerBiome = Biome.getBiome(center);

        if (center != 0 && river >= 2 && riverSubMod == 1 && !isMutation(centerBiome)) {
            Biome mutation = Biome.getMutationForBiome(centerBiome);
            return mutation != null ? Biome.getIdForBiome(mutation) : center;
        }

        long state = start(this.seed, x, z);
        if (riverSubMod != 0 && nextInt(state, 3) != 0) { //check riverSubMod==0 first to avoid having to yet another expensive modulo
            // computation (rng is updated afterward either way)
            return center;
        }
        state = update(state, this.seed);

        Biome mutationBiome = centerBiome;
        if (center == ID_DESERT) {
            mutationBiome = Biomes.DESERT_HILLS;
        } else if (center == ID_FOREST) {
            mutationBiome = Biomes.FOREST_HILLS;
        } else if (center == ID_BIRCH_FOREST) {
            mutationBiome = Biomes.BIRCH_FOREST_HILLS;
        } else if (center == ID_ROOFED_FOREST) {
            mutationBiome = Biomes.PLAINS;
        } else if (center == ID_TAIGA) {
            mutationBiome = Biomes.TAIGA_HILLS;
        } else if (center == ID_REDWOOD_TAIGA) {
            mutationBiome = Biomes.REDWOOD_TAIGA_HILLS;
        } else if (center == ID_COLD_TAIGA) {
            mutationBiome = Biomes.COLD_TAIGA_HILLS;
        } else if (center == ID_PLAINS) {
            if (nextInt(state, 3) == 0) {
                mutationBiome = Biomes.FOREST_HILLS;
            } else {
                mutationBiome = Biomes.FOREST;
            }
        } else if (center == ID_ICE_PLAINS) {
            mutationBiome = Biomes.ICE_MOUNTAINS;
        } else if (center == ID_JUNGLE) {
            mutationBiome = Biomes.JUNGLE_HILLS;
        } else if (center == ID_OCEAN) {
            mutationBiome = Biomes.DEEP_OCEAN;
        } else if (center == ID_EXTREME_HILLS) {
            mutationBiome = Biomes.EXTREME_HILLS_WITH_TREES;
        } else if (center == ID_SAVANNA) {
            mutationBiome = Biomes.SAVANNA_PLATEAU;
        } else if (biomesEqualOrMesaPlateau(center, ID_MESA_ROCK)) {
            mutationBiome = Biomes.MESA;
        } else if (center == ID_DEEP_OCEAN && nextInt(state, 3) == 0) {
            state = update(state, this.seed);
            if (nextInt(state, 2) == 0) {
                mutationBiome = Biomes.PLAINS;
            } else {
                mutationBiome = Biomes.FOREST;
            }
        }

        int mutation = Biome.getIdForBiome(mutationBiome);

        if (riverSubMod == 0 && mutation != center) {
            Biome mutatedMutationBiome = Biome.getMutationForBiome(mutationBiome);
            mutation = mutatedMutationBiome == null ? center : Biome.getIdForBiome(mutatedMutationBiome);
        }

        if (mutation == center) {
            return center;
        } else {
            int count = 0; //count the number of neighboring biomes which are the same
            if (biomesEqualOrMesaPlateau(v0, center)) {
                count++;
            }
            if (biomesEqualOrMesaPlateau(v1, center)) {
                count++;
            }
            if (biomesEqualOrMesaPlateau(v2, center)) {
                count++;
            }
            if (biomesEqualOrMesaPlateau(v3, center)) {
                count++;
            }
            return count >= 3 ? mutation : center;
        }
    }
}