package com.gmail.uprial.customvillage.info;

// https://minecraft.gamepedia.com/Cat

import org.bukkit.block.Block;
import org.bukkit.entity.Cat;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;

class Village {

    // https://minecraft.gamepedia.com/Cat
    private static final int MAX_CATS = 10;
    // https://minecraft.gamepedia.com/Iron_Golem
    private static final int VILLAGERS_PER_GOLEM = 4 * 2;

    final List<Villager> villagers = new ArrayList<>();
    final List<IronGolem> ironGolems = new ArrayList<>();
    final List<Cat> cats = new ArrayList<>();
    final List<Block> bedHeads = new ArrayList<>();

    Village() {
    }

    int getVillagersLimit() {
        return bedHeads.size();
    }

    int getCatsLimit() {
        return MAX_CATS;
    }

    int getIronGolemsLimit() {
        return Math.min(getVillagersLimit(), villagers.size()) / VILLAGERS_PER_GOLEM;
    }
}