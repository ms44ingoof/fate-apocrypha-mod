package com.example.fateapocrypha;

import com.example.fateapocrypha.entity.AstolfoEntity;
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
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(new ResourceLocation(FateApocrypha.MODID, "astolfo").toString()));
}
