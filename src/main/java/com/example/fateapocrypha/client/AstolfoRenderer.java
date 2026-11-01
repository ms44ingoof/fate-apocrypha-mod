package com.example.fateapocrypha.client;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.entity.AstolfoEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class AstolfoRenderer extends HumanoidMobRenderer<AstolfoEntity, HumanoidModel<AstolfoEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(FateApocrypha.MODID, "textures/entity/astolfo.png");

    public AstolfoRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AstolfoEntity entity) {
        return TEXTURE;
    }
}
