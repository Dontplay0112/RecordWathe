package top.dontplay.recordwathe.network;

import top.dontplay.recordwathe.core.WatheConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WatheUploader {

    // 🌟 修复协议冲突：强制指定使用 HTTP_1_1
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1) // <--- 就是加这一行
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static void uploadMatchData(String gameJsonData) {
        String backendUrl = WatheConfig.INSTANCE.backendUrl;

        if (backendUrl == null || backendUrl.isEmpty()) {
            System.out.println("WatheUploader: 后端地址未配置，跳过上传。");
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(backendUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gameJsonData))
                .build();

        // 异步发送
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        System.out.println("Wathe 对局数据成功上传至 Python 后端！");
                    } else {
                        System.out.println("上传失败，状态码：" + response.statusCode() + " 返回内容: " + response.body());
                    }
                })
                .exceptionally(e -> {
                    System.out.println("连接 Python 后端失败 (请检查后端是否开启): " + e.getMessage());
                    return null;
                });
    }
}