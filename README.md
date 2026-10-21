# TikTools

**TikTok 工具箱 · Xposed 模块**

地区预设、地图选点、本机设备参数、界面净化、评论过滤和本机翻译。设置分组控制，修改只在已勾选的目标应用进程中生效。

[下载应用](https://github.com/sunisup666/TikTools/releases/latest) · [更新记录](CHANGELOG.md) · [反馈问题](https://github.com/sunisup666/TikTools/issues) · [隐私说明](docs/PRIVACY.md)

## 使用条件

- Android 13 或以上、ARM64。
- 已安装并启用 Vector 或兼容的 LSPosed / Xposed 框架。
- 包名 `io.github.sunisup666.tiktools`，使用传统 Xposed API 82。
- 已验证：Redmi K40、Android 17、Vector 2.2，TikTok `com.ss.android.ugc.trill` **47.4.3 / 470403**。
- 支持将 `com.zhiliaoapp.musically` 加入作用域；该包及其他 TikTok 版本的界面适配尚未完整验证。

## 安装

1. 从 [Releases](https://github.com/sunisup666/TikTools/releases/latest) 安装 APK。
2. 在 Vector / LSPosed 中启用 **TikTools**，勾选实际使用的 TikTok。其他会修改相同接口的插件请先停用。
3. 打开 TikTools，开启总开关，按需启用各组功能。
4. 设置改变后，强行停止并重新打开 TikTok。

下载和应用内翻译入口：在“界面”中打开“显示应用内工具入口”，然后点击 TikTok 内的 **TT** 按钮。

## 预设与地图

| 页面 | 操作 |
| --- | --- |
| 地区 | 23 个地区预设，一键填入运营商、MCC/MNC、语言、时区和默认城市坐标。手动选择后关闭 IP 联动，避免随后被覆盖。 |
| 定位 | 46 个常用城市，可按城市或国家搜索；城市预设自动匹配地区及时区。 |
| 地图选点 | 拖动、缩放、点击标记。点击“使用此位置”才保存，取消不改变原参数。采用 WGS84，不读取手机真实 GPS。 |
| 设备 | 选择“本机真实设备（推荐）”，自动读取成套设备字段与真实系统版本。当前 alioth 平台还提供 Redmi K40 / POCO F3 常用字段模板。 |

地图或城市选定的坐标固定保留，IP 更新不会重置它。需要重新跟随地区时，点击“恢复跟随地区坐标”。手动输入保留在“手动调整”中。在线地图需要联网，常用城市预设可以离线使用。

## 其他功能

- 模拟目标进程读取的 SIM/网络国家、运营商、Locale、时区、定位、基站、Wi-Fi 和 Build 字段。
- 固定点、位置漂移、圆形行走和自定义路线往返；坐标收藏。
- 顶部/频道/底部导航、侧边按钮、作者、文案、音乐等界面规则；关键词评论过滤和自定义资源匹配。
- Google ML Kit 本机翻译。首次使用在“工具”中下载所需语言模型；通常需要 Wi-Fi。
- 按当前可见文案关联可识别的视频直链，确认后交给系统下载管理器保存。
- JSON 配置导入导出、桌面图标控制和本机诊断。

基站/Wi-Fi 是独立测试参数，不代表所选城市的真实网络。设备模板只模拟应用内信息，不改变实际硬件。系统版本模拟可能影响接口选择，默认保持真实值。

## 适配范围

接口挂钩成功不等于服务端行为一定改变；内容地区还受账号和网络出口影响。定位功能修改已读取的 `Location`，不会凭空产生系统未提供的定位事件。

界面规则只隐藏可识别的小视图，保留包含播放器的容器。部分角标、推广、地点及不同评论布局仅有通用匹配；无法保证所有页面均生效。媒体保存不处理直播、图片帖、DRM、分段流或需要额外身份认证的地址；无法明确对应当前视频时不提供直链。

当前测试说明见 [TESTING](docs/TESTING.md)。短时间复测不代表未来版本兼容性或长期稳定性。

给其他工程 Agent 的分析、实现和验证方法见 [应用插件分析指南](docs/APP_PLUGIN_ANALYSIS_GUIDE.md)，包含从停用对照到实机验收、迁移发布的步骤和交接模板。

## 构建

需要 JDK 17、Android SDK 36、Build Tools 36.1.0、Python 3。在 `local.properties` 中填写本机 SDK 路径，或设置 `ANDROID_HOME`：

```properties
sdk.dir=C:/Android/Sdk
```

```sh
./gradlew :app:assembleDebug :probe:assembleDebug
python tools/build.py
python tools/build.py --install
```

Windows 使用 `gradlew.bat`。本地 Python 构建器也可通过 `--gradle` 指定已安装的 Gradle 8.14.3，通过 `--proxy http://127.0.0.1:10808` 指定代理；默认读取 `HTTPS_PROXY` / `HTTP_PROXY`。设备脚本默认使用 ADB 当前连接的设备，也可设置 `ANDROID_SERIAL`。

正式签名密钥不在仓库中。首次自行构建会生成本地开发签名，不能覆盖不同签名的官方包。维护者可通过 `TIKTOOLS_KEYSTORE`、`TIKTOOLS_STORE_PASSWORD`、`TIKTOOLS_KEY_ALIAS`、`TIKTOOLS_KEY_PASSWORD` 指定自己的密钥。CI 只验证编译，不生成可覆盖正式版的签名发布包。

## 从内部测试版迁移

旧版 TrillKit 的 `dev.local.trillkit` 包名不满足模块仓库的域名归属规则，公开版统一为 `io.github.sunisup666.tiktools`。

可在旧版导出 JSON，再在 TikTools 导入；当前 Vector CLI 环境也可使用 Root ADB 迁移工具 `python tools/migrate_legacy.py` 备份并迁移旧配置、语言模型和作用域。两版请勿同时启用。迁移不会清除 TikTok 的账号或草稿。

## 反馈与依赖

提交问题时请说明 TikTools、TikTok、Android 和框架版本，以及停用模块后的表现。不要公开包含登录信息、私人坐标或他人内容的原始截图。

本项目独立实现，未包含 TikTok 或 GlobalKit 的实现代码，也不代表 TikTok 官方。第三方组件及地图服务信息见 [THIRD_PARTY_NOTICES](THIRD_PARTY_NOTICES.md)。项目代码版权归 sunisup666，尚未指定项目级开源许可证。
