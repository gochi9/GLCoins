package com.deadshotmdf.gLCoins_Server;

import com.deadshotmdf.gLCoins_Server.events.economy.PlayerDepositEvent;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DeductTaxListener implements Listener {

    @EventHandler
    public void onDeductTax(PlayerDepositEvent ev) {
        if(!ev.isShouldDeduceTax())
            return;

        double totalValue = ev.getAmount();
        double taxedAmount = ev.getAmount() * (1 - 0.15);
        //15% tax
        ev.setAmount(taxedAmount);

        OfflinePlayer offlinePlayer = ev.getOfflinePlayer();

        if(offlinePlayer.isOnline())
            offlinePlayer.getPlayer().sendMessage(ChatColor.RED + "You have been taxed "
                    + ChatColor.GOLD + "15%"
                    + ChatColor.RED + " of your total "
                    + ChatColor.AQUA + String.format("%s", totalValue)
                    + ChatColor.RED + " ("
                    + ChatColor.GREEN + String.format("%s", totalValue - taxedAmount)
                    + ChatColor.RED + ").");
    }

}
