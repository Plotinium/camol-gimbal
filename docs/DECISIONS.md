# Camol Gimbal - decisions

An addon for the Camol mod: a tool that changes the pose and look of a camouflage.
How Camol works inside: `CAMOL_NOTES.md`. How the code is put together: `ARCHITECTURE.md`.

## D-1 - The tool changes the states the block already has

The tool rewrites the stored `BlockState` of a camouflage. Every property with a visible effect can
be changed: facing, axis, half, open, shape, hinge, connections, and look such as `lit` or a crop's
age. A state whose values all draw the same is hidden, and so is `waterlogged`, since the camo draws
no water. A camouflaged block takes only the states Minecraft (or its mod) gives that block.

- **Rejected for the first version:** free rotation in 90 degree steps (vertical slabs, carpets on
  walls). It needs a rotation stored by the addon, plus changes to Camol's drawing, lighting and
  collisions.
- **Rejected:** pose properties only. Changing anything that has a shape and a state is the point of
  the addon.

## D-2 - The name is Camol Gimbal

A gimbal is the joint that orients an object on its axes.

- **Rejected:** Camol Pivot, Camol Poses, Camol Tinker, Camol Tilt, Camol Spanner, Camol Giravolta,
  Camol Girella, Camol Wrench.
- A web search for "Camol Gimbal" found no mod with this name (verified 2026-11-26). A different
  mod called "Gimbal" (building helper, Fabric 1.19.2) is on Modrinth.

## D-3 - The addon and a proposal to Camol's author, together

The addon is the main work. Plotinium opens the proposal to Camol's author; it is prepared here. It
contains:

- placement on the clicked face;
- a way for another item to show camouflages as a camo item does (for example an item tag), which
  replaces the two item checks Camol hardcodes.

- **Rejected:** addon only, or proposal only.

## D-4 - The state is picked in a radial menu with every state in sight

Based on the logic of Create's radial wrench menu: the wheel shows the values of one state, each with
a 3D preview. A list beside the wheel shows every state with its current value; a click on the list,
or the mouse wheel, picks which state the wheel shows. The menu reopens on the state picked last,
also on another block that has it; on a block without it, on the first one, without forgetting the
pick.

- **Rejected:** one state at a time with the others reachable only by the mouse wheel (they go
  unnoticed); a list with a button for each value and no wheel (long for blocks with many values); a
  debug stick style cycle; rotation on the clicked face.

## D-5 - Only the Gimbal item

Other wrenches do not act on camouflages, so nothing interferes with their own actions.

## D-6 - For publication, tested by Plotinium

The addon is tested by Plotinium in game and then published. It works in singleplayer and on a
dedicated server. It is made for an international audience: English repository and texts, with
translations. The author name is Plotinium. The license is MIT; the mod is published on CurseForge,
and on Modrinth only if its rules on generative AI allow it (`guide/da-fare.md`).

## D-7 - Minecraft 1.21.1, NeoForge

- **Rejected for now:** 26.1.2.

## D-8 - What the first version contains

- The radial menu (D-4).
- Camouflaged blocks show, while the Gimbal is held, as they do while a Camol camo item is held.
- An on-screen label with the camouflage and its state.
- Copy and apply (D-10), on one block or on many (D-11).
- The menu switches (D-19).
- A camo item applied to a block follows the clicked face (D-13).
- Cleanup of camouflages left behind when their block disappears (D-12).

## D-9 - No extra library

The radial menu adapts the logic of Create's menu and is drawn with Minecraft's own functions. Camol
is the only mod the addon requires.

- **Rejected:** Ponder (and Flywheel, which it depends on) bundled or required; Create required.

## D-10 - Applying what was copied

- On a block that already has a camouflage, the camo keeps its block (unless D-19 replaces it); its
  pose and its type (normal or Shape Retaining) become the copied ones. Only the states the two
  blocks share are applied.
- Changing the type of an existing camo costs nothing: Camol's own recipes convert one type into the
  other, one to one, with no materials.
- On a block without a camouflage, a camouflage of the copied block is placed, in the copied pose and
  type (D-17).

## D-11 - Applying to many blocks

With the series key held (Ctrl by default), applying acts on many blocks of the same kind: on
existing camouflages, those with the same camo block; where new ones are placed, those with the same
real block. Neighbours touch by a face. Removing and copying act on one block.

- Shapes: connected (every direction), face (the plane of the clicked face), row (left to right) and
  column (bottom to top; on a floor or ceiling, away from the player). Row and column extend both
  ways from the clicked block.
- Limit: 8, 16, 32 or 64 blocks; 64 by default.
- Each new camouflage is paid for (D-17). The series stops at the last one that was paid for.

## D-12 - A camo drops when its block disappears

When a block disappears without a player breaking it (explosion, machine, another mod's wrench), its
camouflage drops as an item. "Disappears" means the position becomes empty: air, a fluid, or a
moving piston. A block turned into another block (grass into dirt, oxidised copper, stripped log)
keeps its camouflage. A block moved by a piston or a contraption leaves its position, so the camo
drops there. Moving a camouflage along with its block belongs to Camol.

- **Rejected:** the camouflage vanishes; the addon moves it with the block.

## D-13 - Placement on the clicked face, with a setting

A camo item follows the clicked face, as placing the block would. This is on by default and can be
turned off in the configuration. A vertical trapdoor is its open state, which placement does not
set: it is set from the Gimbal's menu.

## D-14 - Blocks drawn by a block entity renderer come later

Chests, signs and beds as camouflages are outside the first version.

## D-15 - Recipe of the Gimbal

Shaped, 3x3: `" GC"`, `" IG"`, `"I  "`, where G is a gold ingot, I an iron ingot and C a Camo Block.
Two gold, two iron, one Camo Block.

## D-16 - Controls

| Action | Control |
|---|---|
| Apply | right click |
| Copy | left click on a camouflaged block |
| Open the menu | Shift and right click, or left Alt |
| Remove the camouflage | Shift and left click |
| Many blocks | Ctrl held while applying |

The Gimbal never breaks a block. The menu key and the series key can be changed in Minecraft's
controls screen. Only a placed camouflage can be copied: you place a camo, adjust it with the Gimbal,
then copy it. A left click on a block without a camouflage copies nothing.

- **Rejected:** left click applies and right click opens the menu; copy on the middle button; copy on
  Shift and right click; copying a real block.

## D-17 - What a new camouflage costs

A filled camo item of the copied block is spent first. Without one, an empty camo item plus one of
the block are spent, which is what Camol's crafting asks. A filled or empty camo item of the other
type also counts, since conversion is free. In creative nothing is spent, and removing a camouflage
gives nothing back.

- **Rejected:** filled camo items only; free camouflages, which would create items out of nothing
  since a camouflage drops when removed.

## D-18 - What the menu edits

The menu edits what is in front of the player.

- On a camouflage, it shows and changes that camo's current state, as Create's wrench does. The saved
  copy stays as it is.
- In the air, or on a block without a camouflage, it shows and changes the saved copy, which the next
  right click applies.
- The main page has the state list on the left, the wheel in the middle, and on the right the saved
  copy with its values, the button that clears it and the button that opens the settings page. With
  nothing saved and no camo in front, it says so and keeps the two buttons.
- The settings page holds the switches (D-19) in three groups: when you paste; with the series key,
  on many blocks; what you see. A button goes back. The copy and the switches stay saved in the
  Gimbal.
- Held open with left Alt, releasing the key applies the value the mouse points at and closes. Opened
  with Shift and right click, it stays open.
- In both, a left click on a sector applies it and leaves the menu open for the next change, and a
  right click or Esc closes.

- **Rejected:** the saved copy takes priority over the camo looked at; the switches in a side panel on
  the main page (too crowded to understand).

## D-19 - The switches

| Switch | What it does | Default |
|---|---|---|
| Paste type | Off: an existing camo keeps its type | on |
| States to paste | One tick per copied state; an unticked one is not pasted. On a new camo it takes the value of placing on the clicked face. Ticks follow the state's name until the copy is cleared | all on |
| See-through camos | Off: the final look shows with the Gimbal in hand | on |
| Direction | As copied; follows your look (turned by how much you turned since copying); or follows the clicked face (direction states as if placing there) | as copied |
| Series shape and limit | D-11 | connected, 64 |
| Replace the block | On an existing camo the block changes too: the new camo is paid for (D-17), then the old one returns to the inventory, or drops at the player if it is full | off |
| Preview | Shows the result, on every block it would change, before the click | on |

## D-20 - The name and look of the item

The item is called Camol Gimbal. It shows a lilac blob like the one on Camol's items, squeezed at the
waist by closed golden tongs: the camo caught and bent to the player's will. Grips and pivot lie on
one line with the jaws; the jaw in front wraps the blob, and the one behind shows only where it leaves
the pivot, at the left side. It is a 32x32 drawing made for this mod; no Camol file is used. The mod
page icon is the same drawing. It is an AI drawing that the author retouched by hand; it is kept with
the earlier versions in `art/` (the one in use is `art/4-final`), which is not part of the jar.

In first person the item is held like any tool. In third person it is turned over about the line of
the tongs, with the grips in the same place: the blob sits on the other side of that line compared with
a sword blade, so the standard tool pose shows it upside down.

- **Rejected:** a round sceptre with three colours; using Camol's texture; a 16x16 drawing made by
  generator, too small for the face and the tongs; a single rod holding an open ring, which does not
  read as tongs; tongs showing both jaws whole, with grips and jaws pointing in different directions;
  tongs with no part of the back jaw in sight.

## D-22 - The use of generative AI is declared

The README, the project page text and the mod's description say that the mod was made with generative
AI, and which parts.

## D-21 - On screen label and messages

- While a Gimbal is held and a camo is looked at, a small box at the left edge, at mid height, shows
  the block's icon and name and its states in one compact line; Shape Retaining camos say so.
- Every action says what it did on the action bar: copied, pasted, blocks changed, removed, edited,
  copy cleared, nothing to change, refused.

- **Rejected:** a column of lines under the crosshair, which covers the view.

## Constraints

- A Camol fix made by the addon steps aside when Camol has already handled the case (for example,
  act only if the camo entry still exists), so that a fix merged upstream never runs twice and
  duplicates items.
- The copy and every setting that changes what the server does are stored on the Gimbal item, and
  the server reads them from there.
- The server never trusts a state sent by the client: its block must be the block of the stored camo
  (world edit) or of the saved copy (memory edit). Every setting received is clamped to its allowed
  values.
- Edit permission holds for every action (copy, apply, remove, menu, many blocks). A click is
  guarded by its own event: `RightClickBlock` fires before the item acts, and left clicks are handled
  at low priority, never when cancelled. Menu packets and the blocks of a series after the first
  need an explicit server check, confirmed against FTB Chunks.
- A permission check never posts `BlockEvent.BreakEvent`: Camol listens to it, drops the camo item
  and erases the camouflage.
- Cost is spent before the camouflage is written.
- Applying wins over the block's own interaction (machines, doors, levers) from the main hand; from
  the off hand the block's own action comes first.
- Placing a camouflage honors `camol:disallowed_camoable_blocks` for the target and, on the
  ingredient path, `camol:disallowed_camo_blocks` and the need for a block item.
- The server cannot see the client's Ctrl key: the series key is a key binding whose state the client
  sends.
