package com.example.fateapocrypha.client;

import com.example.fateapocrypha.client.anim.AnimStyle;
import com.example.fateapocrypha.client.anim.FateModel;
import com.example.fateapocrypha.entity.ServantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Рисует Слугу в виде игрока с заданным скином (64x64, второй слой поддерживается) и собственной анимацией героя. */
public class ServantRenderer<T extends ServantEntity> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private final ResourceLocation texture;
    private final AnimStyle style;

    public ServantRenderer(EntityRendererProvider.Context context, ResourceLocation texture, boolean slimArms, String styleId) {
        super(context, new FateModel<T>(
                context.bakeLayer(slimArms ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slimArms), 0.5F);
        this.texture = texture;
        this.style = AnimStyle.of(styleId);
    }

    @Override
    public void render(T entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        ((FateModel<T>) this.model).style = this.style;
        float drop = FateModel.poseDrop(entity, partialTicks);
        poseStack.pushPose();
        if (drop != 0.0F) {
            poseStack.translate(0.0D, -drop, 0.0D);
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
