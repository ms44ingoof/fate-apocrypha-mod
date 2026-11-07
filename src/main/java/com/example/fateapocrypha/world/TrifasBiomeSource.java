package com.example.fateapocrypha.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/** Источник биомов Трифаса: биом выбирается по рельефу (TrifasTerrain.biome), а не по климатическому шуму. */
public class TrifasBiomeSource extends BiomeSource {
    public static final Codec<TrifasBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(source -> source.biomes)
    ).apply(instance, TrifasBiomeSource::new));

    private final HolderSet<Biome> biomes;
    private final Map<String, Holder<Biome>> byId = new HashMap<>();
    private final Holder<Biome> fallback;
    private final ThreadLocal<Map<Long, String>> cache = ThreadLocal.withInitial(HashMap::new);

    public TrifasBiomeSource(HolderSet<Biome> biomes) {
        this.biomes = biomes;
        Holder<Biome> first = null;
        for (Holder<Biome> holder : biomes) {
            if (first == null) {
                first = holder;
            }
            holder.unwrapKey().ifPresent(key -> byId.put(key.location().toString(), holder));
        }
        Holder<Biome> plains = byId.get("minecraft:plains");
        this.fallback = plains != null ? plains : first;
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        int x = qx * 4 + 2;
        int z = qz * 4 + 2;
        int y = qy * 4;
        // острова отличаются по высоте, поэтому ключ учитывает "наземный" и "небесный" слой
        long key = (((long) x) & 0xFFFFFFFFL) << 32 | (((long) z) & 0x7FFFFFFFL) << 1 | (y >= 140 ? 1L : 0L);
        Map<Long, String> map = cache.get();
        String id = map.get(key);
        if (id == null) {
            id = TrifasTerrain.biome(x, z, y);
            if (map.size() > 30000) {
                map.clear();
            }
            map.put(key, id);
        }
        Holder<Biome> holder = byId.get(id);
        return holder != null ? holder : fallback;
    }
}
