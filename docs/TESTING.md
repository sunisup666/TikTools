# 验证范围

实机：Redmi K40、Android 17、Vector 2.2、TikTok `com.ss.android.ugc.trill` 47.4.3。

内部测试版本已完成：

- 总开关关闭对照；SIM/网络/运营商、Locale、时区、固定定位、随时间变化的圆形路线、GSM/Wi-Fi、设备和 SDK 字段的实际接口检查。
- 12 类视图规则的诊断页面测试；TikTok 实际顶部/底部导航、侧边操作、作者、文案、音乐及关键词评论过滤检查。
- 英文到中文的真实 ML Kit 模型翻译。
- 系统下载管理器完成视频下载，MP4 视频轨与音频轨校验。
- 23 个地区和 46 个城市预设；本机设备字段逐项对照系统属性。
- 地图瓦片实机加载、点选方向、缩放和平移保留选点、保存、取消，以及 IP 更新保留固定坐标。
- 六个设置页面打开与截图检查；日常配置冷启动后未出现新的进程重启或崩溃。

## 0.4.0 公开包复测

已核对新包名、版本号与原签名证书；旧配置和语言模型迁移成功，Vector 已加载 TikTools 0.4.0，旧模块停用并保留回退。

- 六组接口探针（核心、关闭对照、路线、系统版本、界面、翻译）通过，翻译使用迁移后的真实模型。
- 六个设置页面、地区/城市/本机设备预设及地图加载、点选、缩放、平移、保存、取消和 IP 更新保留手选点通过；原配置按字节恢复。
- 实际 TikTok 评论过滤观察到 3 个命中；切换信息流期间进程未重启。净化时观察到顶部、底部、侧边、作者、文案和音乐规则命中；该轮没有频道规则命中，不能据此声称频道适配已验证。
- 当前两轮保存测试未识别到可明确对应当前内容的直链，未触发下载。因此公开包本轮的端到端保存验证未通过；内部版本的历史下载结果不替代这项检查。不能识别时模块保留“不可用”，不使用预加载邻近视频地址。
- 最终发布 APK 已覆盖安装，安装文件摘要一致；恢复日常配置后观察 24 秒，画面变化、PID 保持、未发现该进程新的崩溃。探针已卸载，作用域仅保留实际 TikTok，原功能开关与屏幕常亮设置保留。

发布资产 `build-info.json` 提供 APK 摘要与最终安装检查结果。原始手机备份、用户配置与截图不进入公开仓库。

## 本机复测

需要 Root ADB、框架及测试设备。下面的脚本会操作当前连接的手机，不适合作为普通电脑上的离线测试。

```sh
python tools/build.py --install --with-probe
python tools/device_test.py core
python tools/device_test.py disabled
python tools/device_test.py motion
python tools/device_test.py version
python tools/device_test.py ui
python tools/device_test.py translation
python tools/ui_smoke.py
python tools/preset_map_test.py
python tools/tiktok_test.py --comments
python tools/tiktok_test.py --purify --save
python tools/finalize_device.py
```

设备探针配置只向同签名的诊断应用提供。涉及主配置的集成脚本会保存原文件并在结束时恢复。`--save` 会保存测试视频；测试不会执行关注、点赞、发布、登录或支付。

坐标数学检查可以独立运行，不需要手机或网络：

```sh
mkdir -p build/map-math
javac -d build/map-math app/src/main/java/io/github/sunisup666/tiktools/WebMercator.java tools/MapMathCheck.java
java -cp build/map-math MapMathCheck
```
