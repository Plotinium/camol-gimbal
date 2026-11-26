# Notes on Create's radial wrench menu

Read from the decompiled `create-1.21.1-6.0.10.jar` (verified 2026-11-26). Nothing here was tried
in game.

## License

From `LICENSE.md` of <https://github.com/Creators-of-Create/Create>, branch `mc1.21.1/dev`
(verified 2026-11-26): code is **MIT**, files under `src/main/resources/assets/` are **All Rights
Reserved**. Adapted code is taken from the GitHub source at that branch, not from the decompiled
jar, and keeps Create's MIT notice in the adapted files. No textures, models or icons are copied
(`AllIcons` included). The Ponder repository (<https://github.com/Creators-of-Create/Ponder>) is
also MIT.

## Classes

Package `com.simibubi.create.content.contraptions.wrench`:

- `RadialWrenchHandler` - opens the screen from a key press. The key "Open Block Rotation Menu" has
  no default binding.
- `RadialWrenchMenu` (about 370 lines) - the screen. For one property it builds the list of states
  by cycling it, draws one sector per state with a 3D block preview, picks by mouse angle, changes
  property with the wheel, applies on key release or left click, closes on right click. Only a fixed
  list of rotation properties is offered.
- `RadialWrenchMenuSubmitPacket` - sends the chosen `BlockState` to the server, which checks only
  that the block matches and sets it in the world.

## What reuse needs

The menu cannot be called as it is on a camouflage: the screen closes when the block in the world
differs from the state it was given, and the packet writes the world block. The constructor is
private. The logic is copied and changed:

- every property of the stored camo state, not Create's list;
- apply through `CamoHelper.setCamo`;
- the server checks the held item, the reach, and that the block of the new state is the block of
  the stored camo;
- the server checks the permission to edit on its own: a packet does not pass through the
  right-click event that claim mods (FTB Chunks is in ATM10) listen to.

`AbstractSimiScreen`, `GuiGameElement`, `UIRenderHelper`, `Color` and `AngleHelper` are in the
`catnip` library inside the Ponder mod (`ponder-neoforge-1.0.82+mc1.21.1.jar`, MIT, depends on
Flywheel), which the Create jar ships. `TransformStack` is Flywheel's. Without those dependencies
the drawing is rewritten with Minecraft's own functions (`BlockRenderDispatcher.renderSingleBlock`
for the preview).
