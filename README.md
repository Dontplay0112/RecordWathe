# RecordWathe

## 一个用于导出Wathe对局记录的模组

需要配合[WatheMatchAnalysis](https://github.com/Dontplay0112/WatheMatchAnalysis)使用，具体参考[WatheMatchAnalysis](https://github.com/Dontplay0112/WatheMatchAnalysis)。

## 开发及构建

1. 下载wathe模组，放入`libs`文件夹
2. 修改`build.gradle`中`modApi files("libs/WATHE_MOD_FILE_NAME")`中的`WATHE_MOD_FILE_NAME`为下载的wathe模组文件名
3. 运行`./gradlew build`，生成的mod文件在`build/libs`文件夹下