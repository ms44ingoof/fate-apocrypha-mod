package com.example.fateapocrypha;

import com.example.fateapocrypha.entity.AstolfoEntity;
import com.example.fateapocrypha.entity.JeanneEntity;
import com.example.fateapocrypha.entity.MordredEntity;
import com.example.fateapocrypha.entity.SemiramisEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, FateApocrypha.MODID);

    public static final RegistryObject<EntityType<AstolfoEntity>> ASTOLFO = ENTITIES.register("astolfo",
            () -> EntityType.Builder.of(AstolfoEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build(new ResourceLocation(FateApocrypha.MODID, "astolfo").toString()));

    public static final RegistryObject<EntityType<MordredEntity>> MORDRED = ENTITIES.register("mordred",
            () -> EntityType.Builder.of(MordredEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build(new ResourceLocation(FateApocrypha.MODID, "mordred").toString()));

    public static final RegistryObject<EntityType<JeanneEntity>> JEANNE = ENTITIES.register("jeanne",
            () -> EntityType.Builder.of(JeanneEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build(new ResourceLocation(FateApocrypha.MODID, "jeanne").toString()));

    public static final RegistryObject<EntityType<SemiramisEntity>> SEMIRAMIS = ENTITIES.register("semiramis",
            () -> EntityType.Builder.of(SemiramisEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build(new ResourceLocation(FateApocrypha.MODID, "semiramis").toString()));
}
