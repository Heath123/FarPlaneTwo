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

package net.daporkchop.fp2.gl.opengl.command;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.daporkchop.fp2.gl.command.DrawCommandArrays;
import net.daporkchop.fp2.gl.command.DrawCommandIndexed;
import net.daporkchop.fp2.gl.command.buffer.DrawCommandBuffer;
import net.daporkchop.fp2.gl.command.buffer.DrawCommandBufferBuilder;
import net.daporkchop.fp2.gl.draw.DrawBinding;
import net.daporkchop.fp2.gl.draw.DrawBindingIndexed;
import net.daporkchop.fp2.gl.opengl.OpenGL;
import net.daporkchop.fp2.gl.opengl.command.arrays.DrawCommandBufferArraysImpl_MultiDrawIndirect;
import net.daporkchop.fp2.gl.opengl.command.elements.DrawCommandBufferElementsImpl_MultiDrawIndirect;

import static net.daporkchop.lib.common.util.PorkUtil.*;

/**
 * @author DaPorkchop_
 */
@RequiredArgsConstructor
public class DrawCommandBufferBuilderImpl implements DrawCommandBufferBuilder.TypeStage, DrawCommandBufferBuilder.OptimizeStage {
    @NonNull
    protected final OpenGL gl;

    protected DrawBinding binding;
    protected boolean elements;

    protected boolean optimizeForCpuSelection;

    //
    // TypeStage
    //

    @Override
    public OptimizeStage<DrawCommandBuffer<DrawCommandArrays>> forArrays(@NonNull DrawBinding binding) {
        this.binding = binding;
        this.elements = false;
        return uncheckedCast(this);
    }

    @Override
    public OptimizeStage<DrawCommandBuffer<DrawCommandIndexed>> forIndexed(@NonNull DrawBindingIndexed binding) {
        this.binding = binding;
        this.elements = true;
        return uncheckedCast(this);
    }

    //
    // OptimizeStage
    //

    @Override
    public DrawCommandBufferBuilder optimizeForCpuSelection() {
        this.optimizeForCpuSelection = true;
        return this;
    }

    //
    // CommandBufferBuilder
    //

    @Override
    public DrawCommandBuffer build() {
        if (this.elements) {
            return new DrawCommandBufferElementsImpl_MultiDrawIndirect(this);
        } else {
            return new DrawCommandBufferArraysImpl_MultiDrawIndirect(this);
        }
    }
}