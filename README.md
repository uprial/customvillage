![CustomVillage Logo](images/customvillage-logo.png)

## Compatibility

Tested on Spigot-1.14, 1.15, 1.16, 1.19, 1.20, 1.20.6, 1.21, 1.21.3.

## Introduction

A Minecraft (Bukkit) plugin that allows to detect and control custom villages.

This plugin takes care of the pain I experienced when I created my own village.
I created a big enough village, and its population took out of control.
Villagers seemed to grow infinitely. Cats were leaving the village and were limited in count in villages only.
Golems were obedient and followed the documented rules, but my god, they anyway followed the infinity of villagers.
I ended up with a mess of hundreds of illegal entities on the map. Fortunately, here I could fix the issue.

## Features

* Detect villages based on locations of villagers
* Calculate a number of beds in the villages and limit villagers population considering the beds
* Limit population of cats (not tamed) and iron golems (non-player created) strongly according to the documentation

## Limit mechanics.

All village-decisive entities: beds, golems, cats, villagers - are clustered in clusters of 32x12x32 size for X, Y, and Z coordinates.

All clusters in a square radius of 1 form one village.

For example, if there are four villagers in the following X, Y, and Z coordinates:

- Villager #1: 100, 50, 100 (cluster coordinates 3, 4, 3)
- Villager #2: 150, 50, 150 (cluster coordinates 4, 4, 4)
- Villager #3: 200, 50, 200 (cluster coordinates 6, 4, 6)
- Villager #4: 250, 50, 250 (cluster coordinates 7, 4, 7)

The #1 and #2 villagers are considered as the villagers of the same village. The #3 and #4 villagers belong to another village.

Then, each village as a set of clusters enforces its own limit of entities.

## Commands

* `customvillage reload` - reload config from disk
* `customvillage info info [@type : villagers(default) | golems | cats | beds] [@loaded : fully | partially(default) | no] [@scale; default=8] ` - show information
* `customvillage optimize` - removes excessive villagers, iron golems and cats

## Permissions

* Access to 'reload' command:
`customvillage.reload` (default: op)

* Access to 'info' command:
`customdamage.info` (default: op)

* Access to 'optimize' command:
`customdamage.optimize` (default: op)

## Configuration
[Default configuration file](src/main/resources/config.yml)

## Author
I will be happy to add some features or fix bugs. My mail: uprial@gmail.com.

## Useful links
* [Project on GitHub](https://github.com/uprial/customvillage/)
* [Project on Bukkit Dev](http://dev.bukkit.org/bukkit-plugins/customvillage/)
* [Project on Spigot](https://www.spigotmc.org/resources/customvillage.69170/)
* [TODO list](TODO.md)

## Related projects
* CustomCreatures: [Bukkit Dev](http://dev.bukkit.org/bukkit-plugins/customcreatures/), [GitHub](https://github.com/uprial/customcreatures), [Spigot](https://www.spigotmc.org/resources/customcreatures.68711/)
* CustomNukes: [Bukkit Dev](http://dev.bukkit.org/bukkit-plugins/customnukes/), [GitHub](https://github.com/uprial/customnukes), [Spigot](https://www.spigotmc.org/resources/customnukes.68710/)
* CustomRecipes: [Bukkit Dev](https://dev.bukkit.org/projects/custom-recipes), [GitHub](https://github.com/uprial/customrecipes/), [Spigot](https://www.spigotmc.org/resources/customrecipes.89435/)
* TakeAim: [Bukkit Dev](https://dev.bukkit.org/projects/takeaim), [GitHub](https://github.com/uprial/takeaim), [Spigot](https://www.spigotmc.org/resources/takeaim.68713/)
