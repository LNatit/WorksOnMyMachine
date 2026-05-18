# `createNewWorld` 流程解析（`WOMMClient`）

> 范围限定：仅覆盖 `WOMMClient#createNewWorld(String worldName)` 及其内部调用的方法。

## 1. 入口初始化阶段（UI + 计时）

**位置**：`createNewWorld` 开头，`Minecraft mc = ...` 到 `long start = ...`

- `Minecraft.getInstance()`：获取客户端单例，作为后续世界创建流程的核心上下文。
- `queueLoadScreen(mc, PREPARING_WORLD_DATA)`：切换到通用加载界面，显示“正在准备世界数据”。
- `Util.getMillis()`：记录起始时间，用于统计资源准备阻塞时长。

### 内部调用：`queueLoadScreen(Minecraft, Component)`

- 执行 `minecraft.setScreenAndShow(new GenericMessageScreen(message))`。
- 副作用：立即切换 UI，向用户展示创建前加载提示。

---

## 2. 世界创建上下文准备阶段（Datapack + 默认维度）

**位置**：`WorldCreationContextMapper ...` 到 `mc.managedBlock(loadResult::isDone)`

- 构造 `worldCreationContext` 映射器：把加载结果（cookie/registries/managers）转换为 `WorldCreationContext`。
- 构造 `settingsFunction`：
  - `WorldOptions.defaultWithRandomSeed()`：默认随机种子；
  - `WorldPresets.createNormalWorldDimensions(...)`：普通世界维度预设。
- 创建 `PackRepository vanillaOnlyPackRepository`，并通过 `ResourcePackLoader.populatePackRepository(...SERVER_DATA...)` 注入服务端数据包来源。
- 生成 `WorldDataConfiguration dataConfig`：
  - IDE 环境：`vanilla + tests`；
  - 非 IDE：`WorldDataConfiguration.DEFAULT`。
- `createDefaultLoadConfig(...)` 生成 `WorldLoader.InitConfig`。
- 调用 `WorldLoader.load(...)` 异步加载数据包与 worldgen registry，返回 `CompletableFuture<WorldCreationContext>`。
- `mc.managedBlock(loadResult::isDone)`：主线程等待异步加载完成。

### 内部调用：`createDefaultLoadConfig(PackRepository, WorldDataConfiguration)`

- 先构造 `WorldLoader.PackConfig(packRepository, config, false, true)`。
- 再构造 `WorldLoader.InitConfig(..., Commands.CommandSelection.INTEGRATED, LevelBasedPermissionSet.GAMEMASTER)`。
- 作用：定义本次世界数据加载所需的 pack、命令环境、权限级别。

---

## 3. 存档目录准备阶段（目标目录 + 临时目录 + 拷贝）

**位置**：`String worldFolder = getTargetFolder(worldName);` 到 `if (newWorldAccess.isEmpty())`

- `getTargetFolder(worldName)`：为新世界选择一个不冲突的存档文件夹名。
- `loadResult.join()`：获取已加载好的 `WorldCreationContext`。
- `getOrCreateTempDataPackDir(mc, worldName)`：创建临时 datapack 目录。
- `createNewWorldDirectory(mc, worldFolder, tempDataPackDir)`：
  - 创建 `LevelStorageAccess`；
  - 若存在临时目录，把内容复制到目标世界 `datapacks/` 目录。
- 若 `newWorldAccess.isEmpty()`：
  - `SystemToast.onPackCopyFailure(...)` 提示失败；
  - `cleanOnFail(...)` 回退界面并清理临时目录；
  - 返回 `false`，流程终止。

### 内部调用：`getTargetFolder(String)`

- 优先使用 `worldName.trim()`，为空则退回 `DEFAULT_WORLD_NAME`。
- 使用 `FileUtil.findAvailableName(...)` 规避重名。
- 异常兜底尝试使用 `"World"`；仍失败则抛出运行时异常。

### 内部调用：`getOrCreateTempDataPackDir(Minecraft, String)`

- `Files.createTempDirectory("mcworld-")` 创建临时目录。
- 异常时：记录日志 + `SystemToast.onPackCopyFailure(...)` + `cleanOnFail(...)`。
- 失败返回 `null`。

### 内部调用：`createNewWorldDirectory(Minecraft, String, Path)`

- 通过 `minecraft.getLevelSource().createAccess(worldFolder)` 打开目标世界存档访问器。
- 若 `tempDataPackDir == null`，直接返回 `access`（不做拷贝）。
- 若不为 `null`：
  - `Files.walk(tempDataPackDir)` 遍历临时目录；
  - `access.getLevelPath(LevelResource.DATAPACK_DIR)` 定位目标 datapacks 目录；
  - `FileUtil.createDirectoriesSafe(targetDir)` 确保目录存在；
  - 对每个源路径执行 `copyBetweenDirs(...)`。
- 拷贝异常时关闭 `access`，并返回 `Optional.empty()`。

### 内部调用：`copyBetweenDirs(Path, Path, Path)`

- 通过 `Util.copyBetweenDirs(...)` 执行文件/目录复制。
- `IOException` 转为 `UncheckedIOException` 抛出，由上层统一处理失败分支。

---

## 4. 世界参数组装阶段（维度/生命周期/规则/元数据）

**位置**：`else` 分支内 `WorldDimensions worldDimensions = ...` 到 `WorldDataAndGenSettings ...`

- `context.selectedDimensions()`：读取当前维度设置。
- `worldDimensions.bake(context.datapackDimensions())`：将维度定义烘焙为最终版本。
- `finalLayers = context.worldgenRegistries().replaceFrom(...)`：将 DIMENSIONS registry 替换成烘焙后的维度 registry。
- 读取 `enabledFeatures = context.dataConfiguration().enabledFeatures()`。
- 计算生命周期：
  - `lifecycleFromFeatures`：feature flags 是否实验性；
  - `lifecycleFromRegistries`：registry 的生命周期；
  - `lifecycle = lifecycleFromRegistries.add(lifecycleFromFeatures)`：合并生命周期。
- 计算 `isDebug`（是否 Debug 世界属性）。
- `createLevelSettings(context, isDebug)` 生成 `LevelSettings`。
- 根据 `isDebug` 生成 `GameRules`：
  - Debug：使用默认规则并关闭 `ADVANCE_TIME`；
  - 非 Debug：按启用特性复制规则。
- 组装最终对象：
  - `PrimaryLevelData worldData`；
  - `WorldGenSettings worldGenSettings`；
  - `LevelDataAndDimensions.WorldDataAndGenSettings worldDataAndGenSettings`。

### 内部调用：`createLevelSettings(WorldCreationContext, boolean)`

- Debug 世界：固定旁观者模式 + 和平难度 + `WorldDataConfiguration.DEFAULT`。
- 非 Debug 世界：
  - 游戏模式取 `context.initialWorldCreationOptions().selectedGameMode().gameType`；
  - 难度固定 `NORMAL`；
  - 数据配置取 `context.dataConfiguration()`。
- 世界显示名称固定为 `DEFAULT_WORLD_NAME.getString().trim()`。

---

## 5. 正式创建与收尾阶段（落盘 + 进入世界 + 清理）

**位置**：`if (worldDataAndGenSettings.data().worldGenSettingsLifecycle() != Lifecycle.stable())` 到方法返回

- 若 worldgen 生命周期非稳定：
  - `((PrimaryLevelData) worldDataAndGenSettings.data()).withConfirmedWarning(true)`，避免下次打开时再次显示实验性确认提示。
- 调用核心创建入口：
  - `mc.createWorldOpenFlows().createLevelFromExistingSettings(newWorldAccess.get(), context.dataPackResources(), finalLayers, worldDataAndGenSettings, Optional.of(gameRules))`。
- 创建完成后 `removeTempDataPackDir(tempDataPackDir)` 清理临时目录。
- 成功返回 `true`。

### 内部调用：`removeTempDataPackDir(Path)`

- 若目录存在：`Files.walk(...)` + `Comparator.reverseOrder()` 逆序删除（先文件后目录）。
- 删除失败只记录日志，不抛出异常中断主流程。

### 内部调用：`cleanOnFail(Minecraft, Path)`（失败回退）

- 切回 `JoinMultiplayerScreen(new TitleScreen())`。
- 调 `removeTempDataPackDir(path)` 做临时目录清理。

---

## 流程时序（简表）

1. 显示“准备世界数据”界面并开始计时。  
2. 准备默认 worldgen + datapack 配置，异步加载并阻塞等待。  
3. 选定存档目录名，创建临时目录与目标世界目录，复制 datapack。  
4. 依据上下文烘焙维度、合并生命周期、构建关卡设置与游戏规则。  
5. 调用 `createLevelFromExistingSettings(...)` 真正创建并打开世界。  
6. 清理临时目录；失败时 toast + 回退多人界面 + 清理后返回失败。

---

## 轻量标注（非主线）

- `skipWarning` 已计算但当前方法内未参与后续逻辑，属于保留变量。  
- `worldName` 用于文件夹命名，但 `LevelSettings` 名称固定 `DEFAULT_WORLD_NAME`，两者可能不一致。  
- `mc.managedBlock(loadResult::isDone)` 会把等待成本集中在该阶段（符合先加载后创建的流程选择）。

