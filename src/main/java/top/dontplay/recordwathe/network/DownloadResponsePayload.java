package top.dontplay.recordwathe.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record DownloadResponsePayload(String matchId, byte[] compressedData) implements CustomPacketPayload {
    public static final Type<DownloadResponsePayload> ID = new Type<>(ResourceLocation.fromNamespaceAndPath("recordwathe", "download_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DownloadResponsePayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.matchId());
                buf.writeByteArray(payload.compressedData()); // 原生支持最多 16MB 的字节数组写入，非常安全
            },
            buf -> new DownloadResponsePayload(buf.readUtf(), buf.readByteArray())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}