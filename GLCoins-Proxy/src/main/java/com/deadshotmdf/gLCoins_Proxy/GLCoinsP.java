package com.deadshotmdf.gLCoins_Proxy;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

//Since the currency is shared across servers, this will be used by the site when a player buys GLCoins
@Plugin(id = "glcoins-proxy", name = "GLCoins-Proxy", version = "1.0.0" , authors = {"DeadshotMDF"})
public class GLCoinsP {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public GLCoinsP(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
    }

}
