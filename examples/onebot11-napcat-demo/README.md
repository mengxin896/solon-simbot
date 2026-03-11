# OneBot11 + NapCat Demo

这是一个基于 `simbot-solon-starter` 的真实联调示例工程。

它演示的是：

- Solon 自动加载 `simbot-solon-starter`
- 通过 `simbot.bots.configurationJsonResources` 自动加载 OneBot v11 bot JSON
- 连接本机 NapCat
- 收到 `ping` 后回复 `pong`

## 默认约定

示例默认读取：

- bot 配置文件：`./simbot-bots/napcat.bot.json`
- NapCat HTTP API：`http://127.0.0.1:3000`
- NapCat 事件 WebSocket：`ws://127.0.0.1:3001`
- `botUniqueId`：`sample-onebot`
- `accessToken`：空

## 运行前准备

1. 在仓库根目录执行一次：

```powershell
mvn -q -DskipTests install
```

2. 确认 NapCat 已经启动，并且当前账号已经登录 QQ

3. 在 NapCat 的 OneBot 网络配置里同时启用：

- HTTP Server
  - host: `127.0.0.1`
  - port: `3000`
  - token: 留空
- WebSocket Server
  - host: `127.0.0.1`
  - port: `3001`
  - token: 留空

注意：

HTTP API 和事件 WebSocket 必须分开配置。
如果你只配了 HTTP，而没有单独启用 WebSocket 事件服务，动作 API 也许能通，但事件不会进来。

## 修改 bot 配置

默认配置文件在：

- [simbot-bots/napcat.bot.json](simbot-bots/napcat.bot.json)

如果你的 NapCat 地址、端口或 token 不同，直接修改这个文件即可。

## 运行

进入示例目录执行：

```powershell
cd examples/onebot11-napcat-demo
mvn -q -DskipTests compile exec:java
```

## 成功标志

看到类似日志后，给机器人发 `ping`：

```text
OneBot11 + NapCat demo started.
Current workdir: ...
Bot configuration file: ./simbot-bots/napcat.bot.json
If NapCat is already connected, send `ping` to the bot and it should reply `pong`.
```

如果机器人回复了 `pong`，说明以下链路全部打通：

- starter 自动加载成功
- OneBot 组件自动安装成功
- bot JSON 自动扫描成功
- NapCat HTTP / WebSocket 均连接成功
- `@Listener` 消息监听成功

## 为什么 CI 只编译不运行

这个示例依赖外部 NapCat 和真实 QQ 登录状态，因此仓库 CI 只做 `compile` 验证，不会在 CI 中真的执行 `exec:java`。
