package com.example.fateapocrypha;

import com.example.fateapocrypha.entity.AstolfoEntity;
import com.example.fateapocrypha.entity.JeanneEntity;
import com.example.fateapocrypha.entity.MordredEntity;
import com.example.fateapocrypha.entity.SemiramisEntity;
import com.example.fateapocrypha.entity.StoryCameraEntity;
import com.example.fateapocrypha.entity.StoryNpcEntity;
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

    public static final RegistryObject<EntityType<StoryNpcEntity>> STORY_NPC = ENTITIES.register("story_npc",
            () -> EntityType.Builder.of(StoryNpcEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F).clientTrackingRange(10)
                    .build(new ResourceLocation(FateApocrypha.MODID, "story_npc").toString()));

    public static final RegistryObject<EntityType<StoryCameraEntity>> STORY_CAMERA = ENTITIES.register("story_camera",
            () -> EntityType.Builder.of(StoryCameraEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).clientTrackingRange(16).updateInterval(1).noSave().fireImmune()
                    .build(new ResourceLocation(FateApocrypha.MODID, "story_camera").toString()));
}
