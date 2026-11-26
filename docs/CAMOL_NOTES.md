# Notes on Camol

Read from the decompiled `camol-1.21.1-0.4.0.jar` (verified 2026-11-26). Nothing here was tried in
game. Camol is **All Rights Reserved**: depend on it, never copy its code or assets, never ship its
jar.

## Where things are

- Jar: `G:\curseforge\minecraft\Instances\All the Mods 10 - ATM10\mods\camol-1.21.1-0.4.0.jar`
- Instance: Minecraft 1.21.1, NeoForge 21.1.266, Create 6.0.10
- Source: <https://github.com/JDKDigital/camol>, branch `dev-1.21.1`. Its `gradle.properties` says
  version 0.3.1, so the released 0.4.0 jar is the reference. Before preparing a proposal, check
  whether the branch has the Sable classes found in the jar: it may be behind the release.
- Mod id `camol`, package `cy.jdkdigital.camol`, author LobsterJonn
- CurseForge project 1214396, file 9019881 for the 0.4.0 jar (from the instance's
  `minecraftinstance.json`)
- Its `neoforge.mods.toml` asks NeoForge `[21.1.0,)` and Minecraft `[1.21,1.21.1)`, yet it loads in
  ATM10 on Minecraft 1.21.1 (instance log, 2026-11-26)

## Data

- Chunk attachment `Camol.CAMO_BLOCK_MAP` (registered as `camol:solid_camo`):
  `Map<String, CamoPosition>`, key `String.valueOf(pos.asLong())`.
- `CamoPosition(String camoType, BlockState state)`; type `"normal"` or `"solid"`. A removed camo
  is stored as an air state.
- `CamoHelper.setCamo(ServerLevel, Map<BlockPos, CamoPosition>)` is public: it saves the entries
  and syncs the players tracking the chunk. Changing a pose needs no mixin.
- The client copy is `CamoHelper.CLIENT_CAMO_MAP` (public static).

## Items, cost and removal

- Items: `camol:camo_item` (normal) and `camol:solid_camo_item`. The disguise block is the
  `Camol.BLOCK_COMPONENT` data component; it holds the block's default state.
- Filling: crafting one camo item with one block item (`camol:simple_camo_crafting`). The recipe
  defines no remaining items, so both are consumed.
- Applying: `CamoItem.useOn` on a position without a camo places one and shrinks the held stack,
  except with infinite materials. It refuses target blocks in
  `camol:disallowed_camoable_blocks` (`Camol.CAMO_BLACKLIST`).
- Removing: using any camo item on a camouflaged position removes the camo and drops its item.
- Dropping, on removal or player break: `CamoItem.getCamoItem` builds the item of the camo's type
  with the block's default state, so the pose is lost.

## Why some poses are unreachable

`CamoItem.useOn` builds the placement context with the player's horizontal direction as the clicked
face, whatever face was really clicked. Every block whose placement reads the clicked face is
affected.

- Trapdoor: `TrapDoorBlock.getStateForPlacement` sets `open` only when the position is powered by
  redstone, so the stored state is closed, which is horizontal.
- Log: `axis` follows the clicked face, so it is never `y`
  ([issue 9](https://github.com/JDKDigital/camol/issues/9)). A second test case.

## Drawing while a camo item is held

- `EventHandler.playerTick` writes `ClientEventHandler.shouldBeTransparent` for every player on
  the client; a change of the flag marks the camouflaged sections for rebuild.
- The see-through drawing is decided inside the synthetic method
  `ClientEventHandler.lambda$geometryEvent$5`, by two `ItemStack.is(Holder)` calls on the main hand
  item. Another item counts only through a mixin there. **A Camol update can rename that method.**
- A held tool therefore needs that mixin, plus code that sets the flag.
- `PlayerTickEvent.Post` fires for remote players too (`RemotePlayer.tick` calls `super.tick`), so
  on a server with other players the last player ticked sets the flag. `ClientTickEvent.Post` fires
  at the end of `Minecraft.tick`, after `ClientLevel.tickEntities`, and before the frame is drawn
  (verified 2026-11-26, NeoForge 21.1.251 sources).
- The off-hand check counts a solid camo item, but the drawing reads only the main hand: the
  sections are rebuilt and stay opaque.
- Section geometry, and with it the lambda, is built in `SectionRenderDispatcher` rebuild tasks on
  worker threads (verified 2026-11-26).

## Limit of the drawing

Camol draws only the baked model of the camo state. Probable, from reading only: blocks drawn by a
block entity renderer (chests, signs, beds, banners, heads) show nothing as a camo.

## Known Camol defects

- A camo stays when its block disappears without a player breaking it
  ([explosion](https://github.com/JDKDigital/camol/issues/12),
  [duplication with the Mekanism configurator](https://github.com/JDKDigital/camol/issues/11)).
- The collision hook allocates on every call and reads a non-thread-safe map
  ([issue 13](https://github.com/JDKDigital/camol/issues/13)).
- Offhand check in `playerTick` tests the solid item twice and never the normal one.
- Probable, from reading only: on a dedicated server the solid camo collision does not apply,
  because only the client fills the map the collision hook reads.
- The ATM10 log shows `cable_facades` losing its `getAppearance` mixin to Camol's.
- [Rotation request](https://github.com/JDKDigital/camol/issues/9), open (verified 2026-11-26).
