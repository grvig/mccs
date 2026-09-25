# Source Movement

A client side Fabric mod for Minecraft 1.21.11 that replaces the local player's walking, jumping and
air movement with CS:GO's Source engine movement: ground friction and acceleration, air strafing,
bunnyhopping and crouch jumps. Everything else in the game stays vanilla.

## How it works

- The movement maths is a straight port of `CGameMovement::FullWalkMove` from the Source SDK 2013
  (`src/main/java/.../core`), with no Minecraft code in it, and is covered by unit tests.
- It runs at CS:GO's 64 ticks a second inside Minecraft's 20. Each Source tick gets its own slice of
  real time and sees the view angle interpolated from rendered frames at that moment, plus the keys
  and scroll notches from that slice, the way a Source usercmd would.
- Moves go through Minecraft's own collision, so step up (stairs and slabs), sneaking at edges and
  fall damage behave as usual. An axis that hits a wall loses its velocity, which is what Source's
  velocity clipping does against axis aligned walls.
- 1 block is 40 units, so the 72 unit CS player matches the 1.8 block Minecraft player. Running speed
  is 250 u/s (6.25 blocks a second), a standing jump peaks at 54.65 units, a ducked one at 57.

## Controls

| Action | Default |
| --- | --- |
| Jump | Jump key, or either scroll direction |
| Crouch jump | Sneak while in the air (pulls your feet up 9 units) |
| Toggle the mod | `]` |
| Toggle the speedometer | `[` |

Scroll jumping replaces hotbar scrolling in the chosen direction. Number keys still pick slots,
and menus, chat and spectator mode scroll as usual.

Commands: `/sourcemovement toggle` and `/sourcemovement reload`.

## Config

`config/sourcemovement.json` is written on first launch. Speeds are in units per second.

| Key | Default | Meaning |
| --- | --- | --- |
| `preset` | `csgo_bhop` | `csgo_bhop`: no jump speed cap. `csgo`: matchmaking's cap of 1.1x max speed on every jump |
| `bhopCapEnabled` | unset | Set to `true` or `false` to override the preset |
| `bhopCapFactor` | 1.1 | The cap, as a multiple of max speed |
| `autohop` | false | Holding jump jumps on every landing |
| `scrollJump` | `both` | `off`, `up`, `down` or `both` |
| `accelerate` | 5.5 | sv_accelerate |
| `airAccelerate` | 12 | sv_airaccelerate |
| `airMaxWishSpeed` | 30 | sv_air_max_wishspeed |
| `friction` | 5.2 | sv_friction |
| `stopSpeed` | 80 | sv_stopspeed |
| `maxSpeed` | 320 | sv_maxspeed, the ceiling for Speed effects |
| `runSpeed` | 250 | Knife running speed |
| `duckSpeedMultiplier` | 0.34 | Crouched speed, 85 u/s |
| `duckJumpLift` | 9 | How far ducking in the air pulls your feet up, in units |
| `gravity` | 800 | sv_gravity |
| `jumpImpulse` | 301.993377 | sv_jump_impulse |
| `maxVelocity` | 3500 | sv_maxvelocity, per axis |
| `unitsPerBlock` | 40 | Units in one block |

Run `/sourcemovement reload` after editing.

## What stays vanilla

Movement switches back to vanilla, carrying your speed across, while flying, gliding with an elytra,
in water or lava, on ladders, vines and scaffolding, riding, sleeping, using a riptide trident or
floating with Levitation. Speed and Slowness, Jump Boost, Slow Falling, Swift Sneak, soul sand,
honey, cobwebs, ice and slime all still apply, mapped onto the Source values. Sprinting keeps its
attack and swimming uses but does not change ground speed, as in CS.

## Not included

CS:GO's stamina (the slowdown after jumping and landing) is left out. Its exact formulas have never
been published outside Valve's own code, and the public convar descriptions are not enough to
rebuild it faithfully.

## Building

```
gradlew.bat build
```

The mod jar ends up in `build/libs`. `gradlew.bat test` runs the movement unit tests and
`gradlew.bat runClientGameTest` plays through the in game checks in a fresh world.
