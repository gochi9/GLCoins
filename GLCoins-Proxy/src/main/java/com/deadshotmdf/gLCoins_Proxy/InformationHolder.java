package com.deadshotmdf.gLCoins_Proxy;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InformationHolder {

    private final Map<UUID, Double> wallets;

    public InformationHolder() {
        this.wallets = new ConcurrentHashMap<>();

        //Implement database loading here
    }

}
