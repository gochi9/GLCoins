package com.deadshotmdf.gLCoins_Server;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class BukkitMainThreadExecutor implements Executor {

    private final GLCoinsS main;

    public BukkitMainThreadExecutor(GLCoinsS main) {
        this.main = main;
    }

    @Override
    public void execute(@NotNull Runnable command) {
        Bukkit.getScheduler().runTask(main, command);
    }
}
