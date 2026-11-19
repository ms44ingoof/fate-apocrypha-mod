package com.example.fateapocrypha.client;

import com.example.fateapocrypha.story.SideBoard;
import com.example.fateapocrypha.story.SideQuest;
import com.example.fateapocrypha.story.SideQuestManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Клиентская копия состояний побочных заданий (приходит с сервера в SideQuestSyncPacket). */
public final class ClientSideQuests {
    private static byte[] states = new byte[0];

    private ClientSideQuests() {
    }

    public static void update(byte[] newStates) {
        states = newStates;
    }

    public static int state(SideQuest quest) {
        return quest.ordinal() < states.length ? states[quest.ordinal()] : SideQuestManager.LOCKED;
    }

    /** Сколько нужного предмета у игрока сейчас (для заданий-поставок: считается на клиенте по инвентарю). */
    public static int have(SideQuest quest) {
        Player player = Minecraft.getInstance().player;
        if (player == null || quest.item() == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(quest.item())) {
                total += stack.getCount();
            }
        }
        return total;
    }

    /**
     * Что показывать на карте у доски: 0 ничего, 1 новое задание (!), 2 задание идёт (~), 3 сдать или доставить сюда (?).
     */
    public static int boardState(SideBoard board) {
        boolean available = false;
        boolean active = false;
        for (SideQuest q : SideQuest.values()) {
            int st = state(q);
            if (q.type() == SideQuest.Type.DELIVER && q.target() == board && st == SideQuestManager.ACTIVE) {
                return 3;
            }
            if (q.giver() != board) {
                continue;
            }
            if (st == SideQuestManager.READY) {
                return 3;
            }
            if (st == SideQuestManager.AVAILABLE) {
                available = true;
            } else if (st == SideQuestManager.ACTIVE) {
                if (q.type() == SideQuest.Type.FETCH && have(q) >= q.count()) {
                    return 3;
                }
                active = true;
            }
        }
        return available ? 1 : active ? 2 : 0;
    }
}
