package com.instantaxtion.zombiesandbox;

import java.io.File;
import java.lang.reflect.Field;

/** Map generation: every preset and size, the 28.0 maps, islands and rivers, roads and routes, buildings. */
final class MapTests extends TestKit {
    static void register() {
        test("maps: every map builds, the classic map at every size, the themed maps with an army base; every city its own", FAST, 25, MapTests.class,
                "everyMapBuilds", "sizesAndOldSizes", "everyCityItsOwn");
        test("maps: the nine 28.0 maps - each its own place, one road network, base and police on it", FAST, 10, MapTests.class,
                "crossroadsCanyonFrozen", "bridgeSwampLighthouse", "airbaseBorderStadium");
        test("maps: islands, the river, the theme park, the space center and the wild country", FULL, 15, MapTests.class,
                "archipelagoRoads", "archipelagoPalms", "riverAndBridges", "riverStreets", "themePark", "spaceCenter", "countryside");
        test("roads and routes: one network, no roads to nowhere, drives joined up; the quick route tables agree with the long way", FAST, 20, MapTests.class,
                "oneRoadNetwork", "quickRoutes", "keptSearch", "pickupGrid");
        test("buildings: kinds and uses, old layouts rebuild, landmarks paint, walk-in rooms, the prison, malls and FBI office", FULL, 25, MapTests.class,
                "kindsAndOldLayouts", "doorsClearOfTrees", "buildingUses", "noGunStoresFbiOffice", "landmarksPaint", "walkInBuildings", "roomToRoom");
    }

    static void everyMapBuilds() throws Exception {
        // Every map at the smallest size; the classic map at every size. (Island maps at Tiny too, not the one size the
        // game gives them: it's whether they build.)
        for (int p = 0; p < CityConfig.PRESETS.length + 5; p++) {
            // (Maps taken out of the game don't build.)
            if (p < CityConfig.PRESETS.length && CityConfig.removed(p)) continue;
            boolean sizes = p >= CityConfig.PRESETS.length;
            int size = sizes ? p - CityConfig.PRESETS.length + 1 : CityConfig.TINY;
            CityConfig c = cfg(sizes ? 0 : p, size, 1000 + p);
            c.v[CityConfig.OPT_SIZE] = size;
            City city = new City(c, 0.1f);
            String name = CityConfig.PRESETS[c.v[CityConfig.OPT_PRESET]] + " size " + c.v[CityConfig.OPT_SIZE];
            check(city.buildings.size() > 20, name + " has buildings");
        }
        // ((26.1) Every map has an army base: the themed ones too.)
        for (int preset : new int[]{CityConfig.THEME_PARK, CityConfig.SPACE_CENTER, 9})
            check(new City(cfg(preset, CityConfig.LARGE, 5), 0.1f).mainBase() != null, CityConfig.PRESETS[preset] + " has an army base");
    }

    static void sizesAndOldSizes() throws Exception {
        // Each size bigger than the one before.
        CityConfig c = cfg(CityConfig.TINY, 1);
        int last = 0;
        for (int s = CityConfig.TINY; s <= CityConfig.HUGE; s++) {
            c.v[CityConfig.OPT_SIZE] = s;
            check(c.tiles() > last, CityConfig.class.getSimpleName() + " size " + s + " is bigger than the one before (" + c.tiles() + ")");
            last = c.tiles();
        }
        // Saved cities rebuild at the size they were made: a Medium map from before the smaller cities (map version 33),
        // and one from before 10.10.
        c.v[CityConfig.OPT_SIZE] = CityConfig.MEDIUM;
        c.civic = 33;
        check(c.tiles() == 320, "a saved Medium map keeps its size (" + c.tiles() + ")");
        c.civic = 2;
        check(c.tiles() == 256, "a city saved before 10.10 rebuilds at its old size");
        CityConfig old = new CityConfig();
        check(old.applyCode("12-44") && old.v[CityConfig.OPT_SIZE] == CityConfig.MEDIUM, "old codes keep their size name");
    }


    static void everyCityItsOwn() throws Exception {
        // Four seeds of the same map: each its own town shape, downtown, district mix and neighbourhoods.
        java.util.HashSet<String> sigs = new java.util.HashSet<String>(), shapes = new java.util.HashSet<String>();
        int newKinds = 0;
        for (int seed = 1; seed <= 4; seed++) {
            City city = new City(cfg(CityConfig.SMALL, seed), 0.1f);
            int[] dt = new int[City.DT_COUNT], nb = new int[City.NB_LOFTS + 1];
            for (City.District d : city.districts) dt[d.type]++;
            for (int[] b : city.blocks) if (b.length > 5 && b[4] == 3) nb[b[5]]++;
            for (int k = City.NB_TRAILERS; k <= City.NB_LOFTS; k++) newKinds += nb[k];
            String shape = city.townX0 + "," + city.townY0 + "-" + city.townX1 + "," + city.townY1;
            shapes.add(shape);
            sigs.add(shape + " " + (int) city.districts.get(0).x + "," + (int) city.districts.get(0).y + " " + java.util.Arrays.toString(dt) + " " + java.util.Arrays.toString(nb));
        }
        check(sigs.size() == 4, "four seeds, four different cities: " + sigs);
        check(shapes.size() >= 3, "the towns are different shapes: " + shapes);
        check(newKinds >= 2, "the new kinds of neighbourhood turn up (" + newKinds + ")");
        // A city built on map version 39 is exactly the one it always was (saves on it rebuild the same streets).
        CityConfig old = cfg(CityConfig.TINY, 1);
        old.civic = 39;
        City o = new City(old, 0.1f);
        long h = 1;
        for (int i = 0; i < o.w * o.h; i++) h = h * 31 + o.tiles[i];
        h = h * 31 + o.buildings.size();
        check(h == 0xedeec66c78d75134L, "a version 39 map is unchanged: " + Long.toHexString(h));
    }

    static void crossroadsCanyonFrozen() throws Exception {
        for (int preset : new int[]{CityConfig.CROSSROADS, CityConfig.CANYON, CityConfig.FROZEN_NORTH})
            for (long seed : new long[]{3, 11}) {
                CityConfig c = cfg(preset, CityConfig.SMALL, seed);
                // ((28.5) Crossroads and Canyon Town aren't offered any more, but still build for old saves and codes.)
                // ((31.1) The Frozen North neither.)
                check(c.preset() == preset && CityConfig.retired(CityConfig.OPT_PRESET, preset), CityConfig.PRESETS[preset] + " is retired");
                City city = new City(c, 0.05f);
                String m = CityConfig.PRESETS[preset] + "/" + seed + ": ";
                int w = city.w, h = city.h, n = w * h;
                byte[] t = city.tiles;
                // One road network.
                int[] comp = roadPieces(city), q = new int[n];
                int main = comp[n], stray = comp[n + 1];
                check(stray == 0, m + "the roads are one network (" + stray + " bits apart)");
                // The base and the police stations by a road of it.
                check(city.mainBase() != null, m + "has an army base");
                int police = 0, off = 0;
                for (City.Facility f : city.facilities) {
                    if (f.kind != City.FACILITY_BASE && f.kind != City.FACILITY_POLICE) continue;
                    if (f.kind == City.FACILITY_POLICE) police++;
                    float fx = f.kind == City.FACILITY_BASE ? f.gateX : f.x, fy = f.kind == City.FACILITY_BASE ? f.gateY : f.y;
                    // (A walk of a few dozen steps over open ground from its door or gate to a road of the network.)
                    boolean near = false;
                    java.util.HashMap<Integer, Integer> seen = new java.util.HashMap<Integer, Integer>();
                    java.util.ArrayDeque<Integer> open = new java.util.ArrayDeque<Integer>();
                    int s0 = Math.min(h - 1, Math.max(0, (int) (fy / City.T))) * w + Math.min(w - 1, Math.max(0, (int) (fx / City.T)));
                    open.add(s0);
                    seen.put(s0, 0);
                    while (!open.isEmpty() && !near) {
                        int cc = open.poll(), d = seen.get(cc), x = cc % w, y = cc / w;
                        if (comp[cc] == main) near = true;
                        if (d >= 60) continue;
                        for (int k = 0; k < 4; k++) {
                            int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                            if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                            int j = ny * w + nx;
                            if (seen.containsKey(j) || (city.solid[j] && t[j] != City.CAR)) continue;
                            seen.put(j, d + 1);
                            open.add(j);
                        }
                    }
                    if (!near) off++;
                }
                check(police > 0 && off == 0, m + "police (" + police + ") and the base by the road network (" + off + " not)");
                int water = 0, rock = 0, pines = 0, trees = 0;
                for (int i = 0; i < n; i++) {
                    if (t[i] == City.WATER) water++;
                    if (t[i] == City.ROCK) rock++;
                    if (t[i] == City.TREE) {
                        trees++;
                        if (city.treeKindAt(i) == Country.TK_PINE) pines++;
                    }
                }
                if (preset == CityConfig.CROSSROADS) {
                    CrossroadsMap x = (CrossroadsMap) city.region;
                    check(city.hwyAxis >= 0 && x.interchange && !city.overpasses.isEmpty() && city.hwyExits.size() >= 1, m + "the second highway crosses the first over an interchange");
                    check(x.built[CrossroadsMap.TRUCK] >= 1 && x.built[CrossroadsMap.MOTEL] >= 1 && x.built[CrossroadsMap.FUEL] >= 1, m + "a truck stop, a motel and fuel by the road (" + x + ")");
                    check((city.townX1 - city.townX0) * (city.townY1 - city.townY0) < w * h / 4, m + "a small town in open country");
                } else if (preset == CityConfig.CANYON) {
                    CanyonMap x = (CanyonMap) city.region;
                    check(x.bridges >= 1 && x.switchbacks >= 1 && x.rocks.size() >= 1, m + "bridges over the gorge, switchbacks down the cliff, buttes (" + x + ")");
                    check(rock > n / 25, m + "a cliff and a gorge of rock (" + rock + ")");
                    check((city.region.ground(10, 10) >> 16 & 255) > (city.region.ground(10, 10) >> 8 & 255), m + "red ground");
                } else {
                    FrozenNorthMap x = (FrozenNorthMap) city.region;
                    check(x.airstrip && city.airport != null && x.station && x.cabins >= 2 && x.piers >= 1, m + "an airstrip, the radar station, cabins and piers (" + x + ")");
                    check(water > n / 40, m + "a frozen bay (" + water + ")");
                    check(pines == trees, m + "every tree a pine");
                    check((city.region.ground(10, 10) & 255) > 200, m + "snowy ground");
                }
            }
    }

    static void bridgeSwampLighthouse() throws Exception {
        for (int preset : new int[]{CityConfig.BRIDGE_CITY, CityConfig.SWAMP, CityConfig.LIGHTHOUSE})
            for (long seed : new long[]{3, 28}) {
                CityConfig c = cfg(preset, CityConfig.SMALL, seed);
                City city = new City(c, 0.05f);
                int w = city.w, h = city.h, n = w * h;
                byte[] t = city.tiles;
                String m = CityConfig.PRESETS[preset] + "/" + seed + ": ";
                check(!CityConfig.retired(CityConfig.OPT_PRESET, preset), m + "offered");
                // The roads and lanes are one network.
                int[] comp = roadPieces(city), q = new int[n];
                int main = comp[n], stray = comp[n + 1];
                check(stray == 0, m + "one road network (" + stray + " bits on their own)");
                // The police stations and the army base are on it, and convoys can drive in.
                int posts = 0, off = 0;
                for (City.Facility f : city.facilities) {
                    if (f.kind != City.FACILITY_POLICE && f.kind != City.FACILITY_BASE) continue;
                    posts++;
                    int fx = (int) (f.x / City.T), fy = (int) (f.y / City.T);
                    boolean on = false;
                    for (int y = Math.max(0, fy - 18); y < Math.min(h, fy + 18) && !on; y++)
                        for (int x = Math.max(0, fx - 18); x < Math.min(w, fx + 18) && !on; x++) if (comp[y * w + x] == main) on = true;
                    if (!on) off++;
                }
                check(city.mainBase() != null && posts >= 2 && off == 0, m + "a base and police stations, all on the roads (" + posts + ", " + off + " off them)");
                float[] in = city.edgeRoad(new java.util.Random(1));
                check(in != null && comp[(int) (in[1] / City.T) * w + (int) (in[0] / City.T)] == main, m + "convoys drive in");
                java.util.HashSet<String> lm = new java.util.HashSet<String>();
                for (float[] d : city.decor) {
                    Landmarks.Def ld = (int) d[0] == City.D_LANDMARK ? Landmarks.get((int) d[5]) : null;
                    if (ld != null) lm.add(ld.name);
                }
                int water = 0, pier = 0, rock = 0, sand = 0, bridges = 0;
                for (int i = 0; i < n; i++) {
                    if (t[i] == City.WATER) water++;
                    if (t[i] == City.PIER) pier++;
                    if (t[i] == City.ROCK) rock++;
                    if (t[i] == City.SAND) sand++;
                    if (city.bridge != null && city.bridge[i] && t[i] == City.ROAD) bridges++;
                }
                if (preset == CityConfig.BRIDGE_CITY) {
                    int il = city.islandRects.length;
                    check(il >= 6 && il <= 10, m + "six to ten islands (" + il + ")");
                    // Every island's streets are on the one network: over the bridges.
                    int joined = 0;
                    for (int[] r : city.islandRects) {
                        boolean on = false;
                        for (int y = r[1]; y < r[3] && !on; y++) for (int x = r[0]; x < r[2] && !on; x++) if (comp[y * w + x] == main) on = true;
                        if (on) joined++;
                    }
                    check(joined == il && bridges > il * 20 && water > n / 3, m + "every island joined by long bridges (" + joined + "/" + il + ", " + bridges + " bridge tiles)");
                } else if (preset == CityConfig.SWAMP) {
                    int stilts = 0;
                    for (City.Building b : city.buildings) {
                        if (b.kind != City.HOUSE) continue;
                        int x0 = (int) (b.x0 / City.T), y0 = (int) (b.y0 / City.T), x1 = (int) Math.ceil(b.x1 / City.T), y1 = (int) Math.ceil(b.y1 / City.T);
                        boolean wet = false, walk = false;
                        for (int y = y0 - 1; y <= y1 && y < h; y++)
                            for (int x = x0 - 1; x <= x1 && x < w; x++) {
                                if (x < 0 || y < 0) continue;
                                if (t[y * w + x] == City.WATER) wet = true;
                                if (t[y * w + x] == City.PIER) walk = true;
                            }
                        if (wet && walk) stilts++;
                    }
                    check(stilts >= 4 && pier >= 80 && bridges > 0 && water > n / 6, m + "stilt houses on boardwalks over the water, a bridge over the bayou (" + stilts + ", " + pier + ", " + bridges + ")");
                    check(lm.contains("Airboat Landing") && lm.contains("Marine Supply"), m + "the airboat landing and the marine supply store: " + lm);
                } else {
                    check(lm.contains("Lighthouse") && lm.contains("Harbour Master"), m + "the lighthouse and the harbour master: " + lm);
                    check(rock > 100 && sand > 20 && water > n / 8 && pier > 10, m + "cliffs, beaches, the sea and piers (" + rock + ", " + sand + ", " + water + ", " + pier + ")");
                }
            }
    }

    static void airbaseBorderStadium() throws Exception {
        for (int preset : new int[]{CityConfig.AIRBASE, CityConfig.BORDER_TOWN, CityConfig.STADIUM_CITY})
            for (long seed : new long[]{2801, 2802}) {
                CityConfig c = cfg(preset, CityConfig.SMALL, seed);
                // ((28.5) Retired: not offered, but they still build for old saves and codes.)
                check(CityConfig.retired(CityConfig.OPT_PRESET, preset) && !CityConfig.removed(preset), CityConfig.PRESETS[preset] + " is retired");
                City city = new City(c, 0.05f);
                String m = CityConfig.PRESETS[preset] + "/" + seed + ": ";
                java.util.HashMap<String, Integer> lm = new java.util.HashMap<String, Integer>();
                for (float[] d : city.decor) {
                    Landmarks.Def ld = (int) d[0] == City.D_LANDMARK ? Landmarks.get((int) d[5]) : null;
                    if (ld != null) lm.put(ld.name, lm.containsKey(ld.name) ? lm.get(ld.name) + 1 : 1);
                }
                java.util.HashMap<String, Integer> named = new java.util.HashMap<String, Integer>();
                for (City.Building b : city.buildings) if (b.name != null) named.put(b.name, named.containsKey(b.name) ? named.get(b.name) + 1 : 1);
                City.Facility base = city.mainBase();
                check(base != null, m + "an army base");
                if (preset == CityConfig.AIRBASE) {
                    check(base.baseType == City.BT_AIR && city.runway != null && city.apron != null, m + "the air base, its runway and apron for the aircraft");
                    check(lm.containsKey("Airfield") && named.containsKey("Control Tower") && named.containsKey("Aircraft Hangar"), m + "airfield, tower and hangars: " + named.keySet());
                    check(named.containsKey("Fuel Farm") || lm.containsKey("Fuel Farm"), m + "a fuel farm: " + lm.keySet());
                    int barracks = 0;
                    for (City.Building b : city.buildings) if (b.kind == City.BARRACKS) barracks++;
                    check(barracks >= 10 && city.helipads.size() >= 4, m + "barracks rows (" + barracks + ") and the heliport (" + city.helipads.size() + ")");
                    CityConfig o = new CityConfig();
                    o.v[CityConfig.OPT_SIZE] = CityConfig.SMALL;
                    int ours = World.forces(c, city)[1], usual = World.forces(o, city)[1];
                    check(ours >= 380 && ours >= usual * 2, m + "far more soldiers (" + ours + " to " + usual + ")");
                } else if (preset == CityConfig.BORDER_TOWN) {
                    BorderMap bm = city.borderMap;
                    int gaps = 0, len = bm.vert ? city.h : city.w;
                    boolean open = false;
                    for (int a = 0; a < len; a++) {
                        byte t = city.tiles[bm.tile(a, bm.bc)];
                        check(t == City.FENCE || t == City.ROAD, m + "the border fence is unbroken but for the crossings (" + a + ": " + t + ")");
                        if (t == City.ROAD && !open) gaps++;
                        open = t == City.ROAD;
                    }
                    check(gaps >= 2 && gaps <= 3 && gaps == bm.crossings.size(), m + "two or three crossings (" + gaps + ")");
                    int patrol = 0;
                    for (City.Facility f : city.facilities) if (f.kind == City.FACILITY_POLICE && f.name.startsWith("Border Patrol")) patrol++;
                    check(patrol == 2, m + "a border patrol station each side (" + patrol + ")");
                    check(lm.containsKey("Customs Yard") && lm.get("Customs Yard") >= 2 * gaps - 1, m + "customs yards at the crossings: " + lm.get("Customs Yard"));
                    check(city.checkpoints.size() >= 2 * gaps, m + "checkpoints at the crossings");
                    check(bm.NEAR != bm.FAR && city.districts.get(city.districtAt.get(bm.tile(len / 2, bm.bc - 2))).theme
                            != city.districts.get(city.districtAt.get(bm.tile(len / 2, bm.bc + BorderMap.FENCE_W + 1))).theme, m + "each side its own look");
                } else {
                    check(lm.containsKey("Grand Stadium") && lm.containsKey("Fan Zone"), m + "the stadium and its fan zones: " + lm.keySet());
                    check(lm.containsKey("Basketball Arena") || lm.containsKey("Ice Rink Arena"), m + "the second arena");
                    check(city.railY0 >= 0 && city.stationX >= 0 && city.stadiumMap.stopX >= 0 && named.containsKey("Stadium Station"), m + "the light rail's two stations");
                    int pitches = 0;
                    for (float[] d : city.decor) if ((int) d[0] == City.D_FIELD) pitches++;
                    check(pitches >= 3, m + "training pitches (" + pitches + ")");
                    int malls = 0;
                    for (City.Building b : city.buildings) if (b.kind == City.MALL) malls++;
                    check(malls == 0, m + "no malls and their car parks");
                }
                // One road network: every road or lane joined to the rest (cars cross lots), the base's gate and
                // every police station on it.
                int w = city.w, h = city.h, n = w * h;
                byte[] t = city.tiles;
                int[] comp = roadPieces(city), q = new int[n];
                int main = comp[n], stray = comp[n + 1];
                check(stray == 0, m + "one road network (" + stray + " pieces apart)");
                for (City.Facility f : city.facilities) {
                    if (f.kind != City.FACILITY_POLICE && f.kind != City.FACILITY_BASE) continue;
                    // (Driven to: from the facility over ground a vehicle can cross, to the road network.)
                    boolean near = false;
                    int[] seen = new int[n];
                    int hd = 0, tl = 0;
                    int s0 = (int) (f.y / City.T) * w + (int) (f.x / City.T);
                    q[tl++] = s0;
                    seen[s0] = 1;
                    while (hd < tl && !near && tl < 200000) {
                        int cc = q[hd++], x = cc % w, y = cc / w;
                        if (comp[cc] == main) near = true;
                        for (int k = 0; k < 4; k++) {
                            int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                            if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                            int j = ny * w + nx;
                            byte o = t[j];
                            if (seen[j] != 0 || !(o == City.ROAD || o == City.DIRT || o == City.LOT || o == City.BASE || o == City.SIDEWALK || o == City.PLAZA || o == City.CAR)) continue;
                            seen[j] = 1;
                            q[tl++] = j;
                        }
                    }
                    check(near, m + f.name + " is on the road network");
                }
            }
    }

    static void archipelagoRoads() throws Exception {
        City city = new City(cfg(CityConfig.ARCHIPELAGO, CityConfig.COLOSSAL, 7920)); // (One whose small island lost every road before.)
        check(city.islandRects.length == 6, "five small islands round the big one (" + (city.islandRects.length - 1) + ")");
        int w = city.w, h = city.h, bad = 0;
        int[] comp = new int[w * h], q = new int[w * h];
        java.util.Arrays.fill(comp, -1);
        for (int s0 = 0; s0 < w * h; s0++) {
            if (city.tiles[s0] == City.WATER || comp[s0] >= 0) continue;
            int hd = 0, tl = 0, road = 0;
            q[tl++] = s0;
            comp[s0] = 1;
            while (hd < tl) {
                int cc = q[hd++], x = cc % w, y = cc / w;
                if (city.tiles[cc] == City.ROAD) road++;
                int[] nb = {x > 0 ? cc - 1 : -1, x < w - 1 ? cc + 1 : -1, y > 0 ? cc - w : -1, y < h - 1 ? cc + w : -1};
                for (int j : nb) if (j >= 0 && comp[j] < 0 && city.tiles[j] != City.WATER) { comp[j] = 1; q[tl++] = j; }
            }
            if (tl > 150 && road == 0) bad++;
        }
        check(bad == 0, "no land without a road on it (" + bad + " patches)");
    }

    static void archipelagoPalms() throws Exception {
        City a = new City(cfg(CityConfig.ARCHIPELAGO, CityConfig.COLOSSAL, 5));
        int palms = 0, outerHouses = 0, outerOther = 0;
        for (int i = 0; i < a.w * a.h; i++) if (a.tiles[i] == City.TREE && a.treeKindAt(i) == Country.TK_PALM) palms++;
        for (City.Building b : a.buildings) {
            if (a.island((int) ((b.x0 + b.x1) / 2 / City.T), (int) ((b.y0 + b.y1) / 2 / City.T)) < 1) continue;
            if (b.kind == City.HOUSE) outerHouses++;
            else if (b.kind == City.WAREHOUSE) outerOther++;
        }
        check(palms > 200 && outerHouses > 100 && outerOther < outerHouses / 10, "palms (" + palms + "), and houses, not works, on the outer islands (" + outerHouses + "/" + outerOther + ")");
    }

    static void riverAndBridges() throws Exception {
        City city = new City(cfg(CityConfig.RIVER_CITY, CityConfig.SMALL, 2201));
        check(city.river != null, "River City has a river");
        int wet = 0;
        for (int i = 0; i < city.tiles.length; i++) if (city.river[i] && city.tiles[i] == City.WATER) wet++;
        int L = city.riverAcross ? city.w : city.h, mid = L / 2;
        int bends = 0;
        for (int a = 1; a < L; a++) if (city.riverEdge0[a] != city.riverEdge0[a - 1]) bends++;
        check(wet > L * 6, "the river runs right across (" + wet + " tiles of water)");
        check(bends > L / 20, "and winds (" + bends + " changes of course)");
        check(city.riverBridges >= 2 && city.riverBridges <= 5, "a few bridges (" + city.riverBridges + ")");
        // Each road bridge carries a street straight on: road at both ends, in line.
        int aligned = 0, roadBridges = 0;
        for (int a = 0; a < L; a++) {
            int b0 = city.riverEdge0[a], b1 = city.riverEdge1[a];
            int t0 = city.riverAcross ? b0 * city.w + a : a * city.w + b0, t1 = city.riverAcross ? b1 * city.w + a : a * city.w + b1;
            if (city.bridge == null || !city.bridge[t0] || city.tiles[t0] != City.ROAD) continue;
            roadBridges++;
            int o0 = city.riverAcross ? (b0 - 1) * city.w + a : a * city.w + b0 - 1, o1 = city.riverAcross ? (b1 + 1) * city.w + a : a * city.w + b1 + 1;
            // (A car parked at the end of one is still the street.)
            if ((city.tiles[o0] == City.ROAD || city.tiles[o0] == City.CAR) && (city.tiles[o1] == City.ROAD || city.tiles[o1] == City.CAR)) aligned++;
        }
        check(roadBridges > 0 && aligned == roadBridges, "bridges line up with the streets (" + aligned + " of " + roadBridges + " tiles)");
        // The far bank can be walked to.
        int b0 = city.riverEdge0[mid] - 3, b1 = city.riverEdge1[mid] + 3;
        float[] north = city.riverAcross ? city.findWalkable((mid + 0.5f) * City.T, b0 * City.T) : city.findWalkable(b0 * City.T, (mid + 0.5f) * City.T);
        float[] south = city.riverAcross ? city.findWalkable((mid + 0.5f) * City.T, b1 * City.T) : city.findWalkable(b1 * City.T, (mid + 0.5f) * City.T);
        int W = city.w, start = city.tileIndex(north[0], north[1]), goal = city.tileIndex(south[0], south[1]);
        boolean[] seen = new boolean[W * city.h];
        int[] q = new int[W * city.h];
        int hd = 0, tl = 0;
        q[tl++] = start;
        seen[start] = true;
        while (hd < tl && !seen[goal]) {
            int cc = q[hd++];
            int[] nb = {cc - 1, cc + 1, cc - W, cc + W};
            for (int j : nb) if (j >= 0 && j < seen.length && !seen[j] && !city.solid[j]) { seen[j] = true; q[tl++] = j; }
        }
        check(seen[goal], "the far bank can be walked to");
    }

    static void riverStreets() throws Exception {
        // River City: no street runs into the water but onto a bridge.
        City rv = new City(cfg(CityConfig.RIVER_CITY, CityConfig.LARGE, 11), 0.1f);
        int stubs = 0, wide = 0;
        for (int y = 1; y < rv.h - 1; y++)
            for (int x = 1; x < rv.w - 1; x++) {
                int t = y * rv.w + x;
                if (rv.tiles[t] != City.ROAD || (rv.bridge != null && rv.bridge[t])) continue;
                // A road tile with water ahead along its way and road behind: a street ending in the river
                // (however wide; a street along the bank, with the water at its side, is fine).
                int dir = rv.roadDirAt(x, y);
                boolean ns = rv.tiles[t - rv.w] == City.ROAD || rv.tiles[t + rv.w] == City.ROAD;
                boolean we = rv.tiles[t - 1] == City.ROAD || rv.tiles[t + 1] == City.ROAD;
                if (ns && !we && (rv.tiles[t - rv.w] == City.WATER || rv.tiles[t + rv.w] == City.WATER)) stubs++;
                else if (we && !ns && (rv.tiles[t - 1] == City.WATER || rv.tiles[t + 1] == City.WATER)) stubs++;
                else if (dir == 2 && ((rv.tiles[t - 1] == City.WATER && rv.tiles[t + 1] == City.ROAD) || (rv.tiles[t + 1] == City.WATER && rv.tiles[t - 1] == City.ROAD))) wide++;
                else if (dir == 1 && ((rv.tiles[t - rv.w] == City.WATER && rv.tiles[t + rv.w] == City.ROAD) || (rv.tiles[t + rv.w] == City.WATER && rv.tiles[t - rv.w] == City.ROAD))) wide++;
            }
        // (A wide street's end a tile or two from the water at a junction on the bank, or the ring road at the
        // map's edge, is allowed for.)
        check(stubs == 0 && wide <= 3, "no street runs into the river (" + stubs + " tiles, " + wide + " wide)");
        check(rv.riverC != null && rv.bankAt(rv.riverC.length / 2, true) < rv.bankAt(rv.riverC.length / 2, false), "the banks' true line is known");
    }

    static void themePark() throws Exception {
        City city = new City(cfg(CityConfig.THEME_PARK, CityConfig.MASSIVE, 2501), 0.05f);
        check(city.parkRect != null && city.ferry != null && city.causewayEnd != null, "the park, the ferry and the road to the mainland");
        java.util.HashSet<String> rides = new java.util.HashSet<String>();
        for (float[] d : city.decor) {
            Landmarks.Def ld = (int) d[0] == City.D_LANDMARK ? Landmarks.get((int) d[5]) : null;
            if (ld != null && ld.districts == 0) rides.add(ld.name);
        }
        check(rides.contains("Thunder Mountain Coaster") && rides.contains("Ferris Wheel") && rides.size() >= 8, "the coaster, the wheel and its rides: " + rides);
        boolean clinic = false;
        for (City.Building b : city.buildings) if (Variants.get(b.variant) != null && "Staff Clinic".equals(Variants.get(b.variant).name)) clinic = city.island((int) (b.x0 / City.T), (int) (b.y0 / City.T)) == 1;
        check(clinic, "the staff clinic, in the staff town on the mainland");
        // One bridge: the bridge tiles are all in one run across the strait.
        int cols = 0;
        for (int x = 0; x < city.w; x++) {
            boolean any = false;
            for (int y = 0; y < city.h; y++) if (city.bridge[y * city.w + x]) any = true;
            if (any) cols++;
        }
        check(cols > 0 && cols <= 5, "one bridge to the mainland (" + cols + " columns of bridge)");
        City other = new City(cfg(CityConfig.MEDIUM, 2502), 0.05f);
        for (float[] d : other.decor) {
            Landmarks.Def ld = (int) d[0] == City.D_LANDMARK ? Landmarks.get((int) d[5]) : null;
            check(ld == null || ld.districts != 0, "no theme park rides elsewhere: " + (ld == null ? "" : ld.name));
        }
    }

    static void spaceCenter() throws Exception {
        City city = new City(cfg(CityConfig.SPACE_CENTER, CityConfig.MASSIVE, 2601), 0.05f);
        java.util.HashSet<String> ours = new java.util.HashSet<String>();
        for (float[] d : city.decor) {
            Landmarks.Def ld = (int) d[0] == City.D_LANDMARK ? Landmarks.get((int) d[5]) : null;
            if (ld != null && ld.districts == 0) ours.add(ld.name);
        }
        for (String n : new String[]{"Launch Pad A", "Vehicle Assembly Building", "Crawlerway", "Mission Control", "Landing Strip"})
            check(ours.contains(n), n + " is there: " + ours);
        boolean hangar = false;
        for (City.Building b : city.buildings)
            if (Variants.get(b.variant) != null && "Vehicle Assembly Building".equals(Variants.get(b.variant).name)) hangar = b.capacity >= 150 && b.doorX > 0;
        check(hangar, "the assembly building takes people in");
        City other = new City(cfg(CityConfig.MEDIUM, 2602), 0.05f);
        for (float[] d : other.decor) {
            Landmarks.Def ld = (int) d[0] == City.D_LANDMARK ? Landmarks.get((int) d[5]) : null;
            check(ld == null || !ours.contains(ld.name), "none of the space center's on Metropolis: " + (ld == null ? "" : ld.name));
        }
    }

    static void countryside() throws Exception {
        // A USA city: hills, a mountain, water, a park with trails, a campsite.
        City city = new City(cfg(CityConfig.LARGE, 7), 0.1f);
        int[] n = new int[20];
        for (byte b : city.tiles) n[b]++;
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (int q = 0; q < city.elev.length; q++) {
            float e = city.elev.get(q);
            lo = Math.min(lo, e);
            hi = Math.max(hi, e);
        }
        check(hi - lo > 30, "the land isn't flat (" + (int) lo + " to " + (int) hi + ")");
        check(n[City.ROCK] > 0 && n[City.WATER] > 0 && n[City.TRAIL] > 10, "mountain rock, water and trails ("
                + n[City.ROCK] + ", " + n[City.WATER] + ", " + n[City.TRAIL] + ")");
        check(city.parkName != null, "a nature park");
        boolean camp = false;
        for (City.Building b : city.buildings) if (b.name != null && b.name.startsWith("Camp Store")) camp = b.residents > 0;
        check(camp, "a campsite with campers");
    }

    static void oneRoadNetwork() throws Exception {
        for (int[] ps : new int[][]{{0, 3}, {1, 3}, {4, 3}, {2, 5}}) {
            City city = new City(cfg(ps[0], CityConfig.LARGE, ps[1]), 0.05f);
            int w = city.w, h = city.h, n = w * h;
            byte[] t = city.tiles;
            String m = "map " + ps[0] + "/" + ps[1] + ": ";
            // The roads and lanes are one network.
            int[] comp = roadPieces(city), q = new int[n];
            int main = comp[n], stray = comp[n + 1];
            check(stray == 0, m + "no bits of road or lane on their own (" + stray + ")");
            // Every home reaches that network by its own drive (or is on a street, by the pavement).
            int homes = 0, off = 0;
            int[] seen = new int[n];
            int stamp = 0;
            for (City.Building b : city.buildings) {
                if (b.kind != City.HOUSE && b.kind != City.BARN) continue;
                homes++;
                stamp++;
                int hd = 0, tl = 0;
                boolean ok = false;
                for (int y = (int) (b.y0 / City.T) - 1; y <= (int) Math.ceil(b.y1 / City.T); y++)
                    for (int x = (int) (b.x0 / City.T) - 1; x <= (int) Math.ceil(b.x1 / City.T); x++) {
                        if (x < 0 || y < 0 || x >= w || y >= h) continue;
                        int j = y * w + x;
                        if (t[j] == City.SIDEWALK) ok = true;
                        if ((t[j] == City.LOT || t[j] == City.DIRT || t[j] == City.ROAD || t[j] == City.CAR) && seen[j] != stamp) {
                            seen[j] = stamp;
                            q[tl++] = j;
                        }
                    }
                while (hd < tl && !ok && tl < 20000) {
                    int cc = q[hd++], x = cc % w, y = cc / w;
                    if ((t[cc] == City.ROAD || t[cc] == City.DIRT) && comp[cc] == main) ok = true;
                    for (int k = 0; k < 4; k++) {
                        int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        int j = ny * w + nx;
                        if (seen[j] == stamp || !(t[j] == City.LOT || t[j] == City.DIRT || t[j] == City.ROAD || t[j] == City.CAR)) continue;
                        seen[j] = stamp;
                        q[tl++] = j;
                    }
                }
                if (!ok) off++;
            }
            check(off * 50 <= homes, m + "every home (bar the odd one) is on a road or lane (" + off + " of " + homes + " not)");
            // No lane or road ending in the middle of nowhere.
            int dead = 0;
            for (int y = 1; y < h - 1; y++)
                for (int x = 1; x < w - 1; x++) {
                    int i = y * w + x;
                    if (!(t[i] == City.ROAD || t[i] == City.DIRT) || (city.bridge != null && city.bridge[i])) continue;
                    int four = 0;
                    for (int k = 0; k < 4; k++) {
                        byte o = t[i + (k == 0 ? 1 : k == 1 ? -1 : k == 2 ? w : -w)];
                        if (o == City.ROAD || o == City.DIRT || o == City.CAR || o == City.LOT) four++;
                    }
                    if (four > 1) continue;
                    boolean near = false;
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dx = -1; dx <= 1; dx++) {
                            byte o = t[(y + dy) * w + x + dx];
                            if (o == City.LOT || o == City.BUILDING || o == City.BASE || o == City.PIER || o == City.SIDEWALK || o == City.PLAZA
                                    || o == City.RAIL || o == City.TRAIL || o == City.WATER || o == City.SAND) near = true;
                        }
                    if (!near) dead++;
                }
            check(dead <= 6, m + "hardly a lane or road ends in nothing (" + dead + ")");
            // Drives known for drawing, and none winding from house to house: those are lanes now.
            check(city.drive != null, m + "drives are known");
            int[] dc = new int[n];
            java.util.Arrays.fill(dc, -1);
            int chains = 0;
            for (int i = 0; i < n; i++) {
                if (dc[i] >= 0 || !city.drive[i] || t[i] != City.LOT) continue;
                int hd = 0, tl = 0;
                q[tl++] = i;
                dc[i] = i;
                java.util.Set<Integer> hs = new java.util.HashSet<Integer>();
                while (hd < tl) {
                    int cc = q[hd++], x = cc % w, y = cc / w;
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx, ny = y + dy;
                            if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                            int j = ny * w + nx, bi = city.buildingAt.get(j);
                            // ((10.52) A house's wing is the same house.)
                            City.Building hb = bi >= 0 ? city.buildings.get(bi) : null;
                            if (hb != null && hb.main != null) hb = hb.main;
                            if (hb != null && (hb.kind == City.HOUSE || hb.kind == City.BARN)) hs.add(city.buildings.indexOf(hb));
                            if ((dx != 0 && dy != 0) || dc[j] >= 0 || !city.drive[j] || t[j] != City.LOT) continue;
                            dc[j] = i;
                            q[tl++] = j;
                        }
                }
                if (hs.size() >= 3) chains++;
            }
            check(chains == 0, m + "no tarmac drive winding past three homes (" + chains + ")");
        }
    }

    static void quickRoutes() throws Exception {
        int routes = 0, spots = 0;
        for (int preset : new int[]{0, 8, 14}) {
            World w = world(cfg(preset, CityConfig.SMALL, 21 + preset));
            City city = w.city;
            java.util.Random r = new java.util.Random(5);
            int[] f1 = new int[city.w * city.h], f2 = new int[city.w * city.h];
            // An army truck leaves its bay and another parks: the routes know.
            City.Facility base = city.mainBase();
            if (base != null) city.takeParkedTruck(base.x, base.y, 600);
            for (int k = 0; k < 25; k++) {
                float tx = r.nextFloat() * city.worldW(), ty = r.nextFloat() * city.worldH();
                int from = r.nextBoolean() ? -1 : r.nextInt(city.w * city.h);
                boolean strict = r.nextBoolean();
                City.slowSteps = false;
                boolean a = city.driveField(f1, tx, ty, strict, from);
                City.slowSteps = true;
                boolean b = city.driveField(f2, tx, ty, strict, from);
                City.slowSteps = false;
                check(a == b && java.util.Arrays.equals(f1, f2), "the same route from the table as the long way (" + preset + ", " + k + ")");
                routes++;
                // ((13.6) And off road, where the table now does the steps a car could take too.)
                City.slowSteps = false;
                a = city.offroadField(f1, tx, ty, from);
                City.slowSteps = true;
                b = city.offroadField(f2, tx, ty, from);
                City.slowSteps = false;
                check(a == b && java.util.Arrays.equals(f1, f2), "the same off-road route from the table as the long way (" + preset + ", " + k + ")");
            }
            // A barricade goes up: nobody walks through it.
            float[] spot = null;
            for (int k = 0; k < 200 && spot == null; k++) {
                float x = r.nextFloat() * city.worldW(), y = r.nextFloat() * city.worldH();
                if (!city.circleBlocked(x, y, 4)) spot = w.placeBarricade(x, y);
            }
            if (spot != null) check(city.circleBlocked(spot[0], spot[1], 4), "a new barricade blocks the way");
            for (int k = 0; k < 4000; k++) {
                float x = r.nextFloat() * city.worldW(), y = r.nextFloat() * city.worldH(), rad = 2 + r.nextFloat() * 10;
                City.slowSteps = false;
                boolean a = city.circleBlocked(x, y, rad);
                City.slowSteps = true;
                boolean b = city.circleBlocked(x, y, rad);
                City.slowSteps = false;
                check(a == b, "the open-ground shortcut agrees with the long way");
                spots++;
            }
        }
        check(routes > 50 && spots > 10000, "checked enough");
    }

    static void keptSearch() throws Exception {
        World w = new World(cfg(CityConfig.LARGE, 36));
        City city = w.city;
        java.util.Random r = new java.util.Random(9);
        int compared = 0, same = 0;
        for (int round = 0; round < 2; round++) {
            if (round == 1) {
                // A barricade goes up somewhere: the kept searches are thrown away and the routes still agree.
                for (int k = 0; k < 200; k++) {
                    float x = r.nextFloat() * city.worldW(), y = r.nextFloat() * city.worldH();
                    if (!city.circleBlocked(x, y, 4) && w.placeBarricade(x, y) != null) break;
                }
            }
            for (int goal = 0; goal < 6; goal++) {
                float gx, gy;
                do {
                    gx = r.nextFloat() * city.worldW();
                    gy = r.nextFloat() * city.worldH();
                } while (city.circleBlocked(gx, gy, 4));
                for (int k = 0; k < 12; k++) {
                    float sx, sy;
                    do {
                        sx = r.nextFloat() * city.worldW();
                        sy = r.nextFloat() * city.worldH();
                    } while (city.circleBlocked(sx, sy, 4) || Math.max(Math.abs(sx - gx), Math.abs(sy - gy)) < 40 * City.T);
                    int[] kept = city.findPath(sx, sy, gx, gy, 9000);
                    Nav.plainSearch = true;
                    int[] plain = city.findPath(sx, sy, gx, gy, 9000);
                    Nav.plainSearch = false;
                    check((kept == null) == (plain == null), "a way there either way, or neither");
                    if (kept == null || plain == null) continue;
                    float lk = routeLength(city, sx, sy, kept), lp = routeLength(city, sx, sy, plain);
                    // ((16.0) Within 8%, not 3: the kept search is the exact cheapest by the routes' own costs - a step
                    // beside a wall or over rough ground dearer - where the other's estimate can cut closer along a
                    // wall. Out of the fenced airport grounds (a single gate) that came to 6% further on foot.)
                    check(lk <= lp * 1.08f + 2 * City.T, "as short from the kept search (" + lk + " vs " + lp + ")");
                    check(kept[kept.length - 1] == plain[plain.length - 1], "to the same place");
                    compared++;
                    if (java.util.Arrays.equals(kept, plain)) same++;
                }
            }
        }
        check(compared > 100 && same * 2 > compared, "compared enough (" + compared + ", " + same + " the very same)");
    }

    static void pickupGrid() throws Exception {
        World w = new World(cfg(CityConfig.MEDIUM, 9));
        java.util.Random r = new java.util.Random(3);
        int same;
        // Things on the ground: the same, by the grid and the long way.
        w.pickups.clear();
        for (int k = 0; k < 120; k++) {
            World.Pickup p = new World.Pickup();
            p.x = r.nextFloat() * w.city.worldW();
            p.y = r.nextFloat() * w.city.worldH();
            p.claimed = r.nextInt(5) == 0;
            w.pickups.add(p);
        }
        same = 0;
        for (int k = 0; k < 400; k++) {
            float x = r.nextFloat() * w.city.worldW(), y = r.nextFloat() * w.city.worldH(), radius = 50 + r.nextFloat() * 1500;
            World.Pickup want = null;
            float bd = radius * radius;
            for (World.Pickup p : w.pickups) {
                if (p.claimed || p.drop > 0 || p.melee > 0) continue;
                float d = (p.x - x) * (p.x - x) + (p.y - y) * (p.y - y);
                if (d < bd) {
                    bd = d;
                    want = p;
                }
            }
            if (w.nearestPickup(x, y, radius) == want) same++;
            // (And when one goes, it's gone from the grid at once.)
            if (k == 200) w.pickups.remove(0);
        }
        check(same == 400, "the nearest thing on the ground by the grid is the nearest one (" + same + " of 400)");
        // Open ground, kept a bit a tile: the same as working it out the long way, and after a tile changes.
        int differ = 0;
        for (int k = 0; k < 4000; k++) {
            float x = r.nextFloat() * w.city.worldW(), y = r.nextFloat() * w.city.worldH(), rad = 2 + r.nextFloat() * 5;
            boolean quick = w.city.circleBlocked(x, y, rad);
            City.slowSteps = true;
            boolean slow = w.city.circleBlocked(x, y, rad);
            City.slowSteps = false;
            if (quick != slow) differ++;
        }
        check(differ == 0, "open ground by the bits agrees with the long way (" + differ + " differ)");
    }

    static void kindsAndOldLayouts() throws Exception {
        {
            City city = new City(cfg(CityConfig.MASSIVE, 42));
            java.util.HashSet<String> names = new java.util.HashSet<String>();
            for (City.Building b : city.buildings) if (b.typeName() != null) names.add(b.typeName());
            int found = 0;
            for (String n : new String[]{"Barber shop", "Brownstone", "Trailer home", "BBQ smokehouse"}) if (names.contains(n)) found++;
            check(found >= 2, found + " of the newer kinds of building");
        }
        // (The same city as version 10.0 built from this seed, building for building, for games saved
        // before the government quarter: map version 0, kept for old saves.)
        CityConfig c = cfg(0, CityConfig.MEDIUM, 42);
        c.civic = 0;
        City city = new City(c);
        long h = 17;
        for (City.Building b : city.buildings) h = h * 31 + (long) (b.x0 * 7 + b.y0 * 13 + b.x1 * 17 + b.y1 * 19 + b.kind);
        check(city.buildings.size() == 396 && h == 0x66f79941134006cfL, "same layout as before (" + city.buildings.size() + ", " + Long.toHexString(h) + ")");
    }

    static void doorsClearOfTrees() throws Exception {
        City city = new City(cfg(0, CityConfig.MEDIUM, 6), 0.1f);
        int blocked = 0;
        for (City.Building b : city.buildings) {
            if (b.doorX <= 0) continue;
            int tx = (int) (b.doorX / City.T), ty = (int) (b.doorY / City.T);
            for (int dy = -1; dy <= 1; dy++)
                for (int dx = -1; dx <= 1; dx++) if (city.tiles[(ty + dy) * city.w + tx + dx] == City.TREE) blocked++;
        }
        check(blocked == 0, blocked + " trees in doorways");
    }

    static void buildingUses() throws Exception {
        City city = new City(cfg(CityConfig.SMALL, 3), 0.1f);
        check(Variants.use(Variants.gov(City.ARMORY)) == Variants.U_NONE, "government buildings have no special use now");
        int tools = -1, vault = -1, glass = -1;
        for (int i = 0; i < Variants.ALL_VARIANTS.size(); i++) {
            String n = Variants.ALL_VARIANTS.get(i).name;
            if (n.equals("Lumber yard")) tools = i;
            if (n.equals("Self storage")) vault = i;
            if (n.equals("Boutique")) glass = i;
        }
        check(Variants.use(tools) == Variants.U_TOOLS && Variants.use(vault) == Variants.U_VAULT && Variants.use(glass) == Variants.U_GLASS,
                "lumber yards, storage units and boutiques each have their use");
        City.Building b = city.buildings.get(0);
        b.use = Variants.U_VAULT;
        float strong = World.doorWeakness(b);
        b.use = Variants.U_GLASS;
        check(strong < 0.5f && World.doorWeakness(b) > 1.2f, "a vault door holds far longer than a glass front");
    }

    static void noGunStoresFbiOffice() throws Exception {
        World w = world(cfg(CityConfig.MEDIUM, 77));
        for (int f = 0; f < 60; f++) w.update(1 / 30f);
        // A walled prison on the edge of town, and two malls, on a Medium map.
        int malls = 0;
        for (City.Building b : w.city.buildings) if (b.kind == City.MALL) malls++;
        check(w.city.prison != null, "a walled prison on the edge of town");
        check(malls >= 2, "two malls (" + malls + ")");
        int guns = 0, fire = 0;
        for (City.Building b : w.city.buildings) if (b.kind == City.SHOP && b.shopType == 1) guns++;
        for (City.Facility f : w.city.facilities) if (f.kind == City.FACILITY_FIRE) fire++;
        check(guns == 0 && fire == 0, "no gun stores (" + guns + ") and no fire stations (" + fire + ")");
        City.Facility office = null;
        for (City.Facility f : w.city.facilities) if (f.fed) office = f;
        check(office != null && office.kind == City.FACILITY_POLICE, "an FBI field office");
        int agents = 0, cars = 0;
        for (Entity e : w.entities) if (e.type == Entity.COP && e.agency == 4 && Math.hypot(e.x - office.x, e.y - office.y) < 400) agents++;
        for (Fleet.Vehicle v : w.fleet.vehicles) if (v.agency == 4 && Math.hypot(v.x - office.x, v.y - office.y) < 500) {
            cars++;
            agents += v.crew.size();
        }
        check(agents >= 8 && cars >= 2, "agents (" + agents + ") and their cars (" + cars + ") at the office from the start");
        // A small share of civilians carry a gun.
        int civ = 0, armed = 0;
        for (Entity e : w.entities) if (e.type == Entity.CIVILIAN) {
            civ++;
            if (e.hasGun) armed++;
        }
        check(armed > 0 && armed < civ * 0.16f, "a few civilians armed (" + armed + " of " + civ + ")");
    }


    static void landmarksPaint() throws Exception {
        check(Landmarks.all().size() >= 250, "the landmark designs (" + Landmarks.all().size() + ")");
        // Every design paints, small and large, wide and tall, with its buildings inside the block.
        android.graphics.Bitmap bm = android.graphics.Bitmap.createBitmap(600, 600, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Paint p = new android.graphics.Paint();
        City small = new City(cfg(CityConfig.TINY, 1), 0.1f);
        for (Landmarks.Def d : Landmarks.all()) {
            for (int[] sz : new int[][]{{d.minS, d.minL}, {d.minL, d.minS}, {24, 18}}) {
                android.graphics.Canvas cv = new android.graphics.Canvas(bm);
                int bw = sz[0], bh = sz[1];
                d.ground(small, cv, p, 0, 0, bw * City.T, bh * City.T, new java.util.Random(1));
                int[][] plan = d.plan(bw, bh, new java.util.Random(2));
                for (int k = 0; k < plan.length; k++) {
                    int[] q = plan[k];
                    check(q[0] >= 0 && q[1] >= 0 && q[0] + q[2] <= bw && q[1] + q[3] <= bh && q[2] > 0 && q[3] > 0, d.name + " inside its " + bw + "x" + bh + " block");
                    d.roof(small, cv, p, k, q[0] * City.T, q[1] * City.T, (q[0] + q[2]) * City.T, (q[1] + q[3]) * City.T, new java.util.Random(3));
                }
            }
        }
        // A Standard city: ten or more, all different, and fewer whole blocks of car park.
        City city = new City(cfg(CityConfig.LARGE, 5), 0.1f);
        java.lang.reflect.Field df = City.class.getDeclaredField("decor");
        df.setAccessible(true);
        java.util.Set<Integer> kinds = new java.util.HashSet<Integer>();
        int n = 0;
        for (Object o : (java.util.List<?>) df.get(city)) {
            float[] d = (float[]) o;
            if ((int) d[0] == City.D_LANDMARK) {
                n++;
                kinds.add((int) d[5]);
            }
        }
        check(n >= 10 && kinds.size() == n, "landmarks in a Standard city, none twice (" + n + ", " + kinds.size() + " kinds)");
    }

    static void walkInBuildings() throws Exception {
        CityConfig c = cfg(CityConfig.LARGE, 41);
        World w = new World(c);
        w.populate(c);
        City city = w.city;
        // A building people can walk into, with a room or two.
        City.Building b = null;
        int enterable = 0;
        for (City.Building o : city.buildings)
            if (city.enterable(o)) {
                enterable++;
                if (b == null && city.planOf(o).rooms.size() >= 3 && o.x1 - o.x0 < 140 && o.y1 - o.y0 < 140) b = o;
            }
        check(enterable > city.buildings.size() / 3, "most buildings can be walked into (" + enterable + " of " + city.buildings.size() + ")");
        check(b != null && b.walls != null && b.walls.length >= 20, "with walls round it and between its rooms");
        // The walls are in the way; the doorway isn't.
        check(!city.circleBlocked(b.inX, b.inY, 3.8f) && !city.circleBlocked(b.gapX, b.gapY, 3.8f), "the doorway is open");
        float wx = (b.x0 + b.x1) / 2, wy = b.y0 + 1;
        if (Math.abs(b.gapY - (b.y0 + 1)) < 2 && Math.abs(b.gapX - wx) < 12) wy = b.y1 - 1;
        check(city.circleBlocked(wx, wy, 3.8f), "a wall is in the way");
        check(city.solidAt(b.x0 + 1, (b.y0 + b.y1) / 2) && !city.solidAt(b.inX, b.inY), "solid where the walls are, not inside");
        Interiors.Plan p = city.planOf(b);
        float[] far = null;
        for (float[] sp : p.spots) if (far == null || Math.hypot(sp[0] - b.inX, sp[1] - b.inY) > Math.hypot(far[0] - b.inX, far[1] - b.inY)) far = sp;
        check(far != null && city.insideOf(far[0], far[1]) == b, "a place to be inside it");
        // Seen through the doorway, not through the walls.
        check(city.los(b.doorX, b.doorY, b.inX, b.inY), "seen through the door");
        float ox = b.doorX < b.x0 ? b.x0 - 20 : b.x1 + 20, oy = (b.y0 + b.y1) / 2;
        if (b.doorX >= b.x0 && b.doorX <= b.x1) ox = b.x0 - 20;
        check(!city.los(ox, oy, (b.x0 + b.x1) / 2, (b.y0 + b.y1) / 2), "not through the wall");
        // Walking in from the street to the far room, and out again: every step a short one, never a jump.
        WM walk = wm("walkToSpot", Entity.class, float.class, float.class, float.class);
        walk.setAccessible(true);
        WM move = wm("move", Entity.class, float.class);
        move.setAccessible(true);
        java.lang.reflect.Field budget = World.class.getDeclaredField("pathBudget");
        budget.setAccessible(true);
        float[] start = city.findWalkable(b.doorX + (b.doorX - (b.x0 + b.x1) / 2) * 0.6f, b.doorY + (b.doorY - (b.y0 + b.y1) / 2) * 0.6f);
        Entity e = w.spawn(Entity.CIVILIAN, start[0], start[1]);
        check(city.insideOf(e.x, e.y) == null, "starts outside");
        float jump = 0;
        boolean got = false;
        for (int f = 0; f < 30 * 40 && !got; f++) {
            budget.setFloat(w, 3);
            float px = e.x, py = e.y;
            walk.invoke(w, e, far[0], far[1], e.speed);
            move.invoke(w, e, 1 / 30f);
            jump = Math.max(jump, (float) Math.hypot(e.x - px, e.y - py));
            got = Math.hypot(e.x - far[0], e.y - far[1]) < 5;
        }
        check(got, "walked in through the door to the far room (" + (int) Math.hypot(e.x - far[0], e.y - far[1]) + " short)");
        boolean out = false;
        for (int f = 0; f < 30 * 40 && !out; f++) {
            budget.setFloat(w, 3);
            float px = e.x, py = e.y;
            walk.invoke(w, e, start[0], start[1], e.speed);
            move.invoke(w, e, 1 / 30f);
            jump = Math.max(jump, (float) Math.hypot(e.x - px, e.y - py));
            out = Math.hypot(e.x - start[0], e.y - start[1]) < 5;
        }
        check(out, "and back out to the street");
        check(jump < 3, "on foot all the way, no jumps (" + jump + ")");
        e.dead = true;
        e.removed = true;
        // An ordinary day: people go in and out of buildings and are there inside, never taken off the map.
        int entered = 0, left = 0, seenInside = 0;
        java.util.IdentityHashMap<Entity, Boolean> was = new java.util.IdentityHashMap<Entity, Boolean>();
        for (int f = 0; f < 30 * 60; f++) {
            w.update(1 / 30f);
            w.evCount = 0;
            if (f % 15 != 0) continue;
            for (Entity o : w.entities) {
                if (o.dead) continue;
                boolean in = city.insideOf(o.x, o.y) != null;
                Boolean prev = was.put(o, in);
                if (prev != null && !prev && in) entered++;
                if (prev != null && prev && !in) left++;
            }
        }
        for (Entity o : w.entities) if (!o.dead && city.insideOf(o.x, o.y) != null) seenInside++;
        int listed = 0;
        for (City.Building o : city.buildings) listed += o.visitors.size() + o.occupants.size();
        check(entered > 30 && left > 10, "people walk in and out (" + entered + " in, " + left + " out)");
        check(seenInside > 10 && listed == 0, "there inside, on the map (" + seenInside + "), nobody vanished into a building (" + listed + ")");
        w.countInside();
        int over = 0;
        for (City.Building o : w.occupied) if (o.inside > World.room(o) * 2) over++;
        check(over == 0, "no building packed past its room (" + over + ")");
        // One of the dead after someone who's gone inside: in it goes after them.
        Entity v = null;
        for (Entity o : w.entities)
            if (!o.dead && o.type == Entity.CIVILIAN && o.stayIn != null && city.insideOf(o.x, o.y) == o.stayIn) {
                v = o;
                break;
            }
        check(v != null, "someone inside");
        City.Building vb = city.insideOf(v.x, v.y);
        Entity z = w.spawn(Entity.ZOMBIE, vb.doorX, vb.doorY);
        z.memory = 10;
        z.lastX = v.x;
        z.lastY = v.y;
        // (Nobody else about to shoot it down or draw it off first: this is about it following them in.)
        Quiet calm = quiet(w, vb.doorX, vb.doorY, 400, v, z);
        boolean zin = false;
        for (int f = 0; f < 30 * 15 && !zin && !z.dead; f++) {
            step(w, calm);
            zin = city.insideOf(z.x, z.y) == vb;
        }
        check(zin, "the dead follow them in through the door");
    }

    static void roomToRoom() throws Exception {
        CityConfig c = cfg(CityConfig.TINY, 2751);
        World w = new World(c);
        w.populate(c);
        // Through buildings, from the first room to each of the others: everyone gets there - none pressed into a
        // corner by a doorway, or a wall that ends in the middle of one, for good.
        WM move = wm("move", Entity.class, float.class);
        move.setAccessible(true);
        int walks = 0, stuck = 0, buildings = 0;
        String where = "";
        for (City.Building o : w.city.buildings) {
            if (buildings >= 30) break;
            if (!w.city.enterable(o) || w.city.planOf(o).rooms.size() < 3) continue;
            buildings++;
            java.util.List<float[]> rooms = w.city.planOf(o).rooms;
            float ax = (rooms.get(0)[0] + rooms.get(0)[2]) / 2, ay = (rooms.get(0)[1] + rooms.get(0)[3]) / 2;
            if (w.city.circleBlocked(ax, ay, 4)) continue;
            for (int k = 1; k < rooms.size(); k++) {
                float bx = (rooms.get(k)[0] + rooms.get(k)[2]) / 2, by = (rooms.get(k)[1] + rooms.get(k)[3]) / 2;
                if (w.city.circleBlocked(bx, by, 4)) continue;
                Entity e = w.spawn(Entity.CIVILIAN, ax, ay);
                boolean got = false;
                for (int f = 0; f < 30 * 25 && !got; f++) {
                    w.indoors.indoorMove(e, bx, by, e.speed);
                    move.invoke(w, e, 1 / 30f);
                    float d = (float) Math.hypot(e.x - bx, e.y - by);
                    got = d < 5 || e.routeGiveUp && d < 12;
                }
                walks++;
                if (!got) {
                    stuck++;
                    where = " e.g. at " + (int) e.x + "," + (int) e.y + ", " + (int) Math.hypot(e.x - bx, e.y - by) + " short";
                }
                e.dead = e.removed = true;
            }
        }
        check(walks > 40, "walks room to room (" + walks + ")");
        check(stuck == 0, "every one gets there (" + stuck + " of " + walks + " stuck" + where + ")");
    }
}
