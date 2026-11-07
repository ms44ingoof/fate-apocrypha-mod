package com.example.fateapocrypha.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;

/** Генератор открытого мира Трифаса по функции рельефа TrifasTerrain (без шумовых настроек ванили). */
public class TrifasChunkGenerator extends ChunkGenerator {
    public static final Codec<TrifasChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(TrifasChunkGenerator::getBiomeSource)
    ).apply(instance, instance.stable(TrifasChunkGenerator::new)));

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState SNOW = Blocks.SNOW_BLOCK.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState ANDESITE = Blocks.ANDESITE.defaultBlockState();
    private static final BlockState TUFF = Blocks.TUFF.defaultBlockState();
    private static final BlockState[] BANDS = {
            Blocks.TERRACOTTA.defaultBlockState(), Blocks.ORANGE_TERRACOTTA.defaultBlockState(),
            Blocks.TERRACOTTA.defaultBlockState(), Blocks.YELLOW_TERRACOTTA.defaultBlockState(),
            Blocks.BROWN_TERRACOTTA.defaultBlockState(), Blocks.TERRACOTTA.defaultBlockState(),
            Blocks.RED_TERRACOTTA.defaultBlockState(), Blocks.WHITE_TERRACOTTA.defaultBlockState(),
            Blocks.TERRACOTTA.defaultBlockState(), Blocks.LIGHT_GRAY_TERRACOTTA.defaultBlockState()
    };

    public TrifasChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /** Без структур ванили (деревни, шахты и т. д.): мир наполняют сюжет и свои объекты. */
    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> structureSets, RandomState randomState, long seed) {
        return ChunkGeneratorStructureState.createForFlat(randomState, seed, this.biomeSource, Stream.empty());
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
        // пещер нет: сюжетные площадки стоят прямо на земле
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState random, ChunkAccess chunk) {
        // поверхность уже выложена в fillFromNoise
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        ChunkPos chunkPos = region.getCenter();
        net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome =
                region.getBiome(chunkPos.getWorldPosition().atY(region.getMaxBuildHeight() - 1));
        net.minecraft.world.level.levelgen.WorldgenRandom random = new net.minecraft.world.level.levelgen.WorldgenRandom(
                new net.minecraft.world.level.levelgen.LegacyRandomSource(
                        net.minecraft.world.level.levelgen.RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(region.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());
        net.minecraft.world.level.NaturalSpawner.spawnMobsForChunkGeneration(region, biome, chunkPos, random);
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState random,
                                                        StructureManager structures, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("wgen_fill_trifas", () -> {
            fill(chunk);
            return chunk;
        }), Util.backgroundExecutor());
    }

    @Override
    public int getSeaLevel() {
        return TrifasTerrain.SEA;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        int h = TrifasTerrain.height(x, z);
        boolean floor = type == Heightmap.Types.OCEAN_FLOOR || type == Heightmap.Types.OCEAN_FLOOR_WG;
        return floor ? h + 1 : Math.max(h, TrifasTerrain.SEA) + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int h = TrifasTerrain.height(x, z);
        BlockState[] states = new BlockState[level.getHeight()];
        for (int i = 0; i < states.length; i++) {
            int y = level.getMinBuildHeight() + i;
            states[i] = y <= h ? STONE : y <= TrifasTerrain.SEA ? WATER : AIR;
        }
        return new NoiseColumn(level.getMinBuildHeight(), states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
    }

    // ---------- заполнение чанка ----------
    private static int hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        return h ^ (h >>> 16);
    }

    private static void put(ChunkAccess chunk, BlockPos.MutableBlockPos pos, Heightmap floorMap, Heightmap surfMap,
                            int lx, int y, int lz, BlockState state) {
        chunk.setBlockState(pos.set(lx, y, lz), state, false);
        floorMap.update(lx, y, lz, state);
        surfMap.update(lx, y, lz, state);
    }

    private void fill(ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap floorMap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surfMap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int minY = chunk.getMinBuildHeight();

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = cp.getMinBlockX() + lx;
                int wz = cp.getMinBlockZ() + lz;
                int h = TrifasTerrain.height(wx, wz);
                int slope = Math.max(Math.abs(h - TrifasTerrain.height(wx + 3, wz)),
                        Math.abs(h - TrifasTerrain.height(wx, wz + 3)));
                int kind = TrifasTerrain.surfaceKind(wx, wz, h, slope);

                for (int y = minY; y <= h; y++) {
                    put(chunk, pos, floorMap, surfMap, lx, y, lz, groundBlock(kind, y, h, wx, wz, minY));
                }
                for (int y = h + 1; y <= TrifasTerrain.SEA; y++) {
                    put(chunk, pos, floorMap, surfMap, lx, y, lz, WATER);
                }

                // парящий остров над землёй
                double thick = TrifasTerrain.islandThickness(wx, wz);
                if (thick > 3.0D) {
                    int top = TrifasTerrain.islandTop(wx, wz);
                    int bottom = top - (int) thick;
                    if (h + 22 < bottom) {
                        for (int y = bottom; y <= top; y++) {
                            BlockState state;
                            if (y == top) {
                                state = GRASS;
                            } else if (y >= top - 3) {
                                state = DIRT;
                            } else {
                                state = hash(wx, y, wz) % 7 == 0 ? TUFF : STONE;
                            }
                            put(chunk, pos, floorMap, surfMap, lx, y, lz, state);
                        }
                    }
                }
            }
        }
    }

    private static BlockState groundBlock(int kind, int y, int h, int wx, int wz, int minY) {
        if (y == minY) {
            return BEDROCK;
        }
        if (y <= minY + 4 && Math.floorMod(hash(wx, y, wz), 5) < (minY + 5 - y)) {
            return BEDROCK;
        }
        int depth = h - y;
        if (kind == TrifasTerrain.KIND_MESA && depth <= 14) {
            return BANDS[Math.floorMod(y / 2 + (hash(wx >> 3, 0, wz >> 3) & 3), BANDS.length)];
        }
        if (depth == 0) {
            switch (kind) {
                case TrifasTerrain.KIND_STONE: return hash(wx, y, wz) % 5 == 0 ? ANDESITE : STONE;
                case TrifasTerrain.KIND_SNOW: return SNOW;
                case TrifasTerrain.KIND_SAND: return SAND;
                case TrifasTerrain.KIND_GRAVEL: return GRAVEL;
                default: return GRASS;
            }
        }
        if (depth <= 3) {
            switch (kind) {
                case TrifasTerrain.KIND_STONE: return STONE;
                case TrifasTerrain.KIND_SNOW: return depth == 1 ? SNOW : STONE;
                case TrifasTerrain.KIND_SAND: return SAND;
                case TrifasTerrain.KIND_GRAVEL: return GRAVEL;
                default: return DIRT;
            }
        }
        if (y < 0) {
            int blend = Math.floorMod(hash(wx, y, wz), 8);
            return y < -8 || blend < -y ? DEEPSLATE : STONE;
        }
        return STONE;
    }
}
