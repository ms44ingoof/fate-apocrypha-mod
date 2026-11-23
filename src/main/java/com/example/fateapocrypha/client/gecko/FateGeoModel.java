package com.example.fateapocrypha.client.gecko;

import com.example.fateapocrypha.FateApocrypha;
import com.example.fateapocrypha.client.anim.AnimStyle;
import com.example.fateapocrypha.entity.AstolfoEntity;
import com.example.fateapocrypha.entity.JeanneEntity;
import com.example.fateapocrypha.entity.MordredEntity;
import com.example.fateapocrypha.entity.ServantEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * GeckoLib-модель Слуги: геометрия игрока (обычные или тонкие руки) в geo/servant_*.geo.json, циклы ходьбы, бега и покоя
 * в animations/servant.animation.json, скин из textures/entity/. Жесты, удары и отшатывание считает PoseSolver.
 */
public class FateGeoModel extends GeoModel<ServantEntity> {
    private static final ResourceLocation WIDE = new ResourceLocation(FateApocrypha.MODID, "geo/servant_wide.geo.json");
    private static final ResourceLocation SLIM = new ResourceLocation(FateApocrypha.MODID, "geo/servant_slim.geo.json");
    private static final ResourceLocation ANIM = new ResourceLocation(FateApocrypha.MODID, "animations/servant.animation.json");

    private PoseSolver slimSolver;
    private PoseSolver wideSolver;

    static boolean slim(ServantEntity e) {
        return e instanceof AstolfoEntity || e instanceof JeanneEntity;
    }

    static String style(ServantEntity e) {
        if (e instanceof AstolfoEntity) {
            return "astolfo";
        }
        if (e instanceof JeanneEntity) {
            return "jeanne";
        }
        if (e instanceof MordredEntity) {
            return "mordred";
        }
        return "semiramis";
    }

    @Override
    public ResourceLocation getModelResource(ServantEntity e) {
        return slim(e) ? SLIM : WIDE;
    }

    @Override
    public ResourceLocation getTextureResource(ServantEntity e) {
        return new ResourceLocation(FateApocrypha.MODID, "textures/entity/" + style(e) + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(ServantEntity e) {
        return ANIM;
    }

    private PoseSolver solver(boolean slim) {
        if (slim) {
            if (slimSolver == null) {
                slimSolver = new PoseSolver(true);
            }
            return slimSolver;
        }
        if (wideSolver == null) {
            wideSolver = new PoseSolver(false);
        }
        return wideSolver;
    }

    @Override
    public void setCustomAnimations(ServantEntity e, long instanceId, AnimationState<ServantEntity> state) {
        super.setCustomAnimations(e, instanceId, state);
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        float yaw = data != null ? data.netHeadYaw() : 0.0F;
        float pitch = data != null ? data.headPitch() : 0.0F;
        boolean gesture = !e.getAnim().isEmpty() || e.hurtTime > 0;
        if (gesture) {
            PoseSolver s = solver(slim(e));
            s.solve(e, state.getLimbSwing(), state.getLimbSwingAmount(), e.tickCount + state.getPartialTick(),
                    yaw, pitch, AnimStyle.of(style(e)));
            s.apply(name -> getAnimationProcessor().getBone(name));
        } else {
            CoreGeoBone head = getAnimationProcessor().getBone("head");
            if (head != null) {
                head.setRotX(head.getRotX() + pitch * Mth.DEG_TO_RAD);
                head.setRotY(head.getRotY() + yaw * Mth.DEG_TO_RAD);
            }
        }
    }
}
