package com.gmail.uprial.customvillage.listeners;

import com.gmail.uprial.customvillage.CustomVillage;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

public class CustomVillageBlocksListener implements Listener {
    private final CustomVillage plugin;

    public CustomVillageBlocksListener(CustomVillage plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPlace(BlockPlaceEvent event) {
        if(!event.isCancelled()) {
            plugin.onBlockChange(event.getBlock());
        }
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        if(!event.isCancelled()) {
            plugin.onBlockChange(event.getBlock());
        }
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        if(!event.isCancelled()) {
            List<Block> blocks = event.getBlocks();
            for(Block block : blocks) {
                maybeMoveBlock(block, event.getDirection());
            }
        }
    }
    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        if(!event.isCancelled() && event.isSticky()) {
            List<Block> blocks = event.getBlocks();
            for(Block block : blocks) {
                maybeMoveBlock(block, event.getDirection());
            }
        }
    }

    private void maybeMoveBlock(Block block, BlockFace direction) {
        plugin.onBlockChange(block);
        plugin.onBlockChange(getBlockInDirection(block, direction));
    }

    private static Block getBlockInDirection(Block block, BlockFace direction) {
        return block.getWorld().getBlockAt(block.getX() + direction.getModX(),
                                            block.getY() + direction.getModY(),
                                            block.getZ() + direction.getModZ());
    }
}
