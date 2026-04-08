package top.dontplay.recordwathe.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import top.dontplay.recordwathe.network.DownloadResponsePayload;
import top.dontplay.recordwathe.core.GzipUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class RecordCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("record")
                .then(Commands.literal("list")
                        .executes(context -> executeList(context.getSource())))
                .then(Commands.literal("download")
                        // 语法 1: /record download <序号>
                        .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                .executes(context -> executeDownloadRange(
                                        context.getSource(),
                                        IntegerArgumentType.getInteger(context, "index"),
                                        IntegerArgumentType.getInteger(context, "index") // 起点和终点相同
                                ))
                                // 语法 2: /record download <起始序号> <结束序号>
                                .then(Commands.argument("endIndex", IntegerArgumentType.integer(1))
                                        .executes(context -> executeDownloadRange(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "index"),
                                                IntegerArgumentType.getInteger(context, "endIndex")
                                        ))
                                )
                        )
                )
        );
    }

    /**
     * 获取按时间从旧到新排序的对局文件列表
     */
    private static List<File> getSortedRecordFiles() {
        File exportDir = new File(FabricLoader.getInstance().getGameDir().toFile(), "wathe_exports");
        if (!exportDir.exists() || !exportDir.isDirectory()) {
            return new ArrayList<>();
        }

        File[] files = exportDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }

        List<File> list = new ArrayList<>(Arrays.asList(files));
        // 按文件最后修改时间排序 (时间戳越小越靠前)
        list.sort(Comparator.comparingLong(File::lastModified));
        return list;
    }

    private static int executeList(CommandSourceStack source) {
        List<File> files = getSortedRecordFiles();
        if (files.isEmpty()) {
            source.sendSuccess(() -> Component.literal("服务端暂无任何对局记录。").withStyle(ChatFormatting.RED), false);
            return 0;
        }

        source.sendSuccess(() -> Component.literal("=== 服务端对局记录列表 ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("提示: 使用 /record download <起点> <终点> 可批量下载").withStyle(ChatFormatting.GRAY), false);

        for (int i = 0; i < files.size(); i++) {
            File file = files.get(i);
            String fileName = file.getName();
            String matchId = fileName.substring(0, fileName.length() - 5);
            int displayIndex = i + 1; // 序号从 1 开始

            MutableComponent clickableName = Component.literal(String.format("[%d] ■ %s", displayIndex, matchId))
                    .withStyle(style -> style
                            .withColor(ChatFormatting.AQUA)
                            .withUnderlined(true)
                            // 点击后直接执行对应序号的单文件下载
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/record download " + displayIndex))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("点击下载第 " + displayIndex + " 局的记录")))
                    );

            source.sendSuccess(() -> clickableName, false);
        }

        return 1;
    }

    private static int executeDownloadRange(CommandSourceStack source, int startIndex, int endIndex) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;

        // 拦截未装 Mod 的玩家
        if (!ServerPlayNetworking.canSend(player, DownloadResponsePayload.ID)) {
            source.sendFailure(Component.literal("下载失败：你需要安装此 Mod 的客户端版本才能将文件保存到本地！"));
            return 0;
        }

        // 确保起始序号不大于结束序号
        int start = Math.min(startIndex, endIndex);
        int end = Math.max(startIndex, endIndex);

        List<File> files = getSortedRecordFiles();
        if (files.isEmpty()) {
            source.sendFailure(Component.literal("服务端暂无任何对局记录。"));
            return 0;
        }

        // 越界检查
        if (start < 1 || start > files.size()) {
            source.sendFailure(Component.literal("起始序号超出范围！可用范围: 1 ~ " + files.size()));
            return 0;
        }
        if (end > files.size()) {
            end = files.size(); // 如果输入的终点偏大，自动截断到最大可用序号
        }

        int downloadCount = end - start + 1;
        Component startMsg = Component.literal(String.format("正在为你打包发送 %d 份对局记录 (%d 到 %d)...", downloadCount, start, end)).withStyle(ChatFormatting.GREEN);
        source.sendSuccess(() -> startMsg, false);

        int successCount = 0;
        for (int i = start - 1; i <= end - 1; i++) {
            File targetFile = files.get(i);
            String matchId = targetFile.getName().substring(0, targetFile.getName().length() - 5);

            try {
                String jsonContent = Files.readString(targetFile.toPath());
                byte[] compressed = GzipUtils.compress(jsonContent);

                DownloadResponsePayload response = new DownloadResponsePayload(matchId, compressed);
                ServerPlayNetworking.send(player, response);
                successCount++;
            } catch (Exception e) {
                source.sendFailure(Component.literal("读取文件 " + matchId + " 时发生错误。"));
                e.printStackTrace();
            }
        }

        int finalSuccessCount = successCount;
        source.sendSuccess(() -> Component.literal("已成功发送 " + finalSuccessCount + " 份记录。").withStyle(ChatFormatting.GREEN), false);

        return finalSuccessCount;
    }
}