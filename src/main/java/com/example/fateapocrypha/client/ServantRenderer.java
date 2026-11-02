package com.example.fateapocrypha.client;

import com.example.fateapocrypha.entity.ServantEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Рисует Слугу в виде игрока с заданным скином (64x64, второй слой поддерживается). */
public class ServantRenderer<T extends ServantEntity> extends HumanoidMobRenderer<T, PlayerModel<T>> {
    private final ResourceLocation texture;

    public ServantRenderer(EntityRendererProvider.Context context, ResourceLocation texture, boolean slimArms) {
        super(context, new PlayerModel<>(
                context.bakeLayer(slimArms ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slimArms), 0.5F);
        this.texture = texture;
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return texture;
    }
}
