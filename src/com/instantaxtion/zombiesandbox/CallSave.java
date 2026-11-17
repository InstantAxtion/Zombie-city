package com.instantaxtion.zombiesandbox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * ((27.5) Save version 80, after the vehicles.) The calls going on (Dispatch.Incident) kept in a save, and everything
 * that pointed at one: the vehicles on their way to a call or waiting at the scene (incident, onward, pickupFor), the
 * police and soldiers on foot answering one (their task and call), and the lifts soldiers and officers were given
 * (the lift is a call of its own, off the board, that knows what they go back to doing: e.rideFrom). Until now a
 * loaded game had no calls at all: every car on its way to one turned round and went back on patrol, the trucks at a
 * scene drove off, and everyone on foot answering one stood down.
 *
 * Not kept: the posts on the cordon round an infection zone and the guard spots at its checkpoints (calls too, but the
 * zone puts them up again itself). A call that's over is kept only if a vehicle or someone still points at it, and isn't
 * put back on the board. A call's way there on foot is worked out again on load.
 * Its numbers, flags and words are written by name, as VehicleSave writes a vehicle's.
 */
final class CallSave {
    private CallSave() {
    }

    /**
     * A call worth keeping: a real one (not a cordon post or checkpoint), or a lift - one that's over too, if a vehicle
     * or someone still points at it (a car picking its officers up after a call, say): it goes on with what it was doing.
     */
    private static boolean keep(Dispatch.Incident inc) {
        return inc != null && inc.cordonPost < 0 && inc.checkpointSpot == null;
    }

    private static void add(Dispatch.Incident inc, List<Dispatch.Incident> calls, IdentityHashMap<Dispatch.Incident, Integer> index) {
        if (!keep(inc) || index.containsKey(inc)) return;
        index.put(inc, calls.size());
        calls.add(inc);
    }

    private static int at(Dispatch.Incident inc, IdentityHashMap<Dispatch.Incident, Integer> index) {
        Integer i = inc == null ? null : index.get(inc);
        return i == null ? -1 : i;
    }

    // ------------------------------------------------------------------ writing

    /**
     * kept: the vehicles VehicleSave wrote, in its order. people: everyone written, by their index in the save (those
     * aboard kept vehicles among them).
     */
    static void write(DataOutputStream out, World w, List<Fleet.Vehicle> kept, IdentityHashMap<Entity, Integer> people) throws IOException {
        Dispatch d = w.dispatch;
        ArrayList<Dispatch.Incident> calls = new ArrayList<Dispatch.Incident>();
        IdentityHashMap<Dispatch.Incident, Integer> index = new IdentityHashMap<Dispatch.Incident, Integer>();
        // The board first (in its order), then the lifts and anything else only a vehicle or a person still points at.
        for (Dispatch.Incident inc : d.incidents) if (!inc.resolved) add(inc, calls, index);
        int onBoard = calls.size();
        for (Fleet.Vehicle v : kept) {
            add(v.incident, calls, index);
            add(v.onward, calls, index);
            add(v.pickupFor, calls, index);
        }
        for (Entity e : people.keySet()) {
            add(e.incident, calls, index);
            add(e.rideFrom, calls, index);
        }
        List<Field> f = VehicleSave.fields(Dispatch.Incident.class, new String[0]);
        VehicleSave.writeTable(out, f);
        out.writeInt(calls.size());
        out.writeInt(onBoard);
        for (Dispatch.Incident inc : calls) {
            VehicleSave.writeValues(out, f, inc);
            out.writeInt(d.zones.indexOf(inc.rideZone));
        }
        // The vehicles' links, in VehicleSave's order.
        out.writeInt(kept.size());
        for (Fleet.Vehicle v : kept) {
            out.writeInt(at(v.incident, index));
            out.writeInt(at(v.onward, index));
            out.writeInt(at(v.pickupFor, index));
        }
        // The people's: who's answering which call (and on the scene or not), and whose lift it was; and those walking
        // over to a vehicle to climb in (the save writes them as having nothing to do, and they gave up the lift).
        int n = 0;
        for (Entity e : people.keySet()) if (listed(e, index)) n++;
        out.writeInt(n);
        for (java.util.Map.Entry<Entity, Integer> en : people.entrySet()) {
            Entity e = en.getKey();
            if (!listed(e, index)) continue;
            int ci = at(e.incident, index), ri = at(e.rideFrom, index);
            out.writeInt(en.getValue());
            out.writeInt(ci);
            out.writeInt(ri);
            out.writeInt(e.task);
            out.writeBoolean(e.onScene);
        }
    }

    private static boolean listed(Entity e, IdentityHashMap<Dispatch.Incident, Integer> index) {
        return at(e.incident, index) >= 0 || at(e.rideFrom, index) >= 0 || (e.task == Dispatch.T_BOARD && e.rig != null);
    }

    // ------------------------------------------------------------------ reading

    /** kept: the vehicles VehicleSave read back, in its order. people: everyone read, in the order they were written. */
    static void read(DataInputStream in, World w, List<Fleet.Vehicle> kept, List<Entity> people) throws IOException {
        Dispatch d = w.dispatch;
        VehicleSave.Table f = VehicleSave.readTable(in, Dispatch.Incident.class);
        int n = in.readInt(), onBoard = in.readInt();
        ArrayList<Dispatch.Incident> calls = new ArrayList<Dispatch.Incident>(n);
        for (int i = 0; i < n; i++) {
            Dispatch.Incident inc = new Dispatch.Incident();
            VehicleSave.readValues(in, f, inc);
            int zi = in.readInt();
            inc.rideZone = zi >= 0 && zi < d.zones.size() ? d.zones.get(zi) : null;
            calls.add(inc);
        }
        // Back on the board, each with its way there on foot worked out again (within the board's budget).
        for (int i = 0; i < onBoard && i < n; i++) {
            Dispatch.Incident inc = calls.get(i);
            d.incidents.add(inc);
            d.giveField(inc);
        }
        int vn = in.readInt();
        for (int i = 0; i < vn; i++) {
            Fleet.Vehicle v = i < kept.size() ? kept.get(i) : null;
            Dispatch.Incident inc = at(calls, in.readInt()), onward = at(calls, in.readInt()), pickupFor = at(calls, in.readInt());
            if (v == null) continue;
            v.incident = inc;
            v.onward = onward;
            v.pickupFor = pickupFor;
        }
        int pn = in.readInt();
        for (int k = 0; k < pn; k++) {
            Entity e = at(people, in.readInt());
            Dispatch.Incident inc = at(calls, in.readInt()), ride = at(calls, in.readInt());
            int task = in.readInt();
            boolean onScene = in.readBoolean();
            if (e == null) continue;
            e.rideFrom = ride;
            // (On their way to climb into a vehicle that's kept: VehicleSave has tied them to it again.)
            if (task == Dispatch.T_BOARD && !e.dead && e.rig != null && e.task == Dispatch.T_NONE) e.task = Dispatch.T_BOARD;
            if (inc == null) continue;
            e.incident = inc;
            // (Those on foot answering it go on answering it: the save writes them as having nothing to do.)
            if (task == Dispatch.T_RESPOND && !e.dead && e.task == Dispatch.T_NONE) {
                e.task = Dispatch.T_RESPOND;
                e.onScene = onScene;
            }
        }
    }

    private static <T> T at(List<T> list, int i) {
        return i >= 0 && i < list.size() ? list.get(i) : null;
    }
}
