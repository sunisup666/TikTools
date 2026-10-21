# 数据与隐私

TikTools 没有自建数据服务器。配置、收藏和界面诊断保存在设备本机；诊断不保存视频文案、作者、媒体直链或登录令牌。

- **地区与 IP 联动**：启用后向 `ipwho.is` 查询当前网络出口的国家。服务方会收到网络请求及其来源 IP。关闭联动后不进行自动查询。
- **在线地图**：向 OpenStreetMap 请求当前视野的瓦片，服务方可推断所查看的大致区域。选点不调用手机真实 GPS，不请求定位权限。常用城市预设保存在应用内，无需网络。
- **翻译**：文案通过本机进程间调用交给 Google ML Kit，在设备上处理。ML Kit 会下载语言模型，并可能向 Google 发送 SDK 性能和使用统计；输入文案与译文不作为翻译请求发送到 Google 服务器。见 [Google ML Kit 隐私说明](https://developers.google.com/ml-kit/terms#privacy)。
- **保存视频**：用户确认后，由系统下载管理器访问已识别的视频地址并保存到 `Movies/TikTools`。不上传这些地址到 TikTools 服务。
- **配置导出**：用户选择导出后，JSON 文件包含其填写的地区、坐标及设备参数。请自行决定如何保存或分享。

仓库、发布包和问题反馈位于 [sunisup666/TikTools](https://github.com/sunisup666/TikTools)。反馈时请先检查日志和截图中的私人信息。
