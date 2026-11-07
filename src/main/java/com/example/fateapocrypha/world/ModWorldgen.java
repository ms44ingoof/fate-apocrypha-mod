package com.example.fateapocrypha.world;

import com.example.fateapocrypha.FateApocrypha;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Регистрация генератора мира и источника биомов Трифаса (используются в data/.../dimension/trifas.json). */
public class ModWorldgen {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, FateApocrypha.MODID);
    public static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, FateApocrypha.MODID);

    public static final RegistryObject<Codec<? extends ChunkGenerator>> TRIFAS_GENERATOR =
            CHUNK_GENERATORS.register("trifas", () -> TrifasChunkGenerator.CODEC);
    public static final RegistryObject<Codec<? extends BiomeSource>> TRIFAS_BIOMES =
            BIOME_SOURCES.register("trifas", () -> TrifasBiomeSource.CODEC);
}
