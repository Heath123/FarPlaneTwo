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

package net.daporkchop.fp2.strategy.common;

import io.netty.buffer.ByteBuf;
import lombok.NonNull;
import net.daporkchop.fp2.strategy.RenderMode;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author DaPorkchop_
 */
public interface IFarPiece<POS extends IFarPos> extends ReadWriteLock {
    /**
     * Timestamp indicating that the piece does not contain any data.
     */
    long PIECE_EMPTY = Long.MIN_VALUE;
    /**
     * Timestamp indicating that the piece's rough generation has been completed.
     */
    long PIECE_ROUGH_COMPLETE = 0L;

    /**
     * Gets a timestamp indicating that the piece contains rough data generated at the given level.
     *
     * @param level the lowest level that has been generated roughly
     * @return a timestamp indicating that the piece contains rough data generated at the given level
     */
    static long pieceRough(int level) {
        return -level;
    }

    /**
     * @return the {@link RenderMode} that this piece is used for
     */
    RenderMode mode();

    /**
     * @return this piece's position
     */
    POS pos();

    /**
     * Gets this piece's timestamp.
     * <p>
     * A timestamp serves as a revision number to indicate the most recently updated data point contained by this piece. The value itself is an arbitrary
     * positive global value that is guaranteed never to decrease.
     *
     * @return this piece's timestamp
     * @see #PIECE_EMPTY
     * @see #pieceRough(int)
     * @see #PIECE_ROUGH_COMPLETE
     */
    long timestamp();

    /**
     * Atomically updates this piece's timestamp.
     *
     * @param timestamp the new timestamp
     * @throws IllegalArgumentException if the new timestamp is not greater than the current timestamp
     */
    void updateTimestamp(long timestamp) throws IllegalArgumentException;

    boolean isDirty();

    void markDirty();

    boolean clearDirty();

    /**
     * Writes this piece to the given {@link ByteBuf}.
     * <p>
     * The written data must be deserializable by this piece's render mode's {@link RenderMode#readPiece(ByteBuf)} method.
     *
     * @param dst the {@link ByteBuf} to write to
     */
    void writePiece(@NonNull ByteBuf dst);
}
