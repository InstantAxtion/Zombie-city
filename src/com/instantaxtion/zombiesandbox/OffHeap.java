package com.instantaxtion.zombiesandbox;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;

/**
 * (16.11) Big per-tile tables kept outside the game's own memory (the Java heap Android gives an app, about 512 MB on a
 * Galaxy S26 Ultra even with a large heap asked for). On Android 8.1 and later they are shared memory (ashmem): real
 * memory, never written out to storage, and not counted against that limit. Before that, or off Android (the tests), a
 * memory-mapped scratch file deleted as soon as it's mapped; failing that, an ordinary direct buffer. Each is freed when
 * nothing refers to it any more.
 */
final class OffHeap {
    private OffHeap() {
    }

    /** Where the tables in use came from, for the record: 0 shared memory, 1 a mapped file, 2 the heap. */
    static volatile int lastKind = -1;

    static ByteBuffer alloc(int bytes, Object[] owner) {
        int n = Math.max(8, bytes);
        // Android's shared memory, looked up by name: the game is built against an older Android than has it.
        try {
            Class<?> c = Class.forName("android.os.SharedMemory");
            Object m = c.getMethod("create", String.class, int.class).invoke(null, "zombiecity", n);
            ByteBuffer b = (ByteBuffer) c.getMethod("mapReadWrite").invoke(m);
            owner[0] = m;
            lastKind = 0;
            return b.order(ByteOrder.nativeOrder());
        } catch (Throwable ignored) {
            // (Not there: the next way.)
        }
        try {
            File f = File.createTempFile("zc", ".tbl");
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            try {
                raf.setLength(n);
                ByteBuffer b = raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, n);
                lastKind = 1;
                return b.order(ByteOrder.nativeOrder());
            } finally {
                raf.close();
                // (Mapped, it stays: the name can go at once.)
                if (!f.delete()) f.deleteOnExit();
            }
        } catch (Throwable ignored) {
            // (No scratch space: the last way.)
        }
        lastKind = 2;
        return ByteBuffer.allocateDirect(n).order(ByteOrder.nativeOrder());
    }

    /** A table of whole numbers. */
    static final class Ints {
        private final IntBuffer b;
        private final Object[] owner = new Object[1];
        final int length;

        Ints(int n) {
            length = n;
            b = alloc(n * 4, owner).asIntBuffer();
        }

        int get(int i) {
            return b.get(i);
        }

        void set(int i, int v) {
            b.put(i, v);
        }

        void fill(int v) {
            for (int i = 0; i < length; i++) b.put(i, v);
        }
    }

    /** A table of numbers with a fraction. */
    static final class Floats {
        private final FloatBuffer b;
        private final Object[] owner = new Object[1];
        final int length;

        Floats(int n) {
            length = n;
            b = alloc(n * 4, owner).asFloatBuffer();
        }

        /** The same numbers as an ordinary array, kept outside (the array can then be let go). */
        static Floats of(float[] a) {
            if (a == null) return null;
            Floats f = new Floats(a.length);
            for (int i = 0; i < a.length; i++) f.b.put(i, a[i]);
            return f;
        }

        float get(int i) {
            return b.get(i);
        }

        void set(int i, float v) {
            b.put(i, v);
        }
    }

    /** A table of small numbers, a byte each. */
    static final class Bytes {
        private final ByteBuffer b;
        private final Object[] owner = new Object[1];
        final int length;

        Bytes(int n) {
            length = n;
            b = alloc(n, owner);
        }

        byte get(int i) {
            return b.get(i);
        }

        void set(int i, byte v) {
            b.put(i, v);
        }
    }
}
