package com.example.fateapocrypha;

import com.example.fateapocrypha.item.CommandSealItem;
import com.example.fateapocrypha.item.SummonCatalystItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FateApocrypha.MODID);

    // durability(3) = 3 заряда, предмет сам не стакается
    public static final RegistryObject<Item> COMMAND_SEAL = ITEMS.register("command_seal",
            () -> new CommandSealItem(new Item.Properties().durability(3)));

    public static final RegistryObject<Item> SUMMON_CATALYST = ITEMS.register("summon_catalyst",
            () -> new SummonCatalystItem(new Item.Properties().stacksTo(16)));
}
