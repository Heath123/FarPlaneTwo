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

package net.daporkchop.fp2.impl.mc.forge1_12_2.client.render;

import lombok.Getter;
import lombok.NonNull;
import net.daporkchop.fp2.asm.interfaz.client.renderer.IMixinRenderGlobal;
import net.daporkchop.fp2.client.ReversedZ;
import net.daporkchop.fp2.client.gl.MatrixHelper;
import net.daporkchop.fp2.common.util.DirectBufferHackery;
import net.daporkchop.fp2.core.client.render.GlobalUniformAttributes;
import net.daporkchop.fp2.core.client.render.TerrainRenderingBlockedTracker;
import net.daporkchop.fp2.core.client.render.WorldRenderer;
import net.daporkchop.fp2.core.util.GlobalAllocators;
import net.daporkchop.fp2.gl.GL;
import net.daporkchop.fp2.impl.mc.forge1_12_2.FakeFarWorldClient;
import net.daporkchop.fp2.impl.mc.forge1_12_2.world.registry.GameRegistry1_12_2;
import net.daporkchop.fp2.impl.mc.forge1_12_2.ResourceProvider1_12_2;
import net.daporkchop.fp2.util.SingleBiomeBlockAccess;
import net.daporkchop.lib.common.pool.array.ArrayAllocator;
import net.daporkchop.lib.unsafe.PUnsafe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.nio.FloatBuffer;

import static net.daporkchop.fp2.client.gl.OpenGL.*;
import static net.daporkchop.fp2.compat.of.OFHelper.*;
import static net.daporkchop.fp2.util.BlockType.*;
import static net.daporkchop.lib.common.math.PMath.*;
import static net.minecraft.util.math.MathHelper.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * @author DaPorkchop_
 */
@Getter
public class WorldRenderer1_12_2 implements WorldRenderer, AutoCloseable {
    protected final Minecraft mc;
    protected final GL gl;

    protected final FakeFarWorldClient world;

    protected final GameRegistry1_12_2 registry;
    protected final byte[] renderTypeLookup;

    public WorldRenderer1_12_2(@NonNull Minecraft mc, @NonNull FakeFarWorldClient world) {
        this.mc = mc;
        this.world = world;

        this.registry = world.fp2_IFarWorld_registry();

        //look up and cache the render type for each block state
        this.renderTypeLookup = new byte[this.registry.states().max().getAsInt() + 1];
        this.registry.states().forEach(state -> {
            int typeIndex;
            switch (this.registry.id2state(state).getBlock().getRenderLayer()) {
                default:
                    typeIndex = RENDER_TYPE_OPAQUE;
                    break;
                case CUTOUT:
                case CUTOUT_MIPPED:
                    typeIndex = RENDER_TYPE_CUTOUT;
                    break;
                case TRANSLUCENT:
                    typeIndex = RENDER_TYPE_TRANSLUCENT;
                    break;
            }
            this.renderTypeLookup[state] = (byte) typeIndex;
        });

        this.gl = GL.builder()
                .withResourceProvider(new ResourceProvider1_12_2(this.mc))
                .wrapCurrent();
    }

    @Override
    public int renderTypeForState(int state) {
        return this.renderTypeLookup[state];
    }

    @Override
    public int tintFactorForStateInBiomeAtPos(int state, int biome, int x, int y, int z) {
        return this.mc.getBlockColors().colorMultiplier(this.registry.id2state(state), new SingleBiomeBlockAccess().biome(this.registry.id2biome(biome)), new BlockPos(x, y, z), 0);
    }

    @Override
    public TerrainRenderingBlockedTracker blockedTracker() {
        return ((IMixinRenderGlobal) this.mc.renderGlobal).fp2_vanillaRenderabilityTracker();
    }

    @Override
    public GlobalUniformAttributes globalUniformAttributes() {
        GlobalUniformAttributes attributes = new GlobalUniformAttributes();

        //optifine compatibility: disable fog if it's turned off, because optifine only does this itself if no vanilla terrain is being rendered
        //  (e.g. it's all being discarded in frustum culling)
        if (OF && (PUnsafe.getInt(this.mc.gameSettings, OF_FOGTYPE_OFFSET) == OF_OFF && PUnsafe.getBoolean(this.mc.entityRenderer, OF_ENTITYRENDERER_FOGSTANDARD_OFFSET))) {
            GlStateManager.disableFog();
        }

        { //camera
            this.initModelViewProjectionMatrix(attributes);

            float partialTicks = this.mc.getRenderPartialTicks();
            Entity entity = this.mc.getRenderViewEntity();
            double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks;
            double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks;
            double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks;

            attributes.u_positionFloorX = floorI(x);
            attributes.u_positionFloorY = floorI(y);
            attributes.u_positionFloorZ = floorI(z);

            attributes.u_positionFracX = (float) frac(x);
            attributes.u_positionFracY = (float) frac(y);
            attributes.u_positionFracZ = (float) frac(z);
        }

        { //fog
            this.initFogColor(attributes);

            attributes.u_fogDensity = glGetFloat(GL_FOG_DENSITY);
            attributes.u_fogStart = glGetFloat(GL_FOG_START);
            attributes.u_fogEnd = glGetFloat(GL_FOG_END);
            attributes.u_fogScale = 1.0f / (attributes.u_fogEnd - attributes.u_fogStart);
        }

        return attributes;
    }

    private void initModelViewProjectionMatrix(GlobalUniformAttributes attributes) {
        ArrayAllocator<float[]> alloc = GlobalAllocators.ALLOC_FLOAT.get();

        float[] modelView = alloc.atLeast(MAT4_ELEMENTS);
        float[] projection = alloc.atLeast(MAT4_ELEMENTS);
        try {
            //load both matrices into arrays
            MatrixHelper.getFloatMatrixFromGL(GL_MODELVIEW_MATRIX, modelView);
            MatrixHelper.getFloatMatrixFromGL(GL_PROJECTION_MATRIX, projection);

            //pre-multiply matrices on CPU to avoid having to do it per-vertex on GPU
            MatrixHelper.multiply4x4(projection, modelView, attributes.u_modelViewProjectionMatrix);

            //offset the projected points' depth values to avoid z-fighting with vanilla terrain
            MatrixHelper.offsetDepth(attributes.u_modelViewProjectionMatrix, ReversedZ.REVERSED ? -0.00001f : 0.00001f);
        } finally {
            alloc.release(projection);
            alloc.release(modelView);
        }
    }

    private void initFogColor(GlobalUniformAttributes attributes) {
        //buffer needs to fit 16 elements, but only the first 4 will be used
        long addr = PUnsafe.allocateMemory(16 * FLOAT_SIZE);
        try {
            FloatBuffer buffer = DirectBufferHackery.wrapFloat(addr, 16);
            glGetFloat(GL_FOG_COLOR, buffer);

            attributes.u_fogColorR = buffer.get(0);
            attributes.u_fogColorG = buffer.get(1);
            attributes.u_fogColorB = buffer.get(2);
            attributes.u_fogColorA = buffer.get(3);
        } finally {
            PUnsafe.freeMemory(addr);
        }
    }

    @Override
    public Object terrainTextureId() {
        return this.mc.getTextureMapBlocks().getGlTextureId();
    }

    @Override
    public Object lightmapTextureId() {
        return this.mc.entityRenderer.lightmapTexture.getGlTextureId();
    }

    @Override
    public void close() {
        this.gl.close();
    }
}