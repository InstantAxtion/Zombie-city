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
 * ((27.3) Out of GameView.) Draws the vehicles: cars and their bodies, police cars, trucks, tanks, motorcycles, helicopters,
 * jets, trains and boats.
 */
final class VehicleRenderer {
    final GameView gv;

    VehicleRenderer(GameView gv) {
        this.gv = gv;
    }


    /** A car, police cruiser, army truck or ambulance, with its damage showing. */
    void drawVehicle(Canvas c, Fleet.Vehicle v) {
        if (v.type == Fleet.TANK) {
            drawTank(c, v);
            return;
        }
        if (v.type == Fleet.TRAIN) {
            drawTrain(c, v);
            return;
        }
        if (v.type == Fleet.BOAT) {
            if (v.kind == Fleet.K_POLICE_BOAT) drawPoliceBoat(c, v);
            else drawLandingCraft(c, v);
            return;
        }
        if (v.kind == Fleet.K_BIKE) {
            drawBike(c, v);
            return;
        }
        boolean truck = v.type == Fleet.TRUCK, amb = v.type == Fleet.AMBULANCE;
        float hl = truck || amb ? 9.5f : 8f, hw = truck ? 5.2f : amb ? 4.9f : 4.4f;
        // ((27.5) The army's armoured ambulance: longer and broader.)
        if (v.kind == Fleet.K_ARMD_AMB) {
            hl = 10.5f;
            hw = 5.5f;
        }
        if (v.kind == Fleet.K_APC) {
            hl = 11;
            hw = 5.6f;
        } else if (v.kind == Fleet.K_HUMVEE) {
            hl = 8.5f;
            hw = 4.9f;
        } else if (v.kind == Fleet.K_BEARCAT) {
            hl = 10.2f;
            hw = 5.4f;
        } else if (v.kind == Fleet.K_DOZER) {
            hl = 8.5f;
            hw = 5.4f;
        } else if (v.kind == Fleet.K_IFV) {
            hl = 11.5f;
            hw = 6f;
        } else if (Fleet.isPost(v)) {
            hl = 11.5f;
            hw = 5.4f;
        }
        int model = v.type == Fleet.CAR ? v.model : -1;
        if (model >= 0) {
            hl = Fleet.MODEL_HL[model];
            hw = Fleet.MODEL_HW[model];
        }
        // Shadow, always down and to the right like everything else.
        c.save();
        c.translate(v.x + 1.6f, v.y + 2.2f);
        c.rotate((float) Math.toDegrees(v.angle));
        gv.fill.setColor(0x48000000);
        gv.oval.set(-hl - 0.5f, -hw - 0.5f, hl + 0.5f, hw + 0.5f);
        c.drawRoundRect(gv.oval, 2.5f, 2.5f, gv.fill);
        c.restore();
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));
        // Zoomed well out a vehicle is a couple of pixels: just its shape in its colour.
        // ((11.5) Everyday cars from further in than before - a dozen pixels long, the wheels, windows and lamps were
        // dozens of drawing calls no one could make out - police, army and ambulances once they're a bit smaller.)
        boolean speck = gv.scale < (v.type == Fleet.CAR ? 0.8f : 0.5f);
        if (!v.burnt && !speck) {
            // Tyres poking out at the corners.
            gv.fill.setColor(0xFF111214);
            float wx = hl * 0.62f, ww = truck ? 1.8f : 1.5f;
            for (int sx = -1; sx <= 1; sx += 2) {
                c.drawRect(sx * wx - ww, -hw - 0.5f, sx * wx + ww, -hw + 1f, gv.fill);
                c.drawRect(sx * wx - ww, hw - 1f, sx * wx + ww, hw + 0.5f, gv.fill);
            }
        }
        boolean civ = v.type == Fleet.CAR;
        Country land0 = gv.world.city.country;
        int body = civ ? v.color : v.kind == Fleet.K_BEARCAT ? 0xFF1A1C20 : v.kind == Fleet.K_COMMAND ? 0xFFE4E6EA : v.kind == Fleet.K_ARMY_POST ? 0xFF4F5A33 : v.recon ? 0xFF9C8A5E
                : v.kind == Fleet.K_RIOT_VAN ? land0.cruiserBody
                : truck ? (v.restock ? 0xFFE6E3DA : v.swat ? 0xFF1C2026 : v.guardUnit ? 0xFF8C8260 : v.kind == Fleet.K_APC ? 0xFF46512E : 0xFF4F5A33) : amb ? (v.kind == Fleet.K_ARMD_AMB ? 0xFF5F6B40 : 0xFFF2F2F2)
                : v.agency == 1 ? land0.hpBody : v.agency == 2 ? land0.ruralBody : v.agency == 3 ? 0xFF3E5634 : v.agency == 4 ? 0xFF15171A : land0.cruiserBody;
        if (v.burnt) body = 0xFF2B2623;
        else if (v.broken) body = City.darken(body, 0.65f);
        gv.fill.setColor(body);
        gv.oval.set(-hl, -hw, hl, hw);
        boolean shell = shellModel(v);
        if (shell) carShell(c, hl, hw, hw * (v.model == Fleet.M_SUV || v.model == Fleet.M_PICKUP ? 0.38f : 0.55f), hw * 0.32f);
        else if (model == Fleet.M_BEETLE) c.drawRoundRect(gv.oval, hw, hw * 0.9f, gv.fill);
        else c.drawRoundRect(gv.oval, model == Fleet.M_KEI || model == Fleet.M_BUS || model == Fleet.M_VAN ? 1.2f : 2.5f,
                model == Fleet.M_KEI || model == Fleet.M_BUS || model == Fleet.M_VAN ? 1.2f : 2.5f, gv.fill);
        if (speck) {
            c.restore();
            return;
        }
        if (v.burnt) {
            // A burnt-out shell.
            gv.fill.setColor(0xFF4A2F22);
            c.drawRect(-hl + 2, -hw + 1.2f, hl - 2, hw - 1.2f, gv.fill);
            gv.fill.setColor(0xFF151312);
            c.drawRect(-2, -hw + 1.5f, 3, hw - 1.5f, gv.fill);
            c.restore();
            return;
        }
        // ((11.5) Police, army and ambulances zoomed out: the body and, on a call, the lights; the cab, lamps, markings
        // and wear are too small to see, and were a couple of dozen drawing calls a vehicle.)
        if (!civ && gv.scale < 0.9f) {
            if (!v.broken && (v.type == Fleet.CRUISER || amb)) lightBar(c, amb ? hl * 0.44f : hl * 0.06f, hw, v.lightsOn(), v.anim);
            c.restore();
            return;
        }
        // ((11.0) The sheen of the light along the body.)
        if (!civ) {
            gv.fill.setColor(0x24FFFFFF);
            c.drawRect(-hl * 0.6f, -hw * 0.82f, hl * 0.75f, -hw * 0.38f, gv.fill);
        }
        drawVehicleKind(c, v, truck, amb, hl, hw, civ, land0, body, shell);
        if (!v.broken && shell) lamps(c, hl, hw, !v.parked, !v.parked && v.speed < 25);
        else if (!v.broken) {
            // Brake lights: bright red when slowing or stopped in traffic.
            boolean braking = !v.parked && v.speed < 25;
            gv.fill.setColor(braking ? 0xFFFF2A20 : 0xFF7A1410);
            c.drawRect(-hl, -hw + 0.8f, -hl + 0.9f, -hw + 2f, gv.fill);
            c.drawRect(-hl, hw - 2f, -hl + 0.9f, hw - 0.8f, gv.fill);
            if (braking) {
                gv.fill.setColor(0x40FF2A20);
                c.drawCircle(-hl - 1, -hw + 1.4f, 2f, gv.fill);
                c.drawCircle(-hl - 1, hw - 1.4f, 2f, gv.fill);
            }
            // A glint on the windscreen.
            gv.stroke.setColor(0x66FFFFFF);
            gv.stroke.setStrokeWidth(0.5f);
            float wsx = civ ? 3f : truck ? hl * 0.57f : amb ? hl * 0.66f : 3.7f;
            c.drawLine(wsx - 0.6f, -hw + 1.6f, wsx + 0.6f, -hw * 0.1f, gv.stroke);
        }
        if (gv.realistic && !v.burnt) {
            // Shading: a sheen along the roof, darker flanks and a crisp outline.
            gv.fill.setColor(0x22FFFFFF);
            c.drawRect(-hl + 2, -hw * 0.35f, hl - 2.5f, hw * 0.05f, gv.fill);
            gv.fill.setColor(0x1E000000);
            c.drawRect(-hl + 1, hw * 0.55f, hl - 1, hw - 0.3f, gv.fill);
            gv.stroke.setColor(0x55000000);
            gv.stroke.setStrokeWidth(0.45f);
            gv.oval.set(-hl, -hw, hl, hw);
            if (!shell) c.drawRoundRect(gv.oval, 2.5f, 2.5f, gv.stroke);
            // Wing mirrors.
            gv.fill.setColor(City.darken(civ ? v.color : 0xFF333333, 0.8f));
            c.drawRect(hl * 0.35f, -hw - 1f, hl * 0.35f + 1.2f, -hw, gv.fill);
            c.drawRect(hl * 0.35f, hw, hl * 0.35f + 1.2f, hw + 1f, gv.fill);
        }
        // ((26.0) A roof gun with its gunner on the trucks and vans that carry one now (and anything given one).)
        if (!v.burnt && Fleet.addedGun(v)) drawRoofGun(c, v, v.kind == Fleet.K_BEARCAT ? -hl * 0.3f : v.type == Fleet.CAR ? -hl * 0.35f : hl * 0.1f, 2.3f, 6.5f, body);
        float dmg = 1 - Math.max(0, v.hp) / v.maxHp;
        if (dmg > 0.3f) {
            // Dents, scrapes and a cracked windscreen.
            gv.stroke.setColor(0xAAD8D8D8);
            gv.stroke.setStrokeWidth(0.5f);
            c.drawLine(hl * 0.3f, -hw * 0.6f, hl * 0.55f, hw * 0.2f, gv.stroke);
            gv.fill.setColor(0x66000000);
            c.drawCircle(hl - 2, -hw + 1.5f, 1.3f, gv.fill);
            if (dmg > 0.6f) {
                c.drawCircle(-hl + 2.5f, hw - 1.5f, 1.6f, gv.fill);
                c.drawLine(-hl * 0.2f, hw * 0.7f, hl * 0.1f, -hw * 0.3f, gv.stroke);
            }
        }
        c.restore();
    }

    /** ((27.3) From drawVehicle.) The body and the details of each kind: a car's model, the army's vehicles, the police's vans and trucks, the command post, an ambulance, a police car's light bar. */
    void drawVehicleKind(Canvas c, Fleet.Vehicle v, boolean truck, boolean amb, float hl, float hw, boolean civ, Country land0, int body, boolean shell) {
        if (civ) {
            drawCarDetails(c, v, hl, hw, body, shell);
        } else if (v.kind == Fleet.K_APC) {
            // An eight-wheeled armoured personnel carrier: sloped glacis, hatches and a gun turret.
            gv.fill.setColor(0xFF111214);
            for (int k = 0; k < 4; k++) {
                float wx = -hl * 0.7f + k * hl * 0.45f;
                c.drawRect(wx - 1.6f, -hw - 0.6f, wx + 1.6f, -hw + 0.8f, gv.fill);
                c.drawRect(wx - 1.6f, hw - 0.8f, wx + 1.6f, hw + 0.6f, gv.fill);
            }
            gv.fill.setColor(City.darken(body, 0.8f));
            c.drawRect(-hl + 1, -hw + 1, hl - 1, hw - 1, gv.fill);
            gv.fill.setColor(City.lighten(body, 0.12f));
            c.drawRect(hl * 0.55f, -hw + 1, hl - 0.5f, hw - 1, gv.fill);
            gv.fill.setColor(City.darken(body, 0.65f));
            c.drawRect(-hl * 0.75f, -2.2f, -hl * 0.45f, -0.6f, gv.fill);
            c.drawRect(-hl * 0.75f, 0.6f, -hl * 0.45f, 2.2f, gv.fill);
            drawRoofGun(c, v, 0, 3.2f, 7.5f, body);
        } else if (v.kind == Fleet.K_IFV) {
            // ((14.6) The infantry fighting vehicle: tracks down each side, a sloped hull and a turret with a long barrel.)
            gv.fill.setColor(0xFF1C1D1A);
            c.drawRect(-hl, -hw - 0.6f, hl, -hw + 1.8f, gv.fill);
            c.drawRect(-hl, hw - 1.8f, hl, hw + 0.6f, gv.fill);
            gv.fill.setColor(City.darken(body, 0.8f));
            c.drawRect(-hl + 1, -hw + 1.8f, hl - 1, hw - 1.8f, gv.fill);
            gv.fill.setColor(City.lighten(body, 0.14f));
            c.drawRect(hl * 0.55f, -hw + 1.8f, hl - 0.5f, hw - 1.8f, gv.fill);
            drawRoofGun(c, v, -hl * 0.1f, 3.6f, 11f, body);
        } else if (v.kind == Fleet.K_GUNTRUCK) {
            // ((14.6) The gun truck: the cab in front, an open bed behind, and the gun mounted in it.)
            gv.fill.setColor(City.lighten(body, 0.1f));
            c.drawRect(hl * 0.35f, -hw + 0.8f, hl - 0.6f, hw - 0.8f, gv.fill);
            gv.fill.setColor(0xFF1E2A33);
            c.drawRect(hl * 0.35f, -hw + 1.2f, hl * 0.5f, hw - 1.2f, gv.fill);
            gv.fill.setColor(City.darken(body, 0.7f));
            c.drawRect(-hl + 0.8f, -hw + 0.8f, hl * 0.3f, hw - 0.8f, gv.fill);
            gv.fill.setColor(0xFF6A5A3A);
            c.drawRect(-hl * 0.65f, -hw + 1.4f, -hl * 0.4f, hw - 1.4f, gv.fill);
            drawRoofGun(c, v, -hl * 0.3f, 2.4f, 8f, body);
        } else if (v.kind == Fleet.K_DOZER) {
            // ((14.0) The engineers' bulldozer: tracks down each side, the cab at the back, the blade across the front.)
            gv.fill.setColor(0xFF1C1D1A);
            c.drawRect(-hl, -hw - 0.6f, hl * 0.7f, -hw + 1.6f, gv.fill);
            c.drawRect(-hl, hw - 1.6f, hl * 0.7f, hw + 0.6f, gv.fill);
            gv.fill.setColor(City.darken(body, 0.8f));
            c.drawRect(-hl * 0.85f, -hw * 0.55f, -hl * 0.1f, hw * 0.55f, gv.fill);
            gv.fill.setColor(0xFF1E2A33);
            c.drawRect(-hl * 0.75f, -hw * 0.4f, -hl * 0.35f, hw * 0.4f, gv.fill);
            gv.fill.setColor(0xFF2A2A26);
            c.drawCircle(hl * 0.25f, -hw * 0.35f, 0.9f, gv.fill);
            gv.fill.setColor(v.broken ? 0xFF6A6A66 : 0xFF9A9C96);
            c.drawRect(hl * 0.8f, -hw - 1.2f, hl + 1.4f, hw + 1.2f, gv.fill);
            gv.fill.setColor(0xFF6E706A);
            c.drawRect(hl * 0.8f, -hw - 1.2f, hl * 0.95f, hw + 1.2f, gv.fill);
        } else if (v.kind == Fleet.K_HUMVEE) {
            // A Humvee: broad and low, the windscreen, and the gunner's ring on the roof.
            gv.fill.setColor(City.darken(body, 0.85f));
            c.drawRect(-hl + 0.8f, -hw + 0.8f, hl * 0.2f, hw - 0.8f, gv.fill);
            gv.fill.setColor(0xFF1E2A33);
            c.drawRect(hl * 0.25f, -hw + 1, hl * 0.45f, hw - 1, gv.fill);
            gv.fill.setColor(City.lighten(body, 0.1f));
            c.drawRect(hl * 0.5f, -hw + 1, hl - 0.6f, hw - 1, gv.fill);
            drawRoofGun(c, v, -hl * 0.2f, 2.4f, 6f, body);
        } else if (v.kind == Fleet.K_RIOT_VAN) {
            // The riot van: a high box with grilles over the windows and a light bar.
            gv.fill.setColor(v.broken ? 0xFF9A9A9A : land0.cruiserDoor);
            c.drawRect(-hl + 1, -hw, hl * 0.3f, -hw + 1.2f, gv.fill);
            c.drawRect(-hl + 1, hw - 1.2f, hl * 0.3f, hw, gv.fill);
            gv.fill.setColor(0xFF2A2C30);
            for (float gx = -hl + 2; gx < hl * 0.2f; gx += 2.6f) c.drawRect(gx, -hw + 1.6f, gx + 0.6f, hw - 1.6f, gv.fill);
            gv.fill.setColor(0xFF1E2A33);
            c.drawRect(hl * 0.45f, -hw + 1, hl * 0.7f, hw - 1, gv.fill);
            if (!v.broken) {
                boolean on = v.state == 1 || v.state == 0, blink = ((int) (v.anim * 8)) % 2 == 0;
                gv.fill.setColor(on && blink ? 0xFFFF3A30 : 0xFF5A1A18);
                c.drawRect(hl * 0.33f, -hw + 1, hl * 0.42f, 0, gv.fill);
                gv.fill.setColor(on && !blink ? 0xFF3A7BFF : 0xFF1A2A5A);
                c.drawRect(hl * 0.33f, 0, hl * 0.42f, hw - 1, gv.fill);
            }
        } else if (v.kind == Fleet.K_BEARCAT) {
            // ((11.0) The BearCat: an armoured black truck, a push bar on the front, a roof hatch and a light bar.)
            gv.fill.setColor(0xFF0E0F12);
            c.drawRect(hl - 0.4f, -hw + 0.6f, hl + 1.4f, hw - 0.6f, gv.fill);
            gv.fill.setColor(v.broken ? 0xFF2A2A2A : 0xFF262A30);
            c.drawRect(-hl + 0.8f, -hw + 0.8f, hl * 0.35f, hw - 0.8f, gv.fill);
            gv.fill.setColor(0xFF3A3F47);
            c.drawCircle(-hl * 0.3f, 0, 2, gv.fill);
            gv.fill.setColor(0xFF0F1418);
            c.drawRect(hl * 0.45f, -hw + 1.2f, hl * 0.62f, hw - 1.2f, gv.fill);
            gv.fill.setColor(0xFFE8E8E8);
            c.drawRect(-hl + 1, -hw, hl * 0.35f, -hw + 0.7f, gv.fill);
            c.drawRect(-hl + 1, hw - 0.7f, hl * 0.35f, hw, gv.fill);
            if (!v.parked && !v.broken) {
                boolean blink = ((int) (v.anim * 8)) % 2 == 0;
                gv.fill.setColor(blink ? 0xFFFF3A30 : 0xFF3A7BFF);
                c.drawRect(hl * 0.36f, -hw + 1, hl * 0.43f, 0, gv.fill);
                gv.fill.setColor(blink ? 0xFF3A7BFF : 0xFFFF3A30);
                c.drawRect(hl * 0.36f, 0, hl * 0.43f, hw - 1, gv.fill);
            }
        } else if (Fleet.isPost(v)) {
            // ((26.0) The mobile command post: a big white box truck, a blue band, the cab, and the radio mast on its roof.
            // ((26.1) The army's: olive drab, a tan band.))
            boolean armyPost = v.kind == Fleet.K_ARMY_POST;
            gv.fill.setColor(v.broken ? 0xFF5A5C60 : armyPost ? 0xFFB8A878 : land0.cruiserBody == 0xFFF2F2F2 || land0.cruiserBody == 0xFFE8E8E8 ? 0xFF1E4AA8 : land0.cruiserBody);
            c.drawRect(-hl + 1, -hw, hl * 0.4f, -hw + 1.2f, gv.fill);
            c.drawRect(-hl + 1, hw - 1.2f, hl * 0.4f, hw, gv.fill);
            gv.fill.setColor(armyPost ? 0xFF5E6A3E : 0xFFC8CCD2);
            c.drawRect(-hl + 1.2f, -hw + 1.6f, hl * 0.38f, hw - 1.6f, gv.fill);
            gv.fill.setColor(0xFF1E2A33);
            c.drawRect(hl * 0.5f, -hw + 1, hl * 0.72f, hw - 1, gv.fill);
            gv.fill.setColor(0xFF3A3F47);
            c.drawCircle(-hl * 0.55f, 0, 1.8f, gv.fill);
            gv.stroke.setColor(0xFF2A2C30);
            gv.stroke.setStrokeWidth(0.7f);
            c.drawLine(-hl * 0.55f, 0, -hl * 0.1f, -hw * 0.6f, gv.stroke);
            gv.fill.setColor(v.broken ? 0xFF5A1A18 : ((int) (gv.world.time * 2)) % 2 == 0 ? 0xFFFF3A30 : 0xFF7A1A18);
            c.drawCircle(-hl * 0.1f, -hw * 0.6f, 0.9f, gv.fill);
            if (!v.broken && v.state != Fleet.SCENE) lightBar(c, hl * 0.42f, hw, true, v.anim);
        } else if (truck && v.swat) {
            // The SWAT van: armoured black box body, a white band and a light bar.
            gv.fill.setColor(v.broken ? 0xFF2A2A2A : 0xFF2C3036);
            c.drawRect(-hl + 0.8f, -hw + 0.8f, hl * 0.3f, hw - 0.8f, gv.fill);
            gv.fill.setColor(0xFFE8E8E8);
            c.drawRect(-hl + 1, -hw, hl * 0.3f, -hw + 0.9f, gv.fill);
            c.drawRect(-hl + 1, hw - 0.9f, hl * 0.3f, hw, gv.fill);
            gv.fill.setColor(0xFF1E2A33);
            c.drawRect(hl * 0.45f, -hw + 1, hl * 0.7f, hw - 1, gv.fill);
            if (!v.parked && !v.broken) {
                boolean blink = ((int) (v.anim * 8)) % 2 == 0;
                gv.fill.setColor(blink ? 0xFFFF3A30 : 0xFF3A7BFF);
                c.drawRect(hl * 0.33f, -hw + 1, hl * 0.42f, 0, gv.fill);
                gv.fill.setColor(blink ? 0xFF3A7BFF : 0xFFFF3A30);
                c.drawRect(hl * 0.33f, 0, hl * 0.42f, hw - 1, gv.fill);
            }
        } else if (truck) {
            gv.fill.setColor(v.broken ? 0xFF3C4428 : 0xFF5F6B40);
            c.drawRect(-hl + 0.8f, -hw + 0.8f, hl * 0.3f, hw - 0.8f, gv.fill);
            gv.fill.setColor(0xFF1E2A33);
            c.drawRect(hl * 0.45f, -hw + 1, hl * 0.7f, hw - 1, gv.fill);
        } else if (amb) {
            drawAmbulance(c, v, hl, hw);
        } else {
            // (10.44) A patrol car: a saloon in its force's colours, the doors marked out, and the light bar across
            // the roof just behind the windscreen.
            Country land = gv.world.city.country;
            boolean city = v.agency == 0;
            int door = v.broken ? 0xFFA0A0A0 : v.agency == 1 ? land.hpDoor : v.agency == 2 ? land.ruralDoor : v.agency == 3 ? 0xFFD8C898
                    : v.agency == 4 ? 0xFF15171A : land.cruiserDoor;
            if (city && land.cruiserChecks) {
                // A chequered band down each side.
                for (int q = 0; q < 6; q++) {
                    float x0 = -hl + 1.5f + q * (hl * 2 - 3) / 6f, x1 = x0 + (hl * 2 - 3) / 6f;
                    gv.fill.setColor(v.broken ? 0xFF9A9A9A : q % 2 == 0 ? land.checkA : land.checkB);
                    c.drawRect(x0, -hw + 0.15f, x1, -hw + 1.3f, gv.fill);
                    gv.fill.setColor(v.broken ? 0xFF9A9A9A : q % 2 == 0 ? land.checkB : land.checkA);
                    c.drawRect(x0, hw - 1.3f, x1, hw - 0.15f, gv.fill);
                }
            } else {
                // The doors in the force's second colour (a stripe the length of the car, out on the highway).
                float d0 = city ? -hl * 0.5f : -hl + 1.2f, d1 = city ? hl * 0.3f : hl - 1.5f;
                gv.fill.setColor(door);
                c.drawRect(d0, -hw + 0.15f, d1, -hw + 1.4f, gv.fill);
                c.drawRect(d0, hw - 1.4f, d1, hw - 0.15f, gv.fill);
            }
            int roof = city && land.cruiserRoof != 0 ? (v.broken ? 0xFFA0A0A0 : land.cruiserRoof) : body;
            cabin(c, hl, hw, roof, 0.42f, 0.14f, -0.46f, -0.68f);
            if (city && land.cruiserRoof != 0) {
                gv.fill.setColor(roof);
                c.drawRect(-hl * 0.46f + 0.3f, -hw + 1.6f, hl * 0.14f - 0.3f, hw - 1.6f, gv.fill);
            }
            // The unit number on the roof, for the helicopter.
            gv.fill.setColor(City.darken(roof, 0.55f));
            c.drawRect(-hl * 0.32f, -0.9f, -hl * 0.3f + 0.5f, 0.9f, gv.fill);
            c.drawRect(-hl * 0.22f, -0.9f, -hl * 0.2f + 0.5f, 0.9f, gv.fill);
            // A push bar on the front.
            gv.fill.setColor(0xFF1A1B1E);
            c.drawRect(hl - 0.1f, -hw * 0.55f, hl + 0.7f, hw * 0.55f, gv.fill);
            if (!v.broken) lightBar(c, hl * 0.06f, hw, v.lightsOn(), v.anim);
        }
    }

    /** ((27.3) From drawVehicleKind.) An everyday car: its model's body, roof and glass. */
    void drawCarDetails(Canvas c, Fleet.Vehicle v, float hl, float hw, int body, boolean shell) {
        drawCarBody(c, v, body, hl, hw);
        gv.fill.setColor(0x1EFFFFFF);
        c.drawRect(-hl * 0.5f, -hw * 0.8f, hl * 0.6f, -hw * 0.42f, gv.fill);
        if (!v.parked && !shell) {
            gv.fill.setColor(0xFFFFF4C0);
            c.drawCircle(hl - 0.8f, -hw + 1.3f, 0.8f, gv.fill);
            c.drawCircle(hl - 0.8f, hw - 1.3f, 0.8f, gv.fill);
        }
        if (!v.riders.isEmpty()) {
            // Heads of the people riding along.
            gv.fill.setColor(0xFF4A2E1A);
            for (int k = 0; k < Math.min(3, v.riders.size()); k++)
                c.drawCircle(-1.8f + (k == 2 ? -2.5f : 0), k % 2 == 0 ? -1.6f : 1.6f, 1.1f, gv.fill);
        }
    }

    /** ((27.3) From drawVehicleKind.) An ambulance. */
    void drawAmbulance(Canvas c, Fleet.Vehicle v, float hl, float hw) {
        if (v.kind == Fleet.K_ARMD_AMB) {
            // ((27.5) The army's armoured ambulance: olive drab, the slit windscreen, and a red cross on a white square on
            // its roof.)
            gv.fill.setColor(0xFF2A2E22);
            c.drawRect(hl * 0.55f, -hw + 1.2f, hl * 0.72f, hw - 1.2f, gv.fill);
            gv.fill.setColor(0xFF4B5534);
            c.drawRect(-hl + 0.8f, -hw + 0.8f, hl * 0.45f, hw - 0.8f, gv.fill);
            gv.fill.setColor(0xFFF0F0EC);
            c.drawRect(-4.2f, -3.2f, 1.2f, 3.2f, gv.fill);
            gv.fill.setColor(0xFFD83A3A);
            c.drawRect(-3.6f, -0.8f, 0.6f, 0.8f, gv.fill);
            c.drawRect(-2.3f, -2.4f, -0.7f, 2.4f, gv.fill);
            boolean on = v.lightsOn() && ((int) (v.anim * 6)) % 2 == 0;
            gv.fill.setColor(on ? 0xFF4A8AFF : 0xFF26324A);
            c.drawRect(hl * 0.46f, -hw + 0.6f, hl * 0.52f, -hw + 1.6f, gv.fill);
            return;
        }
        gv.fill.setColor(0xFF1E2A33);
        c.drawRect(hl * 0.55f, -hw + 1, hl * 0.78f, hw - 1, gv.fill);
        gv.fill.setColor(0xFFD83A3A);
        c.drawRect(-hl, -hw, hl * 0.5f, -hw + 1, gv.fill);
        c.drawRect(-hl, hw - 1, hl * 0.5f, hw, gv.fill);
        c.drawRect(-3.5f, -0.8f, 0.5f, 0.8f, gv.fill);
        c.drawRect(-2.3f, -2f, -0.7f, 2f, gv.fill);
        // (10.44) The cab's roof, its light bar, and flashers on the back corners of the box.
        gv.fill.setColor(0xFFE4E4E4);
        c.drawRect(hl * 0.5f, -hw + 0.4f, hl * 0.55f, hw - 0.4f, gv.fill);
        lightBar(c, hl * 0.44f, hw, v.lightsOn(), v.anim);
        boolean blink = ((int) (v.anim * 8)) % 2 == 0;
        gv.fill.setColor(v.lightsOn() && !blink ? 0xFFFF4A3A : 0xFF8A2A24);
        c.drawRect(-hl + 0.2f, -hw + 0.2f, -hl + 1.2f, -hw + 1.2f, gv.fill);
        gv.fill.setColor(v.lightsOn() && blink ? 0xFFFF4A3A : 0xFF8A2A24);
        c.drawRect(-hl + 0.2f, hw - 1.2f, -hl + 1.2f, hw - 0.2f, gv.fill);
    }

    /** (10.44) Car-shaped models (not the boxes: vans, buses, kei cars, Beetles), and patrol cars. */
    static boolean shellModel(Fleet.Vehicle v) {
        if (v.type == Fleet.CRUISER) return v.kind != Fleet.K_RIOT_VAN && v.kind != Fleet.K_BIKE;
        if (v.type != Fleet.CAR) return false;
        int m = v.model;
        return m == Fleet.M_SEDAN || m == Fleet.M_TAXI || m == Fleet.M_HATCH || m == Fleet.M_SUV || m == Fleet.M_PICKUP;
    }

    /** (10.44) A car's outline seen from above: a rounded nose, squarer tail, slightly pinched at the cabin. */
    void carShell(Canvas c, float hl, float hw, float nose, float tail) {
        Path pa = gv.carPath;
        pa.reset();
        float pinch = hw * 0.06f;
        pa.moveTo(-hl + tail, -hw);
        pa.lineTo(-hl * 0.1f, -hw + pinch);
        pa.lineTo(hl - nose, -hw);
        pa.quadTo(hl, -hw, hl, -hw + nose);
        pa.lineTo(hl, hw - nose);
        pa.quadTo(hl, hw, hl - nose, hw);
        pa.lineTo(-hl * 0.1f, hw - pinch);
        pa.lineTo(-hl + tail, hw);
        pa.quadTo(-hl, hw, -hl, hw - tail);
        pa.lineTo(-hl, -hw + tail);
        pa.quadTo(-hl, -hw, -hl + tail, -hw);
        pa.close();
        c.drawPath(pa, gv.fill);
    }

    /** A pane of glass seen from above: wider at one end (w0, at x0) than the other (w1, at x1). */
    void pane(Canvas c, float x0, float w0, float x1, float w1) {
        Path pa = gv.carPath;
        pa.reset();
        pa.moveTo(x0, -w0);
        pa.lineTo(x1, -w1);
        pa.lineTo(x1, w1);
        pa.lineTo(x0, w0);
        pa.close();
        c.drawPath(pa, gv.fill);
    }

    /**
     * (10.44) The cabin of a car from above, as fractions of its half-length (front +1, back -1): the windscreen
     * from wsF back to wsR, the roof from there to rwF, the rear window back to rwR, with the side windows along
     * it, a glint on the glass and the bonnet's crease.
     */
    void cabin(Canvas c, float hl, float hw, int body, float wsF, float wsR, float rwF, float rwR) {
        int glass = 0xFF1C2630;
        float in = hw - 0.9f, top = hw - 1.6f;
        gv.fill.setColor(glass);
        pane(c, hl * wsF, in, hl * wsR, top);
        if (rwR < rwF) pane(c, hl * rwR, in - 0.2f, hl * rwF, top);
        // Side windows.
        gv.fill.setColor(0xFF2A3540);
        c.drawRect(hl * rwF, -in, hl * wsR, -top + 0.25f, gv.fill);
        c.drawRect(hl * rwF, top - 0.25f, hl * wsR, in, gv.fill);
        // The roof, catching the light.
        gv.fill.setColor(City.lighten(body, 0.12f));
        c.drawRect(hl * rwF + 0.3f, -top, hl * wsR - 0.3f, top, gv.fill);
        gv.fill.setColor(0x26FFFFFF);
        c.drawRect(hl * rwF + 0.6f, -top + 0.4f, hl * wsR - 0.6f, -top * 0.2f, gv.fill);
        // A glint across the windscreen.
        gv.stroke.setColor(0x70FFFFFF);
        gv.stroke.setStrokeWidth(0.45f);
        c.drawLine(hl * (wsF * 0.7f + wsR * 0.3f), -in + 0.6f, hl * (wsF * 0.35f + wsR * 0.65f), -top * 0.15f, gv.stroke);
        // The bonnet: a crease down the middle.
        gv.stroke.setColor(City.darken(body, 0.82f));
        gv.stroke.setStrokeWidth(0.35f);
        c.drawLine(hl * wsF + 0.6f, 0, hl - 1.2f, 0, gv.stroke);
    }

    /** (10.44) Head and tail lamps, set into the corners of a car's shell. */
    void lamps(Canvas c, float hl, float hw, boolean lit, boolean braking) {
        gv.fill.setColor(lit ? 0xFFFFF6D0 : 0xFFD8D4C4);
        c.drawRect(hl - 1.1f, -hw + 0.9f, hl - 0.3f, -hw + 2.3f, gv.fill);
        c.drawRect(hl - 1.1f, hw - 2.3f, hl - 0.3f, hw - 0.9f, gv.fill);
        gv.fill.setColor(braking ? 0xFFFF2A20 : 0xFF8A1A14);
        c.drawRect(-hl + 0.3f, -hw + 0.8f, -hl + 1.0f, -hw + 2.4f, gv.fill);
        c.drawRect(-hl + 0.3f, hw - 2.4f, -hl + 1.0f, hw - 0.8f, gv.fill);
        if (braking) {
            gv.fill.setColor(0x40FF2A20);
            c.drawCircle(-hl - 1, -hw + 1.6f, 2f, gv.fill);
            c.drawCircle(-hl - 1, hw - 1.6f, 2f, gv.fill);
        }
    }

    /** (10.44) A patrol car's light bar across the roof: red one side, blue the other, flashing in turn. */
    void lightBar(Canvas c, float x, float hw, boolean on, float anim) {
        boolean blink = ((int) (anim * 8)) % 2 == 0;
        float y = hw - 1.1f;
        gv.fill.setColor(0xFF2A2C30);
        c.drawRect(x - 0.95f, -y, x + 0.95f, y, gv.fill);
        gv.fill.setColor(!on ? 0xFF6A2420 : blink ? 0xFFFF4A3A : 0xFF7A2A24);
        c.drawRect(x - 0.7f, -y + 0.25f, x + 0.7f, -0.3f, gv.fill);
        gv.fill.setColor(!on ? 0xFF22346A : !blink ? 0xFF4A8AFF : 0xFF2A3C7A);
        c.drawRect(x - 0.7f, 0.3f, x + 0.7f, y - 0.25f, gv.fill);
        if (on) {
            gv.fill.setColor(blink ? 0x55FF4A3A : 0x554A8AFF);
            c.drawCircle(x, blink ? -y * 0.5f : y * 0.5f, 2.6f, gv.fill);
        }
    }

    /**
     * The top of an everyday car, by model: windscreens, roof and the things that set it apart (a pickup's
     * bed, a taxi's roof sign, a bus's roof hatches, a Beetle's round wings).
     */
    void drawCarBody(Canvas c, Fleet.Vehicle v, int body, float hl, float hw) {
        int glass = 0xFF1E2A33;
        int roofCol = City.lighten(body, 0.15f);
        switch (v.model) {
            case Fleet.M_PICKUP:
                cabin(c, hl, hw, body, 0.55f, 0.3f, -0.02f, -0.12f);
                // The open bed, with something tied down in it now and then.
                gv.fill.setColor(City.darken(body, 0.55f));
                c.drawRect(-hl + 1.0f, -hw + 1.0f, -hl * 0.2f, hw - 1.0f, gv.fill);
                gv.fill.setColor(City.darken(body, 0.42f));
                for (float x = -hl + 2.2f; x < -hl * 0.25f; x += 2.2f) c.drawRect(x, -hw + 1.2f, x + 0.35f, hw - 1.2f, gv.fill);
                if (v.kind == Fleet.K_PICKUP) {
                    // ((26.0) A survivors' armed pickup: a gun on a post in the bed, a gunner behind it, turning to its target.)
                    float gx = -hl * 0.55f, rel = v.turret - v.angle;
                    float ca = (float) Math.cos(rel), sa = (float) Math.sin(rel);
                    if (!v.crew.isEmpty() && v.crew.size() >= 2) {
                        gv.fill.setColor(0xFF6A5A44);
                        c.drawCircle(gx - ca * 1.6f, -sa * 1.6f, 1.5f, gv.fill);
                    }
                    gv.stroke.setColor(0xFF222428);
                    gv.stroke.setStrokeWidth(1.0f);
                    c.drawLine(gx, 0, gx + ca * 5.5f, sa * 5.5f, gv.stroke);
                    gv.fill.setColor(0xFF34373C);
                    c.drawCircle(gx, 0, 1.1f, gv.fill);
                } else if ((v.number & 1) == 0) {
                    gv.fill.setColor(0xFF8A6A48);
                    c.drawRect(-hl * 0.8f, -hw * 0.45f, -hl * 0.42f, hw * 0.35f, gv.fill);
                }
                break;
            case Fleet.M_SUV:
                cabin(c, hl, hw, body, 0.5f, 0.24f, -0.7f, -0.88f);
                // Roof rails.
                gv.fill.setColor(0xFF2A2C30);
                c.drawRect(-hl * 0.62f, -hw + 1.7f, hl * 0.16f, -hw + 2.2f, gv.fill);
                c.drawRect(-hl * 0.62f, hw - 2.2f, hl * 0.16f, hw - 1.7f, gv.fill);
                break;
            case Fleet.M_KEI:
                // A tall little box: the windscreen right at the front.
                gv.fill.setColor(glass);
                c.drawRect(hl * 0.42f, -hw + 0.8f, hl * 0.78f, hw - 0.8f, gv.fill);
                gv.fill.setColor(roofCol);
                c.drawRect(-hl * 0.85f, -hw + 1f, hl * 0.4f, hw - 1f, gv.fill);
                gv.fill.setColor(glass);
                c.drawRect(-hl * 0.97f, -hw + 1f, -hl * 0.86f, hw - 1f, gv.fill);
                break;
            case Fleet.M_HATCH:
                cabin(c, hl, hw, body, 0.46f, 0.16f, -0.66f, -0.88f);
                break;
            case Fleet.M_VAN:
                gv.fill.setColor(glass);
                c.drawRect(hl * 0.55f, -hw + 0.9f, hl * 0.8f, hw - 0.9f, gv.fill);
                gv.fill.setColor(roofCol);
                c.drawRect(-hl + 1, -hw + 1, hl * 0.53f, hw - 1, gv.fill);
                gv.fill.setColor(City.darken(body, 0.85f));
                for (float x = -hl + 3; x < hl * 0.45f; x += 3) c.drawRect(x, -hw + 1.2f, x + 0.4f, hw - 1.2f, gv.fill);
                break;
            case Fleet.M_BUS: {
                Country land = gv.world.city.country;
                gv.fill.setColor(glass);
                c.drawRect(hl - 2.2f, -hw + 0.8f, hl - 0.6f, hw - 0.8f, gv.fill);
                gv.fill.setColor(roofCol);
                c.drawRect(-hl + 1, -hw + 1, hl - 2.6f, hw - 1, gv.fill);
                // Roof hatches and the air conditioning.
                gv.fill.setColor(0xFFB8BCC0);
                c.drawRect(-hl * 0.2f, -hw * 0.55f, hl * 0.25f, hw * 0.55f, gv.fill);
                gv.fill.setColor(City.darken(body, 0.75f));
                c.drawRect(-hl * 0.75f, -1.6f, -hl * 0.55f, 1.6f, gv.fill);
                c.drawRect(hl * 0.45f, -1.6f, hl * 0.65f, 1.6f, gv.fill);
                if (land.id == Country.USA) {
                    // School bus: black rub rails down the sides.
                    gv.fill.setColor(0xFF1A1A1A);
                    c.drawRect(-hl + 0.5f, -hw + 0.3f, hl - 0.5f, -hw + 0.8f, gv.fill);
                    c.drawRect(-hl + 0.5f, hw - 0.8f, hl - 0.5f, hw - 0.3f, gv.fill);
                } else {
                    int stripe = 0xFF1F3F8A;
                    gv.fill.setColor(stripe);
                    c.drawRect(-hl + 0.5f, -hw + 0.3f, hl - 0.5f, -hw + 1f, gv.fill);
                    c.drawRect(-hl + 0.5f, hw - 1f, hl - 0.5f, hw - 0.3f, gv.fill);
                }
                break;
            }
            case Fleet.M_BEETLE:
                // Round wings at the corners and a domed roof.
                gv.fill.setColor(City.darken(body, 0.82f));
                for (int sx = -1; sx <= 1; sx += 2)
                    for (int sy = -1; sy <= 1; sy += 2) c.drawCircle(sx * hl * 0.6f, sy * (hw - 0.8f), 1.5f, gv.fill);
                gv.fill.setColor(glass);
                gv.oval.set(-hl * 0.55f, -hw + 1f, hl * 0.35f, hw - 1f);
                c.drawOval(gv.oval, gv.fill);
                gv.fill.setColor(roofCol);
                gv.oval.set(-hl * 0.4f, -hw + 1.6f, hl * 0.18f, hw - 1.6f);
                c.drawOval(gv.oval, gv.fill);
                break;
            default:
                cabin(c, hl, hw, body, 0.42f, 0.12f, -0.44f, -0.66f);
                if (v.model == Fleet.M_TAXI) {
                    // The roof sign (and the chequered stripe down the sides).
                    gv.fill.setColor(0xFFF2E8A0);
                    c.drawRect(-1.6f, -1.6f, 0.2f, 1.6f, gv.fill);
                    for (int q = 0; q < 6; q++) {
                        gv.fill.setColor(q % 2 == 0 ? 0xFF1A1A1A : 0xFFF2F2F2);
                        float x0 = -hl + 2 + q * 1.6f;
                        c.drawRect(x0, -hw + 0.2f, x0 + 1.6f, -hw + 0.9f, gv.fill);
                        c.drawRect(x0, hw - 0.9f, x0 + 1.6f, hw - 0.2f, gv.fill);
                    }
                }
                break;
        }
    }

    /** (11.0) A Marine landing craft: a grey hull with a ramp at the bow, the Marines in rows, and its wake. */
    void drawLandingCraft(Canvas c, Fleet.Vehicle v) {
        float hl = 13, hw = 5.5f;
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));
        if (v.speed > 5) {
            gv.fill.setColor(0x60FFFFFF);
            for (int k = 1; k <= 3; k++) {
                float back = -hl - k * 5, spread = hw + k * 2.2f;
                c.drawRect(back - 2, -spread, back, -spread + 1.4f, gv.fill);
                c.drawRect(back - 2, spread - 1.4f, back, spread, gv.fill);
            }
            gv.fill.setColor(0x40FFFFFF);
            c.drawRect(-hl - 14, -2, -hl, 2, gv.fill);
        }
        gv.fill.setColor(0x38000000);
        gv.oval.set(-hl + 1.5f, -hw + 2, hl + 1.5f, hw + 2);
        c.drawRoundRect(gv.oval, 2, 2, gv.fill);
        gv.fill.setColor(0xFF5E666C);
        gv.oval.set(-hl, -hw, hl, hw);
        c.drawRoundRect(gv.oval, 2, 2, gv.fill);
        gv.fill.setColor(0xFF41484D);
        c.drawRect(-hl + 1.5f, -hw + 1.2f, hl - 2.5f, hw - 1.2f, gv.fill);
        gv.fill.setColor(0xFF6E777D);
        c.drawRect(hl - 2.5f, -hw, hl, hw, gv.fill);
        gv.fill.setColor(0xFF2E3337);
        c.drawRect(-hl + 1, -2, -hl + 4.5f, 2, gv.fill);
        if (v.passengers > 0) {
            gv.fill.setColor(0xFF3E4E62);
            for (int k = 0; k < Math.min(8, v.passengers); k++)
                c.drawCircle(-hl + 7 + (k / 2) * 3.8f, (k % 2 == 0 ? -1.8f : 1.8f), 1.3f, gv.fill);
        }
        c.restore();
    }

    void drawTrain(Canvas c, Fleet.Vehicle v) {
        float dir = (float) Math.cos(v.angle);
        for (int k = 0; k < 4; k++) {
            float front = v.x - dir * k * 34, back = front - dir * 32;
            float x0 = Math.min(front, back), x1 = Math.max(front, back);
            gv.fill.setColor(0x55000000);
            c.drawRect(x0 + 2, v.y - 6, x1 + 2, v.y + 9, gv.fill);
            boolean loco = k == 0;
            gv.fill.setColor(loco ? 0xFFB8302A : 0xFF3A5A8A);
            gv.oval.set(x0, v.y - 7, x1, v.y + 7);
            c.drawRoundRect(gv.oval, 3, 3, gv.fill);
            gv.fill.setColor(loco ? 0xFF8E2420 : 0xFF2E4A72);
            c.drawRect(x0 + 1, v.y - 7, x1 - 1, v.y - 5.5f, gv.fill);
            if (loco) {
                // Cab windows at the front.
                gv.fill.setColor(0xFF1E2A33);
                float cab = front - dir * 6;
                c.drawRect(Math.min(front, cab) + 0.5f, v.y - 5, Math.max(front, cab) - 0.5f, v.y + 5, gv.fill);
                gv.fill.setColor(0xFFFFF4C0);
                c.drawCircle(front - dir * 0.5f, v.y - 4, 0.9f, gv.fill);
                c.drawCircle(front - dir * 0.5f, v.y + 4, 0.9f, gv.fill);
            } else {
                gv.fill.setColor(0xFFCFE0EE);
                for (float x = x0 + 3; x < x1 - 3; x += 5) c.drawRect(x, v.y - 5.5f, x + 3, v.y + 5.5f, gv.fill);
                gv.fill.setColor(0xFF3A5A8A);
                c.drawRect(x0 + 2, v.y - 4.5f, x1 - 2, v.y + 4.5f, gv.fill);
            }
        }
    }

    /** A strike jet high overhead: its shadow far below, swept wings and a contrail (drawn as smoke). */
    void drawJet(Canvas c, Fleet.Vehicle v) {
        // ((15.10) A plane at the airport: an airliner, or a grey military transport; its shadow falls further off
        // the higher it is.)
        if (v.kind == Fleet.K_CRASH) {
            // ((25.0) The airliner coming down: bigger and closer, its shadow rushing in to meet it, a wing on fire.)
            float u = 19f, off = 4 + v.alt * 60;
            c.save();
            c.translate(v.x + off, v.y + off * 1.3f);
            c.rotate((float) Math.toDegrees(v.angle));
            gv.fill.setColor(0x38000000);
            c.drawRect(-4.5f * u, -0.6f * u, 4.5f * u, 0.6f * u, gv.fill);
            c.drawRect(-1.6f * u, -4.2f * u, -0.6f * u, 4.2f * u, gv.fill);
            c.restore();
            City.drawAirliner(c, gv.fill, v.x, v.y, u, v.angle, 0xFFB8302A);
            float ca = (float) Math.cos(v.angle), sa = (float) Math.sin(v.angle);
            float ex = v.x - ca * 6 - sa * 2 * u, ey = v.y - sa * 6 + ca * 2 * u;
            float f = 0.5f + 0.5f * (float) Math.sin(gv.world.time * 23);
            gv.fill.setColor(alpha(0xFFFF6A1A, 0.7f));
            c.drawCircle(ex, ey, 9 + 3 * f, gv.fill);
            gv.fill.setColor(alpha(0xFFFFE08A, 0.9f));
            c.drawCircle(ex, ey, 4 + 2 * f, gv.fill);
            return;
        }
        if (v.kind == Fleet.K_AIRLINER || v.kind == Fleet.K_TRANSPORT) {
            float u = 14f, off = 4 + v.alt * 60;
            c.save();
            c.translate(v.x + off, v.y + off * 1.3f);
            c.rotate((float) Math.toDegrees(v.angle));
            gv.fill.setColor(0x30000000);
            c.drawRect(-4.5f * u, -0.6f * u, 4.5f * u, 0.6f * u, gv.fill);
            c.drawRect(-1.6f * u, -4.2f * u, -0.6f * u, 4.2f * u, gv.fill);
            c.restore();
            if (v.kind == Fleet.K_TRANSPORT) {
                c.save();
                c.translate(v.x, v.y);
                c.rotate((float) Math.toDegrees(v.angle));
                gv.fill.setColor(0xFF6A7060);
                c.drawRect(-1.4f * u, -5 * u, -0.4f * u, 5 * u, gv.fill);
                c.drawRect(-4.6f * u, -1.8f * u, -3.8f * u, 1.8f * u, gv.fill);
                gv.fill.setColor(0xFF7A806E);
                c.drawRect(-4.6f * u, -0.8f * u, 4 * u, 0.8f * u, gv.fill);
                c.drawCircle(4 * u, 0, 0.8f * u, gv.fill);
                gv.fill.setColor(0xFF4A5040);
                for (float ey : SIDE_MARKS) c.drawRect(-0.6f * u, ey * u - 0.35f * u, 0.4f * u, ey * u + 0.35f * u, gv.fill);
                c.restore();
            } else City.drawAirliner(c, gv.fill, v.x, v.y, u, v.angle, 0xFF2E6EB8);
            return;
        }
        c.save();
        c.translate(v.x + 40, v.y + 55);
        c.rotate((float) Math.toDegrees(v.angle));
        gv.fill.setColor(0x30000000);
        drawJetShape(c, 0.9f);
        c.restore();
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));
        gv.fill.setColor(0xFF6E7680);
        drawJetShape(c, 1f);
        gv.fill.setColor(0xFF2E3A48);
        c.drawRect(6, -1.2f, 11, 1.2f, gv.fill);
        c.restore();
    }

    void drawJetShape(Canvas c, float s) {
        android.graphics.Path p = new android.graphics.Path();
        p.moveTo(16 * s, 0);
        p.lineTo(2 * s, -3 * s);
        p.lineTo(-4 * s, -14 * s);
        p.lineTo(-8 * s, -14 * s);
        p.lineTo(-6 * s, -3 * s);
        p.lineTo(-14 * s, -3 * s);
        p.lineTo(-17 * s, -7 * s);
        p.lineTo(-19 * s, -7 * s);
        p.lineTo(-18 * s, 0);
        p.lineTo(-19 * s, 7 * s);
        p.lineTo(-17 * s, 7 * s);
        p.lineTo(-14 * s, 3 * s);
        p.lineTo(-6 * s, 3 * s);
        p.lineTo(-8 * s, 14 * s);
        p.lineTo(-4 * s, 14 * s);
        p.lineTo(2 * s, 3 * s);
        p.close();
        c.drawPath(p, gv.fill);
    }

    /** A tank: tracks, hull and a turret that turns on its own. */
    void drawTank(Canvas c, Fleet.Vehicle v) {
        float hl = 11, hw = 7;
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));
        gv.fill.setColor(0x55000000);
        c.drawRect(-hl + 2, -hw + 2, hl + 2, hw + 2, gv.fill);
        gv.fill.setColor(0xFF2B2E24);
        c.drawRect(-hl, -hw, hl, -hw + 2.6f, gv.fill);
        c.drawRect(-hl, hw - 2.6f, hl, hw, gv.fill);
        // Track links rolling by.
        gv.fill.setColor(0xFF454A3A);
        float off = (v.anim * v.speed * 0.3f) % 2.5f;
        for (float x = -hl + off; x < hl; x += 2.5f) {
            c.drawRect(x, -hw, x + 0.8f, -hw + 2.6f, gv.fill);
            c.drawRect(x, hw - 2.6f, x + 0.8f, hw, gv.fill);
        }
        gv.fill.setColor(v.broken ? 0xFF3A3E2C : 0xFF55623A);
        c.drawRect(-hl + 1, -hw + 2.4f, hl - 0.5f, hw - 2.4f, gv.fill);
        gv.fill.setColor(0xFF4A5532);
        c.drawRect(-hl + 1, -1, -hl + 4, 1, gv.fill);
        c.rotate((float) Math.toDegrees(v.turret - v.angle));
        gv.stroke.setColor(0xFF3A4228);
        gv.stroke.setStrokeWidth(1.6f);
        float recoil = v.cannonCd > 3.2f ? -2 : 0;
        c.drawLine(3 + recoil, 0, 17 + recoil, 0, gv.stroke);
        gv.fill.setColor(v.broken ? 0xFF434830 : 0xFF627042);
        gv.oval.set(-5, -4.2f, 5, 4.2f);
        c.drawRoundRect(gv.oval, 2, 2, gv.fill);
        gv.fill.setColor(0xFF3E4829);
        c.drawCircle(-1.5f, -1.5f, 1.3f, gv.fill);
        c.restore();
    }

    /** The helicopter: its shadow on the ground (further off the higher it flies), body, tail and rotor. */
    /** ((26.0) A police launch: a white hull with a blue band, the wheelhouse and its light bar, a wake behind it at speed. */
    void drawPoliceBoat(Canvas c, Fleet.Vehicle v) {
        float hl = 10, hw = 4.2f;
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));
        if (v.speed > 5) {
            // The wake: a V of white water spreading out behind, and the churn off the stern.
            float sp = Math.min(1, v.speed / Ops.BOAT_SPEED);
            gv.fill.setColor(alpha(0xFFFFFFFF, 0.55f * sp));
            for (int k = 1; k <= 4; k++) {
                float back = -hl - k * 5.5f, spread = hw * 0.6f + k * 2.6f;
                c.drawRect(back - 2.2f, -spread, back, -spread + 1.3f, gv.fill);
                c.drawRect(back - 2.2f, spread - 1.3f, back, spread, gv.fill);
            }
            gv.fill.setColor(alpha(0xFFFFFFFF, 0.4f * sp));
            c.drawRect(-hl - 16 * sp, -1.8f, -hl, 1.8f, gv.fill);
            c.drawCircle(hl - 0.5f, 0, 2.2f * sp, gv.fill);
        }
        gv.fill.setColor(0x38000000);
        gv.oval.set(-hl + 1.5f, -hw + 2, hl + 1.5f, hw + 2);
        c.drawRoundRect(gv.oval, hw, hw, gv.fill);
        // The hull, pointed at the bow.
        Path pa = gv.carPath;
        pa.reset();
        pa.moveTo(-hl, -hw);
        pa.lineTo(hl * 0.45f, -hw);
        pa.lineTo(hl, 0);
        pa.lineTo(hl * 0.45f, hw);
        pa.lineTo(-hl, hw);
        pa.close();
        gv.fill.setColor(0xFFF4F4F2);
        c.drawPath(pa, gv.fill);
        gv.fill.setColor(0xFF1E4AA8);
        c.drawRect(-hl, -hw, hl * 0.4f, -hw + 1.1f, gv.fill);
        c.drawRect(-hl, hw - 1.1f, hl * 0.4f, hw, gv.fill);
        gv.fill.setColor(0xFFD8DCE0);
        c.drawRect(-hl + 1.2f, -hw + 1.4f, hl * 0.2f, hw - 1.4f, gv.fill);
        // The wheelhouse, its windscreen, and the light bar on top.
        gv.fill.setColor(0xFFFAFAFA);
        c.drawRect(-hl * 0.35f, -hw * 0.62f, hl * 0.25f, hw * 0.62f, gv.fill);
        gv.fill.setColor(0xFF1E2A33);
        c.drawRect(hl * 0.15f, -hw * 0.55f, hl * 0.28f, hw * 0.55f, gv.fill);
        boolean job = v.boatJob != Ops.J_PATROL && v.boatJob != Ops.J_IDLE, blink = ((int) (v.anim * 8)) % 2 == 0;
        gv.fill.setColor(job && blink ? 0xFFFF3A30 : 0xFF6A2420);
        c.drawRect(-hl * 0.1f, -hw * 0.5f, hl * 0.02f, 0, gv.fill);
        gv.fill.setColor(job && !blink ? 0xFF3A7BFF : 0xFF1A2A6A);
        c.drawRect(-hl * 0.1f, 0, hl * 0.02f, hw * 0.5f, gv.fill);
        // The officers in the stern, and anyone picked up.
        gv.fill.setColor(0xFF22304A);
        int officers = 2 + v.crew.size();
        for (int k = 0; k < Math.min(4, officers); k++) c.drawCircle(-hl + 2.2f + (k / 2) * 2.6f, k % 2 == 0 ? -1.6f : 1.6f, 1.05f, gv.fill);
        gv.fill.setColor(0xFF8A5A3A);
        for (int k = 0; k < Math.min(6, v.riders.size()); k++) c.drawCircle(-hl * 0.3f + (k / 2) * 2.4f - 2.4f, k % 2 == 0 ? -2.6f : 2.6f, 1f, gv.fill);
        if (gv.scale > 1.4f) drawRoofGun(c, v, -hl * 0.55f, 1.3f, 4.5f, 0xFFF4F4F2);
        c.restore();
    }

    /** A gun turret on a vehicle's roof at (x, 0) (in its own frame), turned to v.turret. */
    void drawRoofGun(Canvas c, Fleet.Vehicle v, float x, float ring, float barrel, int body) {
        gv.fill.setColor(City.darken(body, 0.6f));
        c.drawCircle(x, 0, ring, gv.fill);
        gv.fill.setColor(City.lighten(body, 0.05f));
        c.drawCircle(x, 0, ring * 0.75f, gv.fill);
        float a = v.turret - v.angle;
        gv.stroke.setColor(0xFF161616);
        gv.stroke.setStrokeWidth(1.1f);
        c.drawLine(x, 0, x + (float) Math.cos(a) * barrel, (float) Math.sin(a) * barrel, gv.stroke);
        // ((26.0) The gunner behind it, when there's one up there: a helmet behind the gun.)
        if (Fleet.gunManned(v)) {
            boolean police = v.type == Fleet.CRUISER || Fleet.policeTruck(v) || v.type == Fleet.CAR;
            gv.fill.setColor(police ? 0xFF22262E : 0xFF4E5A34);
            c.drawCircle(x - (float) Math.cos(a) * ring * 0.45f, -(float) Math.sin(a) * ring * 0.45f, ring * 0.45f, gv.fill);
        }
    }

    /** ((27.5) How far each motorcycle is leaning into its turn, worked out from how fast it is turning: {angle, anim, lean}.) */
    private final java.util.WeakHashMap<Fleet.Vehicle, float[]> bikeLean = new java.util.WeakHashMap<Fleet.Vehicle, float[]>();

    /** How far a motorcycle leans now (-1 hard left .. 1 hard right), eased in and out as it turns. */
    private float bikeLean(Fleet.Vehicle v) {
        float[] s = bikeLean.get(v);
        if (s == null) {
            bikeLean.put(v, new float[]{v.angle, v.anim, 0});
            return 0;
        }
        float dt = v.anim - s[1];
        if (dt > 0) {
            float da = v.angle - s[0];
            while (da > Math.PI) da -= (float) Math.PI * 2;
            while (da < -Math.PI) da += (float) Math.PI * 2;
            // (A jump - teleported, or long unseen - is no turn.)
            float target = dt > 0.5f || v.parked || v.crew.isEmpty() ? 0 : Math.max(-1, Math.min(1, da / dt * 0.55f));
            s[2] += (target - s[2]) * Math.min(1, dt * 7);
            s[0] = v.angle;
            s[1] = v.anim;
        }
        return s[2];
    }

    /**
     * ((27.5) A police motorcycle, drawn properly: its shadow, two tyres, the frame, the tank in the force's colours
     * with its stripe, the seat, the fairing and windscreen, handlebars with mirrors, panniers and the strobes front
     * and back; the officer astride it in a helmet in police colours, arms out to the bars, leaning into the turns;
     * on a call the red and blue flashing with a glow on the road. Zoomed out, just its shape and the lights.
     */
    void drawBike(Canvas c, Fleet.Vehicle v) {
        Country land = gv.world.city.country;
        int paint = v.broken ? 0xFF6A6A6A : land.cruiserBody == 0xFF1C1D22 ? 0xFFF2F2F2 : land.cruiserBody;
        int stripe = land.cruiserDoor == paint ? 0xFF23408E : land.cruiserDoor;
        boolean ridden = !v.crew.isEmpty() && !v.parked, lights = v.lightsOn();
        boolean blink = ((int) (v.anim * 8)) % 2 == 0;
        float lean = bikeLean(v);
        // Its shadow, down and to the right like every other vehicle's (wider with the rider up on it).
        c.save();
        c.translate(v.x + 1.6f, v.y + 2.2f);
        c.rotate((float) Math.toDegrees(v.angle));
        gv.fill.setColor(0x48000000);
        gv.oval.set(-5.4f, ridden ? -2.3f : -1.9f, 5.4f, ridden ? 2.3f : 1.9f);
        c.drawRoundRect(gv.oval, 1.8f, 1.8f, gv.fill);
        c.restore();
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));
        if (gv.scale < 0.9f) {
            // Zoomed out: the bike and rider as a shape, and the lights.
            gv.fill.setColor(paint);
            gv.oval.set(-4.8f, -1.4f, 4.8f, 1.4f);
            c.drawRoundRect(gv.oval, 1.2f, 1.2f, gv.fill);
            if (ridden) {
                gv.fill.setColor(0xFF1E2A4A);
                c.drawCircle(-0.8f, 0, 1.6f, gv.fill);
            }
            if (lights) {
                gv.fill.setColor(blink ? 0xFFFF3A30 : 0xFF3A7BFF);
                c.drawCircle(4.4f, 0, 1.1f, gv.fill);
            }
            c.restore();
            return;
        }
        // Tyres, front and back, and the frame between them.
        gv.fill.setColor(0xFF111214);
        gv.oval.set(-5.3f, -0.8f, -2.5f, 0.8f);
        c.drawRoundRect(gv.oval, 0.7f, 0.7f, gv.fill);
        gv.oval.set(2.7f, -0.7f, 5.3f, 0.7f);
        c.drawRoundRect(gv.oval, 0.6f, 0.6f, gv.fill);
        gv.fill.setColor(0xFF3A3C40);
        c.drawRect(-4.6f, -0.2f, -3.2f, 0.2f, gv.fill);
        c.drawRect(3.4f, -0.2f, 4.7f, 0.2f, gv.fill);
        gv.fill.setColor(0xFF2A2C30);
        c.drawRect(-3.2f, -0.8f, 3.0f, 0.8f, gv.fill);
        // The panniers either side of the back wheel, in the force's colours, a strobe on each.
        gv.fill.setColor(paint);
        gv.oval.set(-4.7f, -2.5f, -2.0f, -1.1f);
        c.drawRoundRect(gv.oval, 0.5f, 0.5f, gv.fill);
        gv.oval.set(-4.7f, 1.1f, -2.0f, 2.5f);
        c.drawRoundRect(gv.oval, 0.5f, 0.5f, gv.fill);
        gv.fill.setColor(stripe);
        c.drawRect(-4.0f, -2.5f, -3.3f, -1.1f, gv.fill);
        c.drawRect(-4.0f, 1.1f, -3.3f, 2.5f, gv.fill);
        // The tank in front of the seat, the stripe across it, and the fairing round the front.
        gv.fill.setColor(paint);
        gv.oval.set(-0.6f, -1.45f, 3.4f, 1.45f);
        c.drawOval(gv.oval, gv.fill);
        gv.fill.setColor(0x30FFFFFF);
        gv.oval.set(0.2f, -1.2f, 2.8f, -0.3f);
        c.drawOval(gv.oval, gv.fill);
        gv.fill.setColor(stripe);
        c.drawRect(0.9f, -1.42f, 1.5f, 1.42f, gv.fill);
        gv.fill.setColor(v.broken ? 0xFF555555 : 0xFF99B8D0);
        gv.oval.set(2.9f, -1.2f, 4.0f, 1.2f);
        c.drawOval(gv.oval, gv.fill);
        // The seat (and a pillion pad behind it).
        gv.fill.setColor(0xFF15161A);
        gv.oval.set(-3.1f, -1.05f, 0.2f, 1.05f);
        c.drawRoundRect(gv.oval, 0.9f, 0.9f, gv.fill);
        // The handlebars, grips and mirrors.
        gv.stroke.setColor(0xFF1A1B1E);
        gv.stroke.setStrokeWidth(0.45f);
        c.drawLine(2.3f, -2.5f, 2.6f, 2.5f, gv.stroke);
        gv.fill.setColor(0xFF0C0C0E);
        c.drawRect(2.0f, -2.9f, 2.7f, -2.3f, gv.fill);
        c.drawRect(2.2f, 2.3f, 2.9f, 2.9f, gv.fill);
        gv.fill.setColor(0xFFB8BEC4);
        c.drawCircle(2.9f, -3.1f, 0.35f, gv.fill);
        c.drawCircle(3.1f, 3.1f, 0.35f, gv.fill);
        // The strobes: a pair either side of the headlamp, and on the panniers' poles at the back.
        int red = lights ? (blink ? 0xFFFF3A30 : 0xFF7A2420) : 0xFF6A2420, blue = lights ? (blink ? 0xFF2A3C7A : 0xFF4A8AFF) : 0xFF22346A;
        gv.fill.setColor(lights ? 0xFFFFF4C8 : 0xFFD8D4C0);
        c.drawCircle(4.35f, 0, 0.45f, gv.fill);
        gv.fill.setColor(red);
        c.drawCircle(4.1f, -0.95f, 0.38f, gv.fill);
        c.drawCircle(-4.85f, -1.8f, 0.42f, gv.fill);
        gv.fill.setColor(blue);
        c.drawCircle(4.1f, 0.95f, 0.38f, gv.fill);
        c.drawCircle(-4.85f, 1.8f, 0.42f, gv.fill);
        if (ridden) {
            // The officer: leaning into the turn, arms out to the grips, in a helmet with the force's band round it.
            float ly = lean * 1.4f;
            int jacket = v.crew.get(0).body != 0 ? v.crew.get(0).body : 0xFF23408E;
            gv.stroke.setColor(City.darken(jacket, 0.9f));
            gv.stroke.setStrokeWidth(0.55f);
            c.drawLine(-1.0f, -1.4f + ly * 0.6f, 2.2f, -2.5f, gv.stroke);
            c.drawLine(-1.0f, 1.4f + ly * 0.6f, 2.4f, 2.5f, gv.stroke);
            gv.fill.setColor(jacket);
            gv.oval.set(-2.4f, -1.75f + ly * 0.6f, 0.3f, 1.75f + ly * 0.6f);
            c.drawOval(gv.oval, gv.fill);
            // Hi-vis across the shoulders.
            gv.fill.setColor(0xFFD8E84A);
            c.drawRect(-1.5f, -1.6f + ly * 0.6f, -1.0f, 1.6f + ly * 0.6f, gv.fill);
            gv.fill.setColor(0xFFF4F4F2);
            c.drawCircle(-0.8f, ly, 1.12f, gv.fill);
            // The band round the helmet, and a stripe over its crown front to back.
            gv.stroke.setColor(stripe == 0xFFEDEDED ? 0xFF23408E : stripe);
            gv.stroke.setStrokeWidth(0.28f);
            c.drawCircle(-0.8f, ly, 0.92f, gv.stroke);
            gv.fill.setColor(stripe == 0xFFEDEDED ? 0xFF23408E : stripe);
            c.drawRect(-1.7f, -0.16f + ly, 0.1f, 0.16f + ly, gv.fill);
            gv.fill.setColor(0xFF1A1E26);
            gv.oval.set(-0.2f, -0.75f + ly, 0.35f, 0.75f + ly);
            c.drawOval(gv.oval, gv.fill);
        }
        if (lights) {
            // Their glow on the road round it.
            gv.fill.setColor(blink ? 0x38FF4A3A : 0x384A8AFF);
            c.drawCircle(4.1f, blink ? -0.95f : 0.95f, 1.7f, gv.fill);
            c.drawCircle(-4.85f, blink ? -1.8f : 1.8f, 1.4f, gv.fill);
        }
        c.restore();
    }

    void drawHeli(Canvas c, Fleet.Vehicle v) {
        boolean rescue = v.kind == Fleet.K_RESCUE_HELI;
        boolean police = v.kind == Fleet.K_POLICE_HELI || rescue;
        if (rescue && v.team != null) {
            // The winch line down to anyone on it.
            for (int i = 0; i < v.team.size(); i++) {
                Entity s = v.team.get(i);
                if (s.dead || s.roping <= 0) continue;
                gv.stroke.setColor(0xE0E8E8E0);
                gv.stroke.setStrokeWidth(0.7f);
                c.drawLine(v.x, v.y, s.x, s.y, gv.stroke);
            }
        }
        float alt = v.alt;
        gv.fill.setColor(alpha(0x40000000, 0.5f + 0.5f * (1 - alt)));
        c.drawCircle(v.x + 4 + 16 * alt, v.y + 5 + 21 * alt, 11 - 2 * alt, gv.fill);
        c.save();
        c.translate(v.x, v.y);
        c.rotate((float) Math.toDegrees(v.angle));
        // Bigger when it's high (closer to the camera), squashed sideways when it banks.
        float sz = 0.85f + 0.3f * alt;
        c.scale(sz, sz * (1 - Math.min(0.3f, Math.abs(v.bank) * 0.25f)));
        gv.stroke.setColor(police ? 0xFF1F3F8A : 0xFF3E4A2C);
        gv.stroke.setStrokeWidth(2.4f);
        c.drawLine(-6, 0, -20, 0, gv.stroke);
        c.drawLine(-19, -3, -19, 3, gv.stroke);
        // Skids.
        gv.stroke.setColor(0xFF2A2E22);
        gv.stroke.setStrokeWidth(0.9f);
        c.drawLine(-6, -5.5f, 7, -5.5f, gv.stroke);
        c.drawLine(-6, 5.5f, 7, 5.5f, gv.stroke);
        gv.fill.setColor(police ? 0xFFF2F2F2 : 0xFF4F5E36);
        gv.oval.set(-8, -5, 9, 5);
        c.drawOval(gv.oval, gv.fill);
        if (rescue) {
            // Rescue colours: red and white, the winch arm out of the door.
            gv.fill.setColor(0xFFD8302A);
            c.drawRect(-7, -1.6f, 6, 1.6f, gv.fill);
            gv.fill.setColor(0xFFF2F2F2);
            c.drawRect(-1, -1.6f, 1, 1.6f, gv.fill);
            gv.stroke.setColor(0xFF3A3A3A);
            gv.stroke.setStrokeWidth(1);
            c.drawLine(1, 4.5f, 1, 7.5f, gv.stroke);
        } else if (police) {
            // Police colours down the side.
            gv.fill.setColor(0xFF1F3F8A);
            c.drawRect(-7, -1.2f, 6, 1.2f, gv.fill);
        }
        gv.fill.setColor(0xFF7FA3B8);
        gv.oval.set(3, -3.2f, 9, 3.2f);
        c.drawOval(gv.oval, gv.fill);
        boolean blink = ((int) (v.anim * 3)) % 2 == 0;
        gv.fill.setColor(blink ? 0xFFFF3A30 : 0xFF3AFF6A);
        c.drawCircle(-19, 0, 0.9f, gv.fill);
        // Rotor: a blurred disc when spinning fast, separate blades while spooling up or landing.
        // (Still on the pad, idling while the team's out, or spinning up before take-off.)
        float spin = v.state == Fleet.IDLE || v.state == Fleet.MUSTER ? 0 : v.state == Fleet.STANDBY && v.alt <= 0 ? 10
                : v.state == Fleet.SPOOL ? 2 + 28 * Math.max(0, 1 - v.timer / Math.max(1, v.spoolTime)) : 30;
        gv.fill.setColor(alpha(0x30000000, Math.min(1, spin / 30)));
        c.drawCircle(0, 0, 17, gv.fill);
        gv.stroke.setColor(0xB0202020);
        gv.stroke.setStrokeWidth(1.4f);
        float a = v.anim * spin;
        for (int k = 0; k < 2; k++) {
            float ca = (float) Math.cos(a + k * Math.PI / 2) * 17, sa = (float) Math.sin(a + k * Math.PI / 2) * 17;
            c.drawLine(-ca, -sa, ca, sa, gv.stroke);
        }
        c.restore();
    }

    /** A boat under way, with its wake. */
    void drawBoat(Canvas c, float[] b) {
        int kind = (int) b[5];
        float len = kind == 0 ? 10 : kind == 3 ? 13 : 16, wid = kind == 0 ? 3.5f : 5;
        c.save();
        c.translate(b[0], b[1]);
        c.rotate((float) Math.toDegrees(Math.atan2(b[3], b[2])));
        // The wake fanning out behind.
        gv.fill.setColor(0x40F2F6F8);
        for (int k = 1; k <= 4; k++) {
            float wx = -len - k * 5 * (kind == 0 ? 0.5f : 1), sp = wid * 0.6f + k * 1.6f;
            c.drawRect(wx, -sp, wx + 3, -sp + 0.8f, gv.fill);
            c.drawRect(wx, sp - 0.8f, wx + 3, sp, gv.fill);
        }
        gv.fill.setColor(0x30000000);
        gv.boatRect.set(-len + 1.5f, -wid + 2, len + 1.5f, wid + 2);
        c.drawOval(gv.boatRect, gv.fill);
        android.graphics.Path hull = new android.graphics.Path();
        hull.moveTo(-len, -wid);
        hull.lineTo(len * 0.5f, -wid);
        hull.lineTo(len, 0);
        hull.lineTo(len * 0.5f, wid);
        hull.lineTo(-len, wid);
        hull.close();
        gv.fill.setColor(kind == 0 ? 0xFF9A6A3E : kind == 3 ? 0xFF2A3A4E : kind == 2 ? 0xFFE8E4DA : 0xFFF2F2F2);
        c.drawPath(hull, gv.fill);
        if (kind == 0) {
            gv.fill.setColor(0xFF6E4A2A);
            c.drawRect(-2, -wid, 0, wid, gv.fill);
        } else if (kind == 2) {
            gv.fill.setColor(0xFFF8F8F4);
            android.graphics.Path sail = new android.graphics.Path();
            sail.moveTo(0, 0);
            sail.lineTo(-len * 0.7f, -wid - 7);
            sail.lineTo(-len * 0.7f, 0);
            sail.close();
            c.drawPath(sail, gv.fill);
        } else {
            gv.fill.setColor(kind == 3 ? 0xFFE0A030 : 0xFF2A4A6A);
            c.drawRect(-len * 0.4f, -wid + 1.5f, len * 0.15f, wid - 1.5f, gv.fill);
            if (kind == 1) {
                gv.fill.setColor(0xFFD83A3A);
                c.drawRect(-len, -wid, len * 0.5f, -wid + 1, gv.fill);
            }
        }
        c.restore();
    }
}
