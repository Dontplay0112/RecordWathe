package top.dontplay.recordwathe;

import dev.doctor4t.wathe.api.event.RecordEvents;
import net.fabricmc.api.ModInitializer;
import top.dontplay.recordwathe.core.ReplayExporter;
import top.dontplay.recordwathe.core.WatheConfig;
import top.dontplay.recordwathe.network.RecordNetworking;
import top.dontplay.recordwathe.command.RecordCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class Recordwathe implements ModInitializer {

    @Override
    public void onInitialize() {
        WatheConfig.load();
        RecordEvents.ON_RECORD_END.register((world, match) -> {
            ReplayExporter.exportMatch(match);
        });
        // 1. 注册网络包（确保全局只有这一行调用了 registerPayloads）
        RecordNetworking.registerPayloads();

        // 2. 注册指令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RecordCommand.register(dispatcher);
        });
    }
}
