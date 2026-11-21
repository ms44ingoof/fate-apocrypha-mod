package com.example.fateapocrypha.client.anim;

import com.example.fateapocrypha.entity.AnimatedActor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

/**
 * Собственная анимация персонажей мода поверх модели игрока: свой цикл ходьбы и бега (раскачка, подпрыгивание, наклон),
 * «дыхание» в покое, удары четырёх стилей из трёх фаз (замах, удар, возврат) и жесты для сцен: cast, bow, kneel, sit,
 * salute, wave, pray, think, cry, point, dance, charge, laugh, facepalm, shrug, stretch, victory, scared;
 * в покое иногда оглядывается, расправляет плечи, переступает; при ударе отшатывается (flinch). Манера движения задаётся AnimStyle каждого героя.
 */
public class FateModel<T extends Mob> extends PlayerModel<T> {
    public static final float ATTACK_TICKS = 12.0F;

    /** Манера текущего героя (рендерер выставляет её перед отрисовкой). */
    public AnimStyle style = AnimStyle.DEFAULT;

    public FateModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    // ================= математика =================
    private static float smooth(float x) {
        x = Mth.clamp(x, 0.0F, 1.0F);
        return x * x * (3.0F - 2.0F * x);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Плавно ведёт конечность от текущей позы к заданной. */
    private static void pose(ModelPart part, float x, float y, float z, float t) {
        part.xRot = Mth.lerp(t, part.xRot, x);
        part.yRot = Mth.lerp(t, part.yRot, y);
        part.zRot = Mth.lerp(t, part.zRot, z);
    }

    /** На сколько рендерер должен опустить модель (коленопреклонение, сидение). */
    public static float poseDrop(Entity entity, float partialTicks) {
        if (!(entity instanceof AnimatedActor actor)) {
            return 0.0F;
        }
        String name = actor.getAnim();
        if (!name.equals("kneel") && !name.equals("sit")) {
            return 0.0F;
        }
        float t = smooth((entity.tickCount + partialTicks - actor.getAnimStartAge()) / 10.0F);
        return (name.equals("sit") ? 0.62F : 0.36F) * t;
    }

    // ================= главный метод =================
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float age, float netYaw, float pitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, age, netYaw, pitch);
        // эти повороты ванильная модель не сбрасывает сама
        this.body.zRot = 0.0F;
        this.head.zRot = 0.0F;

        AnimStyle st = this.style;
        float amt = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        String action = "";
        float actT = 0.0F;
        if (entity instanceof AnimatedActor actor) {
            action = actor.getAnim();
            actT = age - actor.getAnimStartAge();
        }
        boolean attacking = action.equals("attack") || this.attackTime > 0.0F;

        walk(limbSwing, amt, st);
        if (amt < 0.08F) {
            idle(age + entity.getId() * 53.0F, st);
        }
        if (!action.isEmpty() && !action.equals("attack")) {
            action(action, actT, age);
        }
        if (attacking) {
            float p = action.equals("attack") ? Mth.clamp(actT / ATTACK_TICKS, 0.0F, 0.999F) : this.attackTime;
            attack(p, st);
        }
        if (entity.hurtTime > 0) {
            flinch(entity.hurtTime / (float) Math.max(1, entity.hurtDuration));
        }
        syncOverlays();
    }

    /** Реакция на удар: корпус отшатывается, голова запрокидывается, руки дёргаются вверх и затухают к концу. */
    private void flinch(float h) {
        float k = h * h;
        this.body.xRot -= 0.28F * k;
        this.body.zRot += 0.08F * k;
        this.head.xRot -= 0.35F * k;
        this.rightArm.xRot -= 0.55F * k;
        this.leftArm.xRot -= 0.55F * k;
        this.rightArm.zRot += 0.25F * k;
        this.leftArm.zRot -= 0.25F * k;
    }

    private void syncOverlays() {
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftPants.copyFrom(this.leftLeg);
        this.rightPants.copyFrom(this.rightLeg);
    }

    // ================= ходьба и бег =================
    private void walk(float limbSwing, float a, AnimStyle st) {
        if (a < 0.02F) {
            return;
        }
        float ls = limbSwing * st.stride;
        float run = Mth.clamp((a - 0.6F) / 0.4F, 0.0F, 1.0F);
        this.rightLeg.xRot = Mth.cos(ls) * 1.5F * a * st.legAmp;
        this.leftLeg.xRot = Mth.cos(ls + Mth.PI) * 1.5F * a * st.legAmp;
        float swing = 1.3F * a * st.armAmp * (1.0F + run * 0.5F);
        boolean armed = this.rightArmPose != HumanoidModel.ArmPose.EMPTY;
        float hold = armed ? 0.5F : 1.0F;
        this.rightArm.xRot = Mth.cos(ls + Mth.PI) * swing * hold - (armed ? 0.3F : 0.0F) + run * 0.4F;
        this.leftArm.xRot = Mth.cos(ls) * swing + run * 0.4F;
        this.body.yRot = Mth.cos(ls) * st.sway * a;
        this.head.yRot -= this.body.yRot * 0.6F;
        float lean = st.lean * a * (1.0F + run);
        this.body.xRot += lean * 0.5F;
        this.head.xRot -= lean * 0.35F;
        float bob = Math.abs(Mth.sin(ls)) * st.bounce * a * 1.2F;
        this.body.y -= bob;
        this.head.y -= bob;
        this.rightArm.y -= bob;
        this.leftArm.y -= bob;
    }

    // ================= покой =================
    private void idle(float age, AnimStyle st) {
        float t = age * 0.09F;
        float k = st.idle;
        this.body.xRot += Mth.sin(t) * 0.02F * k;
        this.head.yRot += Mth.sin(age * 0.045F) * 0.07F * k;
        this.head.xRot += Mth.sin(age * 0.07F + 1.0F) * 0.03F * k;
        this.rightArm.zRot += (0.05F + Mth.cos(t) * 0.04F) * k;
        this.leftArm.zRot -= (0.05F + Mth.cos(t + 1.2F) * 0.04F) * k;
        this.body.zRot += Mth.sin(age * 0.03F) * 0.03F * k;   // перенос веса с ноги на ногу
        // редкие жесты в покое: оглядеться по сторонам, расправить плечи, переступить с ноги на ногу
        float cyc = (age % 260.0F) / 260.0F;
        if (cyc > 0.30F && cyc < 0.42F) {
            float look = Mth.sin((cyc - 0.30F) / 0.12F * Mth.PI);
            this.head.yRot += Mth.sin(age * 0.09F) * 0.55F * look * k;
            this.head.xRot -= 0.06F * look;
        } else if (cyc > 0.70F && cyc < 0.78F) {
            float roll = Mth.sin((cyc - 0.70F) / 0.08F * Mth.PI);
            this.rightArm.xRot -= 0.35F * roll * k;
            this.leftArm.xRot -= 0.35F * roll * k;
            this.body.xRot -= 0.05F * roll * k;
            this.head.xRot -= 0.12F * roll * k;
        } else if (cyc > 0.90F && cyc < 0.96F) {
            float shift = Mth.sin((cyc - 0.90F) / 0.06F * Mth.PI);
            this.rightLeg.xRot -= 0.25F * shift;
            this.body.zRot += 0.05F * shift;
        }
    }

    // ================= жесты и действия =================
    private void action(String name, float t, float age) {
        float e = smooth(t / 8.0F);
        switch (name) {
            case "cast" -> {
                float hover = Mth.sin(age * 0.18F) * 0.07F;
                float spread = Mth.sin(age * 0.12F) * 0.06F;
                pose(this.rightArm, -2.85F + hover, 0.0F, -0.35F + spread, e);
                pose(this.leftArm, -2.85F + hover, 0.0F, 0.35F - spread, e);
                this.body.xRot = Mth.lerp(e, this.body.xRot, -0.10F);
                this.head.xRot -= 0.22F * e;
                pose(this.rightLeg, 0.0F, 0.0F, 0.08F, e);
                pose(this.leftLeg, 0.0F, 0.0F, -0.08F, e);
                float lift = e * (0.6F + Mth.sin(age * 0.15F) * 0.5F);   // чуть парит
                this.body.y -= lift;
                this.head.y -= lift;
                this.rightArm.y -= lift;
                this.leftArm.y -= lift;
            }
            case "bow" -> {
                float b = smooth(t / 10.0F);
                this.body.xRot = Mth.lerp(b, this.body.xRot, 0.95F);
                this.head.xRot += 0.2F * b;
                pose(this.rightArm, 0.25F, 0.0F, 0.1F, b);
                pose(this.leftArm, 0.25F, 0.0F, -0.1F, b);
            }
            case "kneel" -> {
                float k = smooth(t / 10.0F);
                this.leftLeg.xRot = Mth.lerp(k, this.leftLeg.xRot, -1.45F);
                this.rightLeg.xRot = Mth.lerp(k, this.rightLeg.xRot, 0.35F);
                this.body.xRot = Mth.lerp(k, this.body.xRot, 0.12F);
                this.head.xRot += 0.15F * k;
                pose(this.rightArm, -0.9F, -0.4F, 0.0F, k);
                pose(this.leftArm, 0.2F, 0.0F, -0.1F, k);
            }
            case "sit" -> {
                float k = smooth(t / 10.0F);
                pose(this.rightLeg, -1.45F, 0.15F, 0.0F, k);
                pose(this.leftLeg, -1.45F, -0.15F, 0.0F, k);
                pose(this.rightArm, -0.5F, -0.2F, 0.1F, k);
                pose(this.leftArm, -0.5F, 0.2F, -0.1F, k);
            }
            case "salute" -> {
                pose(this.rightArm, -2.35F, -0.6F, 0.0F, e);
                this.head.xRot -= 0.1F * e;
            }
            case "wave" -> {
                pose(this.rightArm, -2.8F, 0.0F, -0.15F + Mth.sin(age * 0.55F) * 0.5F, e);
                this.body.zRot += Mth.sin(age * 0.55F) * 0.03F * e;
            }
            case "pray" -> {
                pose(this.rightArm, -1.1F, -0.55F, 0.0F, e);
                pose(this.leftArm, -1.1F, 0.55F, 0.0F, e);
                this.head.xRot += 0.35F * e;
                this.body.xRot = Mth.lerp(e, this.body.xRot, 0.1F);
            }
            case "think" -> {
                pose(this.rightArm, -2.0F, -0.7F, 0.0F, e);
                pose(this.leftArm, -1.0F, 0.9F, 0.0F, e);
                this.head.zRot = 0.15F * e;
                this.head.xRot += 0.1F * e;
            }
            case "cry" -> {
                pose(this.rightArm, -2.1F, -0.9F, 0.0F, e);
                pose(this.leftArm, -2.1F, 0.9F, 0.0F, e);
                this.head.xRot += 0.4F * e;
                this.body.xRot = Mth.lerp(e, this.body.xRot, 0.15F);
                this.body.yRot += Mth.sin(age * 1.2F) * 0.03F * e;
            }
            case "point" -> {
                pose(this.rightArm, -1.57F, -0.1F, 0.0F, e);
                this.body.yRot += 0.15F * e;
            }
            case "dance" -> {
                float d = age * 0.35F;
                pose(this.rightArm, -2.4F + Mth.sin(d) * 0.4F, 0.0F, -0.7F + Mth.cos(d) * 0.3F, e);
                pose(this.leftArm, -2.4F - Mth.sin(d) * 0.4F, 0.0F, 0.7F - Mth.cos(d) * 0.3F, e);
                this.body.yRot += Mth.sin(d * 0.5F) * 0.35F * e;
                this.rightLeg.xRot = Mth.lerp(e, this.rightLeg.xRot, Mth.sin(d) * 0.8F);
                this.leftLeg.xRot = Mth.lerp(e, this.leftLeg.xRot, -Mth.sin(d) * 0.8F);
                float hop = Math.abs(Mth.sin(d)) * 1.5F * e;
                this.body.y -= hop;
                this.head.y -= hop;
                this.rightArm.y -= hop;
                this.leftArm.y -= hop;
            }
            case "laugh" -> {
                float shake = Mth.sin(age * 1.35F) * e;
                this.head.xRot -= (0.38F + Math.abs(shake) * 0.08F) * e;
                this.body.xRot = Mth.lerp(e, this.body.xRot, -0.22F + shake * 0.05F);
                pose(this.rightArm, 0.2F, 0.0F, 0.35F, e);
                pose(this.leftArm, -0.9F, 0.0F, -0.15F, e);
                this.body.y += shake * 0.15F;
                this.head.y += shake * 0.15F;
            }
            case "facepalm" -> {
                pose(this.rightArm, -2.45F, -0.35F, 0.0F, e);
                this.head.xRot += 0.3F * e;
                this.head.yRot += 0.1F * e;
                this.body.xRot = Mth.lerp(e, this.body.xRot, 0.12F);
            }
            case "shrug" -> {
                float up = Mth.sin(Math.min(1.0F, t / 10.0F) * Mth.PI) * 0.6F + e * 0.4F;
                pose(this.rightArm, -0.9F, 0.0F, 0.9F + up * 0.1F, e);
                pose(this.leftArm, -0.9F, 0.0F, -0.9F - up * 0.1F, e);
                this.head.zRot = 0.18F * e;
                this.body.y += 0.15F * up;
            }
            case "stretch" -> {
                pose(this.rightArm, -3.05F, 0.0F, -0.25F, e);
                pose(this.leftArm, -3.05F, 0.0F, 0.25F, e);
                this.body.xRot = Mth.lerp(e, this.body.xRot, -0.2F);
                this.head.xRot -= 0.25F * e;
            }
            case "victory" -> {
                float pump = Mth.sin(age * 0.5F) * 0.12F * e;
                pose(this.rightArm, -2.9F + pump, 0.0F, -0.2F, e);
                this.head.xRot -= 0.18F * e;
                this.body.xRot = Mth.lerp(e, this.body.xRot, -0.08F);
                float hop = Math.abs(Mth.sin(age * 0.5F)) * 0.9F * e;
                this.body.y -= hop;
                this.head.y -= hop;
                this.rightArm.y -= hop;
                this.leftArm.y -= hop;
            }
            case "scared" -> {
                pose(this.rightArm, -1.6F, 0.5F, 0.0F, e);
                pose(this.leftArm, -1.6F, -0.5F, 0.0F, e);
                this.body.xRot = Mth.lerp(e, this.body.xRot, -0.18F);
                this.head.xRot -= 0.15F * e;
                this.body.zRot += Mth.sin(age * 2.2F) * 0.03F * e;
            }
            case "charge" -> {
                this.body.xRot = Mth.lerp(e, this.body.xRot, 0.55F);
                this.head.xRot -= 0.35F * e;
                pose(this.rightArm, 0.9F, 0.0F, -0.1F, e);
                pose(this.leftArm, 0.9F, 0.0F, 0.1F, e);
            }
            default -> {
            }
        }
    }

    // ================= удары =================
    private void attack(float p, AnimStyle st) {
        switch (st.attack) {
            case THRUST -> thrust(p);
            case SMASH -> smash(p);
            case CAST -> castAttack(p);
            case SHOOT -> shoot(p);
            default -> slash(p);
        }
    }

    /** Меч: замах над головой с поворотом корпуса, рубящий удар со шагом вперёд, возврат. */
    private void slash(float p) {
        float armX;
        float armZ;
        float twist;
        float step;
        float lean;
        if (p < 0.32F) {
            float t = smooth(p / 0.32F);
            armX = lerp(-0.35F, -2.75F, t);
            armZ = lerp(0.0F, -0.45F, t);
            twist = lerp(0.0F, 0.55F, t);
            step = lerp(0.0F, 0.45F, t);
            lean = lerp(0.0F, -0.12F, t);
        } else if (p < 0.62F) {
            float t = (p - 0.32F) / 0.30F;
            t = t * t;
            armX = lerp(-2.75F, -0.1F, t);
            armZ = lerp(-0.45F, 0.55F, t);
            twist = lerp(0.55F, -0.6F, t);
            step = lerp(0.45F, -0.55F, t);
            lean = lerp(-0.12F, 0.38F, t);
        } else {
            float t = smooth((p - 0.62F) / 0.38F);
            armX = lerp(-0.1F, 0.0F, t);
            armZ = lerp(0.55F, 0.0F, t);
            twist = lerp(-0.6F, 0.0F, t);
            step = lerp(-0.55F, 0.0F, t);
            lean = lerp(0.38F, 0.0F, t);
        }
        this.rightArm.xRot = armX;
        this.rightArm.yRot = twist * 0.3F;
        this.rightArm.zRot = armZ;
        this.leftArm.xRot = -armX * 0.22F;
        this.leftArm.zRot = 0.25F;
        this.body.yRot = twist;
        this.head.yRot -= twist * 0.7F;
        this.body.xRot = lean;
        this.head.xRot -= lean * 0.3F;
        this.rightLeg.xRot += step * 0.8F;
        this.leftLeg.xRot -= step;
    }

    /** Копьё и рапира: рука отводится, затем выпад с глубоким шагом. */
    private void thrust(float p) {
        float armX;
        float armY;
        float twist;
        float lean;
        float step;
        if (p < 0.30F) {
            float t = smooth(p / 0.30F);
            armX = lerp(-0.9F, -1.3F, t);
            armY = lerp(0.0F, -0.55F, t);
            twist = lerp(0.0F, 0.45F, t);
            lean = lerp(0.0F, -0.15F, t);
            step = lerp(0.0F, 0.35F, t);
        } else if (p < 0.55F) {
            float t = (p - 0.30F) / 0.25F;
            t = t * t;
            armX = lerp(-1.3F, -1.57F, t);
            armY = lerp(-0.55F, 0.15F, t);
            twist = lerp(0.45F, -0.3F, t);
            lean = lerp(-0.15F, 0.45F, t);
            step = lerp(0.35F, -0.9F, t);
        } else {
            float t = smooth((p - 0.55F) / 0.45F);
            armX = lerp(-1.57F, 0.0F, t);
            armY = lerp(0.15F, 0.0F, t);
            twist = lerp(-0.3F, 0.0F, t);
            lean = lerp(0.45F, 0.0F, t);
            step = lerp(-0.9F, 0.0F, t);
        }
        this.rightArm.xRot = armX;
        this.rightArm.yRot = armY;
        this.rightArm.zRot = 0.0F;
        this.leftArm.xRot = -0.4F;
        this.leftArm.zRot = 0.35F;
        this.body.yRot = twist;
        this.head.yRot -= twist * 0.7F;
        this.body.xRot = lean;
        this.rightLeg.xRot += step * 0.8F;
        this.leftLeg.xRot -= step;
    }

    /** Двуручный удар сверху: оба «оружия» над головой и тяжёлый наклон вперёд. */
    private void smash(float p) {
        float armX;
        float lean;
        if (p < 0.40F) {
            float t = smooth(p / 0.40F);
            armX = lerp(-0.3F, -3.0F, t);
            lean = lerp(0.0F, -0.25F, t);
        } else if (p < 0.65F) {
            float t = (p - 0.40F) / 0.25F;
            t = t * t * t;
            armX = lerp(-3.0F, -0.35F, t);
            lean = lerp(-0.25F, 0.7F, t);
        } else {
            float t = smooth((p - 0.65F) / 0.35F);
            armX = lerp(-0.35F, 0.0F, t);
            lean = lerp(0.7F, 0.0F, t);
        }
        this.rightArm.xRot = armX;
        this.leftArm.xRot = armX;
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.rightArm.zRot = -0.12F;
        this.leftArm.zRot = 0.12F;
        this.body.xRot = lean;
        this.head.xRot -= lean * 0.5F;
        this.rightLeg.xRot += lean * 0.6F;
        this.leftLeg.xRot -= lean * 0.9F;
    }

    /** Магический выпад: руки взлетают в стороны, затем толчок вперёд. */
    private void castAttack(float p) {
        float armX;
        float armZ;
        float lean;
        if (p < 0.35F) {
            float t = smooth(p / 0.35F);
            armX = lerp(-0.2F, -2.2F, t);
            armZ = lerp(0.0F, 0.5F, t);
            lean = lerp(0.0F, -0.1F, t);
        } else if (p < 0.70F) {
            float t = smooth((p - 0.35F) / 0.35F);
            armX = lerp(-2.2F, -1.5F, t);
            armZ = lerp(0.5F, 0.12F, t);
            lean = lerp(-0.1F, 0.2F, t);
        } else {
            float t = smooth((p - 0.70F) / 0.30F);
            armX = lerp(-1.5F, 0.0F, t);
            armZ = lerp(0.12F, 0.0F, t);
            lean = lerp(0.2F, 0.0F, t);
        }
        this.rightArm.xRot = armX;
        this.leftArm.xRot = armX;
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;
        this.rightArm.zRot = -armZ;
        this.leftArm.zRot = armZ;
        this.body.xRot = lean;
    }

    /** Лук: левая рука натягивает тетиву, затем резкий спуск. */
    private void shoot(float p) {
        float draw = p < 0.60F ? smooth(p / 0.60F) : Mth.clamp(1.0F - (p - 0.60F) / 0.15F, 0.0F, 1.0F);
        float f = 1.0F - smooth((p - 0.85F) / 0.15F);
        pose(this.rightArm, -1.5F, -0.1F, 0.0F, f);
        pose(this.leftArm, -1.5F, lerp(0.4F, 1.3F, draw), 0.0F, f);
        this.body.yRot = Mth.lerp(f, this.body.yRot, -0.3F);
        this.head.yRot -= -0.3F * f;
    }
}
