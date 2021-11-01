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

import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.SneakyThrows;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.common.util.exception.ResourceNotFoundException;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.buffer.BufferUsage;
import net.daporkchop.fp2.gl.command.CommandBufferArrays;
import net.daporkchop.fp2.gl.draw.DrawBinding;
import net.daporkchop.fp2.gl.draw.DrawMode;
import net.daporkchop.fp2.gl.layout.DrawLayout;
import net.daporkchop.fp2.gl.shader.FragmentShader;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.shader.ShaderProgram;
import net.daporkchop.fp2.gl.shader.VertexShader;
import net.daporkchop.fp2.gl.vertex.VertexAttribute;
import net.daporkchop.fp2.gl.vertex.VertexAttributeInterpretation;
import net.daporkchop.fp2.gl.vertex.VertexAttributeType;
import net.daporkchop.fp2.gl.vertex.VertexBuffer;
import net.daporkchop.fp2.gl.vertex.VertexFormat;
import net.daporkchop.fp2.gl.vertex.VertexFormatBuilder;
import net.daporkchop.fp2.gl.vertex.VertexWriter;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;

/**
 * @author DaPorkchop_
 */
public class TestLWJGL2 {
    private static void hackNatives() {
        String paths = System.getProperty("java.library.path");
        String nativesDir = "/media/daporkchop/PortableIDE/.gradle/caches/minecraft/net/minecraft/natives/1.12.2";

        if (Strings.isNullOrEmpty(paths)) {
            paths = nativesDir;
        } else {
            paths += File.pathSeparator + nativesDir;
        }

        System.setProperty("java.library.path", paths);

        // hack the classloader now.
        try {
            final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
            sysPathsField.setAccessible(true);
            sysPathsField.set(null, null);
        } catch (Throwable t) {
        }
    }

    public static void main(String... args) throws LWJGLException {
        hackNatives();

        Display.setDisplayMode(new DisplayMode(512, 512));
        Display.setTitle("title");
        Display.create();

        try (GL gl = GL.builder()
                .withResourceProvider(id -> {
                    InputStream in = TestLWJGL2.class.getResourceAsStream(id.path());
                    if (in != null) {
                        return in;
                    }
                    throw new ResourceNotFoundException(id);
                })
                .wrapCurrent()) {
            run(gl);
        } finally {
            Display.destroy();
        }
    }

    @SneakyThrows({ ShaderCompilationException.class, ShaderLinkageException.class })
    private static void run(@NonNull GL gl) {
        VertexAttribute.Int2 attrPos;
        VertexAttribute.Int4 attrColor;
        VertexFormat localFormat;

        {
            VertexFormatBuilder builder = gl.createVertexFormat()
                    .interleaved()
                    .notAligned();

            attrPos = builder.attrib().name("a_pos")
                    .int2(VertexAttributeType.Integer.BYTE)
                    .interpretation(VertexAttributeInterpretation.FLOAT)
                    .build();

            attrColor = builder.attrib().name("a_color")
                    .int4(VertexAttributeType.Integer.UNSIGNED_BYTE)
                    .interpretation(VertexAttributeInterpretation.NORMALIZED_FLOAT)
                    .build();

            localFormat = builder.build();
        }

        DrawLayout layout = gl.createDrawLayout()
                .withUniforms()
                .withGlobals()
                .withLocals(localFormat)
                .build();

        VertexShader vertexShader = gl.createVertexShader()
                .forLayout(layout)
                .include(Identifier.from("test.vert")).endSource()
                .endDefines()
                .build();
        FragmentShader fragmentShader = gl.createFragmentShader()
                .forLayout(layout)
                .include(Identifier.from("test.frag")).endSource()
                .endDefines()
                .build();
        ShaderProgram shaderProgram = gl.linkShaderProgram(layout, vertexShader, fragmentShader);

        VertexBuffer localBuffer = localFormat.createBuffer(BufferUsage.STATIC_DRAW);
        localBuffer.resize(4);

        try (VertexWriter writer = localFormat.createWriter()) {
            writer.set(attrPos, 16, 16).setARGB(attrColor, -1).endVertex();
            writer.set(attrPos, 16, 32).setARGB(attrColor, -1).endVertex();
            writer.set(attrPos, 32, 32).setARGB(attrColor, -1).endVertex();
            writer.set(attrPos, 32, 16).setARGB(attrColor, -1).endVertex();

            localBuffer.set(0, writer);
        }

        DrawBinding binding = layout.createBinding()
                .withUniforms()
                .withGlobals()
                .withLocals(localBuffer)
                .build();

        CommandBufferArrays commandBufferArrays = gl.createCommandBuffer()
                .forArrays(binding)
                .build();

        commandBufferArrays.resize(1);
        commandBufferArrays.set(0, 0, 4);

        while (!Display.isCloseRequested()) {
            commandBufferArrays.execute(DrawMode.QUADS, shaderProgram);

            Display.update();
            Display.sync(60);
        }
    }
}
