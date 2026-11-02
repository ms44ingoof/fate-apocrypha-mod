package com.example.fateapocrypha;

import com.example.fateapocrypha.item.CommandSealItem;
import com.example.fateapocrypha.item.SummonCatalystItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, FateApocrypha.MODID);

    public static final RegistryObject<Item> COMMAND_SEAL = ITEMS.register("command_seal",
            () -> new CommandSealItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> SUMMON_CATALYST = ITEMS.register("summon_catalyst",
            () -> new SummonCatalystItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<Item> ASTOLFO_SWORD = ITEMS.register("astolfo_sword",
            () -> new SwordItem(Tiers.IRON, 3, -2.4F, new Item.Properties()));
}
