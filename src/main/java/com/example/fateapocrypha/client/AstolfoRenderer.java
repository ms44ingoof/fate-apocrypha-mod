package com.example.fateapocrypha.client;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.entity.AstolfoEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Скин Астольфо в формате Alex (тонкие руки) со вторым слоем (волосы, куртка, рукава). */
public class AstolfoRenderer extends HumanoidMobRenderer<AstolfoEntity, PlayerModel<AstolfoEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FateApocrypha.MODID, "textures/entity/astolfo.png");

    public AstolfoRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AstolfoEntity entity) {
        return TEXTURE;
    }
}
