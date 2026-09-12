# ChatHub 2.0 (Forge 1.20.1)

ChatHub 2.0 是原 ChatHub 聊天同步插件的 **Minecraft Forge 1.20.1 单服版**。

原版插件运行在 Velocity 代理上，用于跨服务器转发聊天消息。本模组去掉了跨服聊天功能，改为运行在单个服务器内，并在**游戏聊天框中每位玩家昵称前显示其当前所在的维度名字**（如 `[主世界] Steve: hello`）。

同时保留并移植了原插件的 QQ 群互通能力：支持通过 **OneBot 反向 WebSocket** 或 **QQ 官方机器人 API** 将游戏内聊天转发到 QQ 群，并支持全体 QQ <-> 游戏账号绑定验证。

## 功能

- 🪐 聊天框中玩家昵称前显示当前维度名（维度显示名可在配置中自定义）
- 💬 游戏内聊天 <-> QQ 群实时互转（`{server} <{name}>: {message}` 等格式可自定义）
- 📖 QQ 群里发送 `/在线` 或 `/list` 查询在线玩家列表（转发合并消息）
- 🔗 QQ 玩家与游戏账号绑定 / 解绑（进入游戏时验证码验证，支持群管理解绑、退群自动解绑）
- ⚙️ `/chathub reload` 热重载配置、`/chathub list` 在线列表、`/chathub msg <玩家> <消息>` 私聊消息

## 安装

1. 安装 [Minecraft Forge 1.20.1](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)（需 47.0.0+）
2. 将构建出的 `chathub.jar` 放入服务器的 `mods/` 文件夹
3. 启动服务器，模组会在 `config/chathub/` 目录生成默认 `config.toml`
4. 按需修改 `config.toml` 并执行 `/chathub reload`（或重启服务器）

## 配置

配置文件位于 `config/chathub/config.toml`：

```toml
[minecraft]
# 为 true 时，游戏聊天将由模组接管并按下方格式输出（这样才能显示维度前缀）
completeTakeoverMode = true

[minecraft.message]
# {dimension} 会替换为玩家当前所在维度的显示名（见 [dimension] 一节）
chat = '§7[§b{dimension}§7]§e{name}§r: {message}'

[dimension]
overworld   = '主世界'
the_nether  = '下界'
the_end     = '末地'
# 模组维度的显示名可通过  维度id = '显示名'  继续添加
```

### QQ 互通

**方式一：OneBot 反向 WebSocket（推荐）**

在你的 OneBot 实现（如 go-cqhttp、Lagrange）中配置反向 WS 地址指向服务器：

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `qq.api.host` | `0.0.0.0` | WS 监听地址 |
| `qq.api.wsReversePort` | `9001` | 反向 WS 端口 |
| `qq.api.wsReversePath` | `/ws/` | 反向 WS 路径，须与 OneBot 配置一致 |

**方式二：QQ 官方机器人 API**

填写 `[qq.bot]` 下的 `appId` 与 `appSecret`（留空则走 OneBot 方式）。

两种方式都需要设置：

- `qq.groupId`：接收/发送消息的 QQ 群号
- `qq.enable = true`：启用 QQ 互通
- `[qq.message]`：发送到 QQ 群的消息格式（自动去除 § 颜色码）
- `qq.admins`：可执行"解绑"操作的管理员 QQ 号数组
- `qq.enableBinding`：是否启用游戏账号绑定验证（配合群内"绑定 / 解绑"指令）

> 注意：`completeTakeoverMode` 开启时，QQ 进来的消息在游戏内也会显示为带有 `[QQ]` 前缀的维度格式，保证格式统一。

## 构建

```bash
./gradlew build
```

产物：

- `build/libs/chathub.jar` —— 最终可安装的模组（已通过 Jar-in-Jar 内置依赖 `Java-WebSocket`）
- CI 会在每次推送到 `main` 时自动构建并上传产物，打 `v*` 标签时自动发布 Release。

## 技术说明

- 原插件针对 Velocity 代理开发，本版本将 `minecraft` 平台适配器直接运行在单服内：
  - 跨服聊天 / 服务器切换相关事件已移除
  - 玩家列表、踢出等服务器操作通过 `IPlayerManager` 抽象接口，QQ 侧不依赖 Forge 类
  - JSON 解析改用 Minecraft 自带的 **Gson**（原依赖 fastjson2）；HTTP 改用 JDK `java.net.http`（原依赖 OkHttp）
  - TOML 配置解析使用模组内置的轻量解析器，无额外依赖
- 混淆与代码归属见 `decompiled/` 目录（原插件反编译源码，仅作参考）

## 命令

| 命令 | 权限 | 说明 |
| --- | --- | --- |
| `/chathub reload` | op | 重新加载配置与绑定，并重启各平台适配器 |
| `/chathub list` | 所有玩家 | 广播在线玩家列表 |
| `/chathub msg <玩家> <消息>` | 所有玩家 | 发送私聊消息 |

## 支持与反馈

问题反馈请到 <https://github.com/INORACLE/ChatHub-2.0/issues>。

## 许可证

MIT