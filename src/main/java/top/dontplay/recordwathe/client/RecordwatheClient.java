package top.dontplay.recordwathe.client;

import net.fabricmc.api.ClientModInitializer;
import top.dontplay.recordwathe.network.RecordClientNetworking;

public class RecordwatheClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        RecordClientNetworking.register();

    }
}
