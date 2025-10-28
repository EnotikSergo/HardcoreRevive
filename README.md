# HardcoreRevive

A mod that lets you revive in a Hardcore world - but you have to pay the price.

![hert_converting)](https://github.com/user-attachments/assets/996b128e-b523-460c-ae14-1e71356c8883)

## Features

- After death, teleports you to a random location within a radius of 10,000,000 by default (configurable).
- Clears items dropped on death and your Ender Chest.
- Converts the world from Hardcore to regular Survival, with a smooth heart-transition animation.
- To return the world spawnpoint, you need to set the bonfire at coordinates 0 0.
- Command `/clearcontainer` - clears all storages (chests, dispensers, hoppers, etc.), including storage-like holders such as item frames and armor stands, within the radius of loaded chunks.
- Command `/clearregion <bossbar on/off> <worlds all/current>` - clears all storages across the entire loaded map, except in regions that have chunk loaders (ender pearls don’t count since they disappear on death). If enabled, you can track the cleanup progress in the boss bar. IMPORTANT: make a backup before running, as it works directly with the world’s region files. Afterwards, run `/clearregion confirm` to allow the cleanup in this world.
- ModMenu for Settings (If you need certain containers not to be deleted-for example, because they’re used in decorations or there are keepsake items in an item frame)
<img width="500" height="400" alt="image" src="https://github.com/user-attachments/assets/4a0cefe1-e50e-4cc5-aad8-0b4de575aa2c" />


## Credits

- [Wildez](https://youtu.be/da7AiGInNns?si=PrmdQnTXdZNl7wKG&t=562) for the idea/video concept

- [NikiWright](https://www.youtube.com/@NIKIw) for inspiring the mod, videos, and streams
