package com.instantaxtion.zombiesandbox;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.instantaxtion.zombiesandbox.City.*;

/**
 * (27.3) The airport: its grounds, runway and terminal on the edge of the big cities. Taken out of City.java as it
 * stood; the city itself (its tiles, buildings and everything the game asks of it) is `city`.
 */
final class AirportMap extends CityPart {
    AirportMap(City city) {
        super(city);
    }

    /**
     * (15.10) The airport: a fence round tarmac, the runway along the far side, the terminal by the gate onto the road,
     * the control tower, and airliners parked at their stands.
     */
    void airportGrounds(int x0, int y0, int x1, int y1) {
        city.fill(x0, y0, x1 - x0, y1 - y0, LOT);
        for (int x = x0; x < x1; x++) {
            city.tiles[y0 * city.w + x] = FENCE;
            city.tiles[(y1 - 1) * city.w + x] = FENCE;
        }
        for (int y = y0; y < y1; y++) {
            city.tiles[y * city.w + x0] = FENCE;
            city.tiles[y * city.w + x1 - 1] = FENCE;
        }
        city.airport = new int[]{x0, y0, x1, y1};
        // The gate: in the fence onto the road below, at the terminal's end.
        int gx = x1 - 14;
        for (int x = gx; x < gx + 4; x++) city.tiles[(y1 - 1) * city.w + x] = LOT;
        // The runway along the top, the full length.
        float rx0 = (x0 + 3) * T, rx1 = (x1 - 3) * T, ry0 = (y0 + 3) * T, ry1 = (y0 + 10) * T;
        city.baseDecor.add(new float[]{BD_RUNWAY, rx0, ry0, rx1, ry1});
        city.runway = new float[]{rx0, ry0, rx1, ry1};
        // The terminal by the gate, the control tower beside it.
        // (The terminal to the west of the gate, the control tower to the east: the way in from the gate open between.)
        int th = Math.max(5, Math.min(8, (y1 - y0) / 4));
        gen.govLot(gx - 24, y1 - 2 - th, 22, th, JAIL);
        lotGen.addFacilityLot(gx + 7, y1 - 2 - th, 3, 3, TOWER, 6);
        city.apron = new float[]{(gx - 13) * T, (y1 - 4 - th - 4) * T};
        city.gateArea = new float[]{(gx + 2) * T, (y1 - 5) * T};
        // Airliners at their stands along the apron.
        for (int k = 0; k < 3; k++) {
            float px = (x0 + 8 + k * 14) * T, py = (y1 - 4 - th - 6) * T;
            if (px > (x1 - 34) * T) break;
            city.baseDecor.add(new float[]{BD_AIRLINER, px, py, px + 9 * T, py + 6 * T});
        }
    }
}
