# Village Chief Minecraft Mod

Village Chief is a Minecraft mod for Forge 1.14.3 that lets you lead a settlement from its first night, manage shared village storage, support Builder, Farmer, and Guard villagers, and expand the village with resource-funded huts.

## Install

1. Install Minecraft Java Edition 1.14.3.
2. Install Minecraft Forge 27.0.60 or another compatible Forge 1.14.3 build.
3. Build the mod with `./gradlew build`, or obtain the compiled `villagechief` jar.
4. Copy the jar from `build/libs` into the Minecraft `mods` folder.
5. Start Minecraft with the Forge profile.

Java 8 is required to build the project.

## Use

1. Start a world and place a chest where you want the settlement center.
2. Stand within six blocks of the chest.
3. Run `/chief found`.
4. The mod marks the chest as shared village storage and spawns named Builder, Farmer, and Guard villagers.
5. Put food in the chest. The village accepts bread, wheat, carrots, and potatoes. Food is consumed approximately once every 1,200 world ticks.
6. Put 16 oak planks and 8 cobblestone in the chest for each hut.
7. Run `/chief status` to view the resident count, completed hut count, and storage coordinates.
8. Run `/chief expand` to construct a hut immediately when the shared chest contains the required materials.

The Farmer searches for mature wheat, carrot, and potato crops near the village center. When it reaches a mature crop, it harvests the crop and places produce in the shared chest. Wheat also produces wheat seeds. The farmer does not plant crops.

The Builder moves toward the next construction site and helps trigger construction when materials are available. The mod also checks periodically for automatic expansion. Each hut has a cobblestone floor, oak-plank walls, and an oak-plank roof. The village can build up to eight huts.

The Guard is spawned and named as a founding villager, but the current version does not add special guard combat or patrol behavior. Villagers do not independently retrieve food or gather building materials. The player supplies the shared chest.

Commands:

- `/chief found` - Found a village at a nearby chest.
- `/chief status` - Display village status.
- `/chief expand` - Build the next hut if supplies are available.

---

More Minecraft mod projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/868cf838-d325-4e00-993a-eb736f72fe56?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=868cf838-d325-4e00-993a-eb736f72fe56
