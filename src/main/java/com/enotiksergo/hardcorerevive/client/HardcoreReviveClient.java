package com.enotiksergo.hardcorerevive.client;

import com.enotiksergo.hardcorerevive.net.ReviveNetworking;
import net.fabricmc.api.ClientModInitializer;

public class HardcoreReviveClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ReviveNetworking.registerClientReceivers();
        ClientTerrainWaiter.register();
    }
}