# Camol Gimbal - player guide

The Gimbal changes camouflages placed with [Camol](https://www.curseforge.com/minecraft/mc-mods/camol):
which way they face, whether they are open, top or bottom half, and every other state their block has
that changes how it looks.

## Craft it

Two gold ingots, two iron ingots and one Camo Block (the normal one, not Shape Retaining), in a 3x3
grid:

| | | |
|---|---|---|
| | Gold ingot | Camo Block |
| | Iron ingot | Gold ingot |
| Iron ingot | | |

A Camo Block already filled with a block works too, and the block inside it is lost.

## Controls

| Action | Control |
|---|---|
| Paste the copy | Right click |
| Copy a camo | Left click on it |
| Open the menu | Hold left Alt, or Shift and right click |
| Remove a camo | Shift and left click |
| Paste on many blocks | Hold Ctrl while you right click |

Both keys can be changed in the game's controls screen, under Camol Gimbal. Minecraft may mark them
in red because other actions use the same keys. The Gimbal never breaks blocks.

While you hold the Gimbal, camos are see-through, and a label shows the camo you are looking at.

## The menu

Look at a camo and open the menu to change that camo. Open it while looking at the air, or at a block
without a camo, to change your saved copy.

- Point at a value in the wheel to choose it. Use the mouse wheel to move between states.
- Holding Alt: release the key to apply. Shift and right click: left click a value to apply.
- Right click or Esc closes.

## Copy and paste

Place a camo as usual, adjust it with the Gimbal, then copy it.

- Pasting on a camo changes its state and type. It keeps its block, unless "Replace block" is on.
- Pasting on a block without a camo places a new one. It costs a Camo Block filled with that block,
  or an empty Camo Block plus the block. Either Camo Block type counts. Creative mode is free.
- The copy stays in the Gimbal until you clear it from the menu.

## Switches

| Switch | What it does | Default |
|---|---|---|
| Paste type | Pasting also changes the type of an existing camo | On |
| States to paste | Untick a state to leave it as it is | All ticked |
| See-through camos | Turn it off to see the final look with the Gimbal in hand | On |
| Direction | Copied; turned as much as you turned since copying; or taken from the face you click | Copied |
| Shape and Max | Which blocks Ctrl covers (connected, face, row, column) and how many, up to 64 | Connected, 64 |
| Replace block | An existing camo also takes the copied block; you pay for it and get the old one back | Off |
| Preview | Shows the result before you click | On |

## Other changes to Camol

- A camo drops as an item when its block disappears without a player breaking it: explosions,
  machines, pistons, other tools.
- A Camo Block applied to a block follows the face you click, so logs can stand upright. Server
  owners can turn this off with `followClickedFace` in `camolgimbal-server.toml`.

## Limits

- Chests, signs, beds, banners and heads show nothing when used as a camo.
- A trapdoor placed vertically is its open state: set it from the menu.
- With the Gimbal in the off hand and an empty main hand, doors and levers react to the click.
