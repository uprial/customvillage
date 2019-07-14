package com.gmail.uprial.customvillage;

import com.gmail.uprial.customvillage.common.CustomLogger;
import com.gmail.uprial.customvillage.info.VillageInfoType;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

class CustomVillageCommandExecutor implements CommandExecutor {
    static final String COMMAND_NS = "customvillage";

    private static final int DEFAULT_SCALE = 8;
    private static final VillageInfoType DEFAULT_INFO_TYPE = VillageInfoType.VILLAGERS;

    private final CustomVillage plugin;

    CustomVillageCommandExecutor(CustomVillage plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase(COMMAND_NS)) {
            CustomLogger customLogger = new CustomLogger(plugin.getLogger(), sender);

            if((args.length >= 1) && (args[0].equalsIgnoreCase("reload"))) {
                if (sender.hasPermission(COMMAND_NS + ".reload")) {
                    plugin.reloadConfig(customLogger);
                    customLogger.info("CustomVillage config reloaded.");
                    return true;
                }
            }
            else if((args.length >= 1) && (args[0].equalsIgnoreCase("info"))) {
                if (sender.hasPermission(COMMAND_NS + ".info")) {
                    VillageInfoType infoType;
                    if (args.length >= 2) {
                        infoType = VillageInfoType.valueOf(args[1].toUpperCase());
                    } else {
                        infoType = DEFAULT_INFO_TYPE;
                    }
                    Integer scale;
                    if (args.length >= 3) {
                        scale = Integer.valueOf(args[2]);
                    } else {
                        scale = DEFAULT_SCALE;
                    }
                    customLogger.info("\n" + StringUtils.join(plugin.getVillageInfoTextLines(infoType, scale), "\n"));
                    return true;
                }
            }
            else if((args.length >= 1) && (args[0].equalsIgnoreCase("optimize"))) {
                if (sender.hasPermission(COMMAND_NS + ".optimize")) {
                    plugin.optimize();
                    return true;
                }
            }
            else if((args.length == 0) || (args[0].equalsIgnoreCase("help"))) {
                String helpString = "==== CustomVillage help ====\n";

                if (sender.hasPermission(COMMAND_NS + ".reload")) {
                    helpString += '/' + COMMAND_NS + " reload - reload config from disk\n";
                }
                if (sender.hasPermission(COMMAND_NS + ".info")) {
                    helpString += '/' + COMMAND_NS + " info [villagers|golems|cats|beds;default=villagers] [@scale;default=8] - show information\n";
                }
                if (sender.hasPermission(COMMAND_NS + ".optimize")) {
                    helpString += '/' + COMMAND_NS + " optimize - removes excessive villagers, iron golems and cats\n";
                }

                customLogger.info(helpString);
                return true;
            }
        }
        return false;
    }
}
