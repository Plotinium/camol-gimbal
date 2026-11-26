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
chunk data (`hasData` before `getData`), the client `CamoHelper.CLIENT_CAMO_MAP`. They spend
nothing, write nothing and check no permission.

- `Selection`: the blocks of a series (D-11). "Same kind" is the camo block where the origin has a
  camo, the real block otherwise.
- `Placement`: the state a block takes when placed at a position on a clicked face. A
  `BlockPlaceContext` subclass: `getClickedPos()` is the camo position, `replacingClickedOnBlock()`
  is false, face and click location are the real ones. A plain context is wrong here: it moves to the
  neighbour, or takes the "replacing" branch. Falls back to the default state. Used by apply and by
  the clicked-face mixin; tested with air in front of the clicked face.
- `Planner`: from copy, settings, hit, player look and series flag to a list of changes
  `(pos, before, after, kind)`, kind `PLACE`, `UPDATE` or `REPLACE`. Changes that alter nothing are
  left out. `waterlogged` is never copied.
- Orientation states (used by the direction switch): direction and axis properties, `Half`,
  `AttachFace`, `SlabType`, `FrontAndTop`, `rotation`. Not `DoubleBlockHalf`. "Follows look" rotates
  the copied state by the turn since copying, with the block's own rotate method. "Follows the face"
  takes the orientation states from `Placement`.

## Server actions (`server`)

- `GimbalActions`: apply, copy, remove, edit camo, edit copy, clear copy, change settings. Each
  checks that the player holds the Gimbal, that the block is within reach and loaded, and permission.
- `Permissions.canEdit(player, pos, face)`: `mayInteract`, `mayUseItemAt` and a posted
  `BlockEvent.EntityPlaceEvent` that nobody cancels (claim mods protect placement), its snapshot
  holding the block's current state. Used for every position about to be written, the clicked one
  included, and only for those. **Never post `BlockEvent.BreakEvent`:**
  Camol removes the camo on it.
- `Payment` (D-17), for `PLACE` and `REPLACE`, from the main inventory and the off hand: a filled camo
  item of the block (own type first, then the other); else an empty camo item (own type first) plus
  one block item. The block item must be that block's, not in `camol:disallowed_camo_blocks`, and
  carry no components beyond its default ones. Both stacks are found before either shrinks.
- Order for each change: permission, payment, write with `CamoHelper.setCamo`, return of the old camo
  (`REPLACE`). A series stops at the first change that cannot be paid; a refused position is skipped.
- Removal drops the camo item as Camol does. In creative nothing is spent or given back.
- `PLACE` skips air and blocks in `camol:disallowed_camoable_blocks`.

## Input and packets (`net`, `client`)

- Right click runs through the item's `onItemUseFirst` (after `RightClickBlock`, before the block's
  own action). With sneak it opens the menu on the client and does nothing on the server. In air only
  sneak does something. The item always consumes the click on a block.
- Left click: while the Gimbal is in the main hand the client cancels the attack input on a block,
  every time: the event fires on the press and again on every tick the button is held. The packet
  (copy, or remove with sneak) goes only on the change from released to pressed, tracked by the mod.
  `canAttackBlock` is false as a backstop.
- Client to server: left click, series key held or released, edit camo, edit copy, clear copy,
  settings. The server never takes a block from the client: an edited state must have the block of
  the stored camo or of the saved copy.
- Key mappings: menu (left Alt) and series (left Ctrl).
- The menu opens client side.

## Mixins

- See-through: in `ClientTickEvent.Post` the addon computes one value for the local player, Camol's
  main-hand camo item or a Gimbal in either hand with the switch on, and writes it to
  `ClientEventHandler.shouldBeTransparent`. A mixin (`require = 0`) makes Camol's section drawing,
  which reads only the main hand, use that flag. If the target is missing, the client logs one
  warning at start.
- `LevelChunk.setBlockState`, a very hot path, checks in this order: server side; the new state is
  air, a liquid block or a moving piston; the chunk `hasData`; only then the key. A position with a
  camo is put in a concurrent queue (other mods write blocks from other threads). At the end of the
  level tick, if it is still empty and the camo is still there, the camo drops and is cleared. The
  delay lets Camol move a camo itself.
- Camol `CamoItem.useOn`: the placement state comes from `Placement` when the server setting is on
  (`require = 0`).
