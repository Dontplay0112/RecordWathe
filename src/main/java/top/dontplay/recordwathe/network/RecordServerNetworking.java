package top.dontplay.recordwathe.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import top.dontplay.recordwathe.core.GzipUtils;

import java.io.File;
import java.nio.file.Files;

public class RecordServerNetworking {
    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(DownloadRequestPayload.ID, (payload, context) -> {
            String matchId = payload.matchId();

            // 确保 IO 和逻辑操作在服务端主线程安全执行
            context.server().execute(() -> {
                try {
                    File exportDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "wathe_exports");
                    File targetFile = new File(exportDir, matchId + ".json");

                    if (targetFile.exists()) {
                        // 读取 JSON -> 压缩 -> 打包
                        String jsonContent = Files.readString(targetFile.toPath());
                        byte[] compressed = GzipUtils.compress(jsonContent);

                        DownloadResponsePayload response = new DownloadResponsePayload(matchId, compressed);

                        // 检查客户端是否装了该 Mod（能否接收该 Payload）
                        if (ServerPlayNetworking.canSend(context.player(), DownloadResponsePayload.ID)) {
                            ServerPlayNetworking.send(context.player(), response);
                        }
                    } else {
                        // 可选：向玩家发送文件不存在的提示
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }
}