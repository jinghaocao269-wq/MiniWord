# 极简词典（MiniWord）

一个只负责“输入英文单词 → 显示中文意思”的原生 Android 示例项目。

## 特点
- 原生 Java + Android SDK，不使用第三方 UI 框架或运行时依赖
- 仅申请 INTERNET 网络权限
- 不含广告、账号、数据库、统计 SDK
- 使用 MyMemory 在线翻译接口（免费服务有使用限制，稳定性和可用性由服务方决定）

## 编译
1. 安装 Android Studio。
2. 用 Android Studio 打开本文件夹 `MiniWord`。
3. 等待 Gradle 同步完成。
4. 选择 Build > Build Bundle(s) / APK(s) > Build APK(s)。
5. APK 通常在 `app/build/outputs/apk/debug/app-debug.apk`。

建议先在模拟器或自己的手机上测试。该项目提供源代码，不是已签名发布版 APK。
