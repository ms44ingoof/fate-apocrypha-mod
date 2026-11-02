package com.example.fateapocrypha.story;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Сюжетные NPC. slim = тонкие руки (Alex) в скине. */
public enum NpcType {
    ASTOLFO("astolfo", true, Items.GOLDEN_SWORD),
    CHIRON("chiron", false, Items.BOW);

    private final String id;
    private final boolean slim;
    private final Item heldItem;

    NpcType(String id, boolean slim, Item heldItem) {
        this.id = id;
        this.slim = slim;
        this.heldItem = heldItem;
    }

    public String id() {
        return id;
    }

    public boolean slim() {
        return slim;
    }

    public Item heldItem() {
        return heldItem;
    }

    public static NpcType byId(String id) {
        for (NpcType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return ASTOLFO;
    }
}
