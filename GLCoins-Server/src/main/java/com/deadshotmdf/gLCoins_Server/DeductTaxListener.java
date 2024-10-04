package com.deadshotmdf.gLCoins_Server;

import com.deadshotmdf.gLCoins_Server.events.economy.PlayerDepositEvent;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.text.DecimalFormat;

public class DeductTaxListener implements Listener {

    private static final DecimalFormat df = new DecimalFormat("0.00");

    @EventHandler
    public void onDeductTax(PlayerDepositEvent ev) {
        if(!ev.isShouldDeduceTax())
            return;

        double totalValue = ev.getAmount();
        double taxedAmount = ev.getAmount() * (1 - 0.08);
        //8% tax
        ev.setAmount(taxedAmount);

        OfflinePlayer offlinePlayer = ev.getOfflinePlayer();

        if(offlinePlayer.isOnline())
            offlinePlayer.getPlayer().sendMessage(ChatColor.RED + "You have been taxed "
                    + ChatColor.GOLD + "8%"
                    + ChatColor.RED + " of your total "
                    + ChatColor.AQUA + String.format("%s", totalValue)
                    + ChatColor.RED + " ("
                    + ChatColor.GREEN + String.format("%s", getDigits(totalValue - taxedAmount))
                    + ChatColor.RED + ").");
    }

    public static String getDigits(double number) {
        String formattedNumber = df.format(number);

        return formattedNumber.endsWith(".00") ? formattedNumber.substring(0, formattedNumber.indexOf('.')) : formattedNumber;
    }

}
