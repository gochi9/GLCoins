package com.deadshotmdf.gLCoins_Server;

import com.deadshotmdf.gLCoins_Server.events.economy.EconomyEventsAvailableEvent;
import com.deadshotmdf.glccoinscommon.CoinDatabase;
import com.deadshotmdf.glccoinscommon.ModifyType;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class GLCoinsS extends JavaPlugin implements CommandExecutor {

    private static CoinDatabase database;

    @Override
    public void onEnable() {
        database = new CoinDatabase(new BukkitMainThreadExecutor(this), this.getLogger());

        Logger logger = getLogger();

        PluginManager plugins = getServer().getPluginManager();
        if (!plugins.isPluginEnabled("Vault")) {
            logger.warning("Vault is required for Vault Events -> Plugin Disabled.");
            return;
        }

        Optional<Economy> economy = getEconomy();
        if (economy.isPresent())
            registerWrapper(economy.get());
        else
            tryAgainOnStart();

        Bukkit.getPluginManager().registerEvents(new DeductTaxListener(), this);
        this.getCommand("idk").setExecutor(this);
    }

    @Override
    public void onDisable(){
        database.close();
    }

    public static CoinDatabase getDatabase(){
        return database;
    }

    //Temporary command used until the proxy part is done
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if(sender instanceof Player)
            return true;

        switch(args[0].toLowerCase()) {
            case "add":
                database.modifyEntry(UUID.fromString(args[1]), Double.parseDouble(args[2]), ModifyType.ADD, null);
                return true;
            case "bulk":
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    database.fillRandomEntries(Integer.parseInt(args[1]));
                });
                return true;
            case "retrive":
                database.getEntryAsync(null, UUID.fromString(args[1]), null).thenAccept(value -> this.getLogger().info(args[1] + ": " + value));
                return true;
            default:
                return true;
        }
    }

    private void tryAgainOnStart() {
        getServer().getScheduler().runTaskLater(this,
                () -> getEconomy().ifPresent(this::registerWrapper),
                0); // Run when server has done loading
    }

    private Optional<Economy> getEconomy() {
        ServicesManager services = getServer().getServicesManager();
        RegisteredServiceProvider<Economy> economyService = services.getRegistration(Economy.class);
        if (economyService == null) return Optional.empty();
        Economy economy = economyService.getProvider();
        return Optional.ofNullable(economy);
    }

    private void registerWrapper(Economy original) {
        // Don't wrap Economy twice in case of reloads.
        Economy wrappedEco = original instanceof EconomyWrapper ? original : new EconomyWrapper(original);
        getServer().getServicesManager().register(Economy.class, wrappedEco, this, ServicePriority.Highest);
        getLogger().info("Vault Events registered - Events can now be listened to.");
        Bukkit.getPluginManager().callEvent(new EconomyEventsAvailableEvent(wrappedEco));
    }

}
