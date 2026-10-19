# MoodialEP

Client-side Fabric mod (Minecraft 1.21.11) that highlights the blocks you
choose through walls. It is the **BlockESP module of
[Meteor Client](https://github.com/MeteorDevelopment/meteor-client) (GPL-3.0)
as a standalone mod**, with its own settings window instead of Meteor's GUI.

## Where the code comes from

**From Meteor Client** (`esp/`, `render/`, `util/UnorderedArrayList`, `mixin/LevelChunkMixin`)
- `BlockESP` - the module: worker thread search, chunk and group bookkeeping,
  dimension change check, block update handling, render loop
- `ESPChunk`, `ESPBlock`, `ESPGroup` - chunk search (heightmap based), neighbour
  bits, merging/splitting of block groups, the line/side culling between touching
  blocks, tracers to the center of each group
- `ESPBlockData` - style and colors per block (without the Meteor settings/GUI part)
- `Renderer3D` - same drawing API as Meteor's (`line`, `quad`, `quadVertical`,
  `quadHorizontal`, `box`...), collects lines and quads each frame and draws them
  together after the world, lines first
- `LevelChunkMixin` - Meteor's block update hook

**From the previous MoodialEP project** (settings window and resources)
- `MoodialEpBlockPickerScreen`, `MoodialEpSettingsScreen`, `MoodialEpConfig`,
  `MoodialEpPresets`, `MoodialEpModMenuApi`, `MoodialEpHud`
- everything in `src/main/resources` (lang files, icon, shader, access widener)
- the Gradle project and the 1.21.11 render types/pipelines

## What had to be adapted

Meteor's current source targets a newer Minecraft whose rendering API
(`com.mojang.renderpearl...`) does not exist in 1.21.11, so Meteor's own
`MeshBuilder`/`MeshRenderer`/pipelines/shaders cannot be used here. `Renderer3D`
keeps Meteor's API and geometry, and only the last step (writing the vertices) uses
the 1.21.11 buffers and pipelines of this project. Other small changes:

- Meteor's event bus is replaced by Fabric events and mixins (chunk load, tick,
  world change, block update, render)
- Meteor's `ChunkPos.pack()` / `.x()` / `SectionPos.posToSectionCoord` (newer
  names) are written the 1.21.11 way
- the block list and per-block colors come from the settings window
  (`MoodialEpResolvedConfig`) instead of Meteor's settings system
- quads are drawn with culling off, like Meteor's `WORLD_COLORED` pipeline
- `ESPBlock.render` reads the block shape at its own position (Meteor reused a
  shared position that the worker thread also writes)
- `ESPChunk.searchChunk` first checks the palette of each 16x16x16 section and
  skips the ones that cannot contain the wanted blocks (same result, far less work)
- while the ESP is on, `latest.log` gets a `[perf]` line every 10 seconds (draw time
  per frame, blocks, lines/quads, search thread time) to help find FPS problems

## Settings (Mod Menu > MoodialEP > Config)

**Block picker** (the main window) - search any block by name or ID, click it
to highlight it, click the colored square next to it to pick a color.
"Selected only" shows just what you have switched on. Common ores are grouped
(e.g. Coal = normal + deepslate).

**General settings** (button in the picker): on/off (can also be bound to a key
in Controls), highlight style (lines, sides or both), fill strength, tracers,
ESP badge next to the hotbar.

English and Italian translations are included.

## Build

`./gradlew build` -> `build/libs/MoodialEP-<version>.jar`
