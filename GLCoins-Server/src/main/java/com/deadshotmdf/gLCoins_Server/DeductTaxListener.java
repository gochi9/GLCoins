package com.deadshotmdf.gLCoins_Server;

import com.deadshotmdf.GLC_GUIS.Mayor.Enums.UpgradeType;
import com.deadshotmdf.GLC_GUIS.Mayor.MayorManager;
import com.deadshotmdf.gLCoins_Server.events.economy.PlayerDepositEvent;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.text.DecimalFormat;
import java.util.UUID;

public class DeductTaxListener implements Listener {

    private final GLCoinsS main;
    private static final DecimalFormat df = new DecimalFormat("0.00");

    public DeductTaxListener(GLCoinsS main) {
        this.main = main;
    }

    @EventHandler
    public void onDeductTax(PlayerDepositEvent ev) {
        if(!ev.isShouldDeduceTax())
            return;

        OfflinePlayer offlinePlayer = ev.getOfflinePlayer();
        UUID uuid = offlinePlayer.getUniqueId();
        MayorManager mayorManager = main.getMayorManager();
        double tax = mayorManager == null ? 0.08 : mayorManager.getPlayerUpgrade(uuid, UpgradeType.TAX) < 1 ? 0.08 : mayorManager.getUpgradeBenefit(uuid, UpgradeType.TAX) / 100;
        double totalValue = ev.getAmount();
        double taxedAmount = ev.getAmount() * (1 - Math.max(tax, 0.000));

        if(taxedAmount == 0.000)
            return;

        ev.setAmount(taxedAmount);

        if(offlinePlayer.isOnline())
            offlinePlayer.getPlayer().sendMessage(ChatColor.RED + "You have been taxed "
                    + ChatColor.GOLD + tax * 100
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
