<div align="center">
  <img src="docs/assets/tiktools.svg" width="88" height="88" alt="TikTools" />
  <h1>TikTools</h1>
  <p>你的 TikTok 工具箱</p>
  <p>地区预设 · 地图选点 · 设备参数 · 界面工具</p>
  <p><img src="docs/assets/platforms.svg" width="348" height="28" alt="Android 13+ · Xposed API 82 · MIT" /></p>
  <p><strong>简体中文</strong> · <a href="README.en.md">English</a></p>
  <p>
    <a href="https://github.com/sunisup666/TikTools/releases/latest"><strong>下载最新版本</strong></a> ·
    <a href="#开始使用">开始使用</a> ·
    <a href="CHANGELOG.md">更新记录</a> ·
    <a href="https://github.com/sunisup666/TikTools/issues">问题反馈</a>
  </p>
</div>

## 功能一览

| 功能 | 可以做什么 |
| :--- | :--- |
| **地区预设** | 23 个地区，成套填入运营商、MCC/MNC、语言、时区和默认城市坐标。 |
| **定位与地图** | 46 个常用城市、地图点选、缩放和平移，支持固定点、漂移、圆形行走与自定义路线。 |
| **设备参数** | 读取本机配套字段，提供适用机型的同平台模板，保留手动调整入口。 |
| **界面工具** | 按组控制导航、侧边按钮、作者、文案、音乐等视图规则，支持关键词评论过滤。 |
| **翻译与保存** | 使用 ML Kit 在本机翻译；确认后保存可明确匹配当前内容的视频直链。 |
| **配置管理** | JSON 导入导出、坐标收藏、桌面入口控制和本机诊断。 |

## 开始使用

**使用条件：** Android 13 或以上、ARM64，以及已启用的 Vector 或兼容 LSPosed / Xposed 框架。模块使用传统 Xposed API 82。

1. 从 [Releases](https://github.com/sunisup666/TikTools/releases/latest) 下载并安装 APK。
2. 在框架中启用 **TikTools**，只勾选实际使用的 TikTok。其他修改相同接口的插件请先停用。
3. 打开 TikTools，启用总开关，再按需打开各组功能。
4. 修改参数后，强行停止并重新打开 TikTok。

需要下载或翻译入口时，在 **界面 → 显示应用内工具入口** 中打开 **TT** 按钮。

## 界面语言

在 **概览 → 界面语言** 中选择：

| 选项 | 显示方式 |
| :--- | :--- |
| **跟随系统** | 跟随系统语言；未提供对应翻译时使用英文。 |
| **English** | 英文设置、地图、预设名称和校验提示。 |
| **简体中文** | 简体中文设置、地图、预设名称和校验提示。 |

也可使用 Android 系统的应用语言设置。切换显示语言无需联网，并保留现有模块参数。TikTok 语言模拟仍在“地区”页面独立控制。

## 预设与地图

| 想做的事 | 推荐入口 |
| :--- | :--- |
| 一次填好地区参数 | **地区 → 选择国家/地区** |
| 使用常见城市坐标 | **定位 → 选择常用城市**，支持城市或国家搜索 |
| 精确选择位置 | **定位 → 地图选点**，点击“使用此位置”才保存，取消不更改参数 |
| 使用本机设备字段 | **设备 → 选择设备预设 → 本机真实设备（推荐）** |

地图采用 **WGS84**，不读取真实 GPS。手选或城市预设的坐标会固定保留，IP 国家更新不会覆盖；需要重新跟随地区时，可选择“恢复跟随地区坐标”。手动选地区或城市后会关闭 IP 联动。

常用城市预设可离线使用。在线地图需要联网，翻译首次使用需要下载语言模型。基站和 Wi-Fi 字段是独立测试参数，不代表所选城市的真实网络。

## 适配范围

| 项目 | 当前记录 |
| :--- | :--- |
| 应用包名 | `io.github.sunisup666.tiktools` |
| 已记录的实机基线 | Redmi K40 · Android 17 · Vector 2.2 |
| TikTok 基线 | `com.ss.android.ugc.trill` · 47.4.3 / 470403 |
| 另一 TikTok 包名 | 可加入 `com.zhiliaoapp.musically` 作用域，尚未完整验证 |
| 其他应用 | 抖音、快手、小红书不在当前适配范围 |

界面规则只处理可识别的小视图，部分规则依赖具体版本。保存不支持直播、图片帖、DRM、分段流或无法明确对应当前内容的地址。具体结果见 [验证记录](docs/TESTING.md)。

地区还受账号和网络出口影响；进程内字段模拟不会改变真实硬件或保证服务端行为。定位功能修改应用读取的 `Location`，不会产生系统未提供的定位事件。

## 构建与开发

<details>
<summary><strong>展开本地构建步骤</strong></summary>

需要 JDK 17、Android SDK 36、Build Tools 36.1.0 和 Python 3。设置 `ANDROID_HOME`，或在 `local.properties` 中填写 SDK 路径：

```properties
sdk.dir=C:/Android/Sdk
```

```sh
git clone https://github.com/sunisup666/TikTools.git
cd TikTools
./gradlew :app:assembleDebug :probe:assembleDebug
python tools/build.py
```

Windows 使用 `gradlew.bat`。Python 构建器支持 `--install`、`--gradle` 和 `--proxy`；设备脚本默认使用 ADB 当前连接的设备，也可设置 `ANDROID_SERIAL`。

正式签名密钥不随源码提供。维护者可使用 `TIKTOOLS_KEYSTORE`、`TIKTOOLS_STORE_PASSWORD`、`TIKTOOLS_KEY_ALIAS`、`TIKTOOLS_KEY_PASSWORD` 指定签名；自行生成的开发签名不能覆盖不同签名的发布包。更多步骤见 [发布说明](docs/RELEASING.md)。

</details>

[应用插件分析指南](docs/APP_PLUGIN_ANALYSIS_GUIDE.md)记录了排查、实现、实机验证与 Agent 交接方法，供分析其他应用和开发插件时参考。

## 反馈与许可证

通过 [Issues](https://github.com/sunisup666/TikTools/issues) 提问或反馈，附上 TikTools、TikTok、Android 和框架版本，以及停用模块后的表现。日志和截图请先移除私人信息。

本项目采用 [MIT License](LICENSE)。第三方组件与地图服务的条款见 [依赖说明](THIRD_PARTY_NOTICES.md)，数据处理方式见 [隐私说明](docs/PRIVACY.md)。
