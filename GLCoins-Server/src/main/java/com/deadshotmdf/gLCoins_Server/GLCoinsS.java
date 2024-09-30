package com.deadshotmdf.gLCoins_Server;

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
        test = new DatabaseTest("jdbc:mysql://0.0.0.0:3306/testing", "root", "glcpasswordfortesting");
        this.getCommand("shit").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch(args[0].toLowerCase()) {
            case "add":
                test.addEntry(UUID.fromString(args[1]), Double.parseDouble(args[2]), true);
                return true;
            case "bulk":
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    test.fillRandomEntries(Integer.parseInt(args[1]));
                });
                return true;
            case "retrive":
                this.getLogger().info(""+test.getEntry(UUID.fromString(args[1])));
                return true;
            default:
                return true;
        }
    }

}
