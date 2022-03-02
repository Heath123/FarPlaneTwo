/*
 * Adapted from The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 DaPorkchop_
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

package net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.exactfblockworld;

import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.api.event.FEventHandler;
import net.daporkchop.fp2.api.world.FBlockWorld;
import net.daporkchop.fp2.api.world.GenerationNotAllowedException;
import net.daporkchop.fp2.core.server.event.ColumnSavedEvent;
import net.daporkchop.fp2.core.server.event.CubeSavedEvent;
import net.daporkchop.fp2.core.server.world.ExactFBlockWorldHolder;
import net.daporkchop.fp2.core.util.datastructure.Datastructures;
import net.daporkchop.fp2.core.util.datastructure.NDimensionalIntSegtreeSet;
import net.daporkchop.fp2.core.util.threading.futurecache.IAsyncCache;
import net.daporkchop.fp2.core.util.threading.lazy.LazyFutureTask;
import net.daporkchop.fp2.impl.mc.forge1_12_2.asm.interfaz.world.IMixinWorldServer;
import net.daporkchop.fp2.impl.mc.forge1_12_2.compat.vanilla.region.ThreadSafeRegionFileCache;
import net.daporkchop.fp2.impl.mc.forge1_12_2.util.threading.asyncblockaccess.AsyncCacheNBTBase;
import net.daporkchop.lib.common.function.throwing.ERunnable;
import net.daporkchop.lib.common.math.BinMath;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * Default implementation of {@link ExactFBlockWorldHolder} for vanilla worlds.
 *
 * @author DaPorkchop_
 */
public class VanillaExactFBlockWorldHolder1_12 implements ExactFBlockWorldHolder {
    protected final WorldServer world;
    protected final AnvilChunkLoader io;

    protected final ChunkCache chunks = new ChunkCache();

    protected final NDimensionalIntSegtreeSet chunksExistCache;

    public VanillaExactFBlockWorldHolder1_12(@NonNull WorldServer world) {
        this.world = world;
        this.io = (AnvilChunkLoader) this.world.getChunkProvider().chunkLoader;

        this.chunksExistCache = Datastructures.INSTANCE.nDimensionalIntSegtreeSet()
                .dimensions(2)
                .threadSafe(true)
                .initialPoints(() -> ThreadSafeRegionFileCache.INSTANCE.allChunks(this.io.chunkSaveLocation.toPath().resolve("region"))
                        .map(pos -> new int[]{ pos.x, pos.z })
                        .parallel())
                .build();

        ((IMixinWorldServer) world).fp2_farWorldServer().fp2_IFarWorldServer_eventBus().registerWeak(this);
    }

    @Override
    public FBlockWorld worldFor(@NonNull AllowGenerationRequirement requirement) {
        return new VanillaExactFBlockWorld1_12(this, requirement == AllowGenerationRequirement.ALLOWED);
    }

    @Override
    public void close() {
        this.chunksExistCache.release();
    }

    @FEventHandler
    private void onColumnSaved(@NonNull ColumnSavedEvent event) {
        this.chunksExistCache.add(event.pos().x(), event.pos().y());
        this.chunks.notifyUpdate(new ChunkPos(event.pos().x(), event.pos().y()), (NBTTagCompound) event.data());
    }

    @FEventHandler
    private void onCubeSaved(@NonNull CubeSavedEvent event) {
        throw new UnsupportedOperationException("vanilla world shouldn't have cubes!");
    }

    public boolean containsAnyData(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        //TODO: support arbitrary AABBs
        assert maxX - minX == maxY - minY && maxX - minX == maxZ - minZ : "only cubic AABBs are supported!";
        assert BinMath.isPow2(maxX - minX) : "only cubic AABBs with a side length of a power of two are supported!";

        int level = Integer.numberOfTrailingZeros(maxX - minX);
        int tileX = minX >> level;
        int tileY = minY >> level;
        int tileZ = minZ >> level;
        return tileY < 16 && this.chunksExistCache.containsAny(level, tileX, tileZ);
    }

    protected Chunk getChunk(int chunkX, int chunkZ, boolean allowGeneration) throws GenerationNotAllowedException {
        return GenerationNotAllowedException.throwIfNull(this.chunks.get(new ChunkPos(chunkX, chunkZ), allowGeneration).join());
    }

    protected Stream<Chunk> multiGetChunks(@NonNull Stream<ChunkPos> chunkPositions, boolean allowGeneration) throws GenerationNotAllowedException {
        //collect all futures into an array first in order to issue all tasks at once before blocking, thus ensuring maximum parallelism
        LazyFutureTask<Chunk>[] chunkFutures = uncheckedCast(chunkPositions.map(pos -> this.chunks.get(pos, allowGeneration)).toArray(LazyFutureTask[]::new));

        return LazyFutureTask.scatterGather(chunkFutures).stream()
                .peek(GenerationNotAllowedException.uncheckedThrowIfNull());
    }

    /**
     * {@link IAsyncCache} for chunks.
     *
     * @author DaPorkchop_
     */
    protected class ChunkCache extends AsyncCacheNBTBase<ChunkPos, Object, Chunk> {
        //TODO: this doesn't handle the difference between "chunk is populated" and "chunk and its neighbors are populated", which is important because vanilla is very dumb

        @Override
        protected Chunk parseNBT(@NonNull ChunkPos key, @NonNull Object param, @NonNull NBTTagCompound nbt) {
            Chunk chunk = VanillaExactFBlockWorldHolder1_12.this.io.checkedReadChunkFromNBT(VanillaExactFBlockWorldHolder1_12.this.world, key.x, key.z, nbt);
            return chunk.isTerrainPopulated() ? chunk : null;
        }

        @Override
        @SneakyThrows(IOException.class)
        protected Chunk loadFromDisk(@NonNull ChunkPos key, @NonNull Object param) {
            Object[] data = VanillaExactFBlockWorldHolder1_12.this.io.loadChunk__Async(VanillaExactFBlockWorldHolder1_12.this.world, key.x, key.z);
            Chunk chunk = data != null ? (Chunk) data[0] : null;
            return chunk != null && chunk.isTerrainPopulated() ? chunk : null;
        }

        @Override
        protected void triggerGeneration(@NonNull ChunkPos key, @NonNull Object param) {
            ((IMixinWorldServer) VanillaExactFBlockWorldHolder1_12.this.world).fp2_farWorldServer().fp2_IFarWorld_workerManager().workExecutor().run((ERunnable) () -> {
                int x = key.x;
                int z = key.z;

                //see net.minecraftforge.server.command.ChunkGenWorker#doWork() for more info

                Chunk chunk = VanillaExactFBlockWorldHolder1_12.this.world.getChunk(x, z);
                VanillaExactFBlockWorldHolder1_12.this.world.getChunk(x + 1, z);
                VanillaExactFBlockWorldHolder1_12.this.world.getChunk(x + 1, z + 1);
                VanillaExactFBlockWorldHolder1_12.this.world.getChunk(x, z + 1);

                VanillaExactFBlockWorldHolder1_12.this.io.saveChunk(VanillaExactFBlockWorldHolder1_12.this.world, chunk);
            }).join();
        }
    }
}