package com.example.fateapocrypha;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, FateApocrypha.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fateapocrypha"))
                    .icon(() -> ModItems.COMMAND_SEAL.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.SUMMON_CATALYST.get());
                        output.accept(ModItems.ASTOLFO_SWORD.get());
                        output.accept(ModItems.PRANA_CRYSTAL.get());
                        output.accept(ModItems.HOLY_STEEL.get());
                        output.accept(ModItems.DRAGON_SCALE.get());
                        output.accept(ModItems.FAFNIR_BLOOD.get());
                        output.accept(ModItems.CRIMSON_SHARD.get());
                        output.accept(ModItems.LIGHTNING_CORE.get());
                        output.accept(ModItems.HIPPOGRIFF_FEATHER.get());
                        output.accept(ModItems.HOLY_CLOTH.get());
                        output.accept(ModItems.BLACK_STEEL.get());
                        output.accept(ModItems.BLOOD_THORN.get());
                        output.accept(ModItems.PELIAN_ASH.get());
                        output.accept(ModItems.SILVER_BOWSTRING.get());
                        output.accept(ModItems.GARDENS_GEM.get());
                        output.accept(ModItems.FOG_ESSENCE.get());
                        output.accept(ModItems.BROKEN_CHAIN.get());
                        output.accept(ModItems.BALMUNG.get());
                        output.accept(ModItems.CLARENT.get());
                        output.accept(ModItems.VASAVI_SHAKTI.get());
                        output.accept(ModItems.ARGALIA_LANCE.get());
                        output.accept(ModItems.LA_PUCELLE.get());
                        output.accept(ModItems.BLACK_KEYS.get());
                        output.accept(ModItems.RIPPER_KNIFE.get());
                        output.accept(ModItems.SPARTACUS_BLADE.get());
                        output.accept(ModItems.KAZIKLI_BEY.get());
                        output.accept(ModItems.ACHILLES_SPEAR.get());
                        output.accept(ModItems.ATALANTA_BOW.get());
                        output.accept(ModItems.SEMIRAMIS_STAFF.get());
                    })
                    .build());
}
