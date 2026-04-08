package top.dontplay.recordwathe.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import top.dontplay.recordwathe.core.GzipUtils;

import java.io.File;
import java.io.FileWriter;

public class RecordClientNetworking {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(DownloadResponsePayload.ID, (payload, context) -> {
            String matchId = payload.matchId();
            byte[] compressedData = payload.compressedData();

            context.client().execute(() -> {
                try {
                    // 解压 -> 存入客户端的专属下载目录
                    String jsonContent = GzipUtils.decompress(compressedData);

                    File downloadDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "wathe_downloads");
                    if (!downloadDir.exists()) downloadDir.mkdirs();
                    File outputFile = new File(downloadDir, matchId + ".json");

                    try (FileWriter writer = new FileWriter(outputFile)) {
                        writer.write(jsonContent);
                    }

                    if (context.player() != null) {
                        context.player().displayClientMessage(
                                Component.literal("[系统] 对局记录已成功下载至: " + outputFile.getName()).withStyle(ChatFormatting.GREEN),
                                false
                        );
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (context.player() != null) {
                        context.player().displayClientMessage(
                                Component.literal("[错误] 对局记录下载失败，请查看客户端日志。").withStyle(ChatFormatting.RED),
                                false
                        );
                    }
                }
            });
        });
    }

    // 提供给客户端其他地方调用的快捷方法
    public static void requestDownload(String matchId) {
        if (ClientPlayNetworking.canSend(DownloadRequestPayload.ID)) {
            ClientPlayNetworking.send(new DownloadRequestPayload(matchId));
        }
    }
}