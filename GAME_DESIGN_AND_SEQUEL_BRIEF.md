# Zombie City Sandbox: complete design brief, for building a sequel from scratch

This document describes the game as it stands at version 33.0: what it is, how every system works, how the code is laid
out, what the numbers are, what players liked and disliked, what went wrong technically, and what a ground-up sequel
should keep, improve, or do completely differently. It is written so that an AI (or a person) who has never seen the
code can understand the whole game and plan a better one.

Plain text with Markdown headings. Sections can be read on their own.

---

## 1. The game in one page

**What it is.** A top-down, single-player zombie-outbreak *sandbox* for Android phones. A procedurally generated city
of thousands of people lives a normal day (homes, jobs, police patrols, army bases, buses, trains). The player drops
zombies into it with a brush and watches the city respond: civilians flee, 911 calls come in, police cars race to
calls and throw up cordons, the army rolls out Humvees, trucks, armour and gunships, the National Guard is mobilised,
and the outbreak is either contained or the city falls. The player can also place police, soldiers, vehicles and
defences, give orders, take the wheel of a vehicle, or control one person directly.

**The fantasy.** Watching a believable city and its emergency services fight a huge, spreading zombie outbreak, at a
scale (thousands to tens of thousands of zombies) no other mobile game shows, with a story you can follow: radio
chatter, named units, callouts on the map, and an after-action report.

**Style.** Top-down like early GTA: flat 2D drawn with Android Canvas, buildings and trees with fake height that lean
away from the screen centre as you pan (optional 3D look), day/night, weather. Every graphic is drawn by code. There
are no image or audio files: music and sound effects are synthesised on first run and cached. The APK is about 1.7 MB.

**Who plays it (the main real player).** One very engaged player drove most of the last ~40 versions. Their play style:
- Open a new city, then paint thousands to tens of thousands of zombies with the biggest brush and watch the battle.
- They care most about: the police and army behaving intelligently, it being balanced at huge zombie counts,
  performance on a phone, and the numbers on screen being right.
- They repeatedly asked to *remove* systems that added clutter or cost performance (safe zones, building interiors,
  recruitment, medevac, foot patrols, several maps, the events menu) and to make the core fight better.
- They want patch notes to be brief.

---

## 2. Platform and technology

- **Language:** Java (Java 7 style: no lambdas or streams in game code, for an old Android toolchain), about 133,600
  lines in 148 source files, all in one package `com.instantaxtion.zombiesandbox`.
- **No game engine.** Custom `SurfaceView`-style game loop (`GameView`), custom renderer drawing with `android.graphics.Canvas`.
- **Simulation:** single-threaded, fixed time step of 1/30 s, run 1x/2x/4x/8x by stepping several times per frame.
  The simulation is **deterministic** for a given seed and inputs: all randomness goes through `World.rnd`; no
  wall-clock time is allowed in the simulation (time-based budgets were tried and caused runs to differ, so every
  budget is *counted*, e.g. "search at most 25,000 road tiles a step").
- **Saves:** a custom binary format (`SaveGame.java`, `VehicleSave.java`), version 82, with long chains of
  "if version >= N" compatibility. Three slots plus autosave.
- **Audio:** `Synth.java` generates all music and effects procedurally; `GameAudio` mixes them with distance,
  stereo panning, night mode, mono and captions.
- **Build:** `./build.sh` compiles and packages the APK into `release/ZombieCitySandbox.apk` (committed to the repo).
- **Tests:** `./test.sh` runs 25 headless tests (Android classes are stubbed in `tests/stubs`), about 12 minutes.
  `tools/health.sh` plays two minutes of two maps with a fixed seed and compares ~40 numbers (stuck vehicles, stuck
  units, response times, people shoved by vehicles, simulation ms per step) against a recorded baseline.
- **Map versions ("civic").** Every behaviour change is gated on the map version the city was generated with
  (`CityConfig.civic`, now 61). Old saves and the old tests (mostly built on version 41) keep the behaviour they had.
  This keeps saves and tests stable, but the code is now full of `if (civic >= N)` branches (see section 15).

---

## 3. Code map (where everything lives)

The biggest files and what they do. Line counts are approximate.

| Area | Files | What they do |
|---|---|---|
| World core | `World.java` (3.3k) | Holds all state: entity list, spatial grid, effects buffers, counters; runs `update()` per step (think, move, separate, combat, death cleanup, dispatch, fleet, stats). Entity add/remove (`addEntity`, `listed`). |
| City generation | `City.java` (4k), `CityGenerator.java` (2k), `CityRoads.java` (2.3k), `CityBlocks.java` (1.3k), `CityOutskirts.java` (1.4k), `Terrain.java` (1.3k), `CityConfig.java` (1k) | Tile grid (16 px tiles), districts, road network (widths, ring road, highways, interchanges, bends), blocks, buildings, facilities (police stations, army bases, prison, airport, helipads), countryside, rivers, islands, bridges. `CityConfig` = New Game options, presets, sizes, force sizes, map version. |
| Map painting | `CityPainter.java` (4.6k), `LandmarkArt1..14.java` (~40k total), `Landmarks.java` | Draws the static map into tiles/bitmaps: roads, pavements, lane markings, roofs, landmarks (hundreds of hand-coded vector buildings). |
| Rendering | `WorldRenderer.java` (2k), `EntityRenderer.java`, `VehicleRenderer.java`, `BuildingRenderer.java`, `Hud.java` (1.7k), `MapLabels.java` | Per-frame drawing of map, people, vehicles, buildings with lean/3D, effects, HUD, labels. |
| Input/UI | `GameView.java` (1.8k), `TouchInput.java`, `ToolBar.java`, `Menu.java` (2.3k), `Inspector.java`, `Settings.java`, `PatchNotes.java` | Game loop, camera, tools, menus, cards, settings. |
| People | `Entity.java`, `Population.java` (1.4k), `CivilianAI.java` (1.3k), `DailyLife.java`, `Indoors.java`, `Exodus.java`, `Names.java` | Spawning, civilian goals (daily routines, fleeing, hiding), names. |
| Zombies | `ZombieAI.java` (1.5k), `Death.java` | Senses, chasing, lures (shared routes to noises), hordes, clawing vehicles and doors, feeding, giants; death, turning, corpses. |
| Movement | `Movement.java`, `Nav.java`, `Wilds.java`, `Spatial.java` | Steering, collision with walls, crowd separation, A* on a cluster graph (`Nav`), flow fields, spatial hash queries. |
| Armed units | `ArmedAI.java` (2.2k), `Combat.java` (1.1k), `Posts.java`, `Police.java`, `Experience.java` | Decision scoring (fight / fall back / wounded / search / duty), shooting, grenades, mortars, keeping distance, cordons, guard posts, ranks and perks. |
| Dispatch | `Dispatch.java` (3.8k), `Containment.java`, `Ops.java` (1.4k), `War.java`, `Director.java`, `Balance.java` | 911 incidents, assigning units, backup and outside help, army requests, infection zones and cordons, command posts, outbreak phases, mass-outbreak balance. |
| Vehicles | `Fleet.java` (1.9k) + `FleetDriving` (2k), `FleetService` (1.5k), `FleetPatrols` (1.1k), `FleetArmy`, `FleetCrews`, `FleetHelis`, `FleetRouting`, `FleetMedevac`, `FleetFireSupport`, `FleetCommand`, `FleetTraffic`, `FleetEvac`, `FleetTrains`, `FleetBoats`, `FleetPlayer`, `Convoys`, `Rides` | Every vehicle: driving rules, lanes, junction lights, routing, patrols, responding, crews boarding and leaving, helicopters, gunships, convoys, the player driving. |
| Save/load | `SaveGame.java` (1.4k), `VehicleSave.java` | Binary saves with version compatibility. |
| Audio | `Synth.java` (1.4k), `GameAudio.java`, `Sfx.java` | Procedural audio. |

The code style is heavily commented with the history of each decision, in the form `// ((31.0) What changed and why.)`.
The comments are an excellent record of what was tried and why, but they double the reading cost.

---

## 4. Setting up a game

**New Game screen.**
- **Map preset** (offered now): Metropolis, Islands, Prison Island, Desert City, Archipelago, Bridge City, Swamp Bayou,
  Lighthouse Coast. About 20 more presets exist in code but are retired (still build for old saves and city codes):
  Classic, Downtown, Suburbs, Industrial, Parkland, Old Town, Small Town, Campus, Village, Downtown Towers, Military
  Town, Lakeside, Harbour, River City, Theme Park Island, Space Center, Highway Crossroads, Border Town, Military
  Airbase, Canyon Town, Stadium City, Frozen North.
- **Map size:** Default (424x424 tiles, 6,784 px across), Big (536x536), Large (752x752).
- **Civilians: On / Off.** Off = an empty city (no civilians or prison inmates) with 50% more police and soldiers.
- **Fleeing citizens:** Off / On foot / Bus & train / All ways (people leave the city once the news breaks).
- **Country:** only USA offered now (Australia, Japan, France, Mexico, Malaysia exist with their own architecture,
  street names, traffic side and vehicles, but are retired from the menu).
- **City code:** each city has a code (map, size, seed) to replay or share it.
- A live preview of the city and a forecast of its people, police and soldiers (accurate to within a few since 32.8).

**Typical Default map at 33.0:** ~3,200 civilians, ~1,560 police, ~610 soldiers, ~1,150 buildings, 14 facilities
(police stations and army bases), ~490 vehicles at the start.

**Force sizes.** Police are about one officer per 16 residents plus 8 per station, capped at 800 before scaling, times
a strength factor (`CityConfig.forceCut`, now 0.85 x 1.25, x1.5 more with civilians off). Plus extras: SWAT and riot
teams on standby, federal agents, mounted and traffic police, prison guards, helicopter crews. Soldiers: about 40% of
police, split across the bases, plus checkpoints, quick reaction force, scouts.

---

## 5. The city

- **Tiles** are 16 px. Tile kinds include road, pavement, building, grass, tree, plaza, parking lot, base ground, fence,
  rubble, rail, dirt, water, sand and more.
- **Roads:** since map version 51 town streets are 5 tiles wide and main routes 7 (they were 3), for the traffic
  of hundreds of emergency vehicles. Ring road, highways with slip roads and interchanges, bridges over highways,
  bends at corners (tidied so lane markings line up), traffic lights at junctions, one-way slip roads.
- **Districts:** each city has its own grid, district mix, downtown, parks, colours (since map version 40).
- **Buildings:** offices, houses, warehouses, police stations, barracks, towers, hospital, shops, churches, schools,
  markets, apartments, garages, pharmacies, train station, malls, stadium, power plant, barns, silos, city hall,
  courthouse, jail, call centre, works, armory, plus hundreds of named landmarks. Since 32.9 no building has an
  interior: roofs are always on and people go in out of sight.
- **Facilities:** police stations (precincts, a headquarters, FBI field office), army bases of several types (supply
  depot, air base with runway and gunships, armoured base, rapid response / quick reaction force base, special forces,
  radar station, Guard headquarters), helipads (police, rescue, army), prison with walls and watchtowers, airport.
- **Generation is deterministic from the seed and the map version.** Old cities rebuild identically.

---

## 6. People and creatures

**Entity types:** Civilian, Cop, Soldier, Dog, Zombie, Runner, Brute, Giant, Zombie dog, Bruiser, Armoured zombie
(and several retired types that load as others in old saves).

**Roles** (police and army): rifleman, commander, sniper, machine gunner, riot officer, K9 handler, National Guard, SWAT,
combat medic (corpsman), grenadier, police marksman, search and rescue, pilot, Marine, recon scout, combat engineer,
mortar team, mounted officer, traffic officer, prison guard, special forces. Other police agencies: highway patrol,
sheriff's deputies, park rangers, federal agents.

**Key numbers (map version 61):**
- Civilian: walk ~16-22, run ~32-38. Police: walk 22, run 40, hp 100 (body armour). Soldier: hp 100.
- Zombie: hp 90, walk 12-16, run 24-29, then x0.7 since 32.2 ("slow zombies"), so about 8-11 walk, 17-20 run.
  Measured chase speed in practice is ~22-25 because of surges and horde effects.
- Runner: much faster (~42 after slowing), fragile. Bruiser: quicker and tougher, lunges. Brute: big, shoves,
  charges. Giant: huge, smashes barricades and cars, only ever placed by the player. Armoured: helmet stops the
  first headshot.
- Bites: 17 damage (more for big types), infection chance per bite ~0.4 x 2.2 x difficulty (almost always infects
  civilians). Police since 32.3 wear bite-proof gear: bites do a quarter of the harm and infect one time in twenty.
- Guns: body shots do little to the dead (damage x0.2); headshots kill. Police get close-range headshot bonuses,
  stopping power (a stagger), and doctrine (each agency's training). Ranks from kills improve aim.

**Civilians** have homes, households, jobs, commutes, shifts, shops, schools, errands, routines; they flee, hide,
form groups, follow leaders, panic (trampling), get bitten, turn. A day/night cycle drives routines.

**Zombies:**
- Senses: see ~165 ahead, hear/smell ~65 behind; memory of last seen target.
- Noise draws them: gunfire, alarms, screams. Lures use *shared routes* (one flow patch per area) so thousands
  can head to a noise cheaply.
- Hordes: leaders roam, followers follow. Frenzy when several surround one victim.
- They claw at cars (smash windows and drag people out of slow or stopped vehicles), doors, barricades, walls.
- They feed on bodies, and the bitten turn after a delay.
- Giants smash through things. Brutes charge. Bruisers lunge.

---

## 7. The police

- **Stations** with patrol cars. Since 31.0 there are **no foot patrols**: officers patrol by car, **4 per car**, and
  wait at their station for a car otherwise (on map version 61 they stand at a spot round the yard and give up on
  spots they cannot reach).
- **911 calls / incidents** (`Dispatch.Incident`): created from sightings and attacks; each has a place, a count of
  the dead near it (`zombiesNear`), officers down, backup requests. `needAt` = max(2, 1-2 + zombies/3) + officers down;
  there is no hard cap. Nearest patrol car responds first, officers on foot make up the rest.
- **Perimeter role (since 32.3):** in a big outbreak (over 300 zombies), a call with 4+ zombies is the army's: no
  police are sent in on foot, those there come away to the cordon. Police perimeter rings sit 220-420 px out, cars
  stop at the ring, officers keep 120 back, may fall back from cordons, and sprint away from a zombie on their back
  instead of turning to shoot. Every free officer goes onto infection-zone cordon lines (3 per post).
- **Special units:** SWAT teams and BearCats, riot squads with shields and riot vans, K9, marksmen on rooftops
  (overwatch), mounted police, traffic officers holding junctions, police helicopters (more pads and earlier launch
  since 31.0), police drones, police boats, mobile command posts (move round the fight, pack up when quiet).
- **Outside police backup:** waves from neighbouring forces (highway patrol, sheriffs, SWAT, etc.) every 25-60 s
  when the city is stretched; in a mass outbreak (32.4) waves are up to 4x bigger and twice as often.
- **Behaviour:** decision scoring each 0.4-0.7 s between fight / fall back / wounded / search / duty, based on odds
  (zombies near vs guns near), nerve (doctrine, rank, perks, command post), ammo stock, hp. Fall back together to a
  rally point; rear guard covers. Restock ammo at stations/cars/ammo trucks. Recover hp slowly out of combat (32.3).

---

## 8. The army

- **Bases** with garrisons, parade grounds (one place per soldier), duty posts (sentries, fence patrols, drill, motor
  pool), checkpoints on country roads.
- **Base patrols:** Humvees on loop patrols that sweep several spots before returning; launched a few at a time when
  the gate is clear (gate congestion was a big problem).
- **Quick reaction force:** a dedicated base near the middle of town with vans that go to close calls.
- **Troop trucks** carry squads to calls, wait near them and take them on afterwards (`FleetService.truckAtScene`,
  pickup logic; a truck now gives up after two failed pickups).
- **Vehicles:** Humvee, army truck, APC, IFV, MRAP, gun truck, tanks, bulldozer, fire support trucks (mortars),
  recon Humvees, armoured ambulances (unused since medevac was removed), gunships (since 31.0, on two of every three
  army pads, cannon bursts), transport aircraft.
- **Requests:** police command asks for the army at 4+ zombies (8 before 32.3) once the alert is known; a quick
  reaction force from outside, then the main convoy with armour (2-3 min in a mass outbreak, 4-7 min otherwise), air
  assaults by helicopter while the city is losing.
- **National Guard (32.4):** the first time zombies pass 5,000, one soldier per 20 zombies (up to 1,500) and one
  officer per 40 (up to 800) join the outside reserves, with their own convoy.
- **Combat:** soldiers 1.35x headshot chance (32.3), grenades into crowds (blast weaker in mass outbreaks), launchers,
  mortars with spotters, snipers, machine gunners, commanders boosting nearby soldiers.

---

## 9. Vehicles and driving

- Lanes, traffic lights, junction rules, overtaking, pulling over for sirens, kerb parking at scenes (cars at a busy
  call pull into spaces along the kerb instead of stopping in the middle), slip roads, bridges.
- **Routing:** road searches on the tile grid with counted per-step budgets (`FleetRouting.busy`), fairness so no car
  is starved. Quick route tables for common trips.
- **Crews:** vehicles carry `crew` (people aboard, who leave the entity list while riding), `riders` (passengers),
  a `driver` (a real officer/soldier or a placeholder figure), and `squad` (people it belongs to on foot).
  Boarding (`T_BOARD`), getting out (`crewOut`), stray recovery (`findStrays`, 32.8).
- **Player driving:** take the wheel of any vehicle; let go and the crew carries on its job.
- **Damage:** zombies clawing, crashes, gunfire, blasts; smoke when damaged; wrecks get towed; repairs at home.
- **Civilian traffic:** mostly gone (no civilian drivers since 12.5; buses, trains and emergency vehicles fill the
  roads). Cars parked in streets can be set off by zombies (alarms).

---

## 10. Outbreak flow and balance

- **Alert levels:** 0 calm, 1 the news is out, 2 everyone knows (emergency). Radio messages, news, callouts.
- **Director** (`Director.java`): watches pressure and paces phases (calm, building, peak, respite). Difficulty is fixed
  at Normal (Easy/Hard/Nightmare multipliers still exist in code).
- **Pressure** 0-3 (holding, stretched, losing, overrun) drives outside help timing.
- **War result:** the outbreak ends contained (aftermath, recovery, people return) or the city falls.
- **Mass-outbreak balance (`Balance.java`, 32.0):** scales from 300 zombies (no effect) to 1,800+ (full effect):
  fewer clean headshots for police/army (x0.4), body shots do a third of the damage, blasts do ~a third, police
  stagger mostly gone, bites on armed units +40%, and shooters crowded by zombies lose accuracy. This was needed
  because ~1,800 police and soldiers shredded 3,000 zombies in 90 seconds, mostly with grenades into packed crowds.
- **Measured outcomes (default map, 33.0 era):**
  - 400 zombies: cleared in ~40 s.
  - 3,000 in one spot: ~100 s (the whole city converges; still easy).
  - 6,000 spread: ~3.5-4.5 min, 130-250 officers killed depending on settings.
  - 20,000 spread: ~7+ min, ending with the city winning slowly as Guard and backup pour in.
  - 40,000 spread: ~7 min to get down to ~4,400, army ~12,000 kills to police ~5,000.

---

## 11. The player's tools

Bottom tool bar: **Move** (pan), **Orders** (select and order units), **People**, **Police** (10 kinds), **Military**
(11 kinds), **Zombies** (Zombie, Runner, Bruiser, Brute, Giant, Armoured, Mixed), **Place** (car, police car, tank,
barricade, crate, medkit, BearCat, recon Humvee, police drone, razor wire, MG nest, armoured van, sandbags, concrete
barrier, command post, special forces, police boat), **Bomb**, **Erase**, **Build** (road, pavement, grass, trees,
wall, house, shop, clear), **Squads** (fire team, rifle squad, sniper team, engineers, police response/tactical teams,
riot line, K9 patrol, survivors, hordes of 35 / 150 / 500). Hidden now: Events, Safe Zone, a few others.

Top bar: speed (1x/2x/4x/8x), brush size (10/50/100), Clear (everyone / zombies / bodies and blood / wrecks /
barricades), Menu, Kills. Tap a person or vehicle for a card (name, rank, kills, history, status). Follow camera.
Stats panel (collapsible): people, police, army, zombies, turned, killed, kills per minute, peak, calls, outside
help, time, a live graph, top units and the fallen. Radio log. Minimap. Screenshots, hide buttons.

**Settings:** volumes, 3D or bird's-eye, blood, health bars, screen shake, FPS, max population (Auto or 2,500 up to
100,000), radio messages and lines, minimap, UI size, battery saver, name tags, graphics (Classic/Realistic), effects
level, vibration, destruction, ambience, mono, night mode, captions, shell shock, screen effects, colour grading.

---

## 12. Performance

**Budget reality.** One thread runs everything. On a desktop JVM the simulation costs (33.0):
- Normal play (a few hundred zombies): ~2-6 ms per step.
- 6,000 zombies: ~15-25 ms per step at the start of the fight.
- 20,000: ~30 ms. 40,000: ~60 ms.
A phone is roughly 3-4x slower, so 20,000+ is heavy and 40,000 stutters.

**Where time goes at mass scale** (profiling with JFR): zombie thinking (target search, line of sight, noise
following) ~30-40%; crowd separation ~13-17%; movement and wall collision ~10-15%; armed units' target picking
~10-15% (before optimisation 20%); vehicle routing; path searches.

**Optimisations in place:**
- Spatial hash grid rebuilt each step; zombie and living lists sorted per cell.
- LOD: calm entities off screen think every 2-4 steps; very calm civilians off screen are skipped entirely.
- Target picking: near ring first, keep a pick 0.25 s (mass outbreaks).
- Zombie target cache 0.25-0.5 s (over 1,500 zombies).
- Shared lure routes per 6-10 tile cell kept up to a minute.
- Separation every 2nd/4th/8th step off screen depending on crowd size; standing soldiers and police skip it.
- Counted budgets for road and path searches per step.
- Effects in fixed ring buffers with adaptive quality.

**What would be better:** see section 16.

---

## 13. Testing and tooling

- 25 headless tests covering maps, roads, buildings, combat, police backup, the army, vehicles, saves (two-minute
  outbreak saved and reloaded identically), UI screens, safe zones, survivor bases, etc. Most tests build cities with
  map version 41 so behaviour gated on newer versions doesn't break them.
- `tools/health.sh` metrics: vehicles stuck 30 s, spins, junction holds, station exits, response trip times, call
  route detours, units on foot stuck 20 s, idle near the dead, people shoved by vehicles, civilians running at the
  dead, civilians lost, simulation ms per step. Noisy between code changes (single seed) - treat as rough.
- Probes written during development (not in the repo): mass-zombie battle probe (outcomes, kills by side, step ms),
  broken-state audit (stuck units/vehicles, lost or duplicated entities, NaN, off-map), death-cause logger, per-officer
  last-seconds tracer, JFR profiling. These found most real bugs. A sequel should build such tools in from day one.

---

## 14. History: what was added, changed, and removed (and why)

Things players asked to remove or that were removed because they hurt more than helped:
- **Safe zones** (walled shelters with guards): removed 32.7. Tied down police and soldiers, cluttered the map.
- **Building interiors** (walk-in rooms, furniture, see-through roofs): removed 32.9 for performance and clarity.
- **Recruitment** (civilians enlisting, reserve pilots, volunteers): removed 31.2.
- **Medevac** (wounded units waiting for ambulances): removed 31.2; replaced by slow self-recovery out of combat (32.3).
- **Foot patrols:** removed 31.0 (all patrols by car).
- **Events menu** (spawn disasters): removed 31.0.
- **Maps:** many presets retired (see section 4), including Theme Park Island, Space Center, Frozen North.
- **Countries other than USA:** hidden from the menu.
- **Campaign / missions, difficulty choice, game modes (gangs, invasion):** removed long ago (19.6, 28.2).
- **Civilian drivers:** removed in 12.5 (traffic jams and cost).
- **Some zombie types** (spitter, bloater, screamer, crawler): removed; they load as normal zombies.

Big changes the player asked for and liked:
- Bigger forces (+25%), smaller maps (-20% twice), fewer civilians (5-10k), slow zombies, mass-outbreak balance,
  police holding perimeters while the army kills, National Guard for huge outbreaks, brushes of 10/50/100,
  hordes of 150/500, accurate counts, wide roads, gunships, stronger police.

Things that went wrong and had to be reversed:
- Time-based performance budgets made runs non-deterministic.
- Caps on how many cars could respond (to stop pile-ups) were rejected: the player wanted better driving, not fewer
  responders.
- Pulling all police back to stations in a mass outbreak made things far worse (hordes followed them and overran the
  stations).
- Many single-cause "fixes" for police deaths did nothing because the real causes were elsewhere; only instrumenting
  deaths (what each officer was doing in their last 10 seconds) found them: zombies chasing at ~22-25 vs an officer
  backing off while shooting at ~28, and cordon officers forbidden to fall back.

---

## 15. Known problems and technical debt

- **Monolithic, stateful design.** `World`, `Dispatch`, `Fleet`, `City` are giant classes sharing mutable state.
  Systems poke each other's fields directly. Hard to reason about and to test in isolation.
- **Entity bookkeeping bugs.** Entities move between "in the world list", "aboard a vehicle", "inside a building",
  flagged by booleans (`dead`, `removed`, `listed`) and removed lazily by a sweep. This produced people listed twice
  (moving at double speed) and ~100 police/soldiers lost off-map in a few minutes, until 32.8. A sequel needs one
  owner and one state machine for "where is this entity".
- **Map-version gating everywhere.** Every change is `if (civic >= N)`, 60 versions deep. It preserved saves and tests
  but made code paths multiply. A sequel should version *saves*, not behaviour, or snapshot the simulation rules.
- **Single thread, CPU rendering.** Canvas drawing of tens of thousands of sprites and a single simulation thread cap
  the scale. No GPU instancing, no job system.
- **Emergent balance by hand-tuned constants.** Hundreds of magic numbers tuned against single-seed probes; results
  swing ±30% with small changes (chaotic simulation). Balance needs many seeds and automated scoring.
- **Steering-based crowds.** Every zombie steers individually with wall collision and pairwise separation. Expensive
  and still produces pinning and odd clumps. Hordes should be fluids/flow fields.
- **Vehicle AI complexity.** Dozens of states and special cases (pickup, muster, scene, recall, loop, standby, kerb,
  moveUp...). Many "stuck for a minute" bugs came from state combinations nobody planned.
- **Heavy comments.** Valuable history but huge reading cost; the code is ~50% comments.
- **Saves:** 82 versions of compatibility branches.
- **Content in code.** ~40,000 lines of hand-coded landmark drawing; no data files.

---

## 16. Recommendations for a sequel built from scratch

### Keep (the heart of it)
- The fantasy: a living city, a huge outbreak, police and army responding believably, the player as a sandbox god who
  can also step in.
- Scale as the selling point: tens of thousands of zombies, thousands of responders, on a phone.
- Readable story: radio chatter, named units, callouts, after-action report, a stats panel the player trusts.
- Determinism (seeded replays, shareable city codes, reliable tests).
- Procedural everything (tiny download), but with data-driven content.

### Do differently: architecture
- **Data-oriented simulation (ECS or struct-of-arrays).** Positions, velocities, states in flat arrays; systems iterate
  them. Enables SIMD-friendly loops and multithreading.
- **Multithreaded jobs** for perception, movement and pathing; deterministic by fixed partitioning and ordering.
- **GPU rendering** (OpenGL ES / Vulkan via a thin engine, or an engine such as Godot/Unity/Bevy with instancing):
  one draw call for all zombies, sprite atlases, LOD by zoom.
- **Hierarchical simulation LOD:** full agents near the camera and fights; aggregated "horde blobs" (counts moving on a
  coarse grid, fighting statistically) far away; promote/demote smoothly. This is how to reach 100,000+.
- **Flow fields for hordes:** a few shared fields (towards noise, towards the living, away from danger) on a coarse grid;
  zombies follow the field and only do local avoidance. No per-zombie A*.
- **Explicit entity location states** (OnFoot, InVehicle(slot), InBuilding(id), Dead) owned by one system with
  events; no boolean flags or lazy sweeps.
- **Behaviour trees / utility AI with clear data** for police/army, with squads as first-class objects (a squad has a
  vehicle, an objective, a formation), not each soldier deciding alone.
- **Vehicles as a small set of states plus a planner** (Idle, Driving(route), Deploying, Waiting(for X, timeout),
  Returning), every wait with a timeout. Traffic as lanes on a road graph, not tiles.
- **Versioned save snapshots of the full state**, no behaviour gating; a balance change applies to everyone.
- **Built-in instrumentation from day one:** event log, per-entity history ring, automated audits (stuck, lost,
  duplicated), batch headless simulations across many seeds with dashboards. Balance by statistics, not single runs.
- **Content as data:** buildings, landmarks, unit types, weapons, vehicles in data files; procedural generators read
  them.

### Do differently: design
- **Lean into the mass battle.** The player's favourite activity is painting huge hordes and watching. Give it a mode:
  "Outbreak sandbox" with presets (spread, single mass, waves), a timeline, slow motion, cinematic camera, heatmaps,
  a kill/loss graph per side, replays.
- **Command layer.** Let the player act as incident commander: draw cordon lines, set rally points, call air strikes,
  assign the Guard, prioritise districts. The current game has orders but they are fiddly.
- **Clear roles.** Police: contain and protect (perimeters, evacuation corridors, checkpoints). Army: assault and clear.
  Make the roles visible on the map (zones, lines, fronts).
- **Zombie ecology:** fewer special cases, more systemic behaviour (noise, scent, hunger, light, sound propagation
  through streets), horde formation and splitting, migration across the map.
- **Readable feedback at scale:** aggregated indicators (front lines, density heatmap, infection spread) instead of
  thousands of individual labels.
- **Performance settings that scale gracefully** (simulation LOD distance, max detailed agents) rather than a hard
  population cap.
- **Drop:** interiors, safe-zone walls, recruitment, medevac, micro daily-life detail that nobody sees at scale.
  Consider bringing safe zones back only as a player-drawn tool.
- **Keep it fair and legible:** a unit that dies should have a visible reason (overrun, out of ammo, cornered); show it
  in the after-action report.

### Pitfalls to avoid (learned the hard way)
- Never use wall-clock time in simulation decisions.
- Never cap responders to hide a driving problem.
- Measure balance over many seeds; a single run lies.
- Instrument before fixing: log how and where units die before changing their behaviour.
- Every "wait for X" needs a timeout and a fallback.
- Any list of entities needs one owner; moving between lists must be atomic.

---

## 17. Glossary

- **Map version / civic:** the version of the generation and simulation rules a city uses (61 at 33.0).
- **Incident / call:** a 911 call with a location, nearby zombie count, officers down.
- **Cordon / infection zone / perimeter:** police lines round an area of the dead.
- **QRF:** quick reaction force.
- **Outside help / reserves:** police and army from outside the city that arrive in waves or convoys.
- **National Guard:** extra reserves mobilised past 5,000 zombies.
- **LOD:** level of detail; entities off screen think/move less often.
- **Lure:** a shared route the dead follow towards a noise.
- **Mass outbreak:** over 300 zombies (balance starts scaling), full effect at 1,800.

---

## 18. 33.0 additions

(Filled in when 33.0 is released: the full list of fixes and features in the largest update.)
