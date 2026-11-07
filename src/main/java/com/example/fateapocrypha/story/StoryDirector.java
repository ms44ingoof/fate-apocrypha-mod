package com.example.fateapocrypha.story;

import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.scene.SceneManager;
import com.example.fateapocrypha.scene.Step;
import com.example.fateapocrypha.scene.StoryScenes;
import com.example.fateapocrypha.scene.StoryWorld;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Запускает сцену текущей главы игрока. */
public class StoryDirector {
    public static void startScene(ServerPlayer player) {
        StoryChapter chapter = StoryManager.getChapter(player);
        List<Step> steps = StoryScenes.build(chapter);
        if (steps.isEmpty()) {
            SceneManager.stop(player.getUUID());
            return;
        }
        ServerLevel trifas = player.getServer() == null ? null : player.getServer().getLevel(ModDimensions.TRIFAS);
        if (trifas == null) {
            player.sendSystemMessage(Component.translatable("command.fateapocrypha.no_dimension"));
            return;
        }
        StoryWorld.ensureBuilt(trifas);
        com.example.fateapocrypha.scene.StoryDetails.ensure(trifas);
        SceneManager.start(player, steps);
    }

    /** Вход в мир или возрождение: продолжаем сюжет с начала текущей главы. */
    public static void resume(ServerPlayer player) {
        if (StoryManager.getChapter(player).ordinal() < StoryChapter.TO_BE_CONTINUED.ordinal()) {
            startScene(player);
        } else {
            StoryManager.showObjective(player);
        }
    }
}
