package com.example.fateapocrypha;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FateApocrypha.MODID)
public class FateApocrypha {
    public static final String MODID = "fateapocrypha";

    public FateApocrypha() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModCreativeTabs.TABS.register(bus);
    }
}
