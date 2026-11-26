# Camol Gimbal - open points

State on 2026-11-26.

## Not verified

- The visual check (`gradlew visualCheck`, verified 2026-11-26) covers, in a development client at GUI
  sizes 427x240, 640x360 and 1280x720: the menu's pages, the label, the preview on one block and on a short row,
  and by calls into the screen a sector click, two edits at once, the right click, the release in held
  mode and the settings page in held mode. Real keys and mouse, rebound keys, left click in the world,
  see-through drawing under play and Fabulous graphics have not been tried. The checklist is
  `guide/prove.md`.
- Not tried with Sodium, Iris, shader packs or the rest of ATM10.
- No run on a real dedicated server. Camol's Shape Retaining collision probably does not apply there
  (`CAMOL_NOTES.md`).
- FTB Chunks claims: read in the jars, not tried. Spawn protection cannot be tried by the game tests.
- Left Alt shared with Create's wrench menu, and left Ctrl with sprint: read in the sources, not
  tried.
- Long lists scroll: the list on the main page above 14 states and the list of states to paste above
  11, at 427x240. No block tried has that many, so scrolling has not been seen.
- Moving camouflages with Create contraptions, Mekanism's configurator: not tried.
- The button paths on CurseForge, Modrinth and GitHub written in the guides.

## To do

- Plotinium plays the checklist, then publishes (`guide/pubblicazione.md`).
- The proposal to Camol is written and not sent (`guide/proposta.md`).
- Later, not in this version: blocks drawn by a block entity renderer as camouflages (D-14).
