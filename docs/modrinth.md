# Source Movement

CS:GO movement in Minecraft. Bunnyhop, air strafe and crouch jump exactly like Counter-Strike,
while the rest of the game stays vanilla.

## What it does

Source Movement swaps your walking, jumping and air control for the movement code of the Source
engine, the one CS:GO runs on. The maths is a line by line port of Valve's public Source SDK 2013
`FullWalkMove`, tuned to CS:GO's own values:

- **Bunnyhopping:** jump on the tick you land and ground friction never touches you
- **Air strafing:** hold A or D and turn your mouse the same way to build speed in the air
- **Crouch jumps:** duck mid jump to tuck your feet up and clear higher ledges (and fences)
- **CS:GO numbers:** 250 u/s running speed, 5.5 accelerate, 12 air accelerate, 5.2 friction, 800 gravity

## Why it feels right

Minecraft ticks 20 times a second. CS:GO's movement runs at 64. This mod runs the real 64 tick
simulation in between Minecraft's ticks, and every one of those 64 ticks reads your mouse
interpolated from the frames you actually rendered. Strafes follow your hand as smoothly as in CS,
and jump timing lands on the right tick instead of being rounded to Minecraft's 50 ms.

## Features

- **Scroll wheel jump**, like a CS `mwheel` bind, in either or both directions (the hotbar still
  works with number keys)
- **Speedometer** with KZ style jump stats: takeoff speed, speed gained per hop and strafe sync %
- **Two presets:** `csgo_bhop` (no speed cap, like bhop and KZ servers) and `csgo` (matchmaking's
  1.1x jump speed cap)
- **Autohop** option
- **Controller support** through Controlify
- **Settings screen** in Mod Menu (needs Cloth Config), or edit `config/sourcemovement.json`
- Toggle key (`]`) and `/sourcemovement toggle`

## Still vanilla

Movement hands back to vanilla, keeping your speed, for flying, elytra, swimming, ladders, vines,
scaffolding, riding and Riptide. Stairs and slabs step up as usual, sneaking still stops you at
edges, fall damage is unchanged, and Speed, Slowness, Jump Boost, Slow Falling, ice, slime, soul
sand, honey and cobwebs all still apply.

## Good to know

- **Client side only, singleplayer by default.** Servers do not expect these speeds and may kick
  you, so the mod stays off on servers unless you turn on `allowMultiplayer`. LAN worlds you host
  count as singleplayer.
- **No stamina.** CS:GO's post jump slowdown is left out because its exact formula was never
  published.
- **Higher FPS means smoother strafes**, just like in CS.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.3 or newer
- Fabric API
- Optional: Mod Menu and Cloth Config for the settings screen

Source code and issues: https://github.com/grvig/mccs
