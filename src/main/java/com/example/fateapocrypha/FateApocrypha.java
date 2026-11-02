package com.example.fateapocrypha;

import com.example.fateapocrypha.network.ModNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FateApocrypha.MODID)
public class FateApocrypha {
    public static final String MODID = "fateapocrypha";

    public FateApocrypha() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModCreativeTabs.TABS.register(bus);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }
}
