package com.example.fateapocrypha.client;

import com.example.fateapocrypha.client.screen.QuestBoardScreen;
import com.example.fateapocrypha.story.SideBoard;
import net.minecraft.client.Minecraft;

/** Открывает окно доски объявлений на клиенте. */
public final class ClientBoard {
    private ClientBoard() {
    }

    public static void open(int boardIndex) {
        SideBoard[] boards = SideBoard.values();
        if (boardIndex >= 0 && boardIndex < boards.length) {
            Minecraft.getInstance().setScreen(new QuestBoardScreen(boards[boardIndex]));
        }
    }
}
