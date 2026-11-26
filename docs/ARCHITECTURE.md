# Camol Gimbal - architecture

How the code is put together. What it does is in `DECISIONS.md`; Camol's internals are in
`CAMOL_NOTES.md`. Base package `plotinium.camolgimbal`.

## Data on the item

Two data components, persistent and synced:

- `camolgimbal:copy`, `SavedCopy(String type, BlockState state, Direction look)`: Camol's type
  (`normal` or `solid`) and the horizontal direction the player faced while copying.
- `camolgimbal:settings`, `GimbalSettings(pasteType, skippedStates, seeThrough, direction, shape,
  limit, replaceBlock, preview)`. The constructor clamps: limit 8, 16, 32 or 64; a bounded set of
  names. An absent component means the defaults of D-19.

## Rules shared by server and preview (`rules`)

They read the world through `CamoView` (camo at a position, or none) plus the level; the server reads
chunk data (`isLoaded`, then `hasData` before `getData`, so they never load a chunk or create the
attachment), the client `CamoHelper.CLIENT_CAMO_MAP`. They spend nothing, write nothing and check no
permission. Entry points for the preview: `CamoView` (a lambda over the client map) and
`Planner.plan(level, view, player, copy, settings, hit, look, series)`.

- `Selection`: the blocks of a series (D-11), breadth first from the clicked block, nearest first, up
  to the limit, loaded positions only. "Same kind" is the camo block where the origin has a camo;
  otherwise blocks without a camo whose real block is the origin's. Row on a wall runs along the
  face's clockwise axis, on a floor or ceiling across the player's look; column on a wall is
  vertical, on a floor or ceiling along the look.
- `Placement`: the state a block takes when placed at a position on a clicked face. A
  `BlockPlaceContext` subclass: `getClickedPos()` is the camo position, `replacingClickedOnBlock()`
  is false, face and click location are the real ones. A plain context is wrong here: it moves to the
  neighbour, or takes the "replacing" branch. The overrides read the hit result, which exists before
  the super constructor calls `getClickedPos()`. Falls back to the default state, also when the block
  throws on this context (some need a real player). Used by apply and by the clicked-face mixin.
- `Planner`: from copy, settings, hit, player look and series flag to a list of changes
  `(pos, before, after, kind)`, kind `PLACE`, `UPDATE` or `REPLACE`. Changes that alter nothing are
  left out. `REPLACE` only when the switch is on and the block differs; the same block is an
  `UPDATE`, which costs nothing. States are matched by property name and value name, so a horizontal
  `facing` refuses `up`. A skipped state keeps the existing camo's value, or the placement value on
  a new camo. `waterlogged` is never copied, and a new camo is never waterlogged. Type: a new camo
  takes the copied type; an existing one takes it only with the paste type switch on.
- Orientation states (used by the direction switch): direction and axis properties, `Half`,
  `AttachFace`, `SlabType`, `FrontAndTop`, `rotation`. Not `DoubleBlockHalf`. "Follows look" rotates
  the copied state by the turn since copying, with the block's own rotate method. "Follows the face"
  takes the orientation states from `Placement`.

## Server actions (`server`)

- `GimbalActions`: apply, copy, remove, edit camo, edit copy, clear copy, change settings. Each
  checks that the player holds the Gimbal, and the actions on a block that it is loaded and within
  reach (`canInteractWithBlock`, margin 1) and the permission. Which Gimbal: the clicked hand for
  apply, the main hand for copy and remove, the main hand else the off hand for the menu packets.
  The edits of the copy and the settings have no position and check no permission.
- `Permissions.canEdit(player, pos, face)`: `mayInteract`, `mayUseItemAt` and a posted
  `BlockEvent.EntityPlaceEvent` that nobody cancels (claim mods protect placement), its snapshot
  holding the block's current state. Used for every position about to be written, the clicked one
  included, and for copy, which writes nothing but is an action the Constraints protect. FTB Chunks
  2101.1.22 registers on Architectury's place event (read in its jar, not tried in a run). **Never post `BlockEvent.BreakEvent`:** Camol removes
  the camo on it.
- `Payment` (D-17), for `PLACE` and `REPLACE`, from the main inventory and the off hand: a filled camo
  item of the block (own type first, then the other); else an empty camo item (own type first) plus
  one block item. The block item must be that block's, not in `camol:disallowed_camo_blocks`, and
  carry no components beyond its default ones. Both stacks are found before either shrinks.
- Order for each change: the camo is still the planned `before` (else skipped), permission, payment,
  write with `CamoHelper.setCamo`, return of the old camo (`REPLACE`: inventory, else a drop at the
  player). A series stops at the first change that cannot be paid; a refused position is skipped.
- Removal drops the camo item from the clicked face as Camol does. In creative nothing is spent or
  given back, a replaced camo included.
- `SeriesKeys`: who holds the series key, as the client reports it; cleared on logout.
- `Messages`: action bar texts (nothing copied, nothing to copy, no camo, items missing, series
  stopped after N, refused, N refused).
- `PLACE` skips air and blocks in `camol:disallowed_camoable_blocks`.

## Input and packets (`net`, `client`)

- Right click runs through the item's `onItemUseFirst` (after `RightClickBlock`, before the block's
  own action). With sneak it opens the menu on the client and does nothing on the server. In air only
  sneak does something. The item always consumes the click on a block. The menu opens through
  `item.MenuHook`: common code holds a no-op opener, the client sets the real one at setup.
- Left click: while the Gimbal is in the main hand the client cancels the attack input on a block,
  every time: the event fires on the press and again on every tick the button is held. The packet
  `LeftClickPayload(pos, face, remove)` goes only on the change from released to pressed, tracked by
  the mod, with `remove` set from Shift as the client saw it. `canAttackBlock` is false as a
  backstop. The server does not post `PlayerInteractEvent.LeftClickBlock`: `CommonHooks.onLeftClickBlock`
  posts it to every server listener, which would bring back the click the client suppresses; claims
  answer through the place event.
- Client to server (`net`, handled on the server thread): `LeftClickPayload`,
  `SeriesKeyPayload(held)` on press and on release, `EditCamoPayload(pos, state)`,
  `EditCopyPayload(state)`, `ClearCopyPayload.INSTANCE`, `SettingsPayload(settings)`. The server
  never takes a block from the client: an edited state must have the block of the stored camo or of
  the saved copy; a menu edit never changes `waterlogged`. Settings are clamped when decoded.
- Key mappings: menu (left Alt) and series (left Ctrl).
- The menu opens client side.

## Mixins

- Config `camolgimbal.mixins.json`, declared in `neoforge.mods.toml`, with the plugin
  `CamolGimbalMixinPlugin`.
- See-through (`client.SeeThroughClient`, `mixin.CamolDrawingMixin`): in `ClientTickEvent.Post` the
  addon computes one value for the local player with `item.SeeThrough` (Camol's main-hand camo item,
  or a Gimbal in either hand with the switch on), keeps it in a volatile field of its own and copies
  it into `ClientEventHandler.shouldBeTransparent`, which drives Camol's rebuild of the camouflaged
  sections. The mixin (`@ModifyVariable` on the only boolean local of
  `lambda$geometryEvent$5`, `require = 0`, client only) makes Camol's section drawing use the
  addon's field, not Camol's flag: sections are built on worker threads, and Camol's
  `PlayerTickEvent.Post` handler rewrites its flag during the tick once per client player, remote
  ones included. If the handler is not reached, the plugin logs one warning when Camol's class loads
  at client start.
- `LevelChunk.setBlockState`, a very hot path, checks in this order: server side; the new state is
  air, a liquid block or a moving piston; the chunk `hasData`; only then the key. A position with a
  camo is put in a concurrent queue (other mods write blocks from other threads). At the end of the
  level tick, if it is still empty and the camo is still there, the camo drops and is cleared. The
  delay lets Camol move a camo itself.
- Camol `CamoItem.useOn`: the placement state comes from `Placement` when the server setting is on
  (`require = 0`).
