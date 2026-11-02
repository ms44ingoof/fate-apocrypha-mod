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
                        output.accept(ModItems.COMMAND_SEAL.get());
                        output.accept(ModItems.SUMMON_CATALYST.get());
                        output.accept(ModItems.ASTOLFO_SWORD.get());
                    })
                    .build());
}
