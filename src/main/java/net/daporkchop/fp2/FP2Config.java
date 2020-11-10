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

package net.daporkchop.fp2;

import net.daporkchop.fp2.mode.RenderMode;
import net.daporkchop.lib.common.util.PorkUtil;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.Options;

import static java.lang.Math.*;

/**
 * @author DaPorkchop_
 */
@Config(modid = FP2.MODID)
public class FP2Config {
    @Config.Comment({
            "The mode that will be used for rendering distant terrain."
    })
    @Config.LangKey("config.fp2.renderMode")
    @Config.RequiresWorldRestart
    public static RenderMode renderMode = RenderMode.HEIGHTMAP;

    @Config.Comment({
            "The far plane render distance (in blocks)"
    })
    @Config.RangeInt(min = 1)
    @Config.LangKey("config.fp2.renderDistance")
    public static int renderDistance = 512;

    @Config.Comment({
            "The number of LoD levels to use.",
            "Warning: Increasing this value requires exponentially larger amounts of memory! (for now, this'll be fixed later)"
    })
    @Config.RangeInt(min = 1, max = 32)
    @Config.SlidingOption
    @Config.LangKey("config.fp2.maxLevels")
    @Config.RequiresWorldRestart
    public static int maxLevels = 3;

    @Config.Comment({
            "The distance (in blocks) between LoD transitions.",
            "Note that this value is doubled for each level, so a setting of 64 will mean 64 blocks for the first layer, 128 blocks for the second layer, etc."
    })
    @Config.RangeInt(min = 0)
    @Config.LangKey("config.fp2.levelCutoffDistance")
    public static int levelCutoffDistance = 64;

    @Config.Comment({
            "The number of threads that will be used for generating far plane terrain data.",
            "Default: <cpu count> - 1 (and at least 1)"
    })
    @Config.RangeInt(min = 1)
    @Config.LangKey("config.fp2.generationThreads")
    @Config.RequiresWorldRestart
    public static int generationThreads = max(PorkUtil.CPU_COUNT - 1, 1);

    @Config.Comment({
            "The number of threads that will be used for saving far plane terrain data.",
            "Default: <cpu count> / 4 (and at least 1)"
    })
    @Config.RangeInt(min = 1)
    @Config.LangKey("config.fp2.ioThreads")
    @Config.RequiresWorldRestart
    public static int ioThreads = max(PorkUtil.CPU_COUNT >> 2, 1);

    @Config.Comment({
            "Config options available only on the client."
    })
    @Config.LangKey("config.fp2.client")
    @SideOnly(Side.CLIENT)
    public static Client client = new Client();

    @Config.Comment({
            "Performance options."
    })
    @Config.LangKey("config.fp2.performance")
    public static Performance performance = new Performance();

    @Config.Comment({
            "Options for storage of far terrain tiles."
    })
    @Config.LangKey("config.fp2.storage")
    public static Storage storage = new Storage();

    @Config.Comment({
            "Config options useful while developing the mod."
    })
    @Config.LangKey("config.fp2.debug")
    public static Debug debug = new Debug();

    /**
     * @author DaPorkchop_
     */
    @SideOnly(Side.CLIENT)
    public static class Client {
        @Config.Comment({
                "The number of threads that will be used for preparing far plane terrain data for rendering.",
                "Default: <cpu count> - 1 (and at least 1)"
        })
        @Config.LangKey("config.fp2.client.renderThreads")
        @Config.RequiresWorldRestart
        public int renderThreads = max(PorkUtil.CPU_COUNT - 1, 1);

        @Config.Comment({
                "The factor of the maximum extent of a detail level at which the detail transition will start.",
                "Should be less than levelTransitionEnd."
        })
        @Config.LangKey("config.fp2.client.levelTransitionStart")
        @Config.SlidingOption
        @Config.RangeDouble(min = 0.0d, max = 1.0d)
        public double levelTransitionStart = 0.6d;

        @Config.Comment({
                "The factor of the maximum extent of a detail level at which the detail transition will end.",
                "Should be more than levelTransitionStart."
        })
        @Config.LangKey("config.fp2.client.levelTransitionEnd")
        @Config.SlidingOption
        @Config.RangeDouble(min = 0.0d, max = 1.0d)
        public double levelTransitionEnd = 0.9d;
    }

    /**
     * @author DaPorkchop_
     */
    public static class Performance {
        @Config.Comment({
                "Whether or not tiles can be generated at low resolution.",
                "The generator must support this feature as well."
        })
        @Config.LangKey("config.fp2.performance.lowResolutionEnable")
        public boolean lowResolutionEnable = true;

        @Config.Comment({
                "If low resolution is enabled, allows tiles to be initially generated at low resolution and subsequently be replaced by higher resolution",
                "tiles once available."
        })
        @Config.LangKey("config.fp2.performance.lowResolutionRefine")
        public boolean lowResolutionRefine = false;

        @Config.Comment({
                "If low resolution refine is enabled, allows incomplete pieces to be saved.",
                "This can provide a performance boost if the server is restarted while pieces are still being generated and low resolution refine is",
                "enabled, but will likely significantly reduce overall generation performance."
        })
        @Config.LangKey("config.fp2.performance.savePartial")
        public boolean savePartial = false;
    }

    /**
     * @author DaPorkchop_
     */
    public static class Storage {
        @Config.Comment({
                "The options used by the leveldb databases for storing far terrain data.",
                "Don't touch this unless you know what you're doing!"
        })
        @Config.LangKey("config.fp2.storage.leveldb")
        @Config.RequiresWorldRestart
        public Leveldb leveldb = new Leveldb();

        public static class Leveldb {
            @Config.RangeInt(min = 0)
            public int writeBufferSize = 4 << 20;
            @Config.RangeInt(min = 1)
            public int maxOpenFiles = 1000;
            @Config.RangeInt(min = 1)
            public int blockRestartInterval = 16;
            @Config.RangeInt(min = 1)
            public int blockSize = 4 * 1024;
            public CompressionType compressionType = CompressionType.NONE;
            public boolean verifyChecksums = true;
            public boolean paranoidChecks;
            @Config.RangeInt(min = 0)
            public int cacheSize;

            public Options use()    {
                return new Options()
                        .writeBufferSize(this.writeBufferSize)
                        .maxOpenFiles(this.maxOpenFiles)
                        .blockRestartInterval(this.blockRestartInterval)
                        .blockSize(this.blockSize)
                        .compressionType(this.compressionType)
                        .verifyChecksums(this.verifyChecksums)
                        .paranoidChecks(this.paranoidChecks)
                        .cacheSize(this.cacheSize);
            }
        }
    }

    /**
     * @author DaPorkchop_
     */
    public static class Debug {
        @Config.Comment({
                "If true, disables reading of heightmap tiles from disk.",
                "Overridden by disablePersistence.",
                "Default: false"
        })
        @Config.LangKey("config.fp2.debug.disableRead")
        public boolean disableRead = false;

        @Config.Comment({
                "If true, disables writing of heightmap tiles to disk.",
                "Overridden by disablePersistence.",
                "Default: false"
        })
        @Config.LangKey("config.fp2.debug.disableWrite")
        public boolean disableWrite = false;

        @Config.Comment({
                "If true, completely disables disk persistence.",
                "Far terrain data will never be written to or read from disk.",
                "Default: false"
        })
        @Config.LangKey("config.fp2.debug.disablePersistence")
        public boolean disablePersistence = false;

        @Config.Comment({
                "If true, the vanilla world will not be rendered."
        })
        @Config.LangKey("config.fp2.debug.skipRenderWorld")
        public boolean skipRenderWorld = false;
    }
}
