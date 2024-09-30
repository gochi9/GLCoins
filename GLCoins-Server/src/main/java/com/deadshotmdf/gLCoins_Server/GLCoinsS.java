package com.deadshotmdf.gLCoins_Server;

import com.deadshotmdf.glccoinscommon.DatabaseTest;
import com.deadshotmdf.glccoinscommon.ModifyType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class GLCoinsS extends JavaPlugin implements CommandExecutor {

    private DatabaseTest test;

    @Override
    public void onEnable() {
        test = new DatabaseTest(new BukkitMainThreadExecutor(this), this.getLogger());
        this.getCommand("shit").setExecutor(this);
    }

    @Override
    public void onDisable(){
        test.close();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch(args[0].toLowerCase()) {
            case "add":
                test.modifyEntry(UUID.fromString(args[1]), Double.parseDouble(args[2]), ModifyType.SET, null);
                return true;
            case "bulk":
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    test.fillRandomEntries(Integer.parseInt(args[1]));
                });
                return true;
            case "retrive":
                test.getEntryPartialAsync(null, UUID.fromString(args[1])).thenAccept(value -> this.getLogger().info(args[1] + ": " + value));
                return true;
            case "modify":
                test.modifyEntry(UUID.fromString(args[1]), Double.parseDouble(args[2]), ModifyType.valueOf(args[3].toUpperCase()), null);
            default:
                return true;
        }
    }

}
