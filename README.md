![CustomVillage Logo](images/customvillage-logo.png)

## Compatibility

Tested on Spigot-1.14, 1.15, 1.16.

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

## Commands

* `customvillage reload` - reload config from disk
* `customvillage info [villagers|golems|cats|beds;default=villagers] [@scale;default=8]` - show information
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
* CustomNukes: [Bukkit Dev](http://dev.bukkit.org/bukkit-plugins/customnukes/), [GitHub](https://github.com/uprial/customnukes), [Spigot](https://www.spigotmc.org/resources/customnukes.68710/)
* CustomCreatures: [Bukkit Dev](http://dev.bukkit.org/bukkit-plugins/customcreatures/), [GitHub](https://github.com/uprial/customcreatures), [Spigot](https://www.spigotmc.org/resources/customcreatures.68711/)
* CustomDamage: [Bukkit Dev](http://dev.bukkit.org/bukkit-plugins/customdamage/), [GitHub](https://github.com/uprial/customdamage), [Spigot](https://www.spigotmc.org/resources/customdamage.68712/)
* TakeAim: [Bukkit Dev](https://dev.bukkit.org/projects/takeaim), [GitHub](https://github.com/uprial/takeaim), [Spigot](https://www.spigotmc.org/resources/takeaim.68713/)
