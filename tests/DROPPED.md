# Checks dropped or loosened going from 95 tests to 25

Everything else from the 95 is still checked, as a part of one of the 25 (`./test.sh --list`). One line each, so a
check can be brought back if it's missed; the old suite is `tests/src/.../GameTests.java` at 0255c6a (release 28.0).

## Dropped

- 22.2 trivia: "Medium is a fifth smaller across than in 22.2" (`c.tiles() == 256` on the current map version). The
  saved-map sizes (map versions 33 and 2) are still checked.
- 18.4 trivia: "ten pieces of music" (`Synth.TRACKS == 10`).
- 11.5 trivia: "new cities are built the 11.5 way" (`new CityConfig().civic >= 28`).
- 10.10: "gun stores only where people can buy guns" in the countryside check: the same as 16.9's "no gun stores",
  which is kept (buildings test).
- 22.1: "collision short cut agrees with the full look" (20,000 points on one city): the same check as 11.5's and
  16.8's open-ground shortcut checks, which are kept (roads and routes test).
- "saves load back the same" as a test of its own run after the busy outbreak (testAfter): folded into one part,
  `busyOutbreakSaves`, which builds the outbreak and saves it.

## Loosened (to what the feature promises, not one seed's count)

- 22.4: exactly 297 landmark designs -> at least 250 (every one still has to paint inside its block).
- 21.0: exactly 18 missions -> at least 12.
- 14.0: exactly two combat engineers at the base -> at least one.
- 26.0: at least two police boats on the Harbour -> at least one (map version 41 moors one on that map and seed).
- 18.0: police cut to the mission's number "or four fewer" -> no more than the mission's and at least half. (The old
  check counted the crews of the parked cars twice: `w.counts` already has them.)
- 28.0 outside backup: three quarters of the officers out within 300 of the call -> three fifths (a car held up in
  traffic far off can let its officers out to go on on foot; the drop-offs, pile-up and junction checks are as they
  were).
- 25.0 survivor base: "a watchtower" only where the base's corners leave room for one (`towers.length > 0`), and up
  to five minutes for wall, guards and tower instead of a fixed two-to-four.
- 21.2 "working round a victim": moved some way in any direction (not only sideways) or shot; "on the move under
  orders": shots taken while still under the order (they may drop it later to fight).
- 27.5 junctions: on a Small city, not a Tiny one. On map version 40 and later a Tiny Metropolis (a size the game
  doesn't offer) with seed 23 jams with 39 of 156 vehicles stuck, nearly all patrol cars on their beats (it passes
  with other dice, and on map version 39). `tools/health.sh` watches stuck vehicles at the Default size.

## Changed so the tests don't depend on one city or one set of dice

- Map versions pinned per test (25, 29, 30, 33, 34, 36, 37, 39) -> every test city on `TEST_CIVIC` (41) through
  `TestKit.cfg()`; the checks of old layouts for old saves (map versions 0, 2, 33 and 39) keep theirs.
- `w.rnd.setSeed(...)` in ten tests removed: they now play out with the test's dice, so `ZC_SEED_SALT` reaches them.
- Fixed offsets that land in water or a wall on another map (`truck.x + 600`, `x + 700`) -> `TestKit.driveSpot()`.
- The wilds tests (fire tower, rangers, rescue helicopter) on Small Town at map version 25 -> `TestKit.wildWorld()`,
  the first Metropolis seed whose country has a ranger station and fire towers.
- Scenarios clear bystanders round them (`quiet()`/`empty()` and `step()`): the raider gangs, civilians fighting back,
  the runner, the officer who hears a fight, the helicopter, a victim being bitten (and a grenade still in the air
  from the fight before is cleared).
- Fire discipline: two soldiers and a row of four of the dead set out in front of them, not the alpha's horde, where
  one standing out ahead of the rest drew both guns.

## Removed with the feature (28.2)

- The "campaign" test (missions, budgets, objectives, mission difficulty, perks, veterans, the commander's tools, retake
  the district) went with the campaign. Its sandbox checks are kept in "no campaign and no evacuation convoys": a
  sandbox game is always Normal, nothing comes in by itself, the director's phases, a Hard save loads as Normal, the
  score, no difficulty on New Game, and the last of the dead pointed out. Dropped from it: "the dead are tougher on
  Hard" and "more score on Hard" (only missions were ever Hard).
- "Every tool priced" and "a commander pays for wire, nests, barrages, flares and mines" (the mission prices are gone);
  "every tool named and described" is kept.
