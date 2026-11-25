package com.instantaxtion.zombiesandbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * ((27.4) Save version 79.) The vehicles themselves, kept in a save: every police car, army truck, ambulance, tank,
 * command truck, armed pickup and evacuation bus, where it was and what it was doing, with the people aboard it still
 * aboard, and the supply and evacuation convoys they drive in. Until now a loaded game started its vehicles again from
 * nothing - everyone aboard out on foot where they'd been, and the stations, bases and command trucks stocked afresh at
 * home.
 *
 * Not kept (as before): everyday traffic (more comes along), helicopters, planes, trains and boats (the helicopters are
 * back on their pads, the trains start again, the police boats are back at their moorings - everyone aboard ashore),
 * roadblock cars (the safe zone sets its roadblocks up again), and the car you're driving yourself.
 *
 * A vehicle's numbers and flags are written by name (a table of field names and kinds, once, then each vehicle's
 * values in that order), so a field added to Fleet.Vehicle later is kept without anything more here, and a save from
 * before it (or after one goes) still loads: names it doesn't know are skipped, missing ones keep their defaults.
 * Links to people, places and other vehicles are written as indexes, as the rest of the save does. ((27.5) The calls a
 * vehicle was on its way to are kept after the vehicles, from version 80: see CallSave.) Routes are found again on load.
 */
final class VehicleSave {
    private VehicleSave() {
    }

    /** The parts of a vehicle that are only caches for driving (rebuilt as it drives), or bookkeeping of the list it's on. */
    private static final String[] VEHICLE_SKIP = {"gi", "removedFromFleet", "pathT", "clearedJ", "waitJ", "exitJ", "exitFrom",
            "exitTile", "pdx", "pdy", "corN", "corOfT", "corOfDx", "corOfDy", "corOfPi", "jaFrom", "jaT", "jaDx", "jaDy", "jaId",
            "jaTile", "jaDirX", "jaDirY", "footFrame", "lodTick", "holdDepth", "queued", "queuedHeld", "held"};

    /** Whether a vehicle is kept in a save (the rest are left out, their people saved on foot where they were). */
    static boolean keep(Fleet.Vehicle v) {
        if (v.removedFromFleet || v.player != null || v.block != null || v.guard != null) return false;
        switch (v.type) {
            case Fleet.CRUISER: case Fleet.TRUCK: case Fleet.AMBULANCE: case Fleet.TANK:
                return true;
            case Fleet.CAR:
                // (Not everyday traffic: a bus on an evacuation run, a survivors' pickup, a convoy's bus, or one left parked.)
                return v.parked || v.evacStop >= 0 || v.kind == Fleet.K_PICKUP || v.convoy != null || v.state == Fleet.ABANDONED;
            default:
                return false;
        }
    }

    // ------------------------------------------------------------------ writing

    static void write(DataOutputStream out, World w, List<Fleet.Vehicle> kept, IdentityHashMap<Fleet.Vehicle, Integer> keptIndex,
                      IdentityHashMap<Entity, Integer> people) throws IOException {
        City city = w.city;
        Dispatch d = w.dispatch;
        List<Field> vf = fields(Fleet.Vehicle.class, VEHICLE_SKIP);
        writeTable(out, vf);
        out.writeInt(kept.size());
        for (Fleet.Vehicle v : kept) {
            writeValues(out, vf, v);
            writePeople(out, v.crew, people);
            writePeople(out, v.riders, people);
            writePeople(out, v.squad, people);
            writePeople(out, v.flight, people);
            out.writeBoolean(v.team != null);
            if (v.team != null) writePeople(out, v.team, people);
            out.writeInt(v.realDriver ? person(v.driver, people) : -1);
            out.writeInt(person(v.pilotComing, people));
            out.writeInt(vehicle(v.stopping, keptIndex));
            out.writeInt(vehicle(v.clearing, keptIndex));
            out.writeInt(city.facilities.indexOf(v.ammoRun));
            out.writeInt(city.facilities.indexOf(v.depot));
            out.writeInt(city.facilities.indexOf(v.station));
            out.writeInt(city.facilities.indexOf(v.dozerBase));
            out.writeInt(city.facilities.indexOf(v.restockFac));
            out.writeInt(v.restockShop == null ? -1 : city.buildings.indexOf(v.restockShop));
            out.writeInt(d.zones.indexOf(v.zone));
            out.writeInt(base(v.pbase, city));
            out.writeInt(base(v.ptarget, city));
            out.writeInt(w.convoys.columns.indexOf(v.convoy));
        }
        // The convoys: each column, its vehicles and its place on the map's list of convoys.
        List<Field> cf = fields(Convoys.Column.class, new String[0]);
        List<Field> tf = fields(Dispatch.Convoy.class, new String[0]);
        writeTable(out, cf);
        writeTable(out, tf);
        out.writeInt(w.convoys.columns.size());
        for (Convoys.Column c : w.convoys.columns) {
            writeValues(out, cf, c);
            out.writeInt(c.vs.size());
            for (Fleet.Vehicle v : c.vs) out.writeInt(vehicle(v, keptIndex));
            out.writeInt(c.queue.size());
            for (Integer q : c.queue) out.writeInt(q);
            out.writeInt(d.zones.indexOf(c.zone));
            out.writeInt(base(c.base, city));
            out.writeInt(city.facilities.indexOf(c.station));
            out.writeBoolean(c.tracker != null);
            if (c.tracker != null) writeValues(out, tf, c.tracker);
        }
        // The command trucks (Ops writes the rest of each post: where its tent is, and its tallies).
        out.writeInt(vehicle(w.ops.police.truck, keptIndex));
        out.writeInt(vehicle(w.ops.army.truck, keptIndex));
        // Who on foot belongs to which vehicle (its crew out at a call), and who's walking to one for a lift.
        int links = 0;
        for (Entity e : people.keySet()) {
            if (e.rig != null && keptIndex.containsKey(e.rig)) links++;
            if (e.ride != null && keptIndex.containsKey(e.ride)) links++;
        }
        out.writeInt(links);
        for (java.util.Map.Entry<Entity, Integer> en : people.entrySet()) {
            Entity e = en.getKey();
            if (e.rig != null && keptIndex.containsKey(e.rig)) {
                out.writeInt(en.getValue());
                out.writeInt(keptIndex.get(e.rig));
                out.writeByte(0);
            }
            if (e.ride != null && keptIndex.containsKey(e.ride)) {
                out.writeInt(en.getValue());
                out.writeInt(keptIndex.get(e.ride));
                out.writeByte(1);
            }
        }
    }

    // ------------------------------------------------------------------ reading

    /**
     * Puts the vehicles back. people: everyone read from the save, in the order they were written (those aboard among
     * them, read as if on foot: they're taken off the map again here and put back aboard).
     */
    static void read(DataInputStream in, World w, List<Entity> people) throws IOException {
        City city = w.city;
        Dispatch d = w.dispatch;
        Fleet fleet = w.fleet;
        Table vf = readTable(in, Fleet.Vehicle.class);
        int n = in.readInt();
        ArrayList<Fleet.Vehicle> vs = new ArrayList<Fleet.Vehicle>(n);
        int[][] vlinks = new int[n][];
        IdentityHashMap<Entity, Boolean> aboard = new IdentityHashMap<Entity, Boolean>();
        for (int i = 0; i < n; i++) {
            Fleet.Vehicle v = new Fleet.Vehicle();
            readValues(in, vf, v);
            readPeople(in, v.crew, people, aboard, true);
            readPeople(in, v.riders, people, aboard, true);
            readPeople(in, v.squad, people, aboard, false);
            readPeople(in, v.flight, people, aboard, false);
            if (in.readBoolean()) {
                v.team = new ArrayList<Entity>();
                readPeople(in, v.team, people, aboard, false);
            }
            int drv = in.readInt();
            Entity driver = at(people, drv);
            if (driver != null) {
                v.driver = driver;
                v.realDriver = true;
                aboard.put(driver, Boolean.TRUE);
            } else {
                v.driver = null;
                v.realDriver = false;
            }
            v.pilotComing = at(people, in.readInt());
            vlinks[i] = new int[]{in.readInt(), in.readInt()};
            v.ammoRun = facility(city, in.readInt());
            v.depot = facility(city, in.readInt());
            v.station = facility(city, in.readInt());
            v.dozerBase = facility(city, in.readInt());
            v.restockFac = facility(city, in.readInt());
            int shop = in.readInt();
            v.restockShop = shop >= 0 && shop < city.buildings.size() ? city.buildings.get(shop) : null;
            int zi = in.readInt();
            v.zone = zi >= 0 && zi < d.zones.size() ? d.zones.get(zi) : null;
            v.pbase = base(w, city, in.readInt());
            v.ptarget = base(w, city, in.readInt());
            vlinks[i] = new int[]{vlinks[i][0], vlinks[i][1], in.readInt()};
            v.incident = v.onward = v.pickupFor = null;
            v.escort = null;
            vs.add(v);
        }
        for (int i = 0; i < n; i++) {
            Fleet.Vehicle v = vs.get(i);
            v.stopping = at(vs, vlinks[i][0]);
            v.clearing = at(vs, vlinks[i][1]);
        }
        // The convoys.
        Table cf = readTable(in, Convoys.Column.class);
        Table tf = readTable(in, Dispatch.Convoy.class);
        int cn = in.readInt();
        ArrayList<Convoys.Column> cols = new ArrayList<Convoys.Column>(cn);
        for (int k = 0; k < cn; k++) {
            Convoys.Column c = new Convoys.Column();
            readValues(in, cf, c);
            int m = in.readInt();
            for (int j = 0; j < m; j++) {
                Fleet.Vehicle v = at(vs, in.readInt());
                if (v != null) c.vs.add(v);
            }
            m = in.readInt();
            for (int j = 0; j < m; j++) c.queue.add(in.readInt());
            int zi = in.readInt();
            c.zone = zi >= 0 && zi < d.zones.size() ? d.zones.get(zi) : null;
            c.base = base(w, city, in.readInt());
            c.station = facility(city, in.readInt());
            if (in.readBoolean()) {
                Dispatch.Convoy t = new Dispatch.Convoy();
                readValues(in, tf, t);
                t.managed = true;
                t.vehicles.addAll(c.vs);
                c.tracker = t;
                d.convoys.add(t);
            }
            cols.add(c);
            w.convoys.columns.add(c);
        }
        for (int i = 0; i < n; i++) vs.get(i).convoy = at(cols, vlinks[i][2]);
        Fleet.Vehicle pt = at(vs, in.readInt()), at = at(vs, in.readInt());
        // ((Ops read where each post's tent is and whether it was set up; the truck is this one.))
        link(w.ops.police, pt);
        link(w.ops.army, at);
        int links = in.readInt();
        for (int k = 0; k < links; k++) {
            Entity e = at(people, in.readInt());
            Fleet.Vehicle v = at(vs, in.readInt());
            byte kind = in.readByte();
            if (e == null || v == null) continue;
            if (kind == 0) e.rig = v;
            else e.ride = v;
        }
        // Those aboard: off the map again, and back in their seats.
        for (int i = 0; i < n; i++) {
            Fleet.Vehicle v = vs.get(i);
            for (Entity e : v.crew) seat(e, v, true);
            for (Entity e : v.riders) seat(e, v, false);
            if (v.driver != null) seat(v.driver, v, true);
        }
        for (int i = w.entities.size() - 1; i >= 0; i--) if (aboard.containsKey(w.entities.get(i))) w.entities.remove(i).listed = false;
        int top = fleet.unitCount;
        for (int i = 0; i < n; i++) {
            Fleet.Vehicle v = vs.get(i);
            fleet.vehicles.add(v);
            top = Math.max(top, v.number);
        }
        fleet.unitCount = top;
        fleet.fromSave = true;
        // ((28.2) No evacuation convoys any more: one saved on the road lets its people off and heads back.)
        w.convoys.dropEvacuations();
        for (Entity e : people) if (e.rig != null && !e.dead) fleet.note(e);
        // On their way somewhere: a route there again (the old one isn't kept). With none, the vehicle's own "stuck"
        // handling finds it another way, as it would on the road.
        for (int i = 0; i < n; i++) {
            Fleet.Vehicle v = vs.get(i);
            if (v.parked || v.standby || v.broken) continue;
            if (v.state == Fleet.DRIVE || v.state == Fleet.RETURN || v.state == Fleet.PATROL || v.state == Fleet.PICKUP) {
                if (v.patrol && v.state == Fleet.PATROL && v.incident == null) fleet.routing.newDestination(v);
                else if (v.tx != 0 || v.ty != 0) fleet.routing.route(v, v.tx, v.ty);
            }
        }
    }

    /** Back aboard, off the map (as Fleet.board and the rest leave them): crew tied to the vehicle, riders not. */
    private static void seat(Entity e, Fleet.Vehicle v, boolean crew) {
        e.dead = true;
        e.removed = true;
        e.vx = e.vy = 0;
        if (crew) e.rig = v;
        e.x = v.x;
        e.y = v.y;
    }

    private static void link(Ops.Post p, Fleet.Vehicle v) {
        p.truck = v;
        if (v == null) p.up = false;
    }

    // ------------------------------------------------------------------ the field table

    /** The fields a record keeps: numbers, flags and words, not the transient or final ones (those are caches or lists). */
    static List<Field> fields(Class<?> c, String[] skip) {
        ArrayList<Field> out = new ArrayList<Field>();
        java.util.HashSet<String> no = new java.util.HashSet<String>(java.util.Arrays.asList(skip));
        for (Field f : c.getDeclaredFields()) {
            int m = f.getModifiers();
            if (Modifier.isStatic(m) || Modifier.isTransient(m) || Modifier.isFinal(m) || no.contains(f.getName())) continue;
            if (code(f.getType()) == 0) continue;
            f.setAccessible(true);
            out.add(f);
        }
        // (In name order, so two saves of the same game write the same file whatever order the VM lists them in.)
        java.util.Collections.sort(out, new java.util.Comparator<Field>() {
            @Override
            public int compare(Field a, Field b) {
                return a.getName().compareTo(b.getName());
            }
        });
        return out;
    }

    private static char code(Class<?> t) {
        if (t == int.class) return 'I';
        if (t == float.class) return 'F';
        if (t == boolean.class) return 'Z';
        if (t == byte.class) return 'B';
        if (t == long.class) return 'J';
        if (t == String.class) return 'S';
        return 0;
    }

    static void writeTable(DataOutputStream out, List<Field> fs) throws IOException {
        out.writeInt(fs.size());
        for (Field f : fs) {
            out.writeUTF(f.getName());
            out.writeByte(code(f.getType()));
        }
    }

    /** A table as written: each value's kind, and the field it goes in now (null where that name isn't one any more). */
    static final class Table {
        final Field[] fields;
        final char[] kinds;

        Table(int n) {
            fields = new Field[n];
            kinds = new char[n];
        }
    }

    static Table readTable(DataInputStream in, Class<?> c) throws IOException {
        int n = in.readInt();
        Table t = new Table(n);
        for (int i = 0; i < n; i++) {
            String name = in.readUTF();
            char k = (char) in.readByte();
            t.kinds[i] = k;
            try {
                Field f = c.getDeclaredField(name);
                int m = f.getModifiers();
                if (!Modifier.isStatic(m) && !Modifier.isFinal(m) && code(f.getType()) == k) {
                    f.setAccessible(true);
                    t.fields[i] = f;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        return t;
    }

    static void writeValues(DataOutputStream out, List<Field> fs, Object o) throws IOException {
        try {
            for (Field f : fs) {
                switch (code(f.getType())) {
                    case 'I': out.writeInt(f.getInt(o)); break;
                    case 'F': out.writeFloat(f.getFloat(o)); break;
                    case 'Z': out.writeBoolean(f.getBoolean(o)); break;
                    case 'B': out.writeByte(f.getByte(o)); break;
                    case 'J': out.writeLong(f.getLong(o)); break;
                    default:
                        String s = (String) f.get(o);
                        out.writeBoolean(s != null);
                        if (s != null) out.writeUTF(s);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IOException(e.toString());
        }
    }

    static void readValues(DataInputStream in, Table t, Object o) throws IOException {
        Field[] fs = t.fields;
        char[] k = t.kinds;
        try {
            for (int i = 0; i < fs.length; i++) {
                Field f = fs[i];
                switch (k[i]) {
                    case 'I': { int v = in.readInt(); if (f != null) f.setInt(o, v); break; }
                    case 'F': { float v = in.readFloat(); if (f != null) f.setFloat(o, v); break; }
                    case 'Z': { boolean v = in.readBoolean(); if (f != null) f.setBoolean(o, v); break; }
                    case 'B': { byte v = in.readByte(); if (f != null) f.setByte(o, v); break; }
                    case 'J': { long v = in.readLong(); if (f != null) f.setLong(o, v); break; }
                    default: {
                        String s = in.readBoolean() ? in.readUTF() : null;
                        if (f != null) f.set(o, s);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new IOException(e.toString());
        }
    }

    // ------------------------------------------------------------------ links

    private static void writePeople(DataOutputStream out, List<Entity> list, IdentityHashMap<Entity, Integer> people) throws IOException {
        int n = 0;
        for (Entity e : list) if (people.containsKey(e)) n++;
        out.writeInt(n);
        for (Entity e : list) {
            Integer i = people.get(e);
            if (i != null) out.writeInt(i);
        }
    }

    private static void readPeople(DataInputStream in, List<Entity> list, List<Entity> people, IdentityHashMap<Entity, Boolean> aboard,
                                   boolean seated) throws IOException {
        int n = in.readInt();
        for (int k = 0; k < n; k++) {
            Entity e = at(people, in.readInt());
            if (e == null || list.contains(e)) continue;
            list.add(e);
            if (seated) aboard.put(e, Boolean.TRUE);
        }
    }

    private static int person(Entity e, IdentityHashMap<Entity, Integer> people) {
        Integer i = e == null ? null : people.get(e);
        return i == null ? -1 : i;
    }

    private static int vehicle(Fleet.Vehicle v, IdentityHashMap<Fleet.Vehicle, Integer> keptIndex) {
        Integer i = v == null ? null : keptIndex.get(v);
        return i == null ? -1 : i;
    }

    /** A survivor base by the building it's round (the bases are put back from their buildings, maybe in another order). */
    private static int base(SurvivorBases.Base b, City city) {
        return b == null || b.b == null ? -1 : city.buildings.indexOf(b.b);
    }

    private static SurvivorBases.Base base(World w, City city, int bi) {
        if (bi < 0 || bi >= city.buildings.size()) return null;
        City.Building b = city.buildings.get(bi);
        for (SurvivorBases.Base o : w.bases) if (o.b == b) return o;
        return null;
    }

    private static City.Facility facility(City city, int i) {
        return i >= 0 && i < city.facilities.size() ? city.facilities.get(i) : null;
    }

    private static <T> T at(List<T> list, int i) {
        return i >= 0 && i < list.size() ? list.get(i) : null;
    }
}
