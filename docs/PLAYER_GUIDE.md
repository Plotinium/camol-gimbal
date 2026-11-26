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

While you hold the Gimbal, camos are see-through, and a small box at the left edge of the screen shows
the camo you are looking at: its block and its values.

## The menu

Look at a camo and open the menu to change that camo. Open it while looking at the air, or at a block
without a camo, to change your saved copy.

- The list on the left shows every state of the block and its value. Click a row, or use the mouse
  wheel, to pick the one the wheel in the middle shows.
- Left click a value in the wheel to apply it. The menu stays open for the next change.
- Holding Alt: point at a value and release the key to apply it and close.
- Right click or Esc closes.
- On the right are your saved copy, the button that clears it and the Settings button.

## Copy and paste

Place a camo as usual, adjust it with the Gimbal, then copy it.

- Pasting on a camo changes its state and type. It keeps its block, unless "Block of a camo already there" is set to Replaced.
- Pasting on a block without a camo places a new one. It costs a Camo Block filled with that block,
  or an empty Camo Block plus the block. Either Camo Block type counts. Creative mode is free.
- The copy stays in the Gimbal until you clear it from the menu.

## Settings

Press Settings in the menu. They stay saved in the Gimbal.

| Setting | What it does | Default |
|---|---|---|
| **When you paste** | | |
| Camo Block type | Copied: a camo already there takes the copied type. Keeps: it keeps its own | Copied |
| Direction | Copied; turned as much as you turned since copying; or taken from the face you click | Copied |
| Block of a camo already there | Keeps, or Replaced: you pay for the new camo and get the old one back | Keeps |
| States to paste | Opens a list: untick a state to leave it as it is | All |
| **With Ctrl, on many blocks** | | |
| Shape | Connected, face, row or column | Connected |
| At most | How many blocks one click changes: 8, 16, 32 or 64 | 64 |
| **What you see** | | |
| See-through camos | Turn it off to see the final look with the Gimbal in hand | Yes |
| Preview before the click | Shows the result before you click | Yes |

## Other changes to Camol

- A camo drops as an item when its block disappears without a player breaking it: explosions,
  machines, pistons, other tools.
- A Camo Block applied to a block follows the face you click, so logs can stand upright. Server
  owners can turn this off with `followClickedFace` in `camolgimbal-server.toml`.

## Limits

- Chests, signs, beds, banners and heads show nothing when used as a camo.
- A trapdoor placed vertically is its open state: set it from the menu.
- From the off hand, a block's own action comes first: doors, levers and machines react to the
  right click. The preview shows only with the Gimbal in the main hand.
- Other mods may count a change made with the Gimbal as a block placed by you.
- The first time you look at a very complex block, the game can pause for a moment.
