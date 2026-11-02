# 假名打字练习 · Android 版

> Windows 桌面版已归档，后续功能只更新 Android 版。

一个离线日语假名罗马音练习应用，支持双向练习：看假名打罗马音，或看罗马音选假名。每道题答完后自动播放内置的日语发音。

> **[⬇ 直接下载 APK · v1.1.2（约 811 KB）](<https://github.com/yuletao62-droid/-Japanese-KANA-exercise-APP/raw/master/release/%E5%81%87%E5%90%8D%E6%89%93%E5%AD%97%E7%BB%83%E4%B9%A0.apk>)**

## 下载

[**⬇ 直接下载 APK（v1.1.2 / 约 811 KB）**](<https://github.com/yuletao62-droid/-Japanese-KANA-exercise-APP/raw/master/release/%E5%81%87%E5%90%8D%E6%89%93%E5%AD%97%E7%BB%83%E4%B9%A0.apk>)

- 文件名：`假名打字练习.apk`
- SHA256：`5CA4C4BD0CF7175FDCCEAD72FB1E1F840AC85B243E23600697F98C10AAAD3C7F`
- 直链：`https://github.com/yuletao62-droid/-Japanese-KANA-exercise-APP/raw/master/release/%E5%81%87%E5%90%8D%E6%89%93%E5%AD%97%E7%BB%83%E4%B9%A0.apk`

## v1.1.2 更新

- 修复「い」单独发音时会多出一小段尾音、听起来像「いき」的问题。
- 重新生成「い」的发音音频；其他音频不变。
- 补充说明：「を」按现代标准日语读作 o（与「お」同音），罗马音显示 wo 仅用于输入。

## v1.1.1 更新

- 新增 5 个半浊音假名：ぱ / ぴ / ぷ / ぺ / ぽ（パ / ピ / プ / ペ / ポ）。
- 音种增加「半浊音 5」，并加入「全部 71」。
- 练习范围调整为：清音 46 / 浊音 20 / 半浊音 5 / 全部 71。

## v1.1.0 更新

- 新增 20 个浊音假名，并提供「清音 / 浊音 / 全部」三种练习范围。
- 新增「罗马音→假名」反向练习：给出罗马音，从 8 个假名选项中选出正确项。
- 反向练习同样支持平假名 / 片假名切换。
- 每次答完（答对或答错）都会播放一遍正确假名的日语发音。
- 发音音频已内置，不依赖系统 TTS 或网络。
- 优化顶部控制：练习模式、假名范围、平假名/片假名分组显示。

## 安装

- 支持 Android 6.0（API 23）及以上，屏幕方向为竖屏。
- 将 `假名打字练习.apk` 传到手机，点击安装。
- 首次安装需在系统设置里允许当前文件管理器或浏览器“安装未知应用”。APK 使用开发用自签名证书，系统不会自动信任发布者。
- 应用无网络权限，完全离线运行。

## 玩法

顶部控制分三组：

1. **练习模式**：`假名→罗马音` / `罗马音→假名`。
2. **假名种类**：`清音`（46 音）/ `浊音`（20 音）/ `半浊音`（5 音）/ `全部`（71 音）。
3. **平假名 / 片假名**：对所有模式生效。

### 假名→罗马音

屏幕显示一个假名，点击下方字母键输入它的罗马音。输入正确后自动进入下一题；输入错误时显示正确答案，点击「下一题」继续。

### 罗马音→假名

屏幕显示罗马音，下方提供 8 个假名选项，点击正确的假名完成拼写。答对自动进入下一题；答错时正确选项变绿、错误选项变红，并显示正确答案。

半浊音只有 5 个假名，选项仍保持 8 个，不足部分会用其他音种的假名作为干扰项。

### 朗读

每次完成一道题（无论对错），应用都会播放一次正确假名的内置日语发音。发音文件随 APK 打包，不需要联网，也不依赖手机是否安装日文 TTS 语音包。

发音说明：

- 「い」使用单独处理过的音频，避免孤立发音时多出一段尾音。
- 「を」在现代标准日语中读作 o，和「お」同音；应用里的罗马音仍显示 wo，是为了对应假名输入方式，不代表要读成“wo”或“wu”。
- 音频由日语女声预先生成，个别音的音色可能与你熟悉的发音略有差异，但不会在运行时调用系统 TTS。

### 其他操作

- 切换练习模式、假名种类或平/片假名时会重新出题，统计数据保留。
- 长按「删除」键可以清空当前输入（假名→罗马音模式）。
- 实体键盘：英文键直接输入；数字键 `1`–`8` 在罗马音→假名模式下选择选项；`Enter` 继续。

## 功能

- 平假名 / 片假名全模式切换
- 46 清音 + 20 浊音 + 5 半浊音，随机出题，不连续重复
- 双向练习与 8 选 1 假名选择
- 即时判定，答对自动下一题，答错显示正确答案
- 每次答完播放正确假名的内置日语发音
- 统计答对、答错、正确率和连续答对
- 支持 `shi/si`、`chi/ti`、`tsu/tu`、`fu/hu`、`n/nn`；浊音支持 `じ(ji/zi)`、`ぢ(di)`、`づ(du)`
- 高 DPI 自适应，纯离线，无广告、无第三方 SDK

## 开发与构建

构建环境：

- JDK 17 或更高（当前使用 JDK 25 构建通过）
- Android SDK Platform 34
- Android SDK Build-Tools 35.0.0（D8 版本需要支持较新的 JDK 编译器）
- 无需 Gradle

设置环境变量：

```powershell
$env:ANDROID_HOME = "C:\path\to\android-sdk"
$env:JAVA_HOME = "C:\path\to\jdk"
```

然后运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

也可以显式传入路径：

```powershell
.\build.ps1 -SdkRoot "C:\Android\Sdk" -JdkHome "C:\Program Files\Java\jdk-17"
```

构建流程会执行纯 JVM 游戏逻辑测试、编译资源、编译 Android 字节码、组装 APK、签名并校验，输出到 `release\假名打字练习.apk`。

签名密钥位于 `keystore\kana.jks`。当前为开发用自签名密钥，若要发布到应用商店，应替换为自己的正式签名密钥。

## 技术说明

- 包名：`com.kana.typing`
- 版本：1.1.2（versionCode 4）
- minSdk：23，targetSdk：34
- 语音：71 个日语音频文件内置在 `res/raw`，使用 `MediaPlayer` 播放，不依赖系统 TTS
- UI 使用平台原生 View、Canvas 绘制和 XML drawable，不依赖 AndroidX 或第三方库
- 无网络权限

## 项目结构

```
AndroidManifest.xml
java\com\kana\typing\      # Activity、游戏逻辑、自绘视图
res\                       # 布局、颜色、样式、图标、内置发音
tests\                     # 纯 JVM 游戏规则测试
tools\generate_icons.py    # 从 artwork/app_icon.png 生成启动图标
tools\generate_audio.py    # 可选：使用 edge-tts 重新生成内置发音
build.ps1                  # 无 Gradle 的完整构建脚本
artwork\app_icon.png       # 图标源文件
```
