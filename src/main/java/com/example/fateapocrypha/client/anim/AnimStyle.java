package com.example.fateapocrypha.client.anim;

import java.util.HashMap;
import java.util.Map;

/** Манера движения персонажа: шаг, раскачка, подпрыгивание, наклон и тип удара. */
public final class AnimStyle {
    public enum Attack { SLASH, THRUST, SMASH, CAST, SHOOT }

    public final float stride;   // частота шага (1 = ванильная примерно 0.67)
    public final float legAmp;
    public final float armAmp;
    public final float sway;     // поворот корпуса при ходьбе
    public final float bounce;   // подпрыгивание
    public final float lean;     // наклон вперёд при ходьбе и беге
    public final float idle;     // сила «дыхания» в покое
    public final Attack attack;

    private static final Map<String, AnimStyle> STYLES = new HashMap<>();
    public static final AnimStyle DEFAULT = new AnimStyle(0.6662F, 1.0F, 1.0F, 0.12F, 0.35F, 0.10F, 1.0F, Attack.SLASH);

    private AnimStyle(float stride, float legAmp, float armAmp, float sway, float bounce, float lean, float idle, Attack attack) {
        this.stride = stride;
        this.legAmp = legAmp;
        this.armAmp = armAmp;
        this.sway = sway;
        this.bounce = bounce;
        this.lean = lean;
        this.idle = idle;
        this.attack = attack;
    }

    private static void put(String id, float stride, float legAmp, float armAmp, float sway, float bounce, float lean,
                            float idle, Attack attack) {
        STYLES.put(id, new AnimStyle(stride, legAmp, armAmp, sway, bounce, lean, idle, attack));
    }

    static {
        put("astolfo", 0.80F, 1.0F, 0.9F, 0.25F, 1.5F, 0.05F, 1.4F, Attack.THRUST);      // прыгучий, лёгкий
        put("jeanne", 0.62F, 0.9F, 0.6F, 0.08F, 0.35F, 0.12F, 0.8F, Attack.SLASH);       // собранная, плавная
        put("mordred", 0.72F, 1.15F, 1.2F, 0.20F, 0.80F, 0.35F, 0.8F, Attack.SLASH);     // напористая
        put("semiramis", 0.50F, 0.8F, 0.5F, 0.10F, 0.30F, 0.0F, 1.3F, Attack.CAST);      // царственная, плывёт
        put("siegfried", 0.55F, 1.1F, 0.9F, 0.10F, 0.30F, 0.20F, 0.6F, Attack.SLASH);    // тяжёлый шаг
        put("achilles", 0.80F, 1.2F, 1.1F, 0.15F, 0.90F, 0.30F, 0.8F, Attack.THRUST);
        put("atalanta", 0.80F, 1.1F, 0.9F, 0.10F, 1.20F, 0.25F, 1.0F, Attack.SHOOT);     // охотница
        put("spartacus", 0.52F, 1.2F, 1.1F, 0.28F, 0.55F, 0.40F, 0.7F, Attack.SMASH);    // грузный
        put("karna", 0.60F, 0.9F, 0.7F, 0.08F, 0.30F, 0.10F, 0.7F, Attack.SLASH);
        put("jack", 0.85F, 1.1F, 0.7F, 0.20F, 1.00F, 0.45F, 1.2F, Attack.THRUST);        // крадущийся, пригнувшийся
        put("frankenstein", 0.50F, 1.0F, 0.8F, 0.30F, 0.30F, 0.30F, 0.6F, Attack.SMASH); // деревянный шаг
        put("chiron", 0.60F, 0.9F, 0.7F, 0.10F, 0.30F, 0.05F, 1.0F, Attack.CAST);
        put("vlad", 0.60F, 1.0F, 0.8F, 0.12F, 0.40F, 0.20F, 0.8F, Attack.SLASH);
        put("fiore", 0.62F, 0.9F, 0.6F, 0.10F, 0.40F, 0.05F, 1.0F, Attack.CAST);
        put("avicebron", 0.55F, 0.8F, 0.6F, 0.12F, 0.30F, 0.15F, 1.0F, Attack.CAST);
        put("celenike", 0.70F, 0.9F, 0.7F, 0.10F, 0.60F, 0.10F, 1.2F, Attack.CAST);
        put("darnic", 0.60F, 1.0F, 0.8F, 0.10F, 0.30F, 0.10F, 0.7F, Attack.SLASH);
    }

    public static AnimStyle of(String id) {
        AnimStyle style = STYLES.get(id);
        return style == null ? DEFAULT : style;
    }
}
