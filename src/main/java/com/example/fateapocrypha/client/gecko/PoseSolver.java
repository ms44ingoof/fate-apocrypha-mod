package com.example.fateapocrypha.client.gecko;

import com.example.fateapocrypha.client.anim.AnimStyle;
import com.example.fateapocrypha.client.anim.FateModel;
import com.example.fateapocrypha.entity.ServantEntity;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

/**
 * Мост между процедурной анимацией героев (FateModel: удары трёх фаз, жесты сцен, реакция на удар) и GeckoLib.
 * FateModel считает позу как обычно (на невидимой модели игрока), а результат переносится в кости GeckoLib-модели.
 * Так все прежние жесты и атаки работают поверх GeckoLib без потерь, а циклы ходьбы и покоя берутся из JSON-анимаций.
 */
final class PoseSolver {
    static final String[] BONES = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"};

    private final FateModel<ServantEntity> model;
    private final ModelPart[] parts;
    private final float[] initY;

    PoseSolver(boolean slim) {
        ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER);
        this.model = new FateModel<>(root, slim);
        this.parts = new ModelPart[] {model.head, model.body, model.rightArm, model.leftArm, model.rightLeg, model.leftLeg};
        this.initY = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            initY[i] = parts[i].y;
        }
    }

    void solve(ServantEntity e, float limbSwing, float limbAmount, float age, float netHeadYaw, float headPitch, AnimStyle style) {
        for (ModelPart p : parts) {
            p.resetPose();
        }
        model.style = style;
        model.rightArmPose = e.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        model.attackTime = 0.0F;
        model.setupAnim(e, limbSwing, limbAmount, age, netHeadYaw, headPitch);
    }

    /** Переносит позу в кости: повороты как есть (знаки совпадают с головой в примере GeckoLib), вертикальное смещение инвертируется. */
    void apply(Function<String, CoreGeoBone> bones) {
        for (int i = 0; i < parts.length; i++) {
            CoreGeoBone b = bones.apply(BONES[i]);
            if (b == null) {
                continue;
            }
            ModelPart p = parts[i];
            b.setRotX(p.xRot);
            b.setRotY(p.yRot);
            b.setRotZ(p.zRot);
            b.setPosY(-(p.y - initY[i]));
        }
    }
}
