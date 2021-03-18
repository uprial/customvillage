package com.gmail.uprial.customvillage.info;

// https://minecraft.gamepedia.com/Cat

import com.google.common.collect.ImmutableList;
import org.bukkit.block.Block;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;

class Village {

    // https://minecraft.gamepedia.com/Cat
    // One cat spawns for every four valid beds, with a maximum of 10 cats.
    private static final int MAX_CATS = 10;
    private static final int VILLAGERS_PER_CAT = 4;
    // https://minecraft.gamepedia.com/Iron_Golem
    // One iron golem can now spawn for every 10 villagers in a village.
    private static final int VILLAGERS_PER_GOLEM = 10;

    private final List<Villager> villagers = new ArrayList<>();
    private final List<IronGolem> naturalIronGolems = new ArrayList<>();
    private final List<IronGolem> userIronGolems = new ArrayList<>();
    private final List<IronGolem> allIronGolems = new ArrayList<>();
    private final List<Cat> naturalCats = new ArrayList<>();
    private final List<Cat> userCats = new ArrayList<>();
    private final List<Cat> allCats = new ArrayList<>();
    private final List<Block> bedHeads = new ArrayList<>();

    Village() {
    }

    static boolean isUserEntity(final Entity entity) {
        if(entity instanceof IronGolem) {
            return isUser((IronGolem)entity);
        } else if (entity instanceof Cat) {
            return isUser((Cat)entity);
        } else {
            return false;
        }
    }

        // ==== VILLAGERS ====

    ImmutableList<Villager> getVillagers() {
        return ImmutableList.copyOf(villagers);
    }

    int getVillagersLimit() {
        return bedHeads.size();
    }

    void addVillager(final Villager villager) {
        villagers.add(villager);
    }

    void addAllVillagers(final List<Villager> newVillagers) {
        villagers.addAll(newVillagers);
    }

    // ==== IRON GOLEMS ====

    ImmutableList<IronGolem> getNaturalIronGolems() {
        return ImmutableList.copyOf(naturalIronGolems);
    }

    ImmutableList<IronGolem> getUserIronGolems() {
        return ImmutableList.copyOf(userIronGolems);
    }

    ImmutableList<IronGolem> getAllIronGolems() {
        return ImmutableList.copyOf(allIronGolems);
    }

    int getNaturalIronGolemsLimit() {
        return Math.min(getVillagersLimit(), villagers.size()) / VILLAGERS_PER_GOLEM;
    }

    void addIronGolem(final IronGolem ironGolem) {
        if(isUser(ironGolem)) {
            userIronGolems.add(ironGolem);
        } else {
            naturalIronGolems.add(ironGolem);
        }
        allIronGolems.add(ironGolem);
    }

    void addAllIronGolems(final List<IronGolem> ironGolems) {
        for(IronGolem ironGolem : ironGolems) {
            addIronGolem(ironGolem);
        }
    }

    // ==== CATS ====

    ImmutableList<Cat> getNaturalCats() {
        return ImmutableList.copyOf(naturalCats);
    }

    ImmutableList<Cat> getUserCats() {
        return ImmutableList.copyOf(userCats);
    }

    ImmutableList<Cat> getAllCats() {
        return ImmutableList.copyOf(allCats);
    }

    int getNaturalCatsLimit() {
        return Math.min(MAX_CATS, Math.min(getVillagersLimit(), villagers.size()) / VILLAGERS_PER_CAT);
    }

    void addCat(final Cat cat) {
        if(isUser(cat)) {
            userCats.add(cat);
        } else {
            naturalCats.add(cat);
        }
        allCats.add(cat);
    }

    void addAllCats(final List<Cat> cats) {
        for (Cat cat : cats) {
            addCat(cat);
        }
    }

    // ==== BEDS ====

    ImmutableList<Block> getBedHeads() {
        return ImmutableList.copyOf(bedHeads);
    }

    void addAllBedHeads(final List<Block> newnBedHeads) {
        bedHeads.addAll(newnBedHeads);
    }

    // ==== PRIVATE METHODS ====

    private static boolean isUser(final IronGolem ironGolem) {
        return ironGolem.isPlayerCreated();
    }

    private static boolean isUser(final Cat cat) {
        return cat.isTamed();
    }
}