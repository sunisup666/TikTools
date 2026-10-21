# 发布与模块目录

源代码仓库为 <https://github.com/sunisup666/TikTools>。应用 ID 固定为 `io.github.sunisup666.tiktools`；未来正常更新保持包名和签名不变。

1. 同步 `versionCode`、`versionName` 和 `CHANGELOG.md`。
2. 使用维护者的私有密钥构建并验证。不要把密钥、令牌、手机备份或本机配置提交到 Git。
3. 完成与变更有关的设备检查，确认发布 APK 与设备安装包的 SHA-256 一致。
4. 提交源码，使用 `VersionCode-VersionName` 格式创建标签，例如 `5-0.4.0`。
5. 发布 GitHub Release，上传签名 APK、源码归档和校验文件。更换 APK 时应创建新的递增版本。

`python tools/package_release.py` 要求干净且已提交的工作区、当前 `build-result.json`，以及维护者整理的私有 `validation.json`。后者字段为 `version`、`version_code`、`apk_sha256`、`certificate_sha256`、`checks` 和 `limitations`；只填脱敏后的检查结论，不复制原始手机报告。工具通过 `git archive HEAD` 归档源码，并生成 APK、源码 ZIP、`build-info.json` 和 `SHA256SUMS.txt`。未通过或未覆盖的检查必须在结论中保留。

Vector 的官方模块目录指向 LSPosed 模块仓库。收录规则见 [Xposed-Modules-Repo/submission](https://github.com/Xposed-Modules-Repo/submission)。

- 在该仓库提交标题为 `[submission] io.github.sunisup666.tiktools` 的 Issue。
- 使用自己的 GitHub 用户名前缀，无需额外域名 TXT 验证。
- 目录仓库由其机器人创建；接受对应邀请后，仅同步说明文件、`SUMMARY` 与带 APK 的有效 Release。源代码保留在个人仓库。
- 目录仓库描述应为 `TikTools`，主页与反馈指向本项目。若维护者权限无法修改设置，需由目录管理员处理。
- 提交申请、创建目录仓库和实际显示在模块列表是不同阶段。只有检查对应页面及下载条目后，才能确认已经收录。
