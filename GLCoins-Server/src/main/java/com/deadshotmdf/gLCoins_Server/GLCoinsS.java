package com.deadshotmdf.gLCoins_Server;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public class GLCoinsS extends JavaPlugin {

    private HashMap<UUID, UUID> map = new HashMap<>();

    @Override
    public void onEnable() {
        // Plugin startup logic
        map.computeIfAbsent(UUID.randomUUID(), _ -> UUID.randomUUID());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
