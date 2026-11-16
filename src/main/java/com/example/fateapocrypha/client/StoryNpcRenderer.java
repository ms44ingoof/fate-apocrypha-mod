package com.example.fateapocrypha.client;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.client.anim.AnimStyle;
import com.example.fateapocrypha.client.anim.FateModel;
import com.example.fateapocrypha.entity.StoryNpcEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Сюжетный NPC: скин и тип рук зависят от NpcType, манера движения и удары от героя (AnimStyle). */
public class StoryNpcRenderer extends HumanoidMobRenderer<StoryNpcEntity, PlayerModel<StoryNpcEntity>> {
    private final FateModel<StoryNpcEntity> slimModel;
    private final FateModel<StoryNpcEntity> classicModel;

    public StoryNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new FateModel<StoryNpcEntity>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
        this.slimModel = (FateModel<StoryNpcEntity>) this.model;
        this.classicModel = new FateModel<StoryNpcEntity>(context.bakeLayer(ModelLayers.PLAYER), false);
    }

    @Override
    public void render(StoryNpcEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        FateModel<StoryNpcEntity> chosen = entity.getNpcType().slim() ? slimModel : classicModel;
        chosen.style = AnimStyle.of(entity.getNpcType().id());
        this.model = chosen;
        float drop = FateModel.poseDrop(entity, partialTicks);
        poseStack.pushPose();
        if (drop != 0.0F) {
            poseStack.translate(0.0D, -drop, 0.0D);
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(StoryNpcEntity entity) {
        return new ResourceLocation(FateApocrypha.MODID,
                "textures/entity/" + entity.getNpcType().id() + ".png");
    }
}
