package com.deadshotmdf.gLCoins_Server;

import com.deadshotmdf.gLCoins_Server.events.economy.PlayerDepositEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class DeductTaxListener implements Listener {

    @EventHandler
    public void onDeductTax(PlayerDepositEvent ev) {
        if(!ev.isShouldDeduceTax())
            return;

        //15% tax
        ev.setAmount(ev.getAmount() * (1 - 0.15));
    }

}
