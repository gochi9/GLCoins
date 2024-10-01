package com.deadshotmdf.gLCoins_Server;

import com.deadshotmdf.glccoinscommon.CoinDatabase;
import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public class GLCoinsS extends JavaPlugin implements CommandExecutor {

    private static CoinDatabase database;

    @Override
    public void onEnable() {
        database = new CoinDatabase(new BukkitMainThreadExecutor(this), this.getLogger());
    }

    @Override
    public void onDisable(){
        database.close();
    }

    public static CoinDatabase getDatabase(){
        return database;
    }

}
