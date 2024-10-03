package com.deadshotmdf.gLCoins_Server.events.economy;

import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Optional;

/**
 * Event that is fired when Economy#depositPlayer(OfflinePlayer, double) is called.
 *
 * @author Rsl1122
 */
public class PlayerDepositEvent extends Event {

    private final OfflinePlayer player;
    private double amount;
    private final String world;
    private final boolean shouldDeduceTax;

    public PlayerDepositEvent(OfflinePlayer player, double amount, boolean shouldDeduceTax) {
        this(player, amount, null, shouldDeduceTax);
    }

    public PlayerDepositEvent(OfflinePlayer player, double amount, String world, boolean shouldDeduceTax) {
        super(!Bukkit.isPrimaryThread());
        this.player = player;
        this.amount = amount;
        this.world = world;
        this.shouldDeduceTax = shouldDeduceTax;
    }

    public OfflinePlayer getOfflinePlayer() {
        return player;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public boolean isShouldDeduceTax() {
        return shouldDeduceTax;
    }

    public Optional<String> getWorldName() {
        return Optional.ofNullable(world);
    }

    private static final HandlerList HANDLERS = new HandlerList();

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}