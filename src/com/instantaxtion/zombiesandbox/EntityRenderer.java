package com.instantaxtion.zombiesandbox;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.GameView.*;

/**
 * ((27.3) Out of GameView.) Draws people, the dead, dogs and horses on the map, the bodies they leave, and the portrait
 * badges in the tool bar and on the cards.
 */
final class EntityRenderer {
    final GameView gv;

    EntityRenderer(GameView gv) {
        this.gv = gv;
    }


    /** ((25.0) A giant's head: small, sunk between the shoulders, a heavy brow, the jaw hanging open, pale eyes. */
    void drawGiantHead(Canvas c, Entity e, float r) {
        gv.fill.setColor(City.darken(e.skin, 0.85f));
        c.drawCircle(r * 0.28f, 0, r * 0.42f, gv.fill);
        gv.fill.setColor(City.darken(e.skin, 0.6f));
        gv.oval.set(r * 0.4f, -r * 0.36f, r * 0.6f, r * 0.36f);
        c.drawOval(gv.oval, gv.fill);
        gv.fill.setColor(0xFF2A0A0A);
        gv.oval.set(r * 0.55f, -r * 0.15f, r * 0.8f, r * 0.15f);
        c.drawOval(gv.oval, gv.fill);
        gv.fill.setColor(0xFFF0E08A);
        c.drawCircle(r * 0.52f, -r * 0.17f, r * 0.06f, gv.fill);
        c.drawCircle(r * 0.52f, r * 0.17f, r * 0.06f, gv.fill);
    }

    /** A phone above someone calling 911, or a radio bubble above a unit that just spoke. */
    void drawSpeechIcon(Canvas c, Entity e) {
        float x = e.x + e.radius + 2, y = e.y - e.radius - 7;
        if (e.phoneTimer > 0) {
            gv.fill.setColor(0xFF1B1B1B);
            gv.oval.set(x - 2.2f, y - 3.6f, x + 2.2f, y + 3.6f);
            c.drawRoundRect(gv.oval, 0.8f, 0.8f, gv.fill);
            gv.fill.setColor((int) (gv.world.time * 4) % 2 == 0 ? 0xFF7FE0FF : 0xFFFFFFFF);
            c.drawRect(x - 1.5f, y - 2.6f, x + 1.5f, y + 1.8f, gv.fill);
        } else {
            int col = e.type == Entity.SOLDIER ? 0xFFA6DC72 : 0xFF7FB0FF;
            gv.fill.setColor(0xEEFFFFFF);
            gv.oval.set(x - 4.5f, y - 3f, x + 4.5f, y + 3f);
            c.drawRoundRect(gv.oval, 2f, 2f, gv.fill);
            c.drawCircle(x - 2.5f, y + 3.5f, 1f, gv.fill);
            gv.fill.setColor(col);
            for (int k = -1; k <= 1; k++) c.drawCircle(x + k * 2.2f, y, 0.8f, gv.fill);
        }
    }

    void drawDog(Canvas c, Entity e, boolean healthBar) {
        float r = e.radius;
        c.save();
        c.translate(e.x, e.y);
        c.rotate((float) Math.toDegrees(e.angle));
        gv.fill.setColor(0x44000000);
        gv.oval.set(-r * 1.1f + 0.8f, -r * 0.6f + 0.8f, r * 1.2f + 0.8f, r * 0.6f + 0.8f);
        c.drawOval(gv.oval, gv.fill);
        float sw = (float) Math.sin(e.phase * 1.6f) * r * 0.4f;
        gv.stroke.setColor(City.darken(e.body, 0.7f));
        gv.stroke.setStrokeWidth(r * 0.3f);
        c.drawLine(r * 0.55f, -r * 0.4f, r * 0.55f + sw, -r * 0.72f, gv.stroke);
        c.drawLine(r * 0.55f, r * 0.4f, r * 0.55f - sw, r * 0.72f, gv.stroke);
        c.drawLine(-r * 0.6f, -r * 0.4f, -r * 0.6f - sw, -r * 0.72f, gv.stroke);
        c.drawLine(-r * 0.6f, r * 0.4f, -r * 0.6f + sw, r * 0.72f, gv.stroke);
        // Wagging tail.
        gv.stroke.setColor(e.body);
        gv.stroke.setStrokeWidth(r * 0.25f);
        float wag = (float) Math.sin(gv.world.time * (e.fleeTimer > 0 ? 6 : 14) + e.phase) * r * 0.45f;
        c.drawLine(-r * 0.9f, 0, -r * 1.6f, wag, gv.stroke);
        gv.fill.setColor(e.body);
        gv.oval.set(-r * 1.05f, -r * 0.5f, r * 0.85f, r * 0.5f);
        c.drawOval(gv.oval, gv.fill);
        if (e.role == Entity.ROLE_K9) {
            // Police dog vest.
            gv.fill.setColor(0xFF1E2E5A);
            gv.oval.set(-r * 0.6f, -r * 0.52f, r * 0.5f, r * 0.52f);
            c.drawRoundRect(gv.oval, r * 0.2f, r * 0.2f, gv.fill);
            gv.fill.setColor(0xFFE8E8E8);
            c.drawRect(-r * 0.3f, -r * 0.1f, r * 0.2f, r * 0.1f, gv.fill);
        }
        gv.fill.setColor(e.head);
        c.drawCircle(r * 1.05f, 0, r * 0.48f, gv.fill);
        gv.fill.setColor(City.darken(e.head, 0.7f));
        c.drawCircle(r * 0.9f, -r * 0.38f, r * 0.2f, gv.fill);
        c.drawCircle(r * 0.9f, r * 0.38f, r * 0.2f, gv.fill);
        gv.fill.setColor(0xFF1A1A1A);
        c.drawCircle(r * 1.5f, 0, r * 0.14f, gv.fill);
        if (e.isUndead()) {
            // Red eyes and a bloody muzzle.
            gv.fill.setColor(0xFFFF3B2F);
            c.drawCircle(r * 1.2f, -r * 0.2f, r * 0.1f, gv.fill);
            c.drawCircle(r * 1.2f, r * 0.2f, r * 0.1f, gv.fill);
            gv.fill.setColor(0xAA6E0A0A);
            c.drawCircle(r * 1.45f, 0, r * 0.22f, gv.fill);
        }
        if (e.hurt > 0) {
            gv.fill.setColor(alpha(0xFFFFFFFF, e.hurt * 0.7f));
            c.drawCircle(0, 0, r * 1.1f, gv.fill);
        }
        c.restore();
        if (healthBar && e.hp < e.maxHp && e.hp > 0) {
            float bw = 8, bx = e.x - bw / 2, by = e.y - r - 3.5f;
            gv.fill.setColor(0xAA000000);
            c.drawRect(bx - 0.4f, by - 0.4f, bx + bw + 0.4f, by + 1.9f, gv.fill);
            float f = e.hp / e.maxHp;
            gv.fill.setColor(f > 0.5f ? 0xFF4CD964 : f > 0.25f ? 0xFFFFCC00 : 0xFFFF3B30);
            c.drawRect(bx, by, bx + bw * f, by + 1.5f, gv.fill);
        }
    }

    /** Hair, caps and beanies, picked from the person's name so they keep the same look. */
    void drawHair(Canvas c, Entity e, float r) {
        int seed = e.nameSeed & 0x7FFFFFFF;
        int style = seed % 7, hair = HAIR[(seed / 7) % HAIR.length], hat = HATS[(seed / 49) % HATS.length];
        boolean zombie = e.isUndead();
        if (zombie) {
            if (style == 0 || style == 3) style = 4; // Hats are long gone.
            hair = City.darken(hair, 0.8f);
        }
        switch (style) {
            case 0: // Baseball cap.
                gv.fill.setColor(hat);
                c.drawCircle(-r * 0.04f, 0, r * 0.5f, gv.fill);
                gv.fill.setColor(City.darken(hat, 0.75f));
                gv.oval.set(r * 0.3f, -r * 0.3f, r * 0.8f, r * 0.3f);
                c.drawOval(gv.oval, gv.fill);
                break;
            case 1: // Long hair.
                gv.fill.setColor(hair);
                gv.oval.set(-r * 0.62f, -r * 0.54f, r * 0.12f, r * 0.54f);
                c.drawOval(gv.oval, gv.fill);
                break;
            case 2: // Bald.
                break;
            case 3: // Beanie.
                gv.fill.setColor(hat);
                c.drawCircle(-r * 0.06f, 0, r * 0.5f, gv.fill);
                gv.fill.setColor(City.lighten(hat, 0.3f));
                c.drawCircle(-r * 0.12f, 0, r * 0.14f, gv.fill);
                break;
            case 5: // Hair in a bun.
                gv.fill.setColor(hair);
                c.drawCircle(-r * 0.1f, 0, r * 0.44f, gv.fill);
                c.drawCircle(-r * 0.58f, 0, r * 0.2f, gv.fill);
                break;
            default: // Short hair.
                gv.fill.setColor(hair);
                c.drawCircle(-r * 0.12f, 0, r * 0.44f, gv.fill);
                if (zombie) {
                    gv.fill.setColor(e.head);
                    c.drawCircle(-r * 0.2f, r * 0.2f, r * 0.14f, gv.fill);
                }
                break;
        }
    }

    void drawEntity(Canvas c, Entity e, boolean healthBar) {
        if (e.type == Entity.CIVILIAN && gv.world.onTowel(e)) {
            // ((11.1) A beach towel on the sand, and now and then a parasol.)
            int[] towels = {0xFFE0505A, 0xFF3A8AD8, 0xFFF0C040, 0xFF40B080, 0xFFE07AB0};
            float tx = e.spot[0], ty = e.spot[1];
            gv.fill.setColor(towels[e.nameSeed % towels.length & 0x7FFFFFFF]);
            c.drawRect(tx - 3.5f, ty - 6.5f, tx + 3.5f, ty + 6.5f, gv.fill);
            gv.fill.setColor(0xCCF4F4F0);
            c.drawRect(tx - 3.5f, ty - 6.5f, tx + 3.5f, ty - 5.2f, gv.fill);
            c.drawRect(tx - 3.5f, ty + 5.2f, tx + 3.5f, ty + 6.5f, gv.fill);
            if ((e.nameSeed & 3) == 0) {
                gv.fill.setColor(0x30000000);
                c.drawCircle(tx + 7, ty - 1, 7, gv.fill);
                gv.fill.setColor((e.nameSeed & 4) == 0 ? 0xFFE8463A : 0xFF2E7DC8);
                c.drawCircle(tx + 6, ty - 3, 6.5f, gv.fill);
                gv.fill.setColor(0xFFF4F4F0);
                c.drawCircle(tx + 6, ty - 3, 3.2f, gv.fill);
            }
        }
        if (e.type == Entity.CIVILIAN && gv.world.atStall(e)) {
            // The food cart: a striped umbrella over a little stall.
            float cx = e.x + 7, cy = e.y - 2;
            gv.fill.setColor(0x40000000);
            c.drawRect(cx - 3.5f, cy - 2, cx + 5, cy + 5, gv.fill);
            gv.fill.setColor(0xFFB8BCC0);
            c.drawRect(cx - 4, cy - 2.5f, cx + 4, cy + 3.5f, gv.fill);
            gv.fill.setColor(0xFF2A2A2A);
            c.drawCircle(cx - 3, cy + 3.8f, 1, gv.fill);
            c.drawCircle(cx + 3, cy + 3.8f, 1, gv.fill);
            gv.fill.setColor(0xFFE04A3A);
            c.drawCircle(cx, cy - 1, 5.5f, gv.fill);
            gv.fill.setColor(0xFFF2F2F2);
            c.drawCircle(cx, cy - 1, 3.6f, gv.fill);
            gv.fill.setColor(0xFFE04A3A);
            c.drawCircle(cx, cy - 1, 1.8f, gv.fill);
        }
        if (e.type == Entity.DOG || e.type == Entity.ZOMBIE_DOG) {
            drawDog(c, e, healthBar);
            return;
        }
        // ((15.0) A mounted officer: the horse under them.)
        if (e.type == Entity.COP && e.role == Entity.ROLE_MOUNTED) drawHorse(c, e);
        // ((25.0) A giant is drawn far bigger than it stands in the street: two and a half times a brute.)
        float r = e.type == Entity.GIANT ? e.radius * GIANT_DRAW : e.radius;
        // ((11.5) Small on screen - a dozen pixels across - just the body, the head and any gun drawn (see below), and
        // no separate shadow: the dark outline does that job at this size.)
        boolean small = gv.scale < 1.8f && !healthBar && e != gv.world.controlled;
        // One light source for everything: shadows fall down and to the right.
        if (small) {
            // (None.)
        } else if (gv.realistic && gv.scale > 3f) {
            // A soft shadow: a pale outer edge round a darker core.
            gv.fill.setColor(0x1C000000);
            c.drawCircle(e.x + r * 0.4f, e.y + r * 0.55f, r * 1.2f, gv.fill);
            gv.fill.setColor(0x30000000);
            c.drawCircle(e.x + r * 0.35f, e.y + r * 0.5f, r * 0.85f, gv.fill);
        } else {
            gv.fill.setColor(0x40000000);
            c.drawCircle(e.x + r * 0.35f, e.y + r * 0.5f, r * 0.95f, gv.fill);
        }
        c.save();
        c.translate(e.x, e.y);
        c.rotate((float) Math.toDegrees(e.angle));
        float now = gv.world.time, spd = (float) Math.sqrt(e.vx * e.vx + e.vy * e.vy);
        // ((22.0) Moving, stepping and swinging from further out: they read as people, not dots, at the usual zoom.)
        boolean anim = gv.scale > 1.3f;
        if (anim) {
            // Attacks: the dead lunge as they bite, guns kick back.
            float bite = 1 - Math.min(1, (now - e.biteAt) / 0.25f);
            if (bite > 0 && e.isUndead()) c.translate(r * 0.5f * bite, 0);
            float kick = 1 - Math.min(1, (now - e.shotAt) / 0.12f);
            if (kick > 0 && !e.isUndead()) c.translate(-r * 0.2f * kick, 0);
            if (spd > 4) {
                // Walking: a bob in the stride; the dead lurch from side to side.
                float bob = (float) Math.sin(e.phase * 2);
                if (e.isUndead() && e.type != Entity.RUNNER) c.rotate((float) Math.sin(e.phase * 0.5f + (e.nameSeed & 7)) * 9);
                c.scale(1 + 0.05f * bob, 1 - 0.03f * bob);
            }
            if (e.isUndead() && e.fresh > 44) {
                // Just got up: still unfolding.
                float t = Math.max(0, 45 - e.fresh);
                c.scale(0.7f + 0.3f * t, 0.7f + 0.3f * t);
            }
        }

        // ((11.5) The arms, legs, hats, badges and holsters were a dozen drawing calls each for a crowd of hundreds.)
        if (small) {
            drawBodyDot(c, e, r);
        } else {
            drawBody(c, e, r, now, spd, anim);
        }
        drawStatusMarks(c, e, healthBar, r, spd, anim);
    }

    /** ((27.3) From drawEntity.) Zoomed out: the body as a plain oval with its head. */
    void drawBodyDot(Canvas c, Entity e, float r) {
        gv.oval.set(-r * 0.62f, -r, r * 0.62f, r);
        if (gv.scale > 1.2f) {
            gv.fill.setColor(0x8C000000);
            float o = Math.min(r * 0.18f, 0.9f / gv.scale + 0.25f);
            gv.oval.inset(-o, -o);
            c.drawOval(gv.oval, gv.fill);
            gv.oval.inset(o, o);
        }
        gv.fill.setColor(e.body);
        c.drawOval(gv.oval, gv.fill);
        if ((e.isArmed() && gv.world.weaponsOut(e)) || (e.hasGun && e.aiming)) {
            gv.stroke.setColor(0xFF161616);
            gv.stroke.setStrokeWidth(r * 0.38f);
            c.drawLine(r * 0.5f, r * 0.3f, e.type == Entity.SOLDIER ? r * 2.5f : r * 2f, r * 0.3f, gv.stroke);
        } else if (e.isUndead()) {
            // (Arms out in front.)
            gv.stroke.setColor(e.skin);
            gv.stroke.setStrokeWidth(r * 1.3f);
            c.drawLine(r * 0.2f, 0, r * 1.4f, 0, gv.stroke);
        }
        gv.fill.setColor(e.head);
        c.drawCircle(r * 0.08f, 0, r * 0.56f, gv.fill);
    }

    /** ((27.3) From drawEntity.) Close up: legs, body, arms and what they carry, and the head, by kind of person or zombie. */
    void drawBody(Canvas c, Entity e, float r, float now, float spd, boolean anim) {
        if (gv.scale > 1.9f) {
            // Legs stepping out in front and behind as they walk.
            float step = (float) Math.sin(e.phase) * r * 0.55f;
            gv.fill.setColor(e.isUndead() ? City.darken(e.skin, 0.55f) : e.type == Entity.SOLDIER ? 0xFF2E3320 : 0xFF26282C);
            gv.oval.set(step - r * 0.3f, -r * 0.55f, step + r * 0.3f, -r * 0.12f);
            c.drawOval(gv.oval, gv.fill);
            gv.oval.set(-step - r * 0.3f, r * 0.12f, -step + r * 0.3f, r * 0.55f);
            c.drawOval(gv.oval, gv.fill);
        }

        drawArms(c, e, r, now, spd, anim);

        if (anim && !e.isUndead() && now - e.swingAt < 0.3f) {
            // A swing: the arm (and bat, axe or rifle butt) sweeps across in front.
            float t = (now - e.swingAt) / 0.3f;
            double a = Math.toRadians(-70 + 120 * t);
            float hx = (float) Math.cos(a) * r * 1.6f, hy = (float) Math.sin(a) * r * 1.6f;
            gv.stroke.setColor(e.skin);
            gv.stroke.setStrokeWidth(r * 0.34f);
            c.drawLine(0, r * 0.5f, hx * 0.6f, hy * 0.6f + r * 0.2f, gv.stroke);
            gv.stroke.setColor(e.melee == Entity.M_AXE ? 0xFF8A2A1E : e.melee == Entity.M_BAT ? 0xFF9A7448
                    : e.melee == Entity.M_MACHETE ? 0xFFC8CCD0 : e.melee == Entity.M_CROWBAR ? 0xFF7A1E1A : 0xFF2A2A2A);
            gv.stroke.setStrokeWidth(r * 0.28f);
            c.drawLine(hx * 0.55f, hy * 0.55f + r * 0.2f, hx * 1.2f, hy * 1.2f + r * 0.2f, gv.stroke);
            gv.stroke.setColor(0x55FFFFFF);
            gv.stroke.setStrokeWidth(r * 0.12f);
            gv.oval.set(-r * 1.9f, -r * 1.9f, r * 1.9f, r * 1.9f);
            gv.stroke.setStyle(Paint.Style.STROKE);
            c.drawArc(gv.oval, -70, 120 * t, false, gv.stroke);
        }

        gv.oval.set(-r * 0.62f, -r, r * 0.62f, r);
        if (gv.scale > 1.2f) {
            // ((11.0) A dark outline, so everyone stands out from the ground at any zoom.)
            gv.fill.setColor(0x8C000000);
            float o = Math.min(r * 0.18f, 0.9f / gv.scale + 0.25f);
            gv.oval.inset(-o, -o);
            c.drawOval(gv.oval, gv.fill);
            gv.oval.inset(o, o);
        }
        gv.fill.setColor(e.body);
        c.drawOval(gv.oval, gv.fill);
        if (gv.scale > 2.2f) {
            // ((11.0) And the light catching the shoulders, from the north-west like everything else.)
            gv.fill.setColor(0x2EFFFFFF);
            gv.oval.set(-r * 0.5f, -r * 0.85f, r * 0.25f, r * 0.1f);
            c.drawOval(gv.oval, gv.fill);
        }
        if (e.armored) {
            // (10.32) The dead in body armour: the vest over the torso, dark and torn.
            gv.fill.setColor(0xFF2A3038);
            c.drawRect(-r * 0.5f, -r * 0.78f, r * 0.42f, r * 0.78f, gv.fill);
            gv.fill.setColor(0xFF3E4652);
            c.drawRect(-r * 0.36f, -r * 0.6f, r * 0.28f, r * 0.6f, gv.fill);
        }
        if (e.isArmed() && !gv.world.weaponsOut(e) && !e.isUndead()) {
            // (10.24) No emergency: the rifle slung across the back, the pistol in its holster.
            gv.fill.setColor(0xFF161616);
            if (e.type == Entity.SOLDIER) {
                gv.stroke.setColor(0xFF161616);
                gv.stroke.setStrokeWidth(r * 0.3f);
                c.drawLine(-r * 0.7f, -r * 0.9f, -r * 0.3f, r * 1.0f, gv.stroke);
            } else c.drawRect(-r * 0.15f, r * 0.6f, r * 0.35f, r * 0.85f, gv.fill);
        }
        if (e.hasGun && !e.aiming) {
            // A gun tucked in a belt.
            gv.fill.setColor(0xFF1A1A1A);
            c.drawRect(-r * 0.2f, r * 0.55f, r * 0.45f, r * 0.8f, gv.fill);
        }
        drawSoldierKit(c, e, r);

        gv.fill.setColor(e.head);
        if (e.type == Entity.GIANT) drawGiantHead(c, e, r);
        else c.drawCircle(r * 0.08f, 0, r * 0.56f, gv.fill);
        if (e.type == Entity.ARMOURED && e.helmet) {
            // ((27.0) The helmet: a pale rim round it and a dull shine on top.)
            gv.stroke.setColor(City.darken(e.head, 0.6f));
            gv.stroke.setStrokeWidth(r * 0.12f);
            c.drawCircle(r * 0.08f, 0, r * 0.6f, gv.stroke);
            gv.fill.setColor(0x50FFFFFF);
            c.drawCircle(-r * 0.05f, -r * 0.16f, r * 0.14f, gv.fill);
        }
        if (gv.scale > 1.9f && (e.type == Entity.CIVILIAN || e.type == Entity.ZOMBIE
                || e.type == Entity.RUNNER)) drawHair(c, e, r);
        if (e.raider) {
            // ((14.2) A looter: a red bandana over the face.)
            gv.fill.setColor(0xFFB0221C);
            gv.oval.set(r * 0.2f, -r * 0.5f, r * 0.66f, r * 0.5f);
            c.drawOval(gv.oval, gv.fill);
        }
        drawUniformDetails(c, e, r);
    }

    /** ((27.3) From drawBody.) The arms: the dead's reaching out; the living's by what they hold - a gun up when aiming, a melee swing, hands empty. */
    void drawArms(Canvas c, Entity e, float r, float now, float spd, boolean anim) {
        if (e.isUndead()) {
            float sw = (float) Math.sin(e.phase) * r * 0.2f;
            gv.stroke.setColor(e.skin);
            if (e.type == Entity.RUNNER && spd > 30) {
                // Sprinting: arms flung back.
                gv.stroke.setStrokeWidth(r * 0.36f);
                c.drawLine(-r * 0.1f, -r * 0.72f, -r * 1.25f - sw, -r * 1.0f, gv.stroke);
                c.drawLine(-r * 0.1f, r * 0.72f, -r * 1.25f + sw, r * 1.0f, gv.stroke);
            } else if (e.type == Entity.BRUISER) {
                // ((22.6) Thick arms reaching forward, bloodied hands.)
                gv.stroke.setStrokeWidth(r * 0.5f);
                c.drawLine(r * 0.1f, -r * 0.78f, r * 1.3f + sw, -r * 0.62f, gv.stroke);
                c.drawLine(r * 0.1f, r * 0.78f, r * 1.3f - sw, r * 0.62f, gv.stroke);
                gv.fill.setColor(0xFF7A2A22);
                c.drawCircle(r * 1.32f + sw, -r * 0.62f, r * 0.24f, gv.fill);
                c.drawCircle(r * 1.32f - sw, r * 0.62f, r * 0.24f, gv.fill);
            } else if (e.type == Entity.GIANT) {
                // ((25.0) Long, massive arms, the one that swipes sweeping round.)
                float swipe = anim ? 1 - Math.min(1, (now - e.biteAt) / 0.45f) : 0;
                gv.stroke.setStrokeWidth(r * 0.44f);
                float lx = r * 1.3f + sw, ly = -r * 0.95f;
                float rx = r * 1.3f - sw + swipe * r * 0.45f, ry = r * 0.95f - swipe * r * 1.15f;
                c.drawLine(r * 0.05f, -r * 0.85f, lx, ly, gv.stroke);
                c.drawLine(r * 0.05f, r * 0.85f, rx, ry, gv.stroke);
                gv.fill.setColor(City.darken(e.skin, 0.65f));
                c.drawCircle(lx, ly, r * 0.3f, gv.fill);
                c.drawCircle(rx, ry, r * 0.3f, gv.fill);
            } else if (e.type == Entity.BRUTE) {
                // Huge arms ending in fists.
                gv.stroke.setStrokeWidth(r * 0.62f);
                c.drawLine(r * 0.1f, -r * 0.85f, r * 1.4f + sw, -r * 0.8f, gv.stroke);
                c.drawLine(r * 0.1f, r * 0.85f, r * 1.4f - sw, r * 0.8f, gv.stroke);
                gv.fill.setColor(City.darken(e.skin, 0.8f));
                c.drawCircle(r * 1.45f + sw, -r * 0.8f, r * 0.36f, gv.fill);
                c.drawCircle(r * 1.45f - sw, r * 0.8f, r * 0.36f, gv.fill);
            } else {
                gv.stroke.setStrokeWidth(r * 0.42f);
                c.drawLine(r * 0.1f, -r * 0.72f, r * 1.55f + sw, -r * 0.55f, gv.stroke);
                c.drawLine(r * 0.1f, r * 0.72f, r * 1.55f - sw, r * 0.55f, gv.stroke);
            }
        } else if ((e.isArmed() && gv.world.weaponsOut(e)) || (e.hasGun && e.aiming)) {
            boolean soldier = e.type == Entity.SOLDIER;
            gv.stroke.setColor(e.skin);
            gv.stroke.setStrokeWidth(r * 0.36f);
            c.drawLine(0, -r * 0.8f, r * 1.05f, r * 0.15f, gv.stroke);
            c.drawLine(0, r * 0.8f, r * 1.3f, r * 0.35f, gv.stroke);
            gv.stroke.setColor(0xFF161616);
            int role = soldier ? e.role : 0;
            boolean marksman = e.role == Entity.ROLE_MARKSMAN;
            gv.stroke.setStrokeWidth(role == Entity.ROLE_GUNNER ? r * 0.6f : role == Entity.ROLE_COMMANDER ? r * 0.3f
                    : soldier || marksman ? r * 0.42f : r * 0.32f);
            float muzzle = role == Entity.ROLE_SNIPER || marksman ? r * 3.3f : role == Entity.ROLE_GUNNER ? r * 2.6f
                    : role == Entity.ROLE_COMMANDER ? r * 1.8f : soldier ? r * 2.5f : r * 2f;
            // ((11.4) Each gun its own length: a stubby submachine gun, a long marksman rifle, a wooden-stocked shotgun.)
            int gun = e.weapon;
            if (gun == Entity.W_SMG) muzzle = r * 1.9f;
            else if (gun == Entity.W_CARBINE) muzzle = r * 2.4f;
            else if (gun == Entity.W_DMR || gun == Entity.W_HUNTING) muzzle = r * 3.0f;
            else if (gun == Entity.W_SHOTGUN) muzzle = r * 2.5f;
            if (gun == Entity.W_SHOTGUN || gun == Entity.W_HUNTING) gv.stroke.setColor(0xFF5A3A20);
            c.drawLine(r * 0.6f, r * 0.3f, muzzle, r * 0.3f, gv.stroke);
            gv.stroke.setColor(0xFF161616);
            if (gun == Entity.W_SMG) {
                // The magazine sticking down.
                gv.fill.setColor(0xFF161616);
                c.drawRect(r * 1.1f, r * 0.4f, r * 1.3f, r * 0.85f, gv.fill);
            } else if (gun == Entity.W_DMR || gun == Entity.W_HUNTING) {
                gv.fill.setColor(0xFF0E0E0E);
                c.drawRect(r * 1.1f, r * 0.05f, r * 1.8f, r * 0.2f, gv.fill);
            }
            if (role == Entity.ROLE_SF) {
                // ((26.0) The suppressor on the end of the barrel, and the night-vision mount on the helmet.)
                gv.fill.setColor(0xFF0C0C0C);
                c.drawRect(muzzle - r * 0.1f, r * 0.12f, muzzle + r * 0.7f, r * 0.48f, gv.fill);
                c.drawRect(r * 0.35f, -r * 0.2f, r * 0.7f, r * 0.2f, gv.fill);
            } else if (role == Entity.ROLE_GRENADIER) {
                // The launcher slung under the barrel.
                gv.fill.setColor(0xFF34362A);
                c.drawRect(r * 0.9f, r * 0.45f, r * 1.9f, r * 0.85f, gv.fill);
            } else if (role == Entity.ROLE_MORTAR) {
                // ((14.0) The mortar tube slung across the back, and its baseplate.)
                gv.fill.setColor(0xFF2A2E22);
                c.drawRect(-r * 1.1f, -r * 0.25f, r * 0.2f, r * 0.12f, gv.fill);
                c.drawCircle(-r * 0.9f, r * 0.45f, r * 0.32f, gv.fill);
            } else if (role == Entity.ROLE_ENGINEER) {
                // ((14.0) A combat engineer: a hi-vis band across the shoulders and a tool bag on the back.)
                gv.fill.setColor(0xFFF2A21C);
                c.drawRect(-r * 0.25f, -r * 0.95f, r * 0.05f, r * 0.95f, gv.fill);
                gv.fill.setColor(0xFF5A4A2E);
                c.drawRect(-r * 1.0f, -r * 0.4f, -r * 0.55f, r * 0.4f, gv.fill);
            } else if (role == Entity.ROLE_CORPSMAN) {
                // A white armband with a red cross, and the medic's bag on the back.
                gv.fill.setColor(0xFFF2F2F2);
                c.drawCircle(0, -r * 0.82f, r * 0.3f, gv.fill);
                gv.fill.setColor(0xFFD83A3A);
                c.drawRect(-r * 0.2f, -r * 0.86f, r * 0.2f, -r * 0.78f, gv.fill);
                c.drawRect(-r * 0.04f, -r * 1.02f, r * 0.04f, -r * 0.62f, gv.fill);
                gv.fill.setColor(0xFF6A6E52);
                c.drawRect(-r * 1.0f, -r * 0.45f, -r * 0.55f, r * 0.45f, gv.fill);
            }
            if (role == Entity.ROLE_SNIPER || marksman) {
                // Scope.
                gv.fill.setColor(0xFF0E0E0E);
                c.drawRect(r * 1.1f, r * 0.05f, r * 1.9f, r * 0.2f, gv.fill);
            } else if (role == Entity.ROLE_GUNNER) {
                // Ammo box and belt.
                gv.fill.setColor(0xFF4A4A30);
                c.drawRect(r * 0.9f, r * 0.55f, r * 1.5f, r * 1.1f, gv.fill);
                gv.fill.setColor(0xFFC8A040);
                c.drawRect(r * 1.0f, r * 0.45f, r * 1.9f, r * 0.55f, gv.fill);
            }
        } else {
            float sw = (float) Math.sin(e.phase) * r * 0.55f;
            gv.fill.setColor(e.skin);
            c.drawCircle(sw, -r * 0.95f, r * 0.28f, gv.fill);
            c.drawCircle(-sw, r * 0.95f, r * 0.28f, gv.fill);
        }
    }

    /** ((27.3) From drawBody.) A soldier's kit: helmet, vest, pack and the marks of their role. */
    void drawSoldierKit(Canvas c, Entity e, float r) {
        if (e.type == Entity.SOLDIER) {
            boolean sf = e.role == Entity.ROLE_SF;
            if (sf) {
                // ((26.0) Special forces: a black plate carrier with its pouches.)
                gv.fill.setColor(0xFF0E1012);
                gv.oval.set(-r * 0.5f, -r * 0.68f, r * 0.42f, r * 0.68f);
                c.drawRoundRect(gv.oval, r * 0.2f, r * 0.2f, gv.fill);
                gv.fill.setColor(0xFF3A3E44);
                c.drawRect(r * 0.05f, -r * 0.5f, r * 0.3f, -r * 0.2f, gv.fill);
                c.drawRect(r * 0.05f, r * 0.2f, r * 0.3f, r * 0.5f, gv.fill);
            }
            gv.fill.setColor(sf ? 0xFF16181A : 0xFF3E4829);
            gv.oval.set(-r * 0.8f, -r * 0.5f, -r * 0.25f, r * 0.5f);
            c.drawRoundRect(gv.oval, r * 0.15f, r * 0.15f, gv.fill);
        } else if (e.type == Entity.COP) {
            gv.fill.setColor(0xFFE8C547);
            c.drawCircle(r * 0.3f, -r * 0.55f, r * 0.14f, gv.fill);
            if (e.role == Entity.ROLE_SWAT) {
                // Body armour plates and a helmet.
                gv.fill.setColor(0xFF3A3F48);
                gv.oval.set(-r * 0.55f, -r * 0.7f, r * 0.45f, r * 0.7f);
                c.drawRoundRect(gv.oval, r * 0.2f, r * 0.2f, gv.fill);
                gv.fill.setColor(0xFF15181C);
                c.drawCircle(0, 0, r * 0.42f, gv.fill);
                gv.fill.setColor(0xFFE8E8E8);
                c.drawRect(-r * 0.5f, -r * 0.08f, -r * 0.2f, r * 0.08f, gv.fill);
            }
            if (e.role == Entity.ROLE_RIOT) {
                // Riot shield in front.
                gv.fill.setColor(0xB0202A3A);
                gv.oval.set(r * 0.9f, -r * 1.25f, r * 1.35f, r * 1.25f);
                c.drawRoundRect(gv.oval, r * 0.15f, r * 0.15f, gv.fill);
                gv.fill.setColor(0xCCE8E8E8);
                c.drawRect(r * 1.05f, -r * 0.5f, r * 1.2f, r * 0.5f, gv.fill);
            }
        } else if (e.type == Entity.BRUISER) {
            // ((22.6) Broad shoulders in a torn shirt, a dark stain down the back.)
            gv.fill.setColor(City.darken(e.body, 0.8f));
            gv.oval.set(-r * 0.5f, -r * 1.05f, r * 0.35f, r * 1.05f);
            c.drawOval(gv.oval, gv.fill);
            gv.fill.setColor(0x9A6A1A14);
            c.drawCircle(-r * 0.25f, r * 0.2f, r * 0.38f, gv.fill);
        } else if (e.type == Entity.GIANT) {
            // ((25.0) A great hunched back ridged with bone, torn open in places.)
            gv.fill.setColor(City.darken(e.body, 0.7f));
            gv.oval.set(-r * 0.62f, -r * 1.15f, r * 0.35f, r * 1.15f);
            c.drawOval(gv.oval, gv.fill);
            gv.fill.setColor(0xFF8A8270);
            for (int k = 0; k < 4; k++) c.drawCircle(-r * 0.5f + k * r * 0.17f, 0, r * 0.075f, gv.fill);
            gv.fill.setColor(0xB07A1E16);
            c.drawCircle(-r * 0.12f, -r * 0.6f, r * 0.17f, gv.fill);
            c.drawCircle(-r * 0.32f, r * 0.5f, r * 0.12f, gv.fill);
        } else if (e.type == Entity.BRUTE) {
            // A hunched hump of shoulders, wider than the rest of it.
            gv.fill.setColor(0xFF3A2E3C);
            gv.oval.set(-r * 0.55f, -r * 1.2f, r * 0.4f, r * 1.2f);
            c.drawOval(gv.oval, gv.fill);
            gv.fill.setColor(0xFF4C3E4E);
            gv.oval.set(-r * 0.4f, -r * 0.8f, r * 0.2f, r * 0.8f);
            c.drawOval(gv.oval, gv.fill);
        }
    }

    /** ((27.3) From drawBody.) A police officer's cap and badge, and the rest of the uniforms' details by kind. */
    void drawUniformDetails(Canvas c, Entity e, float r) {
        if (e.type == Entity.COP) {
            if (e.agency == 4) {
                // Federal agents: bareheaded, a dark cap of hair.
                gv.fill.setColor(0xFF2A2420);
                c.drawCircle(r * 0.08f, 0, r * 0.4f, gv.fill);
            } else if (e.agency > 0) {
                // Troopers, deputies and park rangers: a wide-brimmed hat.
                gv.fill.setColor(e.agency == 2 ? 0xFF5A4630 : e.agency == 3 ? 0xFF6E5A36 : 0xFF8A7650);
                c.drawCircle(r * 0.08f, 0, r * 0.62f, gv.fill);
                gv.fill.setColor(e.agency == 2 ? 0xFF6E5A3E : e.agency == 3 ? 0xFF8C7448 : 0xFFA08C62);
                c.drawCircle(r * 0.08f, 0, r * 0.36f, gv.fill);
            } else if (e.role == Entity.ROLE_SAR) {
                // Rescue: a white helmet.
                gv.fill.setColor(0xFFF2F2F2);
                c.drawCircle(r * 0.08f, 0, r * 0.5f, gv.fill);
                gv.fill.setColor(0xFFD8302A);
                c.drawRect(-r * 0.05f, -r * 0.12f, r * 0.21f, r * 0.12f, gv.fill);
            } else {
                gv.fill.setColor(0xFF0B1022);
                gv.oval.set(r * 0.35f, -r * 0.45f, r * 0.85f, r * 0.45f);
                c.drawOval(gv.oval, gv.fill);
            }
        } else if (e.type == Entity.SOLDIER) {
            if (e.role == Entity.ROLE_COMMANDER) {
                // Beret with a gold badge.
                gv.fill.setColor(0xFF6E1414);
                c.drawCircle(-r * 0.12f, -r * 0.12f, r * 0.38f, gv.fill);
                gv.fill.setColor(0xFFF2C94C);
                c.drawCircle(r * 0.18f, -r * 0.2f, r * 0.14f, gv.fill);
            } else if (e.role == Entity.ROLE_SNIPER) {
                // Ghillie hood.
                gv.fill.setColor(0xFF4E5A2E);
                c.drawCircle(-r * 0.1f, r * 0.15f, r * 0.22f, gv.fill);
                c.drawCircle(-r * 0.2f, -r * 0.25f, r * 0.2f, gv.fill);
            } else {
                gv.fill.setColor(0xFF4C5833);
                c.drawCircle(r * 0.0f, -r * 0.1f, r * 0.2f, gv.fill);
            }
        } else if (e.isUndead() && e.type != Entity.GIANT) {
            if (e.armored) {
                // ((13.8) Still in their helmet, so they can be told apart at a glance: police navy with the
                // visor up, army olive.)
                boolean cop = e.origin == Entity.COP;
                gv.fill.setColor(cop ? 0xFF1C2438 : 0xFF4C5833);
                c.drawCircle(-r * 0.04f, 0, r * 0.54f, gv.fill);
                gv.fill.setColor(cop ? 0xFF8C9AB4 : 0xFF3A4428);
                gv.oval.set(r * 0.28f, -r * 0.42f, r * 0.62f, r * 0.42f);
                c.drawOval(gv.oval, gv.fill);
            }
            gv.fill.setColor(0xFF5A1414);
            c.drawCircle(-r * 0.1f, r * 0.18f, r * 0.2f, gv.fill);
            if (e.type == Entity.RUNNER) {
                gv.fill.setColor(0xFFFF3B2F);
                c.drawCircle(r * 0.45f, -r * 0.18f, r * 0.08f, gv.fill);
                c.drawCircle(r * 0.45f, r * 0.18f, r * 0.08f, gv.fill);
            }
        }
    }

    /** ((27.3) From drawEntity.) The marks on them: infection, the alpha's crown, a hurt flash, a runner's blur, a stun, rounds left, rank and health. */
    void drawStatusMarks(Canvas c, Entity e, boolean healthBar, float r, float spd, boolean anim) {
        if (e.infected) {
            gv.fill.setColor(alpha(0xFF7CFF3A, 0.35f + 0.25f * (float) Math.sin(gv.world.time * 5)));
            c.drawCircle(r * 0.08f, 0, r * 0.58f, gv.fill);
        }
        if (e.alpha) {
            // ((21.0) The alpha: a dark red ring, and spikes of it, round the one the horde follows.)
            gv.stroke.setStrokeWidth(r * 0.22f);
            gv.stroke.setColor(alpha(0xFFB01818, 0.75f + 0.2f * (float) Math.sin(gv.world.time * 4)));
            c.drawCircle(0, 0, r * 1.45f, gv.stroke);
            for (int k = 0; k < 6; k++) {
                float a2 = k * 1.0472f + gv.world.time * 0.6f, ca = (float) Math.cos(a2), sa = (float) Math.sin(a2);
                c.drawLine(ca * r * 1.45f, sa * r * 1.45f, ca * r * 1.95f, sa * r * 1.95f, gv.stroke);
            }
        }
        if (e.hurt > 0) {
            gv.fill.setColor(alpha(0xFFFFFFFF, e.hurt * 0.7f));
            c.drawCircle(0, 0, r * 1.05f, gv.fill);
        }
        c.restore();
        if (anim && e.type == Entity.RUNNER && spd > 30) {
            // Speed lines behind a sprinting runner.
            float ux = -e.vx / spd, uy = -e.vy / spd;
            gv.stroke.setStrokeWidth(0.6f);
            gv.stroke.setColor(0x55FFFFFF);
            for (int k = -1; k <= 1; k++) {
                float ox = -uy * k * r * 0.6f, oy = ux * k * r * 0.6f;
                c.drawLine(e.x + ux * r * 1.4f + ox, e.y + uy * r * 1.4f + oy, e.x + ux * r * (2.6f + (k & 1)) + ox, e.y + uy * r * (2.6f + (k & 1)) + oy, gv.stroke);
            }
        }
        if (gv.realistic && gv.scale > 3f) {
            // Light from the top left: a soft highlight on the shoulders and head, shade on the far side.
            gv.fill.setColor(0x26FFFFFF);
            c.drawCircle(e.x - r * 0.28f, e.y - r * 0.3f, r * 0.42f, gv.fill);
            gv.fill.setColor(0x1A000000);
            c.drawCircle(e.x + r * 0.3f, e.y + r * 0.32f, r * 0.4f, gv.fill);
        }
        if (e.stun > 0 && e.isUndead()) {
            // Dazed: little stars spinning over its head.
            gv.fill.setColor(0xFFFFE27A);
            for (int k = 0; k < 3; k++) {
                double a = gv.world.time * 6 + k * 2.1;
                c.drawCircle(e.x + (float) Math.cos(a) * r, e.y - r - 2 + (float) Math.sin(a) * 1.5f, 0.8f, gv.fill);
            }
        }
        if (healthBar && e.canShoot() && e.magSize > 0) {
            // Ammo: magazine plus spare rounds, as a thin yellow bar.
            int full = e.type == Entity.SOLDIER ? 180 : e.type == Entity.COP ? 60 : 30;
            float f = Math.min(1, (e.ammo + e.reserve) / (float) full);
            float bw = Math.max(10, r * 3), bx = e.x - bw / 2, by = e.y - r - 2.2f;
            gv.fill.setColor(0x88000000);
            c.drawRect(bx - 0.3f, by - 0.3f, bx + bw + 0.3f, by + 1.1f, gv.fill);
            gv.fill.setColor(f > 0 ? 0xFFE8C547 : 0xFFFF3B30);
            c.drawRect(bx, by, bx + bw * Math.max(f, 0.05f), by + 0.8f, gv.fill);
        }

        // ((10.59) A veteran's rank: a gold chevron for each, zoomed in.)
        int rk = e.isUndead() ? 0 : World.rank(e);
        if (rk >= 2 && gv.scale > 2.2f) {
            gv.stroke.setColor(0xFFF2C94C);
            gv.stroke.setStrokeWidth(0.5f);
            float cy = e.y - r - 6.5f;
            for (int k = 0; k < rk - 1; k++) {
                float yy = cy - k * 1.1f;
                c.drawLine(e.x - 1.6f, yy, e.x, yy - 0.9f, gv.stroke);
                c.drawLine(e.x, yy - 0.9f, e.x + 1.6f, yy, gv.stroke);
            }
        }
        if (healthBar && e.hp < e.maxHp && e.hp > 0) {
            float bw = Math.max(10, r * 3), bx = e.x - bw / 2, by = e.y - r - 4.5f;
            gv.fill.setColor(0xAA000000);
            c.drawRect(bx - 0.4f, by - 0.4f, bx + bw + 0.4f, by + 1.9f, gv.fill);
            float f = e.hp / e.maxHp;
            gv.fill.setColor(e.isUndead() ? 0xFFB5E04A : (f > 0.5f ? 0xFF4CD964 : f > 0.25f ? 0xFFFFCC00 : 0xFFFF3B30));
            c.drawRect(bx, by, bx + bw * f, by + 1.5f, gv.fill);
        }
    }

    void drawCorpse(Canvas c, World.Corpse k) {
        float r = k.radius;
        c.save();
        c.translate(k.x, k.y);
        float a = k.angle;
        if (k.rise > 0 && k.rise < 1.8f) a += (float) Math.sin(gv.world.time * 30) * 0.12f;
        c.rotate((float) Math.toDegrees(a));
        if (gv.settings.gore()) {
            gv.fill.setColor(0x886E0A0A);
            c.drawCircle(r * 0.2f, 0, r * 1.5f, gv.fill);
        }
        gv.fill.setColor(City.darken(k.body, 0.75f));
        gv.oval.set(-r * 1.2f, -r * 0.62f, r * 0.95f, r * 0.62f);
        c.drawOval(gv.oval, gv.fill);
        float up = k.rise > 0 && k.rise < 1.5f ? 1 - k.rise / 1.5f : 0;
        if (up > 0) {
            // Turning: arms lifting off the ground, the head coming up, the colour draining to grey-green.
            gv.stroke.setColor(City.darken(k.body, 0.6f));
            gv.stroke.setStrokeWidth(r * 0.36f);
            float reach = r * (0.5f + 1.3f * up), lift = r * (0.9f - 0.4f * up);
            c.drawLine(r * 0.4f, -r * 0.5f, r * 0.4f + reach, -lift, gv.stroke);
            c.drawLine(r * 0.4f, r * 0.5f, r * 0.4f + reach, lift, gv.stroke);
            gv.fill.setColor(alpha(0xFF6E8A4A, up * 0.55f));
            gv.oval.set(-r * 1.2f, -r * 0.62f, r * 0.95f, r * 0.62f);
            c.drawOval(gv.oval, gv.fill);
        }
        gv.fill.setColor(City.darken(k.head, 0.85f));
        c.drawCircle(r * (1.3f - 0.5f * up), 0, r * (0.5f + 0.08f * up), gv.fill);
        if (k.rise > 0) {
            gv.fill.setColor(alpha(0xFF7CFF3A, 0.25f + 0.2f * (float) Math.sin(gv.world.time * 8)));
            c.drawCircle(0, 0, r * 1.3f, gv.fill);
        }
        c.restore();
    }

    /** What someone is up to, in plain words. */
    /** (15.0) A police horse, seen from above: body, neck and head out in front, legs going, a tail swishing. */
    void drawHorse(Canvas c, Entity e) {
        int[] coats = {0xFF6B4226, 0xFF8A4F2A, 0xFF2E2420, 0xFFB8B0A6};
        int coat = coats[(e.nameSeed & 0x7FFFFFFF) % coats.length];
        float r = e.radius, spd = (float) Math.sqrt(e.vx * e.vx + e.vy * e.vy);
        float gait = spd > 3 ? (float) Math.sin(e.phase * 1.4f) : 0;
        c.save();
        c.translate(e.x, e.y);
        c.rotate((float) Math.toDegrees(e.angle));
        // Shadow.
        gv.fill.setColor(0x30000000);
        gv.oval.set(-r * 2.4f + r * 0.4f, -r * 1.0f + r * 0.5f, r * 2.0f + r * 0.4f, r * 1.0f + r * 0.5f);
        c.drawOval(gv.oval, gv.fill);
        // Legs, out from under the body as they go.
        gv.fill.setColor(City.darken(coat, 0.6f));
        float lg = gait * r * 0.5f;
        c.drawCircle(r * 1.2f + lg, -r * 0.75f, r * 0.28f, gv.fill);
        c.drawCircle(r * 1.2f - lg, r * 0.75f, r * 0.28f, gv.fill);
        c.drawCircle(-r * 1.5f - lg, -r * 0.75f, r * 0.28f, gv.fill);
        c.drawCircle(-r * 1.5f + lg, r * 0.75f, r * 0.28f, gv.fill);
        // Tail.
        gv.stroke.setColor(City.darken(coat, 0.5f));
        gv.stroke.setStrokeWidth(r * 0.35f);
        c.drawLine(-r * 2.2f, 0, -r * 3.0f, gait * r * 0.4f, gv.stroke);
        // Body, neck and head.
        gv.fill.setColor(coat);
        gv.oval.set(-r * 2.3f, -r * 0.95f, r * 1.7f, r * 0.95f);
        c.drawOval(gv.oval, gv.fill);
        gv.oval.set(r * 1.2f, -r * 0.45f, r * 2.6f, r * 0.45f);
        c.drawOval(gv.oval, gv.fill);
        gv.fill.setColor(City.darken(coat, 0.85f));
        gv.oval.set(r * 2.3f, -r * 0.38f, r * 3.4f, r * 0.38f);
        c.drawOval(gv.oval, gv.fill);
        // The saddlecloth, in police blue with a white edge.
        gv.fill.setColor(0xFFF2F2F2);
        c.drawRect(-r * 1.15f, -r * 1.0f, r * 0.55f, r * 1.0f, gv.fill);
        gv.fill.setColor(0xFF1E3E8A);
        c.drawRect(-r * 1.0f, -r * 0.92f, r * 0.4f, r * 0.92f, gv.fill);
        c.restore();
    }

    /**
     * (15.0) A traffic officer. ((16.1) Drawn properly: a fluorescent vest over the uniform with two silver reflective
     * bands across it and over the shoulders, a white peaked cap, white gloves - and, waving traffic through, one arm
     * out with a glowing orange wand, swinging, and the other raised flat to hold the rest.)
     */
    void drawTrafficKit(Canvas c, Entity e) {
        float r = e.radius;
        c.save();
        c.translate(e.x, e.y);
        c.rotate((float) Math.toDegrees(e.angle));
        // The vest over the shoulders and back.
        gv.fill.setColor(0xFFCBEA1E);
        gv.oval.set(-r * 0.78f, -r * 0.92f, r * 0.62f, r * 0.92f);
        c.drawOval(gv.oval, gv.fill);
        // Reflective bands: two across the back, one over each shoulder.
        gv.fill.setColor(0xFFE6EAEE);
        c.drawRect(-r * 0.55f, -r * 0.86f, -r * 0.38f, r * 0.86f, gv.fill);
        c.drawRect(-r * 0.05f, -r * 0.9f, r * 0.12f, r * 0.9f, gv.fill);
        gv.fill.setColor(0xFFB8BEC4);
        c.drawRect(-r * 0.5f, -r * 0.08f, r * 0.05f, r * 0.08f, gv.fill);
        boolean waving = e.waveUntil > gv.world.time;
        float sw = waving ? (float) Math.sin(gv.world.time * 10) * 0.6f : 0;
        if (waving) {
            // The wand arm, swinging, the glow of the wand at its end.
            float a = 1.25f + sw;
            float hx = (float) Math.cos(a) * r * 2.2f, hy = (float) Math.sin(a) * r * 2.2f;
            gv.stroke.setColor(0xFFCBEA1E);
            gv.stroke.setStrokeWidth(r * 0.42f);
            c.drawLine(0, r * 0.6f, hx, hy, gv.stroke);
            gv.stroke.setColor(0x66FF8A1E);
            gv.stroke.setStrokeWidth(r * 0.9f);
            float wx = hx + (float) Math.cos(a) * r * 1.4f, wy = hy + (float) Math.sin(a) * r * 1.4f;
            c.drawLine(hx, hy, wx, wy, gv.stroke);
            gv.stroke.setColor(0xFFFF8A1E);
            gv.stroke.setStrokeWidth(r * 0.36f);
            c.drawLine(hx, hy, wx, wy, gv.stroke);
            gv.fill.setColor(0xFFFFFFFF);
            c.drawCircle(hx, hy, r * 0.3f, gv.fill);
            // The other hand up, flat: stop.
            gv.stroke.setColor(0xFFCBEA1E);
            gv.stroke.setStrokeWidth(r * 0.42f);
            c.drawLine(0, -r * 0.6f, r * 1.4f, -r * 1.1f, gv.stroke);
            gv.fill.setColor(0xFFFFFFFF);
            c.drawCircle(r * 1.5f, -r * 1.15f, r * 0.32f, gv.fill);
        } else {
            // White gloves at the sides.
            gv.fill.setColor(0xFFFFFFFF);
            c.drawCircle(r * 0.3f, -r * 0.95f, r * 0.24f, gv.fill);
            c.drawCircle(r * 0.3f, r * 0.95f, r * 0.24f, gv.fill);
        }
        // The white cap, its black peak to the front.
        gv.fill.setColor(0xFF1A1C20);
        c.drawRect(r * 0.2f, -r * 0.3f, r * 0.62f, r * 0.3f, gv.fill);
        gv.fill.setColor(0xFFF6F6F2);
        c.drawCircle(0, 0, r * 0.46f, gv.fill);
        gv.fill.setColor(0xFF1E3E8A);
        c.drawCircle(0, 0, r * 0.16f, gv.fill);
        c.restore();
    }

    /**
     * (10.50) A spawn button's picture: a head and shoulders seen from the front on a disc in the colour of the
     * side, with what tells them apart - the cop's peaked cap and badge, the soldier's helmet (a beret for the
     * commander, a scope for the sniper), the dead's green skin and red eyes, a runner's speed lines, a brute's bulk.
     */
    void drawPersonBadge(Canvas c, int type, Entity e, float cx, float cy, float size) {
        boolean zombie = type == Entity.ZOMBIE || type == Entity.RUNNER || type == Entity.BRUTE || type == Entity.BRUISER || type == Entity.GIANT
                || type == Entity.SCREAMER || type == Entity.ARMOURED;
        boolean giant = type == Entity.GIANT;
        int disc = zombie ? 0xFF2E4A22 : type == Entity.COP ? 0xFF1E3366 : type == Entity.SOLDIER ? 0xFF3A4328 : 0xFF5A4022;
        float r = size * 1.18f;
        gv.fill.setColor(disc);
        c.drawCircle(cx, cy, r, gv.fill);
        gv.stroke.setColor(zombie ? 0xFF7CC24E : type == Entity.COP ? 0xFF6F9BFF : type == Entity.SOLDIER ? 0xFFB8C47A : 0xFFF0AD4E);
        gv.stroke.setStrokeWidth(1.4f * gv.dp);
        c.drawCircle(cx, cy, r, gv.stroke);
        c.save();
        c.clipRect(cx - r, cy - r, cx + r, cy + r * 0.95f);
        float bulk = giant ? 1.75f : type == Entity.BRUTE ? 1.35f : type == Entity.BRUISER ? 1.17f : 1f;
        // Shoulders.
        int shirt = type == Entity.COP ? e.body : type == Entity.SOLDIER ? e.body : type == Entity.ARMOURED ? 0xFF2A3F78 : type == Entity.SCREAMER ? 0xFF8E8A7C
                : zombie ? 0xFF4A5A3A : 0xFFE07A2A;
        gv.fill.setColor(shirt);
        gv.oval.set(cx - r * 0.78f * bulk, cy + r * 0.28f, cx + r * 0.78f * bulk, cy + r * 1.5f);
        c.drawOval(gv.oval, gv.fill);
        if (type == Entity.BRUISER) {
            // ((22.6) A bruiser: a dark red shirt torn open, blood down the front.)
            gv.fill.setColor(0xFF6A3A34);
            gv.oval.set(cx - r * 0.7f * bulk, cy + r * 0.32f, cx + r * 0.7f * bulk, cy + r * 1.45f);
            c.drawOval(gv.oval, gv.fill);
            gv.fill.setColor(0xFF8E1A12);
            c.drawCircle(cx - r * 0.2f, cy + r * 0.7f, r * 0.18f, gv.fill);
            c.drawCircle(cx + r * 0.3f, cy + r * 0.85f, r * 0.13f, gv.fill);
        } else if (giant) {
            // ((25.0) A giant: vast dark shoulders ridged with bone.)
            gv.fill.setColor(0xFF3A3036);
            gv.oval.set(cx - r * 0.78f * bulk, cy + r * 0.12f, cx + r * 0.78f * bulk, cy + r * 1.5f);
            c.drawOval(gv.oval, gv.fill);
            gv.fill.setColor(0xFF9A927E);
            for (int k = -2; k <= 2; k++) c.drawCircle(cx + k * r * 0.3f, cy + r * 0.3f + Math.abs(k) * r * 0.06f, r * 0.08f, gv.fill);
            gv.fill.setColor(0xFF8E1A12);
            c.drawCircle(cx + r * 0.35f, cy + r * 0.75f, r * 0.12f, gv.fill);
        } else if (zombie) {
            // Torn shirt.
            gv.fill.setColor(0xFF6E0A0A);
            c.drawCircle(cx + r * 0.25f, cy + r * 0.62f, r * 0.12f, gv.fill);
        }
        if (type == Entity.COP) {
            gv.fill.setColor(0xFFE8C547);
            c.drawCircle(cx - r * 0.32f, cy + r * 0.6f, r * 0.1f, gv.fill);
        }
        // Head.
        int skin = zombie ? (giant ? 0xFF5C7448 : type == Entity.BRUTE ? 0xFF6F8D55 : 0xFF8FA173) : 0xFFE0AC69;
        gv.fill.setColor(skin);
        float hr = r * 0.36f * (giant ? 0.85f : type == Entity.BRUTE ? 1.15f : type == Entity.BRUISER ? 1.07f : 1f), hy = cy - r * (giant ? 0.02f : 0.12f);
        c.drawCircle(cx, hy, hr, gv.fill);
        if (giant) {
            // A heavy brow.
            gv.fill.setColor(0xFF3E5030);
            c.drawRect(cx - hr * 0.95f, hy - hr * 0.35f, cx + hr * 0.95f, hy - hr * 0.08f, gv.fill);
        }
        // Eyes.
        gv.fill.setColor(giant ? 0xFFF0E08A : zombie ? 0xFFFF3B2F : 0xFF1A1A1A);
        c.drawCircle(cx - hr * 0.38f, hy + hr * 0.05f, hr * 0.13f, gv.fill);
        c.drawCircle(cx + hr * 0.38f, hy + hr * 0.05f, hr * 0.13f, gv.fill);
        if (zombie) {
            // A gaping mouth. ((27.0) A screamer's: wide open.)
            gv.fill.setColor(0xFF3A0808);
            if (type == Entity.SCREAMER) gv.oval.set(cx - hr * 0.42f, hy + hr * 0.28f, cx + hr * 0.42f, hy + hr * 0.95f);
            else gv.oval.set(cx - hr * 0.3f, hy + hr * 0.4f, cx + hr * 0.3f, hy + hr * 0.75f);
            c.drawOval(gv.oval, gv.fill);
        }
        if (type == Entity.ARMOURED) {
            // ((27.0) Still in its helmet.)
            gv.fill.setColor(0xFF15203F);
            gv.oval.set(cx - hr * 1.1f, hy - hr * 1.2f, cx + hr * 1.1f, hy - hr * 0.1f);
            c.drawOval(gv.oval, gv.fill);
        }
        if (type == Entity.COP) {
            // Peaked cap.
            gv.fill.setColor(0xFF15203F);
            gv.oval.set(cx - hr * 1.05f, hy - hr * 1.15f, cx + hr * 1.05f, hy - hr * 0.25f);
            c.drawOval(gv.oval, gv.fill);
            c.drawRect(cx - hr * 1.2f, hy - hr * 0.5f, cx + hr * 1.2f, hy - hr * 0.3f, gv.fill);
            gv.fill.setColor(0xFFE8C547);
            c.drawCircle(cx, hy - hr * 0.72f, hr * 0.16f, gv.fill);
        } else if (type == Entity.SOLDIER) {
            if (e.role == Entity.ROLE_COMMANDER) {
                gv.fill.setColor(0xFF8E1F1F);
                gv.oval.set(cx - hr * 1.1f, hy - hr * 1.1f, cx + hr * 0.9f, hy - hr * 0.3f);
                c.drawOval(gv.oval, gv.fill);
            } else {
                gv.fill.setColor(e.role == Entity.ROLE_GUARD ? 0xFF6A6248 : 0xFF4A5530);
                gv.oval.set(cx - hr * 1.2f, hy - hr * 1.25f, cx + hr * 1.2f, hy + hr * 0.1f);
                c.drawArc(gv.oval, 180, 180, true, gv.fill);
            }
            if (e.role == Entity.ROLE_SNIPER) {
                gv.stroke.setColor(0xFF161616);
                gv.stroke.setStrokeWidth(r * 0.12f);
                c.drawLine(cx + r * 0.45f, cy + r * 0.2f, cx + r * 0.95f, cy - r * 0.55f, gv.stroke);
            } else if (e.role == Entity.ROLE_GUNNER || e.role == Entity.ROLE_GRENADIER) {
                gv.stroke.setColor(0xFF161616);
                gv.stroke.setStrokeWidth(r * 0.16f);
                c.drawLine(cx - r * 0.9f, cy + r * 0.75f, cx + r * 0.9f, cy + r * 0.45f, gv.stroke);
            }
        } else if (!zombie) {
            // Hair.
            gv.fill.setColor(0xFF4A3020);
            gv.oval.set(cx - hr * 1.02f, hy - hr * 1.1f, cx + hr * 1.02f, hy - hr * 0.1f);
            c.drawArc(gv.oval, 180, 180, true, gv.fill);
        }
        c.restore();
        if (type == Entity.RUNNER) {
            // Speed lines.
            gv.stroke.setColor(0xCCFFFFFF);
            gv.stroke.setStrokeWidth(1.3f * gv.dp);
            for (int k = -1; k <= 1; k++) c.drawLine(cx - r * 1.35f, cy + k * r * 0.35f, cx - r * 0.95f, cy + k * r * 0.35f, gv.stroke);
        }
    }
}
