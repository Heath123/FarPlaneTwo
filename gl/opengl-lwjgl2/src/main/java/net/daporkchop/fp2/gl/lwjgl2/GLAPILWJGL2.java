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

package net.daporkchop.fp2.gl.lwjgl2;

import lombok.NonNull;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.gl.GLVersion;
import net.daporkchop.fp2.gl.opengl.GLAPI;
import net.daporkchop.lib.common.function.throwing.EPredicate;
import net.daporkchop.lib.unsafe.PUnsafe;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLContext;

import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.stream.Stream;

import static java.lang.Math.*;
import static net.daporkchop.fp2.gl.opengl.OpenGLConstants.*;

/**
 * @author DaPorkchop_
 */
public class GLAPILWJGL2 implements GLAPI {
    @Override
    public GLVersion version() {
        ContextCapabilities capabilities = GLContext.getCapabilities();

        return Stream.of(GLVersion.values())
                .filter((EPredicate<GLVersion>) version -> {
                    try {
                        return (boolean) ContextCapabilities.class.getDeclaredField("OpenGL" + version.major() + version.minor()).get(capabilities);
                    } catch (NoSuchFieldException e) {
                        return false; //field not found, therefore the version isn't supported by LWJGL2
                    }
                })
                .max(Comparator.naturalOrder())
                .get();
    }

    //
    //
    // OpenGL 1.1
    //
    //

    @Override
    public int glGetError() {
        return GL11.glGetError();
    }

    @Override
    public int glGetInteger(int pname) {
        return GL11.glGetInteger(pname);
    }

    @Override
    public String glGetString(int pname) {
        return GL11.glGetString(pname);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GL11.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, long indices) {
        GL11.glDrawElements(mode, count, type, indices);
    }

    @Override
    public void glDrawElements(int mode, int count, int type, @NonNull ByteBuffer indices) {
        GL11.glDrawElements(mode, count, type, indices);
    }

    //
    //
    // OpenGL 1.4
    //
    //

    @Override
    public void glMultiDrawArrays(int mode, long first, long count, int drawcount) {
        GL14.glMultiDrawArrays(mode, DirectBufferHackery.wrapInt(first, drawcount), DirectBufferHackery.wrapInt(count, drawcount));
    }

    //
    //
    // OpenGL 1.5
    //
    //

    @Override
    public int glGenBuffer() {
        return GL15.glGenBuffers();
    }

    @Override
    public void glDeleteBuffer(int buffer) {
        GL15.glDeleteBuffers(buffer);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    @Override
    public void glBufferData(int target, long data_size, long data, int usage) {
        if (data_size <= Integer.MAX_VALUE) { //data is small enough to fit in a ByteBuffer
            GL15.glBufferData(target, DirectBufferHackery.wrapInt(data, (int) data_size), usage);
        } else { //LWJGL2 doesn't expose glBufferData with 64-bit data_size...
            //allocate storage and then delegate to glBufferSubData
            GL15.glBufferData(target, data_size, usage);
            this.glBufferSubData(target, 0L, data_size, data);
        }
    }

    @Override
    public void glBufferData(int target, @NonNull ByteBuffer data, int usage) {
        GL15.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferSubData(int target, long offset, long data_size, long data) {
        if (data_size <= Integer.MAX_VALUE) { //data is small enough to fit in a ByteBuffer
            GL15.glBufferSubData(target, offset, DirectBufferHackery.wrapInt(data, (int) data_size));
        } else { //LWJGL2 doesn't expose glBufferSubData with 64-bit data_size...
            //upload data in increments of Integer.MAX_VALUE
            for (long uploaded = 0L; uploaded < data_size; ) {
                int blockSize = (int) min(uploaded - offset, Integer.MAX_VALUE);
                GL15.glBufferSubData(target, offset + uploaded, DirectBufferHackery.wrapByte(data + uploaded, blockSize));
                uploaded += blockSize;
            }
        }
    }

    @Override
    public void glBufferSubData(int target, long offset, @NonNull ByteBuffer data) {
        GL15.glBufferSubData(target, offset, data);
    }

    @Override
    public void glGetBufferSubData(int target, long offset, long data_size, long data) {
        if (data_size <= Integer.MAX_VALUE) { //data is small enough to fit in a ByteBuffer
            GL15.glGetBufferSubData(target, offset, DirectBufferHackery.wrapInt(data, (int) data_size));
        } else { //LWJGL2 doesn't expose glGetBufferSubData with 64-bit data_size...
            //download data in increments of Integer.MAX_VALUE
            for (long downloaded = 0L; downloaded < data_size; ) {
                int blockSize = (int) min(downloaded - offset, Integer.MAX_VALUE);
                GL15.glGetBufferSubData(target, offset + downloaded, DirectBufferHackery.wrapByte(data + downloaded, blockSize));
                downloaded += blockSize;
            }
        }
    }

    @Override
    public void glGetBufferSubData(int target, long offset, @NonNull ByteBuffer data) {
        GL15.glGetBufferSubData(target, offset, data);
    }

    @Override
    public long glMapBuffer(int target, int usage) {
        return PUnsafe.pork_directBufferAddress(GL15.glMapBuffer(target, usage, null));
    }

    @Override
    public void glUnmapBuffer(int target) {
        GL15.glUnmapBuffer(target);
    }

    //
    //
    // OpenGL 2.0
    //
    //

    @Override
    public int glCreateShader(int type) {
        return GL20.glCreateShader(type);
    }

    @Override
    public void glDeleteShader(int shader) {
        GL20.glDeleteShader(shader);
    }

    @Override
    public void glShaderSource(int shader, @NonNull CharSequence... source) {
        GL20.glShaderSource(shader, source);
    }

    @Override
    public void glCompileShader(int shader) {
        GL20.glCompileShader(shader);
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        return GL20.glGetShaderi(shader, pname);
    }

    @Override
    public String glGetShaderInfoLog(int shader) {
        return GL20.glGetShaderInfoLog(shader, GL20.glGetShaderi(shader, GL_INFO_LOG_LENGTH));
    }

    @Override
    public int glCreateProgram() {
        return GL20.glCreateProgram();
    }

    @Override
    public void glDeleteProgram(int program) {
        GL20.glDeleteProgram(program);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GL20.glAttachShader(program, shader);
    }

    @Override
    public void glDetachShader(int program, int shader) {
        GL20.glDetachShader(program, shader);
    }

    @Override
    public void glLinkProgram(int program) {
        GL20.glLinkProgram(program);
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        return GL20.glGetProgrami(program, pname);
    }

    @Override
    public String glGetProgramInfoLog(int program) {
        return GL20.glGetProgramInfoLog(program, GL20.glGetProgrami(program, GL_INFO_LOG_LENGTH));
    }

    @Override
    public void glUseProgram(int program) {
        GL20.glUseProgram(program);
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        GL20.glEnableVertexAttribArray(index);
    }

    @Override
    public void glDisableVertexArray(int index) {
        GL20.glDisableVertexAttribArray(index);
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long buffer_buffer_offset) {
        GL20.glVertexAttribPointer(index, size, type, normalized, stride, buffer_buffer_offset);
    }

    @Override
    public void glBindAttribLocation(int program, int index, @NonNull CharSequence name) {
        GL20.glBindAttribLocation(program, index, name);
    }

    //
    //
    // OpenGL 3.0
    //
    //

    @Override
    public int glGetInteger(int pname, int idx) {
        return GL30.glGetInteger(pname, idx);
    }

    @Override
    public String glGetString(int pname, int idx) {
        return GL30.glGetStringi(pname, idx);
    }

    @Override
    public int glGenVertexArray() {
        return GL30.glGenVertexArrays();
    }

    @Override
    public void glDeleteVertexArray(int array) {
        GL30.glDeleteVertexArrays(array);
    }

    @Override
    public void glBindVertexArray(int array) {
        GL30.glBindVertexArray(array);
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, long buffer_buffer_offset) {
        GL30.glVertexAttribIPointer(index, size, type, stride, buffer_buffer_offset);
    }

    //
    //
    // OpenGL 3.2
    //
    //

    @Override
    public void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex) {
        GL32.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
    }

    @Override
    public void glMultiDrawElementsBaseVertex(int mode, long count, int type, long indices, int drawcount, long basevertex) {
        throw new UnsupportedOperationException();
    }

    //
    //
    // OpenGL 3.3
    //
    //

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {
        GL33.glVertexAttribDivisor(index, divisor);
    }
}
