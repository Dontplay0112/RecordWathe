package top.dontplay.recordwathe.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class WatheConfig {
    // 默认的后端上传地址
    public String backendUrl = "http://127.0.0.1:8897/api/upload_match";

    // 使用带有格式化输出的 Gson，让生成的 json 文件更易读
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    // 配置文件路径：服务端根目录下的 config/recordwathe.json
    private static final File CONFIG_FILE = new File("config/recordwathe.json");

    // 全局单例
    public static WatheConfig INSTANCE = new WatheConfig();

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                INSTANCE = GSON.fromJson(reader, WatheConfig.class);
                System.out.println("成功加载 recordwathe 配置文件。");
            } catch (IOException e) {
                System.err.println("读取配置文件失败，将使用默认配置: " + e.getMessage());
            }
        } else {
            save(); // 文件不存在时，生成默认配置文件
        }
    }

    public static void save() {
        CONFIG_FILE.getParentFile().mkdirs(); // 确保 config 目录存在
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(INSTANCE, writer);
            System.out.println("已生成 recordwathe 默认配置文件。");
        } catch (IOException e) {
            System.err.println("保存配置文件失败: " + e.getMessage());
        }
    }
}