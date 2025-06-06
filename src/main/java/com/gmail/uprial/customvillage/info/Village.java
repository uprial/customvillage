package com.gmail.uprial.customvillage.info;

// https://minecraft.gamepedia.com/Cat

import org.bukkit.block.Block;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Villager;

import java.util.ArrayList;
import java.util.List;

class Village {

    // https://minecraft.wiki/w/Village
    // Cats spawn naturally inside villages, one for every four beds for a maximum of five.
    private static final int MAX_CATS = 5;
    private static final int VILLAGERS_PER_CAT = 4;
    // https://minecraft.gamepedia.com/Iron_Golem
    // When gossiping, 5 or more participants are needed
    // When panicking, 3 or more are needed
    private static final int VILLAGERS_PER_GOLEM = 3;

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

    // Must be MUTABLE List because can be optimized.
    List<Villager> getVillagers() {
        return villagers;
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

    // Must be MUTABLE List because can be optimized.
    List<IronGolem> getNaturalIronGolems() {
        return naturalIronGolems;
    }

    List<IronGolem> getUserIronGolems() {
        return userIronGolems;
    }

    List<IronGolem> getAllIronGolems() {
        return allIronGolems;
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

    // Must be MUTABLE List because can be optimized.
    List<Cat> getNaturalCats() {
        return naturalCats;
    }

    List<Cat> getUserCats() {
        return userCats;
    }

    List<Cat> getAllCats() {
        return allCats;
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

    List<Block> getBedHeads() {
        return bedHeads;
    }

    void addAllBedHeads(final List<Block> newBedHeads) {
        bedHeads.addAll(newBedHeads);
    }

    // ==== PRIVATE METHODS ====

    private static boolean isUser(final IronGolem ironGolem) {
        return ironGolem.isPlayerCreated();
    }

    private static boolean isUser(final Cat cat) {
        return cat.isTamed();
    }

    // ==== COMMON METHODS ====

    @Override
    public String toString() {
        return String.format("Village{villagers: %d, bed-heads: %d, " +
                "iron-golems{natural: %d, user: %d, all: %d}, " +
                       "cats{natural: %d, user: %d, all: %d}}",
                villagers.size(), bedHeads.size(),
                naturalIronGolems.size(), userIronGolems.size(), allIronGolems.size(),
                naturalCats.size(), userCats.size(), allCats.size());
    }
}