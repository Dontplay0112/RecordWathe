package top.dontplay.recordwathe.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class RecordNetworking {
    public static void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(DownloadResponsePayload.ID, DownloadResponsePayload.CODEC);
    }
}