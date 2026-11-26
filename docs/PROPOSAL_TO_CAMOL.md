# Proposal to Camol

Text for an issue on <https://github.com/JDKDigital/camol/issues>. Checked on 2026-11-26 against
branch `dev-1.21.1` (last commit `27a72ab`, 2026-05-11) and the released `camol-1.21.1-0.4.0.jar`.
The branch has both behaviours described below; it does not have the Sable classes found in the jar,
so it is behind the release.

## Title

Camo placement should follow the clicked face, and let other items reveal camos

## Body

Hi, I am writing an addon for Camol, Camol Gimbal, a tool that changes the state of a placed
camouflage. While writing it I found two places where a small change in Camol would help every
player and let the addon drop its mixins.

**1. Placement ignores the clicked face**

`CamoItem.useOn` builds the placement context with the player's horizontal direction as the clicked
face, and offsets the position by that direction. Blocks that read the clicked face in
`getStateForPlacement` therefore never get some states: a log camo can never be vertical (see
issue 9), and a trapdoor camo takes its side from the direction the player looks.

Using the real clicked face is not enough with a plain `BlockPlaceContext`, because it moves the
placement position to the neighbouring block or takes the "replacing" branch. What works is a small
subclass of `BlockPlaceContext` where `getClickedPos()` returns the camouflaged position,
`replacingClickedOnBlock()` returns false, and the face and click location are the real ones. The
overrides should read the hit result, since the parent constructor already calls them. The addon has
this class under the MIT license, and you are welcome to use it.

**2. Other items cannot reveal camos**

The see-through drawing in `ClientEventHandler.geometryEvent` checks the main hand against
`CAMO_ITEM` and `SOLID_CAMO_ITEM` directly. An item tag, for example `camol:reveals_camo`,
containing your two items, would let addons add their own tool.

Two related details in `EventHandler.playerTick`:

- the off hand check tests `SOLID_CAMO_ITEM` twice and never `CAMO_ITEM`;
- the flag is written on every client player tick, remote players included, so another player's
  held item can change what the local player sees. Checking only the local player fixes it.

Would you accept a pull request for either of these?

One more thing: the addon's item and icon show a blob inspired by yours, drawn from scratch, held by a
pair of tongs. If you would rather it did not, tell me and I will change it.

## After it is opened

If Camol takes the first change, the addon's setting `followClickedFace` can be removed in a later
version. If it takes the tag, the drawing mixin can be removed.
