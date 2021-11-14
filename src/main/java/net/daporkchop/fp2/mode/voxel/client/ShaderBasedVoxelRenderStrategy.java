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

package net.daporkchop.fp2.mode.voxel.client;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.client.gl.shader.ShaderManager;
import net.daporkchop.fp2.common.util.Identifier;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.gl.index.IndexFormat;
import net.daporkchop.fp2.gl.index.IndexType;
import net.daporkchop.fp2.gl.layout.DrawLayout;
import net.daporkchop.fp2.gl.shader.DrawShaderProgram;
import net.daporkchop.fp2.gl.shader.FragmentShader;
import net.daporkchop.fp2.gl.shader.ShaderCompilationException;
import net.daporkchop.fp2.gl.shader.ShaderLinkageException;
import net.daporkchop.fp2.gl.shader.VertexShader;
import net.daporkchop.fp2.mode.api.IFarRenderMode;
import net.daporkchop.fp2.mode.common.client.ICullingStrategy;
import net.daporkchop.fp2.mode.common.client.bake.IRenderBaker;
import net.daporkchop.fp2.mode.common.client.bake.indexed.IndexedBakeOutput;
import net.daporkchop.fp2.mode.common.client.strategy.AbstractMultipassIndexedRenderStrategy;
import net.daporkchop.fp2.mode.voxel.VoxelPos;
import net.daporkchop.fp2.mode.voxel.VoxelTile;

import static net.daporkchop.fp2.FP2.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class ShaderBasedVoxelRenderStrategy extends AbstractMultipassIndexedRenderStrategy<VoxelPos, VoxelTile> {
    protected final Attribute.Int4 attrGlobalTilePos;
    protected final AttributeFormat globalFormat;

    protected final Attribute.Int1 attrLocalState;
    protected final Attribute.Int2 attrLocalLight;
    protected final Attribute.Int3 attrLocalColor;
    protected final Attribute.Int3 attrLocalPos;
    protected final AttributeFormat vertexFormat;

    protected final IndexFormat indexFormat;

    protected final DrawLayout drawLayout;

    protected final DrawShaderProgram blockShader;
    protected final DrawShaderProgram stencilShader;
    
    public ShaderBasedVoxelRenderStrategy(@NonNull IFarRenderMode<VoxelPos, VoxelTile> mode, @NonNull GL gl) {
        super(mode, gl);

        {
            AttributeFormatBuilder builder = gl.createAttributeFormat().name("VOXEL_GLOBAL");

            this.attrGlobalTilePos = builder.attrib()
                    .name("in_tile_position")
                    .int4(AttributeType.Integer.INT)
                    .interpretation(AttributeInterpretation.INTEGER)
                    .build();

            this.globalFormat = builder.build();
        }

        {
            AttributeFormatBuilder builder = gl.createAttributeFormat().name("VOXEL_LOCAL");

            this.attrLocalState = builder.attrib()
                    .name("in_state")
                    .int1(AttributeType.Integer.UNSIGNED_INT)
                    .interpretation(AttributeInterpretation.INTEGER)
                    .build();

            this.attrLocalLight = builder.attrib()
                    .name("in_light")
                    .int2(AttributeType.Integer.UNSIGNED_BYTE)
                    .interpretation(AttributeInterpretation.NORMALIZED_FLOAT)
                    .build();

            this.attrLocalColor = builder.attrib()
                    .name("in_color")
                    .int3(AttributeType.Integer.UNSIGNED_BYTE)
                    .interpretation(AttributeInterpretation.NORMALIZED_FLOAT)
                    .build();

            this.attrLocalPos = builder.attrib()
                    .name("in_pos")
                    .int3(AttributeType.Integer.UNSIGNED_BYTE)
                    .interpretation(AttributeInterpretation.FLOAT)
                    .build();

            this.vertexFormat = builder.build();
        }

        this.indexFormat = gl.createIndexFormat()
                .type(IndexType.UNSIGNED_SHORT)
                .build();

        this.drawLayout = gl.createDrawLayout()
                .withGlobals(this.globalFormat)
                .withLocals(this.vertexFormat)
                .build();

        try (
                VertexShader vertexShader = gl.createVertexShader().forLayout(this.drawLayout)
                        .include(Identifier.from(MODID, "shaders/vert/voxel/voxel.vert"))
                        .endSource()
                        .defineAll(ShaderManager.GLOBAL_DEFINES())
                        .endDefines()
                        .build();
                FragmentShader fragmentShader = gl.createFragmentShader().forLayout(this.drawLayout)
                        .include(Identifier.from(MODID, "shaders/frag/block.frag"))
                        .endSource()
                        .defineAll(ShaderManager.GLOBAL_DEFINES())
                        .endDefines()
                        .build();
                FragmentShader fragmentShaderStencil = gl.createFragmentShader().forLayout(this.drawLayout)
                        .include(Identifier.from(MODID, "shaders/frag/stencil.frag"))
                        .endSource()
                        .defineAll(ShaderManager.GLOBAL_DEFINES())
                        .endDefines()
                        .build()) {
            this.blockShader = gl.linkShaderProgram(this.drawLayout, vertexShader, fragmentShader);
            this.stencilShader = gl.linkShaderProgram(this.drawLayout, vertexShader, fragmentShaderStencil);
        } catch (ShaderCompilationException | ShaderLinkageException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public ICullingStrategy<VoxelPos> cullingStrategy() {
        return VoxelCullingStrategy.INSTANCE;
    }

    @Override
    public IRenderBaker<VoxelPos, VoxelTile, IndexedBakeOutput> createBaker() {
        return new VoxelBaker(this);
    }
}
