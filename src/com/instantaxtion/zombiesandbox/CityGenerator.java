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
 * (27.3) Builds the map: the street grid, the blocks, the districts, the facilities and landmarks, step by step
 * (generate), and the helpers every part of the building shares. Taken out of City.java as it stood; the city itself
 * (its tiles, buildings and everything the game asks of it) is `city`.
 */
final class CityGenerator extends CityPart {
    CityGenerator(City city) {
        super(city);
    }

    /** ((22.9) Desert City: the casino strip, the first great street across town. */
    Street stripSt;
    /** ((22.9) Desert City: the army base out in the desert, with its test range ({x0, y0, x1, y1} in tiles). */
    int[] testRange;
    /**
     * ((26.0) The Space Center: the space center's ground ({x0, y0, x1, y1} in tiles, the avenue along the staff town
     * included), which side of the map it's on (0 left, 1 right, 2 top, 3 bottom), the staff town's ground, and the
     * places laid out in it, {x0, y0, x1, y1} with the design's name, put up once the streets are in.
     */
    int[] scRect;
    int[] scTown;
    /** The countryside around a town, or -1. */
    int countryDistrict = -1;
    /** The district of the block currently being filled in. */
    int curDistrict = DT_MIDTOWN;

    /** Scatters district centres over the map, gives each a kind and a name, and assigns every tile to one. */
    void makeDistricts() {
        // Only the town is split into districts; any countryside around it is one area of its own.
        float core = city.cfg.coreFraction();
        int m = core < 0.99f ? (int) (city.w * (1 - core) / 2) : 0;
        int n = 3 + (city.w - 2 * m) / 32;
        if (look != null) n = Math.max(3, n + look.moreDistricts);
        // (Since 10.10 the town sits off to one side of the map: see placeTown.)
        int ox = shiftX, oy = shiftY;
        // (Only the land is split into districts.)
        int landH = city.seaY > 0 ? city.seaY : city.h;
        Random r = city.rnd;
        List<String> words = new ArrayList<String>(Arrays.asList(city.country.districtWords));
        java.util.Collections.shuffle(words, r);
        int word = 0;
        boolean[] usedUnique = new boolean[DT_COUNT];
        for (int k = 0; k < n; k++) {
            District d = new District();
            // The first district sits in the middle of town; the rest spread out with some space between them.
            float bestX = city.w / 2f + ox, bestY = landH / 2f + oy;
            if (city.islandRects != null) {
                // (The heart of the first island's town.)
                int[] r0 = city.islandRects[0];
                bestX = (r0[0] + r0[2]) / 2f;
                bestY = (r0[1] + r0[3]) / 2f;
            }
            // By the sea, the second district is the waterfront: the beach, or the docks.
            boolean front = k == 1 && city.seaY > 0;
            if (front) {
                bestX = city.w * (0.35f + r.nextFloat() * 0.3f);
                bestY = city.seaY - Math.max(14, city.w / 14f);
            } else if (city.themePark && k == 1) {
                // ((25.0) The staff town on the mainland.)
                int[] ir = city.islandRects[1];
                bestX = (ir[0] + ir[2]) / 2f;
                bestY = (ir[1] + ir[3]) / 2f;
            } else if (city.spaceCenter && k == 0) {
                // ((26.0) The first district the space center itself.)
                bestX = (scRect[0] + scRect[2]) / 2f;
                bestY = (scRect[1] + scRect[3]) / 2f;
            } else if (k > 0) {
                float bestScore = -1;
                for (int tries = 0; tries < 30; tries++) {
                    float x = m + ox + 6 + r.nextFloat() * (city.w - 2 * m - 12), y = m + oy + 6 + r.nextFloat() * (landH - 2 * m - 12);
                    if (city.spaceCenter) {
                        // ((26.0) The rest in the staff town.)
                        int[] st = scTown;
                        x = st[0] + 4 + r.nextFloat() * Math.max(1, st[2] - st[0] - 8);
                        y = st[1] + 4 + r.nextFloat() * Math.max(1, st[3] - st[1] - 8);
                    }
                    if (city.islandRects != null) {
                        // ((25.0) Theme Park Island: the rest on the island.)
                        int[] ir = city.islandRects[city.themePark ? 0 : (k + tries) % city.islandRects.length];
                        x = ir[0] + 6 + r.nextFloat() * (ir[2] - ir[0] - 12);
                        y = ir[1] + 6 + r.nextFloat() * (ir[3] - ir[1] - 12);
                    }
                    float near = Float.MAX_VALUE;
                    for (District o : city.districts) near = Math.min(near, (float) World.hypot(o.x - x, o.y - y));
                    if (near > bestScore) {
                        bestScore = near;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
            // ((27.5) Downtown off-centre, a different way in every city.)
            if (look != null && k == 0 && city.islandRects == null && !city.spaceCenter) {
                bestX = Math.max(m + 12, Math.min(city.w - m - 12, bestX + look.dtX * (city.w - 2 * m)));
                bestY = Math.max(m + 12, Math.min(landH - m - 12, bestY + look.dtY * (landH - 2 * m)));
            }
            // ((28.0) Where the map's own builder wants it.)
            if (special != null) {
                float[] at = special.districtSpot(k, bestX, bestY, r);
                bestX = at[0];
                bestY = at[1];
            }
            d.x = bestX;
            d.y = bestY;
            float dist = (float) World.hypot(d.x - city.w / 2f - ox, d.y - city.h / 2f - oy) / (city.w * 0.5f);
            // ((27.5) Measured from downtown, wherever it is, and its spread the city's own.)
            if (look != null && k > 0) dist = (float) World.hypot(d.x - city.districts.get(0).x, d.y - city.districts.get(0).y) / (city.w * 0.5f) / look.core;
            if (k == 0) d.type = city.cfg.coreDistrict();
            else if (front) d.type = city.waterKind == CityConfig.W_HARBOUR ? DT_INDUSTRIAL : DT_MIDTOWN;
            else {
                // Towers towards the middle, homes and parks towards the edge of town.
                float[] wts = new float[DT_COUNT];
                float total = 0;
                for (int t = 0; t < DT_COUNT; t++) {
                    float wt = city.cfg.districtWeight(t);
                    if (t == DT_DOWNTOWN) wt *= Math.max(0.1f, 1.4f - dist * 1.5f);
                    if (t == DT_SUBURB || t == DT_PARKSIDE) wt *= 0.4f + dist;
                    if (t == DT_INDUSTRIAL) {
                        float ang = (float) Math.atan2(d.y - city.h / 2f, d.x - city.w / 2f) - industryAngle;
                        while (ang > Math.PI) ang -= Math.PI * 2;
                        while (ang < -Math.PI) ang += Math.PI * 2;
                        wt *= Math.abs(ang) < 1f ? 2.5f : 0.4f;
                    }
                    if ((t == DT_OLDTOWN || t == DT_CAMPUS) && usedUnique[t]) wt *= 0.3f;
                    if (look != null) wt *= look.weight(t);
                    wts[t] = wt;
                    total += wt;
                }
                float pick = r.nextFloat() * total;
                int t = 0;
                while (t < DT_COUNT - 1 && pick >= wts[t]) pick -= wts[t++];
                d.type = t;
                // ((25.0) The staff town: homes.)
                if (city.themePark && k == 1) d.type = DT_SUBURB;
                // ((26.1) Each island its own kind of place.)
                if (city.islandRects != null && !city.themePark && city.cfg.distinctIslands()) d.type = islandsMap.islandDistrict(city.island((int) d.x, (int) d.y), d.type);
                if (special != null) d.type = special.districtType(k, d.x, d.y, d.type);
            }
            usedUnique[d.type] = true;
            String wd = words.get(word++ % words.size());
            if (city.spaceCenter && k == 0) {
                d.name = "Space Center";
                city.districts.add(d);
                continue;
            }
            // ((11.5) More of the names an American city's districts go by.)
            if (city.cfg.civic >= 28 && !front) {
                String[] opts;
                switch (d.type) {
                    case DT_DOWNTOWN: opts = usedUnique[DT_COUNT - 1] ? new String[]{wd + " Center", "Uptown", "The Financial District"} : new String[]{"Downtown"}; break;
                    case DT_MIDTOWN: opts = new String[]{wd + " Village", wd + " Square", wd + " Crossing", "Midtown", wd + " Junction"}; break;
                    case DT_OLDTOWN: opts = new String[]{"Old Town", "The Old Quarter", "The Historic District", wd + " Historic District"}; break;
                    case DT_SUBURB: opts = new String[]{wd + " Heights", wd + " Hills", wd + " Gardens", wd + " Grove", wd + " Meadows", wd + " Ridge", wd + " Estates", wd + " Woods"}; break;
                    case DT_INDUSTRIAL: opts = new String[]{wd + " Industrial Park", wd + " Works", wd + " Rail Yards", "The Warehouse District", wd + " Flats"}; break;
                    case DT_CAMPUS: opts = new String[]{"University District", "College Hill"}; break;
                    default: opts = new String[]{wd + " Common", wd + " Green", wd + " Fields", wd + " Park"}; break;
                }
                d.name = opts[r.nextInt(opts.length)];
                for (District o : city.districts) if (o.name.equals(d.name)) d.name = wd + " " + DISTRICT_KINDS[d.type];
                if (d.type == DT_DOWNTOWN) usedUnique[DT_COUNT - 1] = true;
                city.districts.add(d);
                continue;
            }
            switch (d.type) {
                case DT_DOWNTOWN: d.name = usedUnique[DT_COUNT - 1] ? wd + " Center" : "Downtown"; break;
                case DT_MIDTOWN: d.name = r.nextBoolean() ? wd + " Village" : wd + " Square"; break;
                case DT_OLDTOWN: d.name = r.nextBoolean() ? "Old Town" : "The Old Quarter"; break;
                case DT_SUBURB: d.name = wd + (new String[]{" Heights", " Hills", " Gardens", " Park", " Grove"})[r.nextInt(5)]; break;
                case DT_INDUSTRIAL: d.name = r.nextBoolean() ? wd + " Industrial Estate" : wd + " Works"; break;
                case DT_CAMPUS: d.name = "University District"; break;
                default: d.name = wd + " Common"; break;
            }
            if (d.type == DT_DOWNTOWN) usedUnique[DT_COUNT - 1] = true;
            if (front) d.name = wd + (city.waterKind == CityConfig.W_HARBOUR ? " Docks" : " Beach");
            d.name = city.country.districtName(d.name, wd, d.type);
            for (District o : city.districts) if (o.name.equals(d.name)) d.name = city.country.districtName(wd + " " + DISTRICT_KINDS[d.type], wd + " " + (k + 1), -1);
            city.districts.add(d);
        }
        int country = -1;
        if (m > 0) {
            District d = new District();
            d.type = DT_PARKSIDE;
            String cw = words.get(word % words.size());
            d.name = city.country.districtName(cw + (r.nextBoolean() ? " Valley" : " Vale"), cw, -2);
            word++;
            country = city.districts.size();
            countryDistrict = country;
            city.districts.add(d);
        }
        // Every tile belongs to its nearest centre, with wobbly borders.
        city.districtAt = new OffHeap.Bytes(city.w * city.h);
        float[] sx = new float[city.districts.size()], sy = new float[city.districts.size()];
        // ((16.2) The wobble along each row and down each column, and each centre's place and weight, worked out once
        // rather than for every tile of the map: the same sums, a good part of building a big city.)
        float[] wobX = new float[city.h], wobY = new float[city.w], kx = new float[n], ky = new float[n], kw = new float[n];
        for (int y = 0; y < city.h; y++) wobX[y] = (float) Math.sin(y * 0.21f + city.cfg.seed) * 3;
        for (int x = 0; x < city.w; x++) wobY[x] = (float) Math.sin(x * 0.17f + city.cfg.seed * 0.7f) * 3;
        for (int k = 0; k < n; k++) {
            District d = city.districts.get(k);
            kx[k] = d.x;
            ky[k] = d.y;
            // Downtown is compact; suburbs sprawl.
            kw[k] = d.type == DT_DOWNTOWN ? 1.25f : d.type == DT_SUBURB ? 0.85f : 1f;
            // ((27.5) A big sprawling downtown in one city, a tight little core in the next.)
            if (look != null && d.type == DT_DOWNTOWN) kw[k] = 1.25f / look.core;
        }
        int prevBest = 0;
        for (int y = 0; y < city.h; y++)
            for (int x = 0; x < city.w; x++) {
                if (country >= 0 && (x < city.townX0 || y < city.townY0 || x >= city.townX1 || y >= city.townY1)) {
                    city.districtAt.set(y * city.w + x, (byte) (country));
                    city.districts.get(country).tiles++;
                    continue;
                }
                float wx = x + wobX[y], wy = y + wobY[x];
                // ((22.0) Starting from the district of the tile before, those too far off across alone are passed
                // over: the same answer (the nearest, the first of any tied), for a fraction of the looking.)
                int best = prevBest;
                float bd = ((kx[best] - wx) * (kx[best] - wx) + (ky[best] - wy) * (ky[best] - wy)) * kw[best];
                for (int k = 0; k < n; k++) {
                    float weight = kw[k], ax = (kx[k] - wx) * (kx[k] - wx) * weight;
                    if (ax > bd) continue;
                    float dd = ((kx[k] - wx) * (kx[k] - wx) + (ky[k] - wy) * (ky[k] - wy)) * weight;
                    if (dd < bd || dd == bd && k < best) {
                        bd = dd;
                        best = k;
                    }
                }
                prevBest = best;
                city.districtAt.set(y * city.w + x, (byte) (best));
                if (y >= landH || (city.islandRects != null && city.island(x, y) < 0)) continue;
                sx[best] += x;
                sy[best] += y;
                city.districts.get(best).tiles++;
            }
        for (int k = 0; k < n; k++) {
            District d = city.districts.get(k);
            if (d.tiles == 0) continue;
            d.cx = (sx[k] / d.tiles + 0.5f) * T;
            d.cy = (sy[k] / d.tiles + 0.5f) * T;
        }
        if (country >= 0) {
            // Its name goes out in the fields, to one side of town.
            District d = city.districts.get(country);
            // (On the wide side, if the town is off to one side.)
            boolean left = city.townX0 >= city.w - city.townX1, top = city.townY0 > city.h - city.townY1, across = city.townY0 != city.h - city.townY1;
            d.cx = (across ? city.w / 2f : left ? city.townX0 / 2f : (city.townX1 + city.w) / 2f) * T;
            d.cy = (!across ? city.h / 2f : top ? city.townY0 / 2f : (city.townY1 + city.h) / 2f) * T;
        }
    }
    /** Farm fields (cities since 10.26): kept clear of the woods the terrain grows round them. */
    boolean[] fieldTile;

    /** Builds a Building from a lot: its door, room for people, toughness, name, food and stock. */
    Building createBuilding(int[] l) {
        {
            float height = l[7] * FLOOR + 3;
            city.maxHeight = Math.max(city.maxHeight, height);
            Building b = new Building(l[0] * T, l[1] * T, (l[0] + l[2]) * T, (l[1] + l[3]) * T, height,
                    l[4], l[8], l[5], l[6]);
            int k = l[6];
            b.variant = l.length > 9 ? l[9] : -1;
            int k2 = k;
            if (k2 == OFFICE || k2 == HOUSE || k == WAREHOUSE || k == SHOP || k == CHURCH || k == SCHOOL || k == MARKET
                    || k == KIOSK || k == APARTMENT || k == GARAGE || k == PHARMACY || k == TRAIN_STATION || k == MALL
                    || k == BARN || (k == STADIUM && !stadiumNamed) || k >= CITY_HALL) placeDoor(b, l);
            // (Nobody shelters in the cells.)
            if (k == JAIL) b.capacity = 0;
            // ((22.0) A mall takes in far more of those sheltering: its shops, its halls, its stores.)
            if (k == MALL) b.capacity = city.cfg.v22() ? 150 : 40;
            if (k == APARTMENT) b.capacity = Math.max(8, Math.min(40, l[2] * l[3] / 3));
            Random rr = new Random(l[5] * 31L + 7);
            if (b.capacity > 0) {
                // Two people to a flat, a flat for every two tiles of floor, on every floor.
                if (k == HOUSE) b.residents = 1 + rr.nextInt(4) + (l[2] * l[3] >= 24 ? 1 : 0);
                else if (k == APARTMENT) b.residents = Math.min(160, l[2] * l[3] * Math.max(1, l[7]) / 9);
                else if (k == SHOP && l[7] >= 2) b.residents = 1 + rr.nextInt(3);
                // Some office blocks are flats (and some old buildings have flats upstairs).
                else if (k == OFFICE && l[7] >= 2 && rr.nextFloat() < 0.2f) b.residents = Math.min(60, l[2] * l[3] * l[7] / 18);
                // Home is somewhere the whole household can shelter.
                b.capacity = Math.max(b.capacity, Math.min(b.residents, 40));
                city.totalResidents += b.residents;
            }
            // Bigger and taller buildings take more to bring down.
            b.maxHp = b.hp = 150 + l[2] * l[3] * 22 + l[7] * 60;
            Random nr = new Random(l[5]);
            if (k == CHURCH) { String[] ch = city.cfg.variety() ? city.country.churches37 : city.country.churches; b.name = ch[nr.nextInt(ch.length)]; }
            else if (k == SCHOOL) { String[] sc = city.cfg.variety() ? city.country.schools37 : city.country.schools; b.name = sc[nr.nextInt(sc.length)]; }
            else if (k == TRAIN_STATION) b.name = "Central Station";
            else if (k == MALL) b.name = city.name + " Mall";
            else if (k == ARMORY) {
                b.name = Country.guard()[0];
                city.armory = b;
            }
            else if (k >= CITY_HALL) {
                b.name = city.country.gov()[0][k - CITY_HALL];
                if (k == CITY_HALL) city.cityHall = b;
                else if (k == COURTHOUSE) city.courthouse = b;
                else if (k == JAIL) city.jail = b;
                else if (k == CALL_CENTRE) city.callCentre = b;
                else city.worksDepot = b;
            }
            else if (k == STADIUM && !stadiumNamed) {
                b.name = city.name + " Stadium";
                stadiumNamed = true;
            }
            else if (k == MARKET) {
                String[] mk = city.cfg.variety() ? city.country.markets37 : city.country.markets;
                b.name = mk[nr.nextInt(mk.length)];
                b.stock = 240;
            } else if (k == PHARMACY) {
                String[] ph = city.cfg.variety() ? city.country.pharmacies37 : city.country.pharmacies;
                b.name = ph[nr.nextInt(ph.length)];
                b.stock = 60;
            } else if (k == SHOP) {
                // Some shops are diners (and the rest are whatever kind of shop they are). ((16.9) No gun stores any more:
                // a few people own a gun, and that's all the guns there are in town. The one in twelve that was a gun
                // store is an ordinary shop - only the shop changes, so saved cities still rebuild street for street.)
                int roll = nr.nextInt(12);
                b.shopType = roll == 0 ? 0 : roll < 3 && b.variant < 0 ? 2 : 0;
            }
            // Food in the cupboards and on the shelves.
            b.food = k == MARKET ? 400 : k == MALL ? 300 : k == WAREHOUSE ? 150 : k == SHOP ? (b.shopType == 2 ? 120 : 40)
                    : k == APARTMENT ? 60 : k == HOUSE ? 25 : k == SCHOOL ? 80 : k == BARN ? 120 : 12;
            // ((11.5) Groceries and the like have a lot more; pawn shops and sporting goods some rounds.)
            int use = Variants.use(b.variant);
            if (use == Variants.U_FOOD) b.food += 160;
            else if (use == Variants.U_ROUNDS) b.stock = 60;
            String own = city.lotNames.get(l);
            if (own != null) {
                b.name = own;
                b.shopType = 0;
                b.stock = 0;
                b.variant = -1;
                Integer res = city.lotResidents.get(l);
                if (res != null && res >= 0) {
                    city.totalResidents += res - b.residents;
                    b.residents = res;
                    b.capacity = Math.max(b.capacity, Math.min(res, 40));
                }
            }
            int[] of = wingOf.get(l);
            if (of != null) {
                int mi = city.buildingAt.get(of[1] * city.w + of[0]);
                if (mi >= 0) b.main = city.buildings.get(mi);
            }
            for (int j = l[1]; j < l[1] + l[3]; j++)
                for (int i = l[0]; i < l[0] + l[2]; i++) city.buildingAt.set(j * city.w + i, city.buildings.size());
            city.buildings.add(b);
            return b;
        }
    }

    /**
     * (10.52) Not every building a box: some of the bigger homes, office and apartment blocks and warehouses are
     * L-shaped (a corner of the lot left as a yard or a paved court), and the biggest office and apartment blocks
     * U-shaped round a courtyard on one side. Each is built of two or three wings (each a building of its own as
     * far as the rest of the game goes), the same walls and roof, a wing now and then a floor lower.
     */
    void shapeLots() {
        int n = city.buildingLots.size();
        for (int i = 0; i < n; i++) {
            int[] l = city.buildingLots.get(i);
            int k = l[6];
            if (k != OFFICE && k != APARTMENT && k != HOUSE && k != WAREHOUSE) continue;
            if (city.lotNames.containsKey(l)) continue;
            // ((26.0) The Space Center's landmarks keep their shapes.)
            if (city.spaceCenter && l.length > 10 && l[10] >= 0) continue;
            int x0 = l[0], y0 = l[1], lw = l[2], lh = l[3];
            int min = k == HOUSE ? 4 : 6;
            if (lw < min || lh < min) continue;
            Random sr = new Random(l[5] * 131L + 24);
            if (sr.nextFloat() > (k == HOUSE ? 0.3f : 0.45f)) continue;
            byte yard = k == HOUSE ? GRASS : PLAZA;
            boolean u = k != HOUSE && lw >= 10 && lh >= 10 && sr.nextFloat() < 0.4f;
            if (!u) {
                // An L: a corner cut out.
                int cw = Math.max(2, lw * (35 + sr.nextInt(20)) / 100), ch = Math.max(2, lh * (35 + sr.nextInt(20)) / 100);
                int corner = sr.nextInt(4);
                boolean right = corner == 1 || corner == 3, bottom = corner >= 2;
                int cx = right ? x0 + lw - cw : x0, cy = bottom ? y0 + lh - ch : y0;
                city.fill(cx, cy, cw, ch, yard);
                // The long wing: the full depth of the lot, beside the cut.
                l[0] = right ? x0 : x0 + cw;
                l[2] = lw - cw;
                // The short one: the rest of the cut column.
                addWing(l, cx, bottom ? y0 : y0 + ch, cw, lh - ch, sr);
            } else {
                // A U: a courtyard into the middle of one side.
                int side = sr.nextInt(4);
                boolean across = side < 2;
                int len = across ? lw : lh, depth = across ? lh : lw;
                int nw = Math.max(3, len / 3), nd = Math.max(3, depth * (40 + sr.nextInt(15)) / 100);
                int a0 = (len - nw) / 2;
                int far = side == 0 || side == 2 ? 0 : depth - nd;
                if (across) city.fill(x0 + a0, y0 + far, nw, nd, yard);
                else city.fill(x0 + far, y0 + a0, nd, nw, yard);
                int back = side == 0 || side == 2 ? nd : 0;
                // The first arm keeps the lot's own entry; the other arm and the back join it.
                if (across) {
                    l[2] = a0;
                    addWing(l, x0 + a0 + nw, y0, lw - a0 - nw, lh, sr);
                    addWing(l, x0 + a0, y0 + back, nw, lh - nd, sr);
                } else {
                    l[3] = a0;
                    addWing(l, x0, y0 + a0 + nw, lw, lh - a0 - nw, sr);
                    addWing(l, x0 + back, y0 + a0, lw - nd, nw, sr);
                }
            }
        }
    }

    /** (10.52) Each wing's lot, and the lot of the building it's part of. */
    final java.util.IdentityHashMap<int[], int[]> wingOf = new java.util.IdentityHashMap<int[], int[]>();

    /** One more wing of a shaped building: like the lot it's part of, without its name or sign. */
    void addWing(int[] l, int x, int y, int ww, int wh, Random sr) {
        if (ww < 1 || wh < 1) return;
        int[] wing = l.clone();
        wing[0] = x;
        wing[1] = y;
        wing[2] = ww;
        wing[3] = wh;
        wing[5] = l[5] * 7 + 3 + city.buildingLots.size();
        if (l[6] != HOUSE && wing[7] > 2 && sr.nextFloat() < 0.5f) wing[7]--;
        if (wing.length > 9) wing[9] = -1;
        wingOf.put(wing, l);
        city.buildingLots.add(wing);
    }

    boolean stadiumNamed;

    /**
     * Emergency services spread across town: a block well away from every police station, base, hospital and
     * fire station already placed (or the furthest there is).
     */
    int spreadOut(List<Integer> options, int fallback) {
        float want = Math.max(28, city.w / 4f) * T;
        List<Integer> far = new ArrayList<Integer>();
        int best = fallback;
        float bestD = -1;
        for (int k : options) {
            int[] b = city.blocks.get(k);
            float cx = (b[0] + b[2]) / 2f * T, cy = (b[1] + b[3]) / 2f * T, d = Float.MAX_VALUE;
            for (Facility f : city.facilities) d = Math.min(d, (float) World.hypot(f.x - cx, f.y - cy));
            if (d >= want) far.add(k);
            if (d > bestD) {
                bestD = d;
                best = k;
            }
        }
        return far.isEmpty() ? best : far.get(city.rnd.nextInt(far.size()));
    }

    /** Nothing grows in a doorway: trees right outside a door are cleared, so the way in is always open. */
    void clearDoorways() {
        city.doorway = new boolean[city.w * city.h];
        for (Building b : city.buildings) city.clearDoorway(b);
    }

    /**
     * (10.33) Within margin tiles of the highway (across it), in a city built since 10.33: nothing joins it or
     * fronts onto it there but the slip roads.
     */
    boolean byHighway(int x, int y, int margin) {
        if (city.cfg.civic < 17 || city.oneWay == null || city.hwyAxis < 0) return false;
        int a = (city.hwyAxis == 0 ? y : x) - city.hwyAt;
        return a >= -margin && a <= 6 + margin;
    }

    void computeSolid() {
        city.nav = null;
        city.stepC = null;
        city.clear3 = null;
        city.clr = null;
        for (int i = 0; i < city.tiles.length; i++) {
            byte t = city.tiles[i];
            city.solid[i] = t == BUILDING || t == TREE || t == CAR || t == STATUE || t == FENCE || t == PUMP || (t == WATER && !city.isFord(i)) || t == ROCK;
            city.opaque[i] = t == BUILDING;
        }
    }

    /** Puts the door in the middle of the first side that opens onto walkable ground. */
    void placeDoor(Building b, int[] l) {
        int x = l[0], y = l[1], lw = l[2], lh = l[3];
        int[][] sides = {{x + lw / 2, y + lh}, {x + lw / 2, y - 1}, {x - 1, y + lh / 2}, {x + lw, y + lh / 2}};
        for (int[] s : sides) {
            if (city.solidTile(s[0], s[1])) continue;
            b.doorX = s[0] * T + T / 2f;
            b.doorY = s[1] * T + T / 2f;
            b.capacity = Math.max(3, Math.min(15, lw * lh / 2));
            return;
        }
    }

    /** Where the industrial district is: an angle around the centre (maps with warehouses on the edge). */
    float industryAngle;

    void carve(Street st) {
        for (int y = st.y0; y < st.y1; y++)
            for (int x = st.x0; x < st.x1; x++) {
                if (x < 0 || y < 0 || x >= city.w || y >= city.h) continue;
                int i = y * city.w + x;
                city.roadDir[i] = city.roadDir[i] != 0 ? 3 : (byte) (st.vertical ? 1 : 2);
                city.tiles[i] = ROAD;
                if (st.main) city.mainRoad[i] = true;
                if (st.width >= 7) {
                    if (city.boulevard == null) city.boulevard = new boolean[city.w * city.h];
                    city.boulevard[i] = true;
                }
            }
        city.streets.add(st);
    }

    /** Where streets have been laid out so far: centres (doubled) of north-south and east-west ones. */
    final List<Integer> vLines = new ArrayList<Integer>(), hLines = new ArrayList<Integer>();

    /**
     * Splits a rectangle of city with a street, then splits each side again, until the pieces are block
     * sized. Big pieces get main roads. Streets carry on straight across junctions: the second side continues
     * the first side's street, or a street lines up with one already laid elsewhere in town; only now and
     * then (more in the old town) does a street jog. Leaves become blocks. Returns the street's centre.
     */
    int split(int x0, int y0, int x1, int y1, int depth, int hintX, int hintY) {
        int bw = x1 - x0, bh = y1 - y0;
        float cx = (x0 + x1) / 2f - city.w / 2f - shiftX, cy = (y0 + y1) / 2f - city.h / 2f - shiftY;
        float d = (float) Math.sqrt(cx * cx + cy * cy) / (city.w * 0.5f);
        // ((27.5) How far out of town, measured from downtown wherever it is.)
        if (look != null && !city.districts.isEmpty())
            d = (float) World.hypot((x0 + x1) / 2f - city.districts.get(0).x, (y0 + y1) / 2f - city.districts.get(0).y) / (city.w * 0.5f);
        boolean rural = city.cfg.density() == 0 && city.cfg.style() == CityConfig.STYLE_HOUSES;
        // Each district has its own grain: small tight blocks downtown and in the old town, big ones in the
        // suburbs, on campus and in the industrial estates.
        int dt = city.districtType((x0 + x1) / 2, (y0 + y1) / 2);
        int maxLeaf;
        switch (dt) {
            case DT_DOWNTOWN: maxLeaf = 9 + city.rnd.nextInt(3); break;
            case DT_MIDTOWN: maxLeaf = 11 + city.rnd.nextInt(4); break;
            case DT_OLDTOWN: maxLeaf = 8 + city.rnd.nextInt(5); break;
            case DT_SUBURB: maxLeaf = 15 + city.rnd.nextInt(7); break;
            case DT_INDUSTRIAL: maxLeaf = 15 + city.rnd.nextInt(6); break;
            case DT_CAMPUS: maxLeaf = 15 + city.rnd.nextInt(5); break;
            default: maxLeaf = 13 + city.rnd.nextInt(6); break;
        }
        // On the biggest maps a stretch of suburb is laid out as a neighbourhood of its own, not more grid.
        // ((11.5) From the Large size up, and the old town too: not everything on the one grid.)
        boolean more = city.cfg.civic >= 28;
        // ((27.5) The city's great park, one big block of it with no streets across, where this city wants it.)
        if (look != null && look.bigPark && bigParkBlock == null && city.w >= 192 && depth >= 1 && bw >= 34 && bh >= 34 && bw <= 80 && bh <= 80
                && city.waterKind != CityConfig.W_ISLANDS && !city.desert) {
            float px = city.townX0 + look.parkX * (city.townX1 - city.townX0), py = city.townY0 + look.parkY * (city.townY1 - city.townY0);
            if (px >= x0 && px < x1 && py >= y0 && py < y1 && dt != DT_DOWNTOWN) {
                bigParkBlock = new int[]{x0, y0, x1, y1};
                city.blocks.add(bigParkBlock);
                return -1;
            }
        }
        if (look != null) {
            // ((27.5) Map version 40: neighbourhoods on every size of map and in more kinds of district, as many as
            // the city leans to; the rest of the grid as fine or coarse as the city's own.)
            float p = dt == DT_SUBURB || dt == DT_PARKSIDE ? 0.75f : dt == DT_MIDTOWN ? (d > 0.4f ? 0.5f : 0.25f)
                    : dt == DT_OLDTOWN ? 0.4f : dt == DT_INDUSTRIAL ? 0.45f : dt == DT_CAMPUS ? 0.55f : 0f;
            // (Never more than a fifth or so of the town, so there are still the ordinary blocks for the landmarks and the rest.)
            boolean room = nbArea + bw * bh <= 0.22f * (city.townX1 - city.townX0) * (city.townY1 - city.townY0);
            if (city.w >= 160 && depth >= 1 && bw >= 38 && bh >= 38 && bw <= 76 && bh <= 76 && room
                    && look.r.nextFloat() < Math.min(0.95f, p * look.nbRate) && landGen.neighbourhood(x0, y0, x1, y1, d)) {
                nbArea += bw * bh;
                return -1;
            }
        } else if (city.w >= (more ? 300 : 400) && depth >= 1 && bw >= 44 && bh >= 44 && bw <= 76 && bh <= 76
                && city.rnd.nextFloat() < (dt == DT_SUBURB || dt == DT_PARKSIDE ? (more ? 0.9f : 0.8f) : dt == DT_MIDTOWN ? (d > 0.45f ? 0.55f : 0.2f)
                        : more && dt == DT_OLDTOWN ? 0.35f
                        : dt == DT_INDUSTRIAL ? 0.45f : 0f)
                && landGen.neighbourhood(x0, y0, x1, y1, d)) return -1;
        if (d > 0.8f) maxLeaf += 2;
        // Bigger blocks: fewer, longer streets, as in a real town.
        maxLeaf += 3;
        if (rural) maxLeaf += 6;
        if (city.cfg.density() == 2) maxLeaf -= 2;
        // ((15.2) Downtown Towers: smaller blocks still, so more and narrower streets between the towers.)
        if (city.cfg.towers()) maxLeaf -= 3;
        // Blocks are twice the size they used to be (in tiles): real buildings need room.
        maxLeaf *= 2;
        if (look != null) maxLeaf = Math.max(14, Math.round(maxLeaf * look.grain));
        if (bw <= maxLeaf && bh <= maxLeaf) {
            city.blocks.add(new int[]{x0, y0, x1, y1});
            return -1;
        }
        boolean big = Math.max(bw, bh) > (city.w > 220 ? 84 : 68) && depth < 3 && !rural;
        // (Since 10.28 more of the bigger streets are avenues: two lanes each way.)
        if (city.cfg.civic >= 12 && !rural && depth < 4 && Math.max(bw, bh) > (city.w > 220 ? 60 : 52)) big = true;
        // ((22.9) Desert City: wide roads, an avenue round nearly every few blocks.)
        if (city.desert && depth < 6 && Math.max(bw, bh) > 44) big = true;
        int roadW = big ? 5 : 3, minSide = 16;
        // ((11.1) The great cross-town routes, the first splits of a big town, are boulevards: three lanes each way.)
        if (city.cfg.civic >= 27 && big && depth <= 1 && Math.max(bw, bh) > 150) roadW = 7;
        // ((22.9) Desert City: the first street across town is the Strip, a boulevard whatever the size of the map.)
        if (city.desert && depth == 0 && stripSt == null) roadW = 7;
        // ((15.2) Downtown Towers: past the first cross-town routes, every street is a narrow one between the towers.)
        if (city.cfg.towers() && depth >= 2) {
            roadW = 3;
            minSide = 13;
        }
        boolean vertical;
        if (bw > bh * 1.3f) vertical = true;
        else if (bh > bw * 1.3f) vertical = false;
        else vertical = city.rnd.nextBoolean();
        int len = vertical ? bw : bh;
        if (len < minSide * 2 + roadW) {
            vertical = !vertical;
            len = vertical ? bw : bh;
            if (len < minSide * 2 + roadW) {
                city.blocks.add(new int[]{x0, y0, x1, y1});
                return -1;
            }
        }
        int lo = (vertical ? x0 : y0) + Math.max(minSide, (int) (len * 0.3f)), hi = (vertical ? x1 : y1) - roadW - Math.max(minSide, (int) (len * 0.3f));
        if (hi < lo) hi = lo;
        int at = lo + city.rnd.nextInt(hi - lo + 1);
        // Carry on the neighbouring street now and then, so some roads run on straight.
        // Streets carry on across junctions far more often than not (a jog at every crossing makes no sense).
        float align = city.cfg.layout() == 0 ? 0.97f : city.cfg.layout() == 1 ? 0.9f : 0.75f;
        // Old streets wander a little; downtown is laid out on a grid.
        if (dt == DT_OLDTOWN) align = 0.6f;
        else if (dt == DT_DOWNTOWN) align = Math.max(align, 0.95f);
        // Big-map suburbs grew a bit at a time: more T-junctions, fewer streets running dead straight.
        if (city.w >= 400 && (dt == DT_SUBURB || dt == DT_PARKSIDE)) align = Math.min(align, 0.55f);
        // (Since 10.24 streets line up across the junctions nearly everywhere; only the old town still wanders.)
        boolean lineUp = city.cfg.civic >= 9;
        if (lineUp) align = dt == DT_OLDTOWN ? 0.8f : 1f;
        // ((27.5) Some cities grew a street at a time: more of their streets jog at the junctions.)
        if (look != null && dt != DT_DOWNTOWN) align -= look.jog;
        // Lines are kept as street centres (doubled, so odd widths stay exact): a narrow street lines up
        // with the middle of a wide one, not with its edge.
        int hint = vertical ? hintX : hintY;
        int snapSide = lineUp ? 11 : minSide;
        int wideLo = (vertical ? x0 : y0) + snapSide, wideHi = (vertical ? x1 : y1) - roadW - snapSide;
        boolean hinted = false, tryLines;
        if (hint >= 0 && city.rnd.nextFloat() < align) {
            int h0 = (hint - roadW) / 2;
            if (h0 >= wideLo && h0 <= wideHi) {
                at = h0;
                hinted = true;
            }
            // (Before 10.24 a hint that didn't fit was simply dropped; now the street looks for another line.)
            tryLines = lineUp && !hinted && city.rnd.nextFloat() < align;
        } else tryLines = city.rnd.nextFloat() < align;
        if (tryLines) {
            // Line up with a street already laid out elsewhere in town, so streets run straight across
            // the map instead of jogging at every junction.
            List<Integer> lines = vertical ? vLines : hLines;
            int best = -1, bd = Integer.MAX_VALUE, mid = (lo + hi) / 2;
            for (int k = 0; k < lines.size(); k++) {
                int l = (lines.get(k) - roadW) / 2;
                if (l < wideLo || l > wideHi) continue;
                if (Math.abs(l - mid) < bd) {
                    bd = Math.abs(l - mid);
                    best = l;
                }
            }
            if (best >= 0) at = best;
        }
        (vertical ? vLines : hLines).add(at * 2 + roadW);
        Street st = vertical ? new Street(at, y0, at + roadW, y1, true, big) : new Street(x0, at, x1, at + roadW, false, big);
        carve(st);
        if (city.desert && depth == 0 && stripSt == null) stripSt = st;
        // The first side's own street, when it runs across this one, is carried on into the second side.
        int nextHint;
        if (vertical) {
            nextHint = split(x0, y0, at, y1, depth + 1, -1, hintY);
            boolean across = nextHint >= 0 && (nextHint & VERT) == 0;
            split(at + roadW, y0, x1, y1, depth + 1, -1, across ? nextHint : hintY);
        } else {
            nextHint = split(x0, y0, x1, at, depth + 1, hintX, -1);
            boolean across = nextHint >= 0 && (nextHint & VERT) != 0;
            split(x0, at + roadW, x1, y1, depth + 1, across ? nextHint & ~VERT : hintX, -1);
        }
        return (at * 2 + roadW) | (vertical ? VERT : 0);
    }

    /**
     * ((27.3) What generate's steps share: how wide the country round the town is (0 for none), the town's rectangle,
     * whether it is a village of houses (no big sites there), and the blocks already spoken for.)
     */
    private int m, bx0, by0, bx1, by1;
    private boolean rural;
    private boolean[] used;

    /** ((27.5) The city's own character, on map version 40 (null before, and on the maps laid out their own way). */
    CityCharacter look;
    /** ((27.5) How much of the town (in tiles) is laid out as neighbourhoods so far.) */
    int nbArea;
    /** ((27.5) The great park's block, or null. */
    int[] bigParkBlock;

    /** ((28.0) Bridge City, Swamp Bayou or Lighthouse Coast: the map's own builder (null on every other map). */
    WaterMap special;

    void generate() {
        special = WaterMap.of(city);
        look = city.cfg.civic >= 40 && !city.cfg.themePark() && !city.cfg.spaceCenter() ? new CityCharacter(city.cfg.seed) : null;
        placeTown();
        layStreets();
        layCountry();
        plantMedians();
        placeBases();
        placeServices();
        placeLandmarks();
        placeBigSites();
        fillBlocks();
        parkCars();
        nameStreets();
        finishGround();
    }

    /**
     * ((27.3) A step of generate.) Where the town stands: the water there is, and the town's rectangle - the whole
     * map, or a core off to one side with country round it.
     */
    private void placeTown() {
        city.origin = 0;
        Arrays.fill(city.tiles, SIDEWALK);
        industryAngle = city.rnd.nextFloat() * (float) Math.PI * 2;
        city.waterKind = city.cfg.water();
        if (city.waterKind == CityConfig.W_HARBOUR) city.seaY = city.h - Math.max(24, (int) (city.h * 0.18f));
        // ((15.6) A wider bay on the Prison Island, with room for the island out in it.)
        // ((15.8) Wider again, for a far bigger prison.)
        if (city.cfg.prisonIsland()) city.seaY = city.h - Math.max(70, (int) (city.h * 0.4f));
        // ((11.0) The four new water maps have the sea along the bottom too, a little narrower.)
        if (special != null) special.layout();
        else if (city.waterKind == CityConfig.W_ISLANDS) islandsMap.islandLayout();
        // Where the town is: the whole map, or a core surrounded by countryside.
        float core = city.cfg.coreFraction();
        m = core < 0.99f ? (int) (city.w * (1 - core) / 2) : 0;
        bx0 = m;
        by0 = m;
        bx1 = city.w - m;
        by1 = city.seaY > 0 ? city.seaY : city.h - m;
        if (city.cfg.nature() && m > 0) {
            // The town sits off to one side, leaving wide open country on the other for the hills and the park.
            Random sr = new Random(city.cfg.seed * 31L + 11);
            int side = sr.nextInt(4), shift = (int) (m * (city.cfg.civic >= 26 ? 0 : 0.55f));
            int dx = side == 0 ? -shift : side == 1 ? shift : 0, dy = side == 2 ? -shift : side == 3 ? shift : 0;
            bx0 += dx;
            bx1 += dx;
            by0 += dy;
            by1 += dy;
            shiftX = dx;
            shiftY = dy;
            wildSide = side == 0 ? 1 : side == 1 ? 0 : side == 2 ? 3 : 2;
            // (10.34) Every map its own shape: the town longer one way than the other, and off-centre the
            // other way too.
            if (city.cfg.civic >= 18 && city.waterKind == CityConfig.W_NONE && city.cfg.civic < 26) {
                // (Only ever narrower than the square, so there's still room round it for the highway and the ring road.)
                float squeeze = 0.78f + sr.nextFloat() * 0.22f;
                boolean across = sr.nextBoolean();
                int cx = (bx0 + bx1) / 2, cy = (by0 + by1) / 2;
                int hx = Math.round((bx1 - bx0) / 2f * (across ? squeeze : 1)), hy = Math.round((by1 - by0) / 2f * (across ? 1 : squeeze));
                int jitter = (int) ((sr.nextFloat() * 2 - 1) * m * 0.35f);
                if (side < 2) cy += jitter;
                else cx += jitter;
                bx0 = Math.max(6, cx - hx);
                bx1 = Math.min(city.w - 6, cx + hx);
                by0 = Math.max(6, cy - hy);
                by1 = Math.min(city.h - 6, cy + hy);
                shiftX = (bx0 + bx1) / 2 - city.w / 2;
                shiftY = (by0 + by1) / 2 - city.h / 2;
            }
            // ((27.5) Map version 40: the town wider than it's tall, or taller than it's wide, and off to one side,
            // a different way in every city (only ever smaller than the square, so the roads round it still fit).
            if (look != null && city.waterKind == CityConfig.W_NONE) {
                float squeeze = 0.86f + look.r.nextFloat() * 0.14f;
                boolean across = look.r.nextBoolean();
                int hx = Math.round((bx1 - bx0) / 2f * (across ? squeeze : 1)), hy = Math.round((by1 - by0) / 2f * (across ? 1 : squeeze));
                int cx = (bx0 + bx1) / 2 + (across ? Math.round((look.r.nextFloat() * 2 - 1) * ((bx1 - bx0) / 2f - hx)) : 0);
                int cy = (by0 + by1) / 2 + (across ? 0 : Math.round((look.r.nextFloat() * 2 - 1) * ((by1 - by0) / 2f - hy)));
                bx0 = cx - hx;
                bx1 = cx + hx;
                by0 = cy - hy;
                by1 = cy + hy;
                shiftX = (bx0 + bx1) / 2 - city.w / 2;
                shiftY = (by0 + by1) / 2 - city.h / 2;
            }
        }
        city.townX0 = bx0;
        city.townY0 = by0;
        city.townX1 = bx1;
        city.townY1 = by1;
        // ((26.0) The Space Center: which side of town the space center takes, before the districts are laid out.)
        if (city.spaceCenter) spaceMap.spaceCenterSplit(bx0, by0, bx1, by1);
    }

    /**
     * ((27.3) A step of generate.) The districts, then the streets: the ring, the railway, and the grid on each side
     * of it - or the islands, the lake, the river, the village, the great base or the space center, each laid out
     * its own way - and the coast.
     */
    private void layStreets() {
        makeDistricts();
        if (city.cfg.neighbourhoods()) Neighbourhoods.assign(city, city.cfg.seed);
        if (look != null && city.cfg.neighbourhoods()) Neighbourhoods.paint(city, look.palette);
        // ((28.0) Laid out the map's own way.)
        if (special != null) {
            special.streets();
            return;
        }
        if (m > 0) city.fill(0, 0, city.w, city.h, GRASS);
        // A ring road around the edge of town (a village just has its lanes).
        boolean village = city.cfg.density() == 0 && city.cfg.style() == CityConfig.STYLE_HOUSES;
        // A town out in the country grows its own shape instead of filling a square.
        boolean organic = m > 0 && !village;
        int in = organic ? 0 : 3;
        if (m == 0 && city.waterKind != CityConfig.W_ISLANDS) {
            carve(new Street(bx0, by0, bx1, by0 + 3, false, false));
            // (By the sea, the bottom of the ring is the seafront.)
            carve(new Street(bx0, by1 - 3, bx1, by1, false, city.seaY > 0));
            carve(new Street(bx0, by0, bx0 + 3, by1, true, false));
            carve(new Street(bx1 - 3, by0, bx1, by1, true, false));
        }
        if (m > 0) city.fill(bx0 + in, by0 + in, bx1 - bx0 - in * 2, by1 - by0 - in * 2, village ? GRASS : SIDEWALK);
        // The railway cuts straight across the middle third of the town; the streets are laid out on each side.
        if (city.cfg.hasRail()) {
            int th = by1 - by0;
            city.railY0 = by0 + th / 3 + city.rnd.nextInt(Math.max(1, th / 3));
            city.railRows = 3;
            splitTop(bx0 + in, by0 + in, bx1 - in, city.railY0);
            split(bx0 + in, city.railY0 + city.railRows, bx1 - in, by1 - in, 0, -1, -1);
            for (int y = city.railY0; y < city.railY0 + city.railRows; y++)
                for (int x = 0; x < city.w; x++) {
                    int i = y * city.w + x;
                    // Streets that reach the line from both sides get a level crossing.
                    boolean above = city.roadDir[(city.railY0 - 1) * city.w + x] == 1, below = city.roadDir[(city.railY0 + city.railRows) * city.w + x] == 1;
                    if ((above && below) || (m == 0 && ((x >= bx0 && x < bx0 + 3) || (x >= bx1 - 3 && x < bx1)))) {
                        city.tiles[i] = ROAD;
                        city.roadDir[i] = 1;
                    } else {
                        city.tiles[i] = RAIL;
                        city.roadDir[i] = 0;
                    }
                }
            roadGen.ensureCrossings(bx0, bx1, city.railY0, city.railRows);
        } else if (city.waterKind == CityConfig.W_ISLANDS) {
            // ((25.0) Theme Park Island: the one island, the park in the middle, the mainland across the strait.)
            if (city.themePark) parkMap.themeParkIsland();
            else islandsMap.islands();
        } else if (city.waterKind == CityConfig.W_LAKE) {
            coastMap.lakeTown(bx0, by0, bx1, by1, in);
        } else if (city.waterKind == CityConfig.W_RIVER) {
            riverMap.riverTown(bx0, by0, bx1, by1, in);
        } else if (village && m > 0) {
            landGen.villageCore(bx0, by0, bx1, by1);
        } else if (city.cfg.militaryTown()) {
            baseGen.militaryTown(bx0, by0, bx1, by1, in);
        } else if (city.spaceCenter) {
            // ((26.0) The space center's roads along one side, the staff town's streets on the rest.)
            spaceMap.spaceCenterTown();
        } else {
            splitTop(bx0 + in, by0 + in, bx1 - in, by1 - in);
        }
        // ((11.1) Not since the thin band of country: whole corners of town left as fields undid it.)
        if (organic && city.cfg.civic < 27) landGen.erodeTown(bx0, by0, bx1, by1);
        if (city.seaY > 0) coastMap.coast();
        if (city.cfg.prisonIsland()) prisonMap.prisonIsland();
    }

    /**
     * ((27.3) A step of generate.) Round the town: the highway, the ring road and the beltway, the bases out in the
     * country, hamlets and farms, and the lie of the land.
     */
    private void layCountry() {
        // Out in the country, a highway runs past the town.
        if (m >= 16) roadGen.highway(bx0, by0, bx1, by1);
        if (m >= 16 && city.cfg.civic >= 12) roadGen.ringRoad(bx0, by0, bx1, by1);
        // (10.52) Map version 22: the beltway right round the map, before the country fills in round it (on the
        // water maps, the roads round the edge of town: see outerBelt).
        if (city.cfg.civic >= 22 && m >= 16 && city.waterKind == CityConfig.W_NONE) roadGen.beltway();
        if (m > 0) {
            int n = city.w >= 400 ? 3 + city.rnd.nextInt(3) : city.w >= 300 ? 1 + city.rnd.nextInt(2) : city.rnd.nextInt(2);
            // (Since 10.20 the country is busier: more hamlets, the bigger ones small towns of their own.)
            if (city.cfg.servicePads()) n = city.w >= 600 ? 7 + city.rnd.nextInt(3) : city.w >= 400 ? 5 + city.rnd.nextInt(2) : city.w >= 300 ? 3 + city.rnd.nextInt(2) : 1 + city.rnd.nextInt(2);
            // (Since 10.30 the Massive and Huge maps' country is much fuller: twice the places, more of them towns.)
            if (city.cfg.civic >= 14 && city.cfg.massive()) n = city.w >= 900 ? 22 + city.rnd.nextInt(4) : city.w >= 650 ? 15 + city.rnd.nextInt(3) : 11 + city.rnd.nextInt(3);
            // (10.33) Fuller still: half as many places again, closer together (as many as there's room for).
            if (city.cfg.civic >= 17) n = n * 3 / 2 + 1;
            // (10.30) A big army base of its own out in the country, on the Massive maps (laid out first, so the
            // towns and farms go round it).
            if (city.cfg.outskirtsBase()) baseGen.outskirtsBase();
            // ((22.9) Desert City: the army base and its test range out in the desert; fewer places out there.)
            if (city.desert && m >= 16 && city.cfg.militaryBase()) baseGen.outskirtsBase();
            if (city.desert) n = Math.max(1, n / 2);
            landGen.hamlets(n);
        }
        if (m > 0) landGen.countryside(city.townX0, city.townY0, city.townX1, city.townY1);
        if (m > 0) roadGen.tidyDeadEnds();
        // (10.30) A big army base of its own out in the country, on the Massive maps.
        // The lie of the land: hills, and out in the country mountains, streams, lakes, a park and campsites.
        if (city.cfg.nature()) city.elev = OffHeap.Floats.of(new Terrain(city, new Random(city.cfg.seed * 977L + 3)).build());
        if (fieldTile != null)
            for (int i = 0; i < city.w * city.h; i++) if (fieldTile[i] && city.tiles[i] == TREE) city.tiles[i] = GRASS;
        // Each neighbourhood is a place of its own, as far as anyone roaming between places is concerned.
        for (int[] b : city.blocks)
            if (b.length > 5 && b[4] == 3) city.settlements.add(new float[]{(b[0] + b[2]) / 2f * T, (b[1] + b[3]) / 2f * T});
    }

    /**
     * ((27.3) A step of generate.) Tree-lined medians down the long avenues (each side one way), and the
     * roundabouts.
     */
    private void plantMedians() {
        // Tree-lined medians down the main roads, open at the junctions.
        for (Street st : city.streets) {
            if (st.width != 5) continue;
            // Only the long avenues get a median.
            if ((st.vertical ? st.y1 - st.y0 : st.x1 - st.x0) < 30 || city.rnd.nextFloat() < 0.35f) continue;
            int n = st.vertical ? st.y1 - st.y0 : st.x1 - st.x0;
            for (int k = 0; k < n; k++) {
                boolean clear = true;
                for (int a = 0; a < 5 && clear; a++) {
                    for (int e = -1; e <= 1 && clear; e++) {
                        int x = st.vertical ? st.x0 + a : st.x0 + k + e, y = st.vertical ? st.y0 + k + e : st.y0 + a;
                        if (x < 0 || y < 0 || x >= city.w || y >= city.h || city.roadDir[y * city.w + x] != (st.vertical ? 1 : 2)) clear = false;
                    }
                }
                // A side street joining from either side leaves a gap so traffic can turn across.
                for (int a = -1; a <= 5 && clear; a += 6)
                    for (int e = -2; e <= 2 && clear; e++) {
                        int x = st.vertical ? st.x0 + a : st.x0 + k + e, y = st.vertical ? st.y0 + k + e : st.y0 + a;
                        if (isRoad(x, y)) clear = false;
                    }
                if (!clear) continue;
                int x = st.vertical ? st.x0 + 2 : st.x0 + k, y = st.vertical ? st.y0 + k : st.y0 + 2;
                city.tiles[y * city.w + x] = k % 2 == 0 ? TREE : GRASS;
                // Each side of the median is one way, like a highway's carriageways: nobody drives up the
                // wrong side of a boulevard (and then has to cross the median to get back).
                if (city.divided == null) city.divided = new byte[city.w * city.h];
                for (int a = 0; a < 5; a++) {
                    if (a == 2) continue;
                    int lx = st.vertical ? st.x0 + a : st.x0 + k, ly = st.vertical ? st.y0 + k : st.y0 + a;
                    boolean firstSide = a < 2, neg = firstSide;
                    city.divided[ly * city.w + lx] = (byte) (st.vertical ? (neg ? 3 : 4) : (neg ? 2 : 1));
                }
            }
        }

        roadGen.roundabouts();
    }

    /**
     * ((27.3) A step of generate.) The blocks spoken for first: the strip, the park's rides, the space center, and
     * the army's bases.
     */
    private void placeBases() {
        // Pick blocks for the military base and police stations.
        used = new boolean[city.blocks.size()];
        // Hamlets out in the country are just homes, farms and the odd shop.
        for (int k = 0; k < city.blocks.size(); k++) if (city.blocks.get(k).length > 4 && city.blocks.get(k)[4] >= 2) used[k] = true;
        // ((27.5) The great park, before the bases and the rest want its block.)
        for (int k = 0; k < city.blocks.size(); k++)
            if (city.blocks.get(k) == bigParkBlock) {
                used[k] = true;
                int[] b = bigParkBlock;
                city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
                lotGen.park(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2);
                int pw = b[2] - b[0], ph = b[3] - b[1];
                if (pw >= 40 && ph >= 40) {
                    // A pond in the middle of a big one, off to one side.
                    float cx = b[0] + pw * (0.35f + look.r.nextFloat() * 0.3f), cy = b[1] + ph * (0.35f + look.r.nextFloat() * 0.3f);
                    float rx = pw * (0.12f + look.r.nextFloat() * 0.08f), ry = ph * (0.1f + look.r.nextFloat() * 0.08f);
                    for (int y = b[1] + 4; y < b[3] - 4; y++)
                        for (int x = b[0] + 4; x < b[2] - 4; x++) {
                            float ox = (x + 0.5f - cx) / rx, oy = (y + 0.5f - cy) / ry;
                            float wob = 1 + 0.15f * (float) Math.sin(Math.atan2(oy, ox) * 3 + cx);
                            if (ox * ox + oy * oy < wob * wob) city.tiles[y * city.w + x] = WATER;
                        }
                } else lotGen.bandstand(b[0] + 1, b[1] + 1, pw - 2, ph - 2);
                city.openAreas.add(new float[]{(b[0] + b[2]) / 2f * T, (b[1] + b[3]) / 2f * T, 0});
            }
        // ((22.9) Desert City: the casino strip, before anything else takes the blocks along it.)
        if (city.desert && stripSt != null) desertMap.strip(used);
        // ((25.0) Theme Park Island: the park's rides, the hotels along the beach, the staff town's clinic and housing.)
        if (city.themePark) parkMap.themeParkPlaces(used);
        // ((26.0) The Space Center: the pads, the assembly building, the crawlerway, mission control and the rest.)
        if (city.spaceCenter) spaceMap.spaceCenterPlaces();
        if (special != null) special.places(used);
        // ((15.5) The Military Town's great base, in the middle where the streets were kept clear of it.)
        if (bigBase != null) {
            baseGen.militaryBase(bigBase[0] + 1, bigBase[1] + 1, bigBase[2] - bigBase[0] - 2, bigBase[3] - bigBase[1] - 2);
        } else if (city.cfg.militaryBase()) {
            int best = -1, bestArea = 0;
            // ((22.9) Desert City: the base is out in the desert already (the extra ones are in town as usual).)
            for (int k = 0; k < (testRange != null ? 0 : city.blocks.size()); k++) {
                int[] b = city.blocks.get(k);
                int bw = b[2] - b[0] - 2, bh = b[3] - b[1] - 2;
                if (used[k] || bw < 9 || bh < 9) continue;
                if (bw * bh > bestArea) {
                    bestArea = bw * bh;
                    best = k;
                }
            }
            if (best >= 0) used[best] = true;
            if (best >= 0) {
                int[] b = city.blocks.get(best);
                city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
                baseGen.militaryBase(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2);
            }
            // ((13.0) More bases on the bigger maps, each on a big block well away from the others, with its own
            // garrison.)
            for (int extra = 0; extra < city.cfg.extraBases(); extra++) {
                int pick = -1, pickArea = 0;
                for (int k = 0; k < city.blocks.size(); k++) {
                    int[] b = city.blocks.get(k);
                    int bw = b[2] - b[0] - 2, bh = b[3] - b[1] - 2;
                    if (used[k] || bw < 9 || bh < 9 || bw * bh <= pickArea) continue;
                    float cx = (b[0] + b[2]) / 2f * T, cy = (b[1] + b[3]) / 2f * T;
                    boolean apart = true;
                    for (Facility f : city.facilities)
                        if (f.kind == FACILITY_BASE && World.hypot(f.x - cx, f.y - cy) < city.w * T * 0.22f) apart = false;
                    if (!apart) continue;
                    pickArea = bw * bh;
                    pick = k;
                }
                if (pick < 0) break;
                used[pick] = true;
                int[] b = city.blocks.get(pick);
                city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
                baseGen.militaryBase(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2, 41 + extra).satellite = true;
            }
        }
    }

    /**
     * ((27.3) A step of generate.) The police stations, the hospital (old cities), the FBI field office and the
     * government buildings.
     */
    private void placeServices() {
        for (int n = 0; n < city.cfg.policeStations(); n++) {
            List<Integer> options = new ArrayList<Integer>();
            for (int k = 0; k < city.blocks.size(); k++) {
                int[] b = city.blocks.get(k);
                if (used[k] || b[2] - b[0] - 2 < 6 || b[3] - b[1] - 2 < 6) continue;
                float cx = (b[0] + b[2]) / 2f * T, cy = (b[1] + b[3]) / 2f * T;
                // (On the Islands, the first three precincts go one to each island.)
                // ((25.0) On Theme Park Island the headquarters is in the staff town.)
                if (city.islandRects != null && n < city.islandRects.length && city.island((b[0] + b[2]) / 2, (b[1] + b[3]) / 2) != (city.themePark ? 1 - n : n)) continue;
                boolean near = false;
                for (Facility f : city.facilities)
                    if ((f.kind == FACILITY_POLICE && World.hypot(f.x - cx, f.y - cy) < city.w * T / (city.cfg.policeStations() > 3 ? 4.5f : 3f))
                            || World.hypot(f.x - cx, f.y - cy) < Math.max(20, city.w / 6f) * T) near = true;
                if (!near) options.add(k);
            }
            if (options.isEmpty()) break;
            int k = options.get(city.rnd.nextInt(options.size()));
            // ((10.52) The first is the police headquarters: the biggest block on offer, up to twice a station's.)
            if (city.cfg.civic >= 23 && n == 0) {
                int area = 0;
                for (int o : options) {
                    int[] b = city.blocks.get(o);
                    int a = Math.min(b[2] - b[0] - 2, 26) * Math.min(b[3] - b[1] - 2, 26);
                    if (a > area) {
                        area = a;
                        k = o;
                    }
                }
            }
            used[k] = true;
            int[] b = city.blocks.get(k);
            city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
            lotGen.serviceBuilding(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2, STATION, city.cfg.civic >= 23 && n == 0 ? 0 : n + 1);
        }

        // One hospital, as far as it can be from the police stations. ((16.5) Not any more: nobody went there for
        // anything. Cities from before still have theirs, as an ordinary building.)
        int hospital = -1;
        float far = -1;
        for (int k = 0; k < (city.cfg.hospital() ? city.blocks.size() : 0); k++) {
            int[] b = city.blocks.get(k);
            if (used[k] || b[2] - b[0] - 2 < 6 || b[3] - b[1] - 2 < 6) continue;
            float cx = (b[0] + b[2]) / 2f * T, cy = (b[1] + b[3]) / 2f * T, d = city.w * T;
            for (Facility f : city.facilities) d = Math.min(d, (float) World.hypot(f.x - cx, f.y - cy));
            d += city.rnd.nextFloat() * T * 6;
            if (d > far) {
                far = d;
                hospital = k;
            }
        }
        if (hospital >= 0) {
            used[hospital] = true;
            int[] b = city.blocks.get(hospital);
            city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
            lotGen.serviceBuilding(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2, HOSPITAL, 0);
        }

        // ((16.9) The FBI field office: a block of its own, away from the police stations, with the agents' black cars
        // on its lot.)
        int fbi = -1;
        float fbiFar = -1;
        for (int k = 0; k < (city.cfg.fbiOffice() ? city.blocks.size() : 0); k++) {
            int[] b = city.blocks.get(k);
            if (used[k] || b[2] - b[0] - 2 < 6 || b[3] - b[1] - 2 < 6) continue;
            float cx = (b[0] + b[2]) / 2f * T, cy = (b[1] + b[3]) / 2f * T, d = city.w * T;
            for (Facility f : city.facilities) d = Math.min(d, (float) World.hypot(f.x - cx, f.y - cy));
            // (In town, not out at the edge: the nearer the middle, the likelier.)
            d -= (float) World.hypot(cx - (city.townX0 + city.townX1) / 2f * T, cy - (city.townY0 + city.townY1) / 2f * T) * 0.5f;
            d += city.rnd.nextFloat() * T * 6;
            if (d > fbiFar) {
                fbiFar = d;
                fbi = k;
            }
        }
        if (fbi >= 0) {
            used[fbi] = true;
            int[] b = city.blocks.get(fbi);
            city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
            fedLot = true;
            lotGen.serviceBuilding(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2, STATION, 99);
            fedLot = false;
            Facility fo = city.facilities.get(city.facilities.size() - 1);
            fo.fed = true;
            fo.name = Dispatch.federal(city.country) + " Field Office";
        }

        if (city.cfg.civic > 0) lotGen.government(used);
    }

    /**
     * ((27.3) A step of generate.) Churches, schools, supermarkets, gas stations and cemeteries, and the train
     * station by the line.
     */
    private void placeLandmarks() {
        // Landmarks: churches, schools, fire stations, supermarkets, gas stations and cemeteries.
        int[][] minSize = {{5, 5}, {8, 8}, {6, 6}, {8, 7}, {5, 5}, {7, 7}};
        for (int kind = 0; kind < 6; kind++)
            for (int n = 0; n < city.cfg.landmarks(kind); n++) {
                List<Integer> options = new ArrayList<Integer>();
                for (int k = 0; k < city.blocks.size(); k++) {
                    int[] b = city.blocks.get(k);
                    int iw = b[2] - b[0] - 2, ih = b[3] - b[1] - 2;
                    if (!used[k] && ((iw >= minSize[kind][0] && ih >= minSize[kind][1])
                            || (ih >= minSize[kind][0] && iw >= minSize[kind][1]))
                            // (On the Islands, a fire station on each island first.)
                            && !(kind == 2 && city.islandRects != null && n < city.islandRects.length && city.island((b[0] + b[2]) / 2, (b[1] + b[3]) / 2) != n))
                        options.add(k);
                }
                if (options.isEmpty()) break;
                int k = options.get(city.rnd.nextInt(options.size()));
                if (kind == 2) k = spreadOut(options, k);
                used[k] = true;
                int[] b = city.blocks.get(k);
                city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
                int ix = b[0] + 1, iy = b[1] + 1, iw = b[2] - b[0] - 2, ih = b[3] - b[1] - 2;
                float ax = (ix + iw / 2f) * T, ay = (iy + ih / 2f) * T;
                switch (kind) {
                    case 0: lotGen.church(ix, iy, iw, ih); break;
                    case 1: lotGen.school(ix, iy, iw, ih); city.openAreas.add(new float[]{ax, ay, 2}); break;
                    case 2: lotGen.serviceBuilding(ix, iy, iw, ih, FIRE_STATION, 0); break;
                    case 3: lotGen.market(ix, iy, iw, ih); city.openAreas.add(new float[]{ax, ay, 2}); break;
                    case 4: lotGen.gasStation(ix, iy, iw, ih); break;
                    default: lotGen.cemetery(ix, iy, iw, ih); break;
                }
            }

        // The train station sits beside the railway.
        if (city.railY0 >= 0) {
            int best = -1;
            float bestD = Float.MAX_VALUE;
            for (int k = 0; k < city.blocks.size(); k++) {
                int[] b = city.blocks.get(k);
                if (used[k] || (b[3] != city.railY0 && b[1] != city.railY0 + city.railRows)) continue;
                if (b[2] - b[0] - 2 < 6 || b[3] - b[1] - 2 < 4) continue;
                float d = Math.abs((b[0] + b[2]) / 2f - city.w / 2f);
                if (d < bestD) {
                    bestD = d;
                    best = k;
                }
            }
            if (best >= 0) {
                used[best] = true;
                int[] b = city.blocks.get(best);
                city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
                lotGen.trainStation(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2, b[3] == city.railY0);
            }
        }
    }

    /**
     * ((27.3) A step of generate.) The big sites: malls, the prison, the stadium, the power station and the airport.
     */
    private void placeBigSites() {
        // Big sites: a shopping mall, a stadium and a power station, on blocks large enough to take them.
        rural = city.cfg.density() == 0 && city.cfg.style() == CityConfig.STYLE_HOUSES;
        if (!rural) {
            int k = bigBlock(used, 11, 9, 0.45f);
            if (k >= 0) {
                used[k] = true;
                int[] b = city.blocks.get(k);
                city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
                lotGen.mall(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2);
            }
        }
        // ((22.0) A second mall on the bigger maps, and a walled prison out towards the edge of town.)
        // ((25.0) Not on Theme Park Island: one mall is plenty.)
        // ((26.0) Nor in the Space Center's small staff town.)
        if (city.cfg.v22() && city.w >= 256 && !rural && !city.themePark && !city.spaceCenter) {
            int k = bigBlock(used, 11, 9, 0.45f);
            if (k >= 0) {
                used[k] = true;
                int[] b = city.blocks.get(k);
                city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
                lotGen.mall(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2);
            }
        }
        if (city.cfg.v22() && city.w >= 256 && city.prison == null && !rural && city.waterKind != CityConfig.W_ISLANDS && !city.spaceCenter) {
            int best = -1;
            float bestD = -1;
            for (int k = 0; k < city.blocks.size(); k++) {
                int[] b = city.blocks.get(k);
                if (used[k] || b[2] - b[0] < 24 || b[3] - b[1] < 18 || b[1] < 2) continue;
                // (Out towards the edge of town: the furthest big block from the middle.)
                float d = (float) Math.hypot((b[0] + b[2]) / 2f - city.w / 2f, (b[1] + b[3]) / 2f - city.h / 2f);
                if (d > bestD) {
                    bestD = d;
                    best = k;
                }
            }
            if (best >= 0) {
                used[best] = true;
                int[] b = city.blocks.get(best);
                prisonMap.landPrison(b[0] + 1, b[1] + 1, b[2] - 1, b[3] - 1);
            }
        }
        // ((25.0) Nor a stadium or a power station on the resort island.)
        if ((city.w >= 256 || city.cfg.density() == 2) && !rural && !city.themePark && !city.spaceCenter) {
            int k = bigBlock(used, 12, 11, 0.6f);
            if (k >= 0) {
                used[k] = true;
                int[] b = city.blocks.get(k);
                city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
                lotGen.stadium(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2);
            }
        }
        if (!rural && !city.themePark) {
            int best = -1;
            float bestD = Float.MAX_VALUE;
            float ix0 = city.w / 2f + shiftX + (float) Math.cos(industryAngle) * city.w * 0.4f, iy0 = city.h / 2f + shiftY + (float) Math.sin(industryAngle) * city.h * 0.4f;
            for (int k = 0; k < city.blocks.size(); k++) {
                int[] b = city.blocks.get(k);
                if (used[k] || b[2] - b[0] - 2 < 8 || b[3] - b[1] - 2 < 7) continue;
                float d = (float) World.hypot((b[0] + b[2]) / 2f - ix0, (b[1] + b[3]) / 2f - iy0);
                if (d < bestD) {
                    bestD = d;
                    best = k;
                }
            }
            if (best >= 0) {
                used[best] = true;
                int[] b = city.blocks.get(best);
                city.fill(b[0], b[1], b[2] - b[0], b[3] - b[1], SIDEWALK);
                lotGen.powerStation(b[0] + 1, b[1] + 1, b[2] - b[0] - 2, b[3] - b[1] - 2);
            }
        }
    }

    /**
     * ((27.3) A step of generate.) Every block left: filled in by its district (CityBlocks.block), the level
     * crossings, and the lanes' stubs.
     */
    private void fillBlocks() {
        if (city.cfg.landmarks()) {
            int open = 0;
            for (int k = 0; k < city.blocks.size(); k++) if (!used[k] && city.blocks.get(k).length <= 4) open++;
            // ((27.5) A little more of what's left in a city that's mostly neighbourhoods and big blocks, so it still gets its ten.)
            int want = Math.min(Math.round(open * (look != null ? 0.6f : 0.5f)), Math.max(10, Math.round(open * 0.15f)));
            lmOpen = open;
            lmLeft = want;
        }
        for (int k = 0; k < city.blocks.size(); k++) {
            int[] b = city.blocks.get(k);
            boolean plot = b.length > 4;
            if (plot && b[4] == 3) {
                landGen.fillNeighbourhood(b);
                continue;
            }
            if (used[k] && !(plot && b[4] == 2)) continue;
            int x0 = b[0], y0 = b[1], x1 = b[2], y1 = b[3];
            if (x1 - x0 < 1 || y1 - y0 < 1) continue;
            city.fill(x0, y0, x1 - x0, y1 - y0, SIDEWALK);
            int ix = x0 + 1, iy = y0 + 1, iw = x1 - x0 - 2, ih = y1 - y0 - 2;
            if (iw < 2 || ih < 2) continue;
            // Farms on the edge of town.
            float fcx = (x0 + x1) / 2f - city.w / 2f - shiftX, fcy = (y0 + y1) / 2f - city.h / 2f - shiftY;
            float fd = (float) Math.sqrt(fcx * fcx + fcy * fcy) / (city.w * 0.5f);
            float farmChance = rural ? 0.4f : city.cfg.style() == CityConfig.STYLE_HOUSES || city.cfg.density() == 0 ? 0.25f : city.cfg.density() == 2 ? 0 : 0.1f;
            curDistrict = city.districtType(ix + iw / 2, iy + ih / 2);
            if (curDistrict != DT_SUBURB && curDistrict != DT_PARKSIDE && curDistrict != DT_OLDTOWN) farmChance = 0;
            // (A massive map's town runs right out to the country band: only the odd farm inside it.)
            if (city.w >= 400) farmChance *= 0.25f;
            // ((11.1) The town's edge isn't worn away any more: the farms start a ring further in.)
            if (fd > (city.cfg.civic >= 27 ? 0.68f : 0.8f) && iw >= 7 && ih >= 7 && city.rnd.nextFloat() < farmChance) {
                landGen.farm(ix, iy, iw, ih);
                // ((27.5) One block fewer the landmarks might have had, so they still come out at their number.)
                if (look != null) lmOpen--;
                continue;
            }
            if (plot) {
                // A village plot: a cottage or two in a garden, now and then a paddock or a shop.
                float r = city.rnd.nextFloat();
                if (r < 0.12f && iw >= 7 && ih >= 7) landGen.farm(ix, iy, iw, ih);
                else if (r < 0.3f) lotGen.block(ix, iy, iw, ih);
                else {
                    city.fill(x0, y0, x1 - x0, y1 - y0, GRASS);
                    lotGen.houses(ix, iy, iw, ih);
                }
                continue;
            }
            if (lotGen.landmark(ix, iy, iw, ih)) continue;
            lotGen.block(ix, iy, iw, ih);
        }
        if (city.cfg.neighbourhoods()) lotGen.placeStrings();

        if (city.railY0 >= 0)
            for (int x = 0; x < city.w; x++) {
                if (city.tiles[city.railY0 * city.w + x] != ROAD || city.underRailBridge(x)) continue;
                int x0 = x;
                while (x < city.w && city.tiles[city.railY0 * city.w + x] == ROAD) x++;
                city.crossings.add(new int[]{x0, x});
            }
        if (city.cfg.dirtLanes()) roadGen.unpaveLaneStubs();
    }

    /**
     * ((27.3) A step of generate.) Cars left parked along the kerbs.
     */
    private void parkCars() {
        // Abandoned cars on the roads (never at intersections, never next to each other).
        float carChance = CAR_CHANCE[city.cfg.traffic()];
        for (int y = 0; y < city.h; y++) {
            for (int x = 0; x < city.w; x++) {
                if (city.tiles[y * city.w + x] != ROAD) continue;
                int dir = city.roadDir[y * city.w + x];
                if (dir == 0 || dir == 3 || city.hasNeighborDir(x, y)) continue;
                if (city.railY0 >= 0 && y >= city.railY0 - 1 && y <= city.railY0 + city.railRows) continue;
                // (Nobody parks on the highway, against the barrier, since 10.29.)
                if (city.cfg.civic >= 13 && city.oneWay != null && city.oneWay[y * city.w + x] != 0) continue;
                if (city.rnd.nextFloat() > carChance) continue;
                if (city.hasNeighbor(x, y, CAR)) continue;
                // Parked against the kerb, never in the middle lanes.
                boolean vertical = dir == 1;
                boolean kerb = vertical ? !isRoad(x - 1, y) || !isRoad(x + 1, y) : !isRoad(x, y - 1) || !isRoad(x, y + 1);
                if (!kerb) continue;
                // Only where the road is wide enough to drive past, and well back from any junction.
                int ox = vertical ? (!isRoad(x - 1, y) ? -1 : 1) : 0, oy = vertical ? 0 : (!isRoad(x, y - 1) ? -1 : 1);
                int width = 0;
                while (width < 8 && isRoad(x - ox * width, y - oy * width)) width++;
                if (width < 4) continue;
                boolean nearJunction = false;
                for (int k = -6; k <= 6 && !nearJunction; k++)
                    if (vertical ? isRoad(x + ox, y + k) : isRoad(x + k, y + oy)) nearJunction = true;
                if (nearJunction) continue;
                city.tiles[y * city.w + x] = CAR;
            }
        }
    }

    /**
     * ((27.3) A step of generate.) The streets' names, the ring road's and the beltway's, and each police station's
     * street after its name.
     */
    private void nameStreets() {
        // Street names: main roads get avenues and boulevards, the rest streets, lanes and courts.
        List<String> names = new ArrayList<String>(Arrays.asList(city.cfg.variety() ? city.country.streets37 : city.country.streets));
        java.util.Collections.shuffle(names, city.rnd);
        int next = 0;
        for (Street st : city.streets) {
            String base = names.get(next++ % names.size());
            int len = st.vertical ? st.y1 - st.y0 : st.x1 - st.x0;
            if (st.x0 == 0 && st.y0 == 0 || st.x1 == city.w || st.y1 == city.h) st.name = city.country.ringRoad;
            else st.name = city.country.streetName(base, st.main || len > 40, city.rnd);
        }
        for (Street st : ringStreets) st.name = city.country.ringRoad;
        if (city.cfg.civic >= 22 && city.waterKind != CityConfig.W_NONE) roadGen.outerBelt();
        for (Street st : beltStreets) {
            st.name = BELTWAY;
            city.streets.add(st);
        }
        for (Facility f : city.facilities)
            if (f.kind == FACILITY_POLICE) {
                String street = city.placeName(f.x, f.y);
                int amp = street.indexOf(" & ");
                f.name = f.name + " (" + (amp > 0 ? street.substring(0, amp) : street) + ")";
            }
    }

    /**
     * ((27.3) A step of generate.) Last: the desert's sand or the tropics' palms, and the street lamps at the
     * corners.
     */
    private void finishGround() {
        if (city.desert) desertMap.desertify();
        if (city.themePark || (city.cfg.archipelago() && city.cfg.distinctIslands())) parkMap.tropics();
        if (special != null) special.finish();
        // Street lamps on the pavement corners at junctions.
        for (int y = 1; y < city.h - 1; y++)
            for (int x = 1; x < city.w - 1; x++) {
                if (city.tiles[y * city.w + x] != SIDEWALK) continue;
                boolean horiz = city.paved(x - 1, y) || city.paved(x + 1, y), vert = city.paved(x, y - 1) || city.paved(x, y + 1);
                if (horiz && vert) city.lamps.add(new float[]{x * T + T / 2f, y * T + T / 2f});
            }
    }


    /**
     * From tile start, the nearest road tile (not the highway, nor inside the rectangle) across open country,
     * keeping a tile clear of buildings, fences and water; prev[] holds the way back. -1 if there's none.
     */
    int roadSearch(int start, int rx, int ry, int rw, int rh, int[] prev) {
        Arrays.fill(prev, -2);
        int[] queue = new int[city.w * city.h];
        int qh = 0, qt = 0;
        prev[start] = -1;
        queue[qt++] = start;
        int sx = start % city.w, sy = start / city.w;
        while (qh < qt) {
            int c = queue[qh++], x = c % city.w, y = c / city.w;
            for (int k = 0; k < 4; k++) {
                int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (nx < 2 || ny < 2 || nx >= city.w - 2 || ny >= city.h - 2) continue;
                int j = ny * city.w + nx;
                if (prev[j] != -2) continue;
                if (nx >= rx - 1 && nx <= rx + rw && ny >= ry - 1 && ny <= ry + rh) continue;
                byte t = city.tiles[j];
                if (t == ROAD && (city.oneWay == null || city.oneWay[j] == 0) && !nearHighwayTile(nx, ny)) {
                    prev[j] = c;
                    return j;
                }
                if (t != GRASS && t != TREE && t != DIRT && t != TRAIL && t != RAIL) continue;
                // (A tile clear all round, except just outside the gate.)
                boolean near = Math.abs(nx - sx) + Math.abs(ny - sy) <= 2, ok = true;
                for (int dy = -1; dy <= 1 && ok && !near; dy++)
                    for (int dx = -1; dx <= 1 && ok; dx++) {
                        byte o = city.tiles[(ny + dy) * city.w + nx + dx];
                        if (o == BUILDING || o == FENCE || o == WATER || o == BASE || o == LOT || o == SIDEWALK || o == PLAZA || o == CAR) ok = false;
                    }
                if (!ok) continue;
                prev[j] = c;
                queue[qt++] = j;
            }
        }
        return -1;
    }

    boolean nearHighwayTile(int x, int y) {
        if (city.hwyAxis < 0) return false;
        int a = (city.hwyAxis == 0 ? y : x) - city.hwyAt;
        return a >= -2 && a <= 8;
    }

    /** (10.30) Roads that stay paved however they join up (the small towns' streets and their roads in). */
    boolean[] keepPaved;

    /** A line {x, y, ...} with its corners cut (the ends kept). */
    static float[] chaikin(float[] l) {
        int n = l.length / 2;
        if (n < 3) return l;
        float[] o = new float[(2 * (n - 1)) * 2];
        int k = 0;
        o[k++] = l[0];
        o[k++] = l[1];
        for (int i = 0; i < n - 1; i++) {
            float ax = l[i * 2], ay = l[i * 2 + 1], bx = l[i * 2 + 2], by = l[i * 2 + 3];
            if (i > 0) {
                o[k++] = ax * 0.75f + bx * 0.25f;
                o[k++] = ay * 0.75f + by * 0.25f;
            }
            if (i < n - 2) {
                o[k++] = ax * 0.25f + bx * 0.75f;
                o[k++] = ay * 0.25f + by * 0.75f;
            }
        }
        o[k++] = l[l.length - 2];
        o[k++] = l[l.length - 1];
        return Arrays.copyOf(o, k);
    }

    /**
     * ((16.3) Moves each district's and island's name onto its own dry land: as far in from the water and its
     * borders as it can, near its middle. The middle of a district's tiles - or of an island's bounds - fell in the
     * harbour, a lake or the sea on the water maps, and the name sat out on the water. Only where the name is drawn
     * changes: nothing that plays.
     */
    void placeLabels() {
        if (city.districtAt == null || city.districts.isEmpty()) return;
        int n = city.w * city.h;
        short[] dist = new short[n];
        // First from the water alone, for the islands; then from the water and every district border.
        for (int pass = 0; pass < 2; pass++) {
            if (pass == 0 && city.islandRects == null) continue;
            for (int y = 0, i = 0; y < city.h; y++)
                for (int x = 0; x < city.w; x++, i++) {
                    if (city.tiles[i] == WATER) {
                        dist[i] = 0;
                        continue;
                    }
                    boolean edge = x == 0 || y == 0 || x == city.w - 1 || y == city.h - 1;
                    if (!edge && pass == 1) {
                        byte me = city.districtAt.get(i);
                        edge = city.districtAt.get(i - 1) != me || city.districtAt.get(i + 1) != me || city.districtAt.get(i - city.w) != me || city.districtAt.get(i + city.w) != me;
                    }
                    dist[i] = edge ? 1 : Short.MAX_VALUE;
                }
            for (int y = 1; y < city.h; y++)
                for (int x = 1, i = y * city.w + 1; x < city.w; x++, i++)
                    if (dist[i] > 1) dist[i] = (short) Math.min(dist[i], Math.min(dist[i - 1], dist[i - city.w]) + 1);
            for (int y = city.h - 2; y >= 0; y--)
                for (int x = city.w - 2, i = y * city.w + x; x >= 0; x--, i--)
                    if (dist[i] > 1) dist[i] = (short) Math.min(dist[i], Math.min(dist[i + 1], dist[i + city.w]) + 1);
            if (pass == 0) {
                city.islandLabel = new float[city.islandRects.length * 2];
                for (int k = 0; k < city.islandRects.length; k++) {
                    int[] q = city.islandRects[k];
                    float mx = (q[0] + q[2]) / 2f, my = (q[1] + q[3]) / 2f, best = -1e9f;
                    city.islandLabel[k * 2] = mx * T;
                    city.islandLabel[k * 2 + 1] = my * T;
                    for (int y = Math.max(0, q[1]); y < Math.min(city.h, q[3]); y++)
                        for (int x = Math.max(0, q[0]); x < Math.min(city.w, q[2]); x++) {
                            int dd = dist[y * city.w + x];
                            if (dd == 0) continue;
                            // (Well in from the shore - enough for big letters - and then as near the middle as can be.)
                            float score = Math.min(dd, 14) - 0.05f * (float) Math.sqrt((x - mx) * (x - mx) + (y - my) * (y - my));
                            if (score > best) {
                                best = score;
                                city.islandLabel[k * 2] = (x + 0.5f) * T;
                                city.islandLabel[k * 2 + 1] = (y + 0.5f) * T;
                            }
                        }
                }
                continue;
            }
            int nd = city.districts.size();
            float[] best = new float[nd], mx = new float[nd], my = new float[nd];
            int[] at = new int[nd], dry = new int[nd];
            for (int k = 0; k < nd; k++) {
                best[k] = -1e9f;
                at[k] = -1;
                mx[k] = city.districts.get(k).cx / T;
                my[k] = city.districts.get(k).cy / T;
            }
            for (int y = 0, i = 0; y < city.h; y++)
                for (int x = 0; x < city.w; x++, i++) {
                    int dd = dist[i];
                    if (dd == 0) continue;
                    int k = city.districtAt.get(i);
                    if (k < 0 || k >= nd) continue;
                    dry[k]++;
                    float score = Math.min(dd, 9) - 0.04f * (float) Math.sqrt((x - mx[k]) * (x - mx[k]) + (y - my[k]) * (y - my[k]));
                    if (score > best[k]) {
                        best[k] = score;
                        at[k] = i;
                    }
                }
            for (int k = 0; k < nd; k++) {
                District d = city.districts.get(k);
                d.unnamed = dry[k] < 40;
                if (at[k] < 0) continue;
                // (Already on dry land of its own, well in: it stays where it was.)
                int i0 = city.tileIndex(d.cx, d.cy);
                if (i0 >= 0 && i0 < n && city.districtAt.get(i0) == k && dist[i0] >= Math.min(6, best[k] + 0.5f)) continue;
                d.cx = (at[k] % city.w + 0.5f) * T;
                d.cy = (at[k] / city.w + 0.5f) * T;
            }
        }
    }

    /**
     * (10.63) Brings the number who live in the city to about target: the homes the layout gave each building are
     * scaled up (or down) together. A house keeps to a household (nine at the most); what's left over goes into the
     * flats (apartment blocks, offices and shops with flats upstairs), in proportion to how many they had.
     */
    void balancePopulation(int target) {
        if (city.totalResidents <= 0 || target <= 0) return;
        float k = target / (float) city.totalResidents;
        int total = 0, flats = 0;
        for (Building b : city.buildings) {
            if (b.residents <= 0) continue;
            int cap = b.kind == HOUSE ? 9 : b.kind == SHOP ? 8 : 2000;
            b.residents = Math.max(1, Math.min(cap, Math.round(b.residents * k)));
            total += b.residents;
            if (b.kind != HOUSE && b.kind != SHOP) flats += b.residents;
        }
        // (Houses full: the rest into the flats.)
        if (total < target && flats > 0) {
            float k2 = 1 + (target - total) / (float) flats;
            total = 0;
            for (Building b : city.buildings) {
                if (b.residents <= 0) continue;
                if (b.kind != HOUSE && b.kind != SHOP) b.residents = Math.min(2000, Math.round(b.residents * k2));
                total += b.residents;
            }
        }
        // (Few flats, as in a town of houses: bigger households, up to fourteen.)
        if (total < target) {
            int houses = 0;
            for (Building b : city.buildings) if (b.residents > 0 && (b.kind == HOUSE || b.kind == SHOP)) houses += b.residents;
            if (houses > 0) {
                float k3 = 1 + (target - total) / (float) houses;
                total = 0;
                for (Building b : city.buildings) {
                    if (b.residents <= 0) continue;
                    if (b.kind == HOUSE || b.kind == SHOP) b.residents = Math.min(14, Math.round(b.residents * k3));
                    total += b.residents;
                }
            }
        }
        for (Building b : city.buildings) if (b.residents > 0) b.capacity = Math.max(b.capacity, Math.min(b.residents, 40));
        city.totalResidents = total;
    }

    /** Which islands are joined by a bridge: {from, to, 1 if one is above the other}; and the causeway's island. */
    int[][] islandLinks;
    int causewayIsland;

    /** Straight-line distance (tenths of a tile) from island il's town to every tile. */
    int[] chamfer(int il) {
        int[] dist = new int[city.w * city.h];
        final int FAR_D = 1 << 20;
        for (int t = 0; t < city.w * city.h; t++) dist[t] = city.tiles[t] != WATER && city.island(t % city.w, t / city.w) == il ? 0 : FAR_D;
        for (int y = 0; y < city.h; y++)
            for (int x = 0; x < city.w; x++) {
                int t = y * city.w + x, d = dist[t];
                if (x > 0) d = Math.min(d, dist[t - 1] + 10);
                if (y > 0) d = Math.min(d, dist[t - city.w] + 10);
                if (x > 0 && y > 0) d = Math.min(d, dist[t - city.w - 1] + 14);
                if (x < city.w - 1 && y > 0) d = Math.min(d, dist[t - city.w + 1] + 14);
                dist[t] = d;
            }
        for (int y = city.h - 1; y >= 0; y--)
            for (int x = city.w - 1; x >= 0; x--) {
                int t = y * city.w + x, d = dist[t];
                if (x < city.w - 1) d = Math.min(d, dist[t + 1] + 10);
                if (y < city.h - 1) d = Math.min(d, dist[t + city.w] + 10);
                if (x < city.w - 1 && y < city.h - 1) d = Math.min(d, dist[t + city.w + 1] + 14);
                if (x > 0 && y < city.h - 1) d = Math.min(d, dist[t + city.w - 1] + 14);
                dist[t] = d;
            }
        return dist;
    }

    /**
     * A road on a line (across the water between islands, or out to the edge of the map): from a point in the
     * gap it runs both ways (or one way, from the map edge) until it meets a street, cutting through any
     * block in its way, with footpaths each side; over the water it's a bridge.
     */
    /**
     * (11.1) Where a bridge across from an island (rectangle a, to b if there is one) should run: in line with a
     * main road of the town there, one that still reaches the shore, and of both towns if they have one in
     * line, so the bridge carries the road on; failing that, the middle (fallback). Map version 27 on.
     */
    int mainLine(boolean vertical, int lo, int hi, int fallback, int[] a, int[] b) {
        if (city.cfg.civic < 27) return fallback;
        int best = fallback;
        float bs = -Float.MAX_VALUE;
        for (Street st : city.streets) {
            if (st.vertical != vertical || !st.main) continue;
            int c0 = vertical ? st.x0 : st.y0, c1 = vertical ? st.x1 : st.y1;
            int at = (c0 + c1) / 2 - 1;
            if (at < lo + 3 || at + 3 > hi - 3) continue;
            boolean inA = inRect(st, a), inB = b != null && inRect(st, b);
            if (!inA && !inB) continue;
            // (Still road somewhere along it: not a street the coast took away.)
            int rx = vertical ? at + 1 : (st.x0 + st.x1) / 2, ry = vertical ? (st.y0 + st.y1) / 2 : at + 1;
            if (!city.paved(rx, ry)) continue;
            float score = (inA ? 10 : 0) + (inB ? 10 : 0) + (c1 - c0) - Math.abs(at - fallback) * 0.05f;
            // In line with one on the other island too: best of all.
            if (b != null)
                for (Street o : city.streets)
                    if (o != st && o.vertical == vertical && o.main && Math.abs((vertical ? o.x0 + o.x1 : o.y0 + o.y1) - (c0 + c1)) <= 2
                            && (inRect(o, a) != inA || inRect(o, b) != inB)) score += 30;
            if (score > bs) {
                bs = score;
                best = at;
            }
        }
        return best;
    }

    static boolean inRect(Street st, int[] r) {
        return r != null && st.x0 >= r[0] - 1 && st.x1 <= r[2] + 1 && st.y0 >= r[1] - 1 && st.y1 <= r[3] + 1;
    }

    void linkRoad(boolean vertical, int at, int mid, boolean both) {
        int len = vertical ? city.h : city.w;
        int lo = reachRoad(vertical, at, mid, -1), hi = both ? reachRoad(vertical, at, mid, 1) : len;
        if (lo < 0 || hi < 0) return;
        int from = lo, to = both ? hi + 1 : len;
        // Blocks in the way are cut in two.
        int cx0 = vertical ? at : from, cx1 = vertical ? at + 3 : to, cy0 = vertical ? from : at, cy1 = vertical ? to : at + 3;
        for (int k = city.blocks.size() - 1; k >= 0; k--) {
            int[] bk = city.blocks.get(k);
            if (bk[2] <= cx0 - 1 || bk[0] >= cx1 + 1 || bk[3] <= cy0 - 1 || bk[1] >= cy1 + 1) continue;
            city.blocks.remove(k);
            if (vertical) {
                if (cx0 - 1 - bk[0] >= 4) city.blocks.add(new int[]{bk[0], bk[1], cx0 - 1, bk[3]});
                if (bk[2] - cx1 - 1 >= 4) city.blocks.add(new int[]{cx1 + 1, bk[1], bk[2], bk[3]});
            } else {
                if (cy0 - 1 - bk[1] >= 4) city.blocks.add(new int[]{bk[0], bk[1], bk[2], cy0 - 1});
                if (bk[3] - cy1 - 1 >= 4) city.blocks.add(new int[]{bk[0], cy1 + 1, bk[2], bk[3]});
            }
        }
        boolean[] wet = new boolean[(to - from) * 5];
        for (int k = from; k < to; k++)
            for (int s = -1; s <= 3; s++) {
                int x = vertical ? at + s : k, y = vertical ? k : at + s;
                if (x < 0 || y < 0 || x >= city.w || y >= city.h) continue;
                byte t = city.tiles[y * city.w + x];
                wet[(k - from) * 5 + s + 1] = t == WATER;
                if ((s == -1 || s == 3) && t != ROAD) city.tiles[y * city.w + x] = SIDEWALK;
            }
        carve(vertical ? new Street(at, from, at + 3, to, true, true) : new Street(from, at, to, at + 3, false, true));
        for (int k = from; k < to; k++)
            for (int s = -1; s <= 3; s++) {
                int x = vertical ? at + s : k, y = vertical ? k : at + s;
                if (x < 0 || y < 0 || x >= city.w || y >= city.h) continue;
                if (wet[(k - from) * 5 + s + 1]) city.bridge[y * city.w + x] = true;
            }
    }

    /** From a point on a line, the first place (going one way) where the three-tile band meets a street. */
    int reachRoad(boolean vertical, int at, int start, int step) {
        for (int k = start; k >= 0 && k < (vertical ? city.h : city.w); k += step)
            for (int s = 0; s < 3; s++) {
                int x = vertical ? at + s : k, y = vertical ? k : at + s;
                if (x >= 0 && y >= 0 && x < city.w && y < city.h && city.tiles[y * city.w + x] == ROAD) return k;
            }
        return -1;
    }

    /**
     * (15.10) The streets of a stretch of town - and on the maps with an airport, the airport kept clear in its top
     * left corner first, with a road along its edges, and the streets laid out round it.
     */
    void splitTop(int x0, int y0, int x1, int y1) {
        int aw = Math.min(120, (int) ((x1 - x0) * 0.55f)), ah = Math.min(40, (int) ((y1 - y0) * 0.6f));
        if (!city.cfg.airport() || aw < 60 || ah < 26) {
            split(x0, y0, x1, y1, 0, -1, -1);
            return;
        }
        int ax1 = x0 + aw, ay1 = y0 + ah;
        if (look != null && look.airportRight) {
            // ((27.5) The airport in the top right corner of this one, the streets round it the other way.)
            int ax0 = x1 - aw;
            carve(new Street(x0, ay1, x1, ay1 + 3, false, true));
            carve(new Street(ax0 - 3, y0, ax0, ay1 + 3, true, true));
            if (ax0 - 3 - x0 >= 6) split(x0, y0, ax0 - 3, ay1, 1, -1, -1);
            if (y1 - (ay1 + 3) >= 6) split(x0, ay1 + 3, x1, y1, 1, -1, -1);
            airportMap.airportGrounds(ax0, y0, x1, ay1);
            return;
        }
        carve(new Street(x0, ay1, x1, ay1 + 3, false, true));
        carve(new Street(ax1, y0, ax1 + 3, ay1 + 3, true, true));
        if (x1 - (ax1 + 3) >= 6) split(ax1 + 3, y0, x1, ay1, 1, -1, -1);
        if (y1 - (ay1 + 3) >= 6) split(x0, ay1 + 3, x1, y1, 1, -1, -1);
        airportMap.airportGrounds(x0, y0, ax1, ay1);
    }

    /** ((15.5) The Military Town's great base: where it goes (tiles: x0, y0, x1, y1), kept clear of the streets.) */
    int[] bigBase;

    /** A government building of the country's own look. */
    void govLot(int x, int y, int lw, int lh, int kind) {
        city.fill(x, y, lw, lh, BUILDING);
        int var = Variants.gov(kind);
        Variants.V v = Variants.get(var);
        int floors = v == null ? 2 : v.floorsMin + city.rnd.nextInt(v.floorsMax - v.floorsMin + 1);
        int roof = v != null ? v.roofCol : 0xFF6A6E74;
        int wall = kind == JAIL ? 0xFFA8A8A0 : kind == WORKS ? 0xFF9A8A78 : 0xFFD8D0C0;
        city.buildingLots.add(new int[]{x, y, lw, lh, roof, city.rnd.nextInt(100000), kind, floors, wall, var});
    }

    /** ((16.9) The lot being laid out is the FBI field office's: its cars are the agents' black ones. */
    boolean fedLot;

    // ((22.4) One-of-a-kind landmarks: about one block in seven (at least ten), each design once before any
    // comes round again, and then only well away from where it already is.)
    int lmLeft, lmOpen;
    Random lmRnd;
    int[] lmCount;

    boolean isRoad(int x, int y) {
        return x >= 0 && y >= 0 && x < city.w && y < city.h && city.tiles[y * city.w + x] == ROAD;
    }
    /** Its sides (named, and added to the streets once the rest are named). */
    final List<Street> beltStreets = new ArrayList<Street>();
    /** (10.30) A curved road being laid out in the country: grass, not pavement, either side of it. */
    boolean curveOnGrass;

    /** The ring road's streets (named after the rest), and its tiles (country lanes carry on across it). */
    final List<Street> ringStreets = new ArrayList<Street>();
    boolean[] ringTile;

    /** A cubic Bezier from (ax, ay) to (dx, dy) by (bx, by) and (cx, cy), as n + 1 points {x, y, ...} in tiles. */
    static float[] bezier(float ax, float ay, float bx, float by, float cx, float cy, float dx, float dy, int n) {
        float[] p = new float[(n + 1) * 2];
        for (int k = 0; k <= n; k++) {
            float t = k / (float) n, u = 1 - t;
            p[k * 2] = u * u * u * ax + 3 * u * u * t * bx + 3 * u * t * t * cx + t * t * t * dx;
            p[k * 2 + 1] = u * u * u * ay + 3 * u * u * t * by + 3 * u * t * t * cy + t * t * t * dy;
        }
        return p;
    }

    /** Digs up the end of a street where there's open ground on both sides (nothing it serves). */
    void trimStub(int[] e, boolean[] dug) {
        int x = e[0], y = e[1], n = e[2], dx = e[3], dy = e[4];
        int px = dx == 0 ? 1 : 0, py = 1 - px;
        byte along = (byte) (dx == 0 ? 1 : 2);
        int bare = 0;
        while (bare < 40) {
            int rx = x - dx * bare, ry = y - dy * bare;
            // (A level crossing with nothing past it goes too: the street can stop at the tracks.)
            boolean ok = (open(rx - px, ry - py) && open(rx + px * n, ry + py * n))
                    || (railAt(rx - px, ry - py) && railAt(rx + px * n, ry + py * n));
            for (int k = 0; k < n && ok; k++) {
                int ax = rx + px * k, ay = ry + py * k;
                ok = ax >= 0 && ay >= 0 && ax < city.w && ay < city.h && city.tiles[ay * city.w + ax] == ROAD && city.roadDir[ay * city.w + ax] == along;
            }
            if (!ok) break;
            bare++;
        }
        // A street that's nothing but stub (a lane's worth of road out in a field) is left for the lane.
        if (bare >= 40) return;
        for (int b = 0; b < bare; b++)
            for (int k = 0; k < n; k++) {
                int rx = x - dx * b, ry = y - dy * b, i = (ry + py * k) * city.w + rx + px * k;
                boolean rail = railAt(rx - px, ry - py);
                unroad(i);
                if (rail) city.tiles[i] = RAIL;
                dug[i] = true;
            }
    }

    boolean railAt(int x, int y) {
        return x >= 0 && y >= 0 && x < city.w && y < city.h && city.tiles[y * city.w + x] == RAIL;
    }

    boolean open(int x, int y) {
        if (x < 0 || y < 0 || x >= city.w || y >= city.h) return false;
        byte t = city.tiles[y * city.w + x];
        return t == GRASS || t == TREE || t == SAND || t == WATER;
    }

    void unroad(int i) {
        city.tiles[i] = GRASS;
        city.roadDir[i] = 0;
        city.mainRoad[i] = false;
    }

    boolean allOpen(int x, int y, int fw, int fh) {
        for (int j = y; j < y + fh; j++)
            for (int i = x; i < x + fw; i++) {
                if (i < 1 || j < 1 || i >= city.w - 1 || j >= city.h - 1) return false;
                byte t = city.tiles[j * city.w + i];
                if (t != GRASS && t != TREE) return false;
            }
        return true;
    }

    /** The unused block with room for (mw x mh) inside, closest to a distance from the centre (0-1). */
    int bigBlock(boolean[] used, int mw, int mh, float want) {
        int best = -1;
        float bestScore = Float.MAX_VALUE;
        for (int k = 0; k < city.blocks.size(); k++) {
            int[] b = city.blocks.get(k);
            int iw = b[2] - b[0] - 2, ih = b[3] - b[1] - 2;
            if (used[k] || !((iw >= mw && ih >= mh) || (iw >= mh && ih >= mw))) continue;
            float cx = (b[0] + b[2]) / 2f - city.w / 2f, cy = (b[1] + b[3]) / 2f - city.h / 2f;
            float d = (float) Math.sqrt(cx * cx + cy * cy) / (city.w * 0.5f);
            float score = Math.abs(d - want) + city.rnd.nextFloat() * 0.15f;
            if (score < bestScore) {
                bestScore = score;
                best = k;
            }
        }
        return best;
    }
    /** How far the town is off the middle of the map (tiles; 0 before 10.10). */
    int shiftX, shiftY;
    /** The wide band of open country (0 west, 1 east, 2 north, 3 south of town; -1 none): kept wild. */
    int wildSide = -1;

    /** In the wide band of country kept wild (see wildSide). */
    boolean inWildBand(int x, int y) {
        return inWildBand(x, y, false);
    }

    /** (10.52) The whole of the wild side, not just its outer half (a Default map's country base keeps off it all). */
    boolean inWildBand(int x, int y, boolean whole) {
        // ((10.33) Only the outer half of it: towns and farms are allowed nearer town.)
        boolean half = city.cfg.civic >= 17 && !whole;
        switch (wildSide) {
            case 0: return x < (half ? city.townX0 / 2 : city.townX0);
            case 1: return x >= (half ? (city.townX1 + city.w) / 2 : city.townX1);
            case 2: return y < (half ? city.townY0 / 2 : city.townY0);
            case 3: return y >= (half ? (city.townY1 + city.h) / 2 : city.townY1);
            default: return false;
        }
    }

    // ------------------------------------------------------------------ ((22.9) Desert City)

    /** A landmark design by its name, or null. */
    static Landmarks.Def landmarkNamed(String name) {
        for (Landmarks.Def d : Landmarks.all()) if (d.name.equals(name)) return d;
        return null;
    }

    /** Places design name on the parcel {x0, y0, x1, y1} if it fits. */
    boolean placeParcel(int[] pc, String name) {
        Landmarks.Def d = landmarkNamed(name);
        int pw = pc[2] - pc[0], ph = pc[3] - pc[1];
        if (d == null || Math.min(pw, ph) < d.minS || Math.max(pw, ph) < d.minL) return false;
        lotGen.placeLandmark(d, pc[0], pc[1], pw, ph);
        return true;
    }
}
