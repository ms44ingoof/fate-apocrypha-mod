package com.example.fateapocrypha.network;

import com.example.fateapocrypha.story.SideBoard;
import com.example.fateapocrypha.story.SideQuest;
import com.example.fateapocrypha.story.SideQuestManager;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Клиент -> сервер: действие в окне доски. action 0: взять задание, 1: сдать. Сервер всё перепроверяет. */
public class BoardActionPacket {
    private final int board;
    private final int quest;
    private final int action;

    public BoardActionPacket(int board, int quest, int action) {
        this.board = board;
        this.quest = quest;
        this.action = action;
    }

    public static void encode(BoardActionPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.board);
        buf.writeVarInt(p.quest);
        buf.writeVarInt(p.action);
    }

    public static BoardActionPacket decode(FriendlyByteBuf buf) {
        return new BoardActionPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(BoardActionPacket p, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            SideBoard[] boards = SideBoard.values();
            SideQuest[] quests = SideQuest.values();
            if (player == null || p.board < 0 || p.board >= boards.length || p.quest < 0 || p.quest >= quests.length) {
                return;
            }
            SideBoard board = boards[p.board];
            if (!SideQuestManager.near(player, board)) {
                return;
            }
            if (p.action == 0) {
                SideQuestManager.accept(player, board, quests[p.quest]);
            } else if (p.action == 1) {
                SideQuestManager.turnIn(player, board, quests[p.quest]);
            }
        });
        context.setPacketHandled(true);
    }
}
