# 电话拼音T9

> An Xposed module that lets the Google Phone dial pad find Chinese contacts by pinyin (full pinyin or initials).

让 Google 电话的拨号盘支持拼音搜索中文联系人。拨号盘上按 `6262` 就能找到「妈妈」。

## 功能

- **全拼**：张三 → `94264726`（zhangsan）
- **首字母**：张三 → `97`（zs）
- 多音字的每个读音都能匹配
- 拼音匹配到的联系人会加进拨号盘原有的搜索结果里，号码搜索和英文名搜索照常工作
- 只在 Google 电话里生效，**不需要任何权限**，不联网，也不修改通讯录

## 要求

- 已 root，并装了支持旧版 Xposed API 的框架（LSPosed、Vector 等）
- Android 8.0 及以上
- Google 电话（`com.google.android.dialer`）

作者的测试环境：一加 Ace 6T，OxygenOS 16，Vector。

## 安装

1. 从 [Releases](../../releases) 下载最新的 APK 并安装
2. 在 Xposed 管理器里启用本模块，作用域勾选「Google 电话」（已默认推荐）
3. 强行停止 Google 电话，重新打开

## 遇到问题

Google 电话更新后，模块可能会失效。请到 [Issues](../../issues) 反馈，并附上：

- 手机型号和系统版本
- Xposed 框架的名称和版本
- Google 电话的版本号
- Xposed 管理器里的模块日志（前缀为 `T9Pinyin:`）

## 原理

Google 电话的拨号盘搜索会查询它自己的 SQLite 表 `smartdial_table`。本模块 hook 了 `SQLiteDatabase` 的查询：识别到拨号盘发出的查找后，用内置的拼音表把能匹配上的中文联系人补进查询结果。

- 拼音表 `app/src/main/assets/pinyin.bin` 由 [pinyin-data](https://github.com/mozillazg/pinyin-data) 生成
- 匹配逻辑主要在 `T9Matcher`、`Pinyin` 和 `LookupHook` 这几个类里

## 说明

原始工程丢失，本仓库的源码由发布版 APK 反编译整理而来，局部变量名是工具生成的，欢迎改进。

## 自己编译

需要 JDK 17 和 Android SDK，用 Android Studio 打开即可编译。编译 Release 包要用你自己的签名密钥，请不要把密钥文件提交到仓库。

## 许可证

本项目代码采用 [MIT 许可证](LICENSE)，可以自由使用、修改和再发布，只需保留版权声明。

拼音数据来自 pinyin-data（MIT）和 Unicode Unihan 数据库（Unicode License），详见 [NOTICE](NOTICE)。

本项目与 Google 无关。
