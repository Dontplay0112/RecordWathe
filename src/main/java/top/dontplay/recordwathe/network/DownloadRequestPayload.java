package top.dontplay.recordwathe.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DownloadRequestPayload(String matchId) implements CustomPacketPayload {
    public static final Type<DownloadRequestPayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath("wathe", "download_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DownloadRequestPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.matchId()),
            buf -> new DownloadRequestPayload(buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}