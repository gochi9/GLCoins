package com.deadshotmdf.gLCoins_Proxy;

import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InformationHolder {

    private final Logger logger;
    private final Map<UUID, Double> wallets;

    public InformationHolder(Logger logger) {
        this.logger = logger;
        this.wallets = new ConcurrentHashMap<>();

        //Implement database loading here
    }

    public void modifyWallet(UUID uuid, Double amount, ModifyType modifyType) {
        if(uuid == null){
            logger.warn("Received command to modify wallte, but UUID was null. Skipping action.");
            return;
        }

        if(amount == null || modifyType == null){
            logger.warn("Tried to modify wallet for {}, but the amount {} or/and the modify type {} is/are invalid", uuid, amount != null ? amount : "NULL", modifyType != null ? modifyType : "NULL");
            return;
        }

        double balance = wallets.computeIfAbsent(uuid, _ -> 0.0);
        switch (modifyType) {
            case ADD -> balance += amount;
            case REMOVE -> balance -= amount;
            case SET -> balance = amount;
        }

        wallets.put(uuid, Math.max(balance, 0.0));
    }

}
