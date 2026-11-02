package com.example.fateapocrypha.servant;

import com.example.fateapocrypha.ModEntities;
import com.example.fateapocrypha.entity.ServantEntity;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Слуги, которых можно призвать катализатором. Порядок = id в пакете. */
public enum ServantType {
    ASTOLFO("astolfo", () -> ModEntities.ASTOLFO.get(), Items.GOLDEN_SWORD),
    MORDRED("mordred", () -> ModEntities.MORDRED.get(), Items.IRON_SWORD),
    JEANNE("jeanne", () -> ModEntities.JEANNE.get(), Items.WHITE_BANNER),
    SEMIRAMIS("semiramis", () -> ModEntities.SEMIRAMIS.get(), Items.AIR);

    private final String id;
    private final Supplier<? extends EntityType<? extends ServantEntity>> entityType;
    private final Item weapon;

    ServantType(String id, Supplier<? extends EntityType<? extends ServantEntity>> entityType, Item weapon) {
        this.id = id;
        this.entityType = entityType;
        this.weapon = weapon;
    }

    public EntityType<? extends ServantEntity> entityType() {
        return entityType.get();
    }

    public String nameKey() {
        return "entity.fateapocrypha." + id;
    }

    public ItemStack weapon() {
        return weapon == Items.AIR ? ItemStack.EMPTY : new ItemStack(weapon);
    }

    public static ServantType byId(int id) {
        ServantType[] all = values();
        return (id >= 0 && id < all.length) ? all[id] : null;
    }
}
