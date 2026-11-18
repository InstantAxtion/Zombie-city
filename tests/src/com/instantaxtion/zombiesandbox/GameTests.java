package com.instantaxtion.zombiesandbox;

/**
 * Runs the game on a desktop JVM (with small stand-ins for the Android classes in tests/stubs) and checks that cities
 * generate, the simulation runs, saves round-trip and every screen draws: 25 tests, by area in MapTests, WorldTests,
 * ForcesTests, DrivingTests and UiTests (the shared helpers in TestKit). Run with ./test.sh; FAST=1 for the fast tier.
 */
public final class GameTests extends TestKit {
    public static void main(String[] args) throws Exception {
        MapTests.register();
        WorldTests.register();
        ForcesTests.register();
        DrivingTests.register();
        UiTests.register();
        if (args.length > 0 && args[0].equals("--list")) {
            for (Entry e : ALL) System.out.println((e.fast ? "FAST  " : "      ") + e.name);
            return;
        }
        runAll();
    }
}
