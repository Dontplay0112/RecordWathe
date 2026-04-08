package top.dontplay.recordwathe.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import dev.doctor4t.wathe.record.replay.ReplayGenerator;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import top.dontplay.recordwathe.network.WatheUploader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

public class ReplayExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void exportMatch(GameRecordManager.MatchRecord match) {
        File exportDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "wathe_exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }

        // 新代码：将毫秒时间戳转换为带时区的日期时间字符串
        // 注意：Windows/Linux 文件名不能包含冒号(:)，所以我们用短横线(-)分隔时分秒
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                .withZone(ZoneId.systemDefault());
        String timeString = formatter.format(Instant.ofEpochMilli(match.getStartMs()));

        // 使用时间字符串作为文件名 (例如: 2026-03-29_11-35-06.json)
        File outputFile = new File(exportDir, timeString + ".json");
        JsonObject rootJson = new JsonObject();

        // 1. 基础对局信息
        rootJson.addProperty("matchId", match.getMatchId().toString());
        rootJson.addProperty("dimension", match.getDimensionId().toString());
        rootJson.addProperty("gameMode", match.getGameModeId().toString());
        rootJson.addProperty("startTick", match.getStartTick());
        rootJson.addProperty("startMs", match.getStartMs());

        // 2. 导出玩家信息映射表 (处理颜色为 #RRGGBB)
        JsonObject playersJson = new JsonObject();
        Map<UUID, ReplayGenerator.PlayerInfo> playerInfoCache = ReplayGenerator.getPlayerInfoCache(match);

        for (Map.Entry<UUID, ReplayGenerator.PlayerInfo> entry : playerInfoCache.entrySet()) {
            JsonObject playerObj = new JsonObject();
            playerObj.addProperty("name", entry.getValue().name());
            playerObj.addProperty("roleTranslationKey", entry.getValue().roleTranslationKey());

            // 将 Java 有符号整数颜色转换为标准的十六进制颜色码，屏蔽掉高位的负数补码
            String hexColor = String.format("#%06X", (0xFFFFFF & entry.getValue().roleColor()));
            playerObj.addProperty("roleColor", hexColor);

            playersJson.add(entry.getKey().toString(), playerObj);
        }
        rootJson.add("players", playersJson);

        // 3. 导出完整的事件流 (结构化解析 NBT)
        JsonArray eventsArray = new JsonArray();
        for (GameRecordEvent event : match.getEvents()) {
            JsonObject eventJson = new JsonObject();
            eventJson.addProperty("seq", event.seq());
            eventJson.addProperty("type", event.type());
            eventJson.addProperty("worldTick", event.worldTick());
            eventJson.addProperty("realTimeMs", event.realTimeMs());

            // 如果 data 存在且不为空，进行深度 JSON 转换
            if (event.data() != null && !event.data().isEmpty()) {
                eventJson.add("data", convertNbtToJson(event.data(), playerInfoCache));
            }

            eventsArray.add(eventJson);
        }
        rootJson.add("events", eventsArray);

        // 1. 将组装好的 JsonObject 转换为 JSON 字符串
        String gameJsonData = GSON.toJson(rootJson);

        // 2. 写入本地文件 (保留原有逻辑，作为本地数据备份)
        try (FileWriter writer = new FileWriter(outputFile)) {
            // 直接写入我们刚刚生成的字符串即可
            writer.write(gameJsonData);
            System.out.println("成功导出结构化对局记录: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("导出对局记录失败: " + e.getMessage());
            e.printStackTrace();
        }

        // 3. 触发异步上传至 Python 后端
        System.out.println("正在将对局数据上传至服务器...");
        WatheUploader.uploadMatchData(gameJsonData);
    }

    /**
     * 将 Minecraft 的 NBT 标签递归转换为标准的 Gson JsonElement
     */
    private static JsonElement convertNbtToJson(Tag tag, Map<UUID, ReplayGenerator.PlayerInfo> playerInfoCache) {
        if (tag == null) return JsonNull.INSTANCE;

        byte type = tag.getId();
        if (type == Tag.TAG_COMPOUND) {
            JsonObject obj = new JsonObject();
            CompoundTag compound = (CompoundTag) tag;
            for (String key : compound.getAllKeys()) {

                // 拦截标准 UUID 数组 (如 actor, target)
                if (compound.hasUUID(key)) {
                    UUID uuid = compound.getUUID(key);
                    ReplayGenerator.PlayerInfo info = playerInfoCache.get(uuid);

                    // 调整1：如果是明确的 "uuid" 字段，保留原始 UUID 字符串，不要翻译
                    if (key.equals("uuid") || info == null) {
                        obj.addProperty(key, uuid.toString());
                    } else {
                        // 命中缓存，写入玩家名字
                        obj.addProperty(key, info.name());
                    }
                } else {
                    // 递归向下解析
                    obj.add(key, convertNbtToJson(compound.get(key), playerInfoCache));
                }
            }
            return obj;
        } else if (type == Tag.TAG_LIST) {
            JsonArray arr = new JsonArray();
            ListTag list = (ListTag) tag;
            for (int i = 0; i < list.size(); i++) {
                arr.add(convertNbtToJson(list.get(i), playerInfoCache));
            }
            return arr;
        } else if (tag instanceof NumericTag) {
            return new JsonPrimitive(((NumericTag) tag).getAsNumber());
        } else if (tag instanceof StringTag) {
            String str = tag.getAsString();

            // 调整2：拦截纯字符串类型的 UUID (比如 winners 列表里的元素)
            if (str.length() == 36) { // UUID 字符串的标准长度
                try {
                    UUID maybeUuid = UUID.fromString(str);
                    ReplayGenerator.PlayerInfo info = playerInfoCache.get(maybeUuid);
                    if (info != null) {
                        return new JsonPrimitive(info.name()); // 如果是玩家 UUID，替换为名字
                    }
                } catch (IllegalArgumentException ignored) {
                    // 不是 UUID 格式，忽略异常继续按普通字符串处理
                }
            }
            return new JsonPrimitive(str);

        } else if (tag instanceof ByteArrayTag) {
            JsonArray arr = new JsonArray();
            for (byte b : ((ByteArrayTag) tag).getAsByteArray()) arr.add(b);
            return arr;
        } else if (tag instanceof IntArrayTag) {
            JsonArray arr = new JsonArray();
            for (int i : ((IntArrayTag) tag).getAsIntArray()) arr.add(i);
            return arr;
        } else if (tag instanceof LongArrayTag) {
            JsonArray arr = new JsonArray();
            for (long l : ((LongArrayTag) tag).getAsLongArray()) arr.add(l);
            return arr;
        }

        return new JsonPrimitive(tag.toString());
    }
}