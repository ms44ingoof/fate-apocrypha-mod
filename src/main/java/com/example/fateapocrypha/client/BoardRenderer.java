package com.example.fateapocrypha.client;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.ModDimensions;
import com.example.fateapocrypha.story.SideBoard;
import com.example.fateapocrypha.story.SideQuest;
import com.example.fateapocrypha.story.SideQuestManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Пишет задания прямо на щите доски объявлений: название доски и список заданий с отметками
 * («!» новое, «~» идёт, «?» можно сдать, «+» выполнено). Блоков-табличек нет, текст рисуется поверх досок щита.
 */
@Mod.EventBusSubscriber(modid = FateApocrypha.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BoardRenderer {
    private static final double RANGE = 22.0D;
    private static final float SCALE = 0.0135F;   // блоков на один пиксель шрифта: щит шириной 3 блока вмещает около 220 пикселей

    private BoardRenderer() {
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimension().equals(ModDimensions.TRIFAS)) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        boolean drew = false;
        for (SideBoard board : SideBoard.values()) {
            double cx = board.x() + 0.5D;
            double cz = board.z() + 0.5D;
            if (Math.abs(cx - cam.x) > RANGE || Math.abs(cz - cam.z) > RANGE) {
                continue;
            }
            drawBoard(mc.font, event.getPoseStack(), buffers, board, cam);
            drew = true;
        }
        if (drew) {
            buffers.endBatch();
        }
    }

    private static Direction direction(int facing) {
        return switch (facing) {
            case 0 -> Direction.NORTH;
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            default -> Direction.WEST;
        };
    }

    private static void drawBoard(Font font, PoseStack pose, MultiBufferSource buffers, SideBoard board, Vec3 cam) {
        Direction f = direction(board.facing());
        // центр лицевой стороны щита: середина по ширине, середина двух блоков высоты, чуть впереди досок
        double px = board.x() + 0.5D + f.getStepX() * 0.52D;
        double py = board.y() + 2.0D;
        double pz = board.z() + 0.5D + f.getStepZ() * 0.52D;
        if (Math.hypot(px - cam.x, pz - cam.z) > RANGE) {
            return;
        }

        List<Line> lines = new ArrayList<>();
        lines.add(new Line(Component.translatable(board.nameKey()).getString(), 0xFFF2E2B8, true));
        int count = 0;
        for (SideQuest q : SideQuest.values()) {
            int st = ClientSideQuests.state(q);
            boolean here = q.giver() == board
                    || (q.type() == SideQuest.Type.DELIVER && q.target() == board && st == SideQuestManager.ACTIVE);
            if (!here || st < SideQuestManager.AVAILABLE) {
                continue;
            }
            String mark;
            int color;
            if (q.type() == SideQuest.Type.DELIVER && q.target() == board && st == SideQuestManager.ACTIVE) {
                mark = "?";
                color = 0xFF7BD389;
            } else if (st == SideQuestManager.READY) {
                mark = "?";
                color = 0xFF7BD389;
            } else if (st == SideQuestManager.AVAILABLE) {
                mark = "!";
                color = 0xFFF5C95A;
            } else if (st == SideQuestManager.DONE) {
                mark = "+";
                color = 0xFF9AA08C;
            } else {
                mark = "~";
                color = 0xFF8EC1F5;
            }
            String title = Component.translatable(q.titleKey()).getString();
            lines.add(new Line(mark + " " + font.plainSubstrByWidth(title, 190), color, false));
            count++;
        }
        if (count == 0) {
            lines.add(new Line(Component.translatable("gui.fateapocrypha.board_world_empty").getString(), 0xFFB5AB92, false));
        }

        pose.pushPose();
        pose.translate(px - cam.x, py - cam.y, pz - cam.z);
        pose.mulPose(Axis.YP.rotationDegrees(-f.toYRot()));
        pose.scale(SCALE, -SCALE, SCALE);
        int rowH = 14;
        int total = lines.size() * rowH;
        float y = -total / 2.0F;
        for (Line line : lines) {
            int w = font.width(line.text);
            float x = line.center ? -w / 2.0F : -100.0F;
            font.drawInBatch(line.text, x, y, line.color, false, pose.last().pose(), buffers,
                    Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            if (line.center) {
                // линия под названием доски
                font.drawInBatch("-------------------", -font.width("-------------------") / 2.0F, y + 7, 0xFF8A7A5A, false,
                        pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
            y += rowH;
        }
        pose.popPose();
    }

    private record Line(String text, int color, boolean center) {
    }
}
