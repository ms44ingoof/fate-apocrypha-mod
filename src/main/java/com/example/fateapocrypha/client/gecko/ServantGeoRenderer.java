package com.example.fateapocrypha.client.gecko;

import com.example.fateapocrypha.client.anim.FateModel;
import com.example.fateapocrypha.entity.ServantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/** Рисует Слугу через GeckoLib; оружие из руки показывается слоем на пустой кости right_item. */
public class ServantGeoRenderer extends GeoEntityRenderer<ServantEntity> {
    public ServantGeoRenderer(EntityRendererProvider.Context context) {
        super(context, new FateGeoModel());
        this.shadowRadius = 0.5F;
        addRenderLayer(new BlockAndItemGeoLayer<ServantEntity>(this) {
            @Override
            protected ItemStack getStackForBone(GeoBone bone, ServantEntity animatable) {
                return "right_item".equals(bone.getName()) ? animatable.getMainHandItem() : null;
            }

            @Override
            protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, ServantEntity animatable) {
                return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            }

            @Override
            protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, ServantEntity animatable,
                                              MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
                // та же посадка предмета в руку, что у обычных гуманоидов (ItemInHandLayer)
                poseStack.pushPose();
                poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.translate(1.0F / 16.0F, 0.125F, -0.625F);
                Minecraft.getInstance().getItemRenderer().renderStatic(animatable, stack,
                        getTransformTypeForStack(bone, stack, animatable), false, poseStack, bufferSource,
                        animatable.level(), packedLight, packedOverlay, animatable.getId());
                poseStack.popPose();
            }
        });
    }

    @Override
    public void render(ServantEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float drop = FateModel.poseDrop(entity, partialTick);   // сесть или преклонить колено: тело опускается
        poseStack.pushPose();
        if (drop != 0.0F) {
            poseStack.translate(0.0D, -drop, 0.0D);
        }
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
