package com.example.fateapocrypha;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FateApocrypha.MODID);

    public static final RegistryObject<Item> COMMAND_SEAL = ITEMS.register("command_seal",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SUMMON_CATALYST = ITEMS.register("summon_catalyst",
            () -> new Item(new Item.Properties().stacksTo(16)));
}
