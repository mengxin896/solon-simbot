# OneBot v11 + NapCat 实战接入指南

本文档面向已经完成 starter 最小接入、准备把真实机器人接进 Solon 应用的用户。

目标场景：

- 业务应用基于 Solon
- 使用 `simbot-solon-starter` 做框架接入
- 使用 OneBot v11 组件连接 NapCat

如果你还没跑通过“starter 最小闭环”，建议先看：

- [user-app-from-zero.md](user-app-from-zero.md)

仓库内的完整示例工程：

- [examples/onebot11-napcat-demo/pom.xml](../examples/onebot11-napcat-demo/pom.xml)
- [examples/onebot11-napcat-demo/README.md](../examples/onebot11-napcat-demo/README.md)
- [examples/onebot11-napcat-demo/src/main/resources/app.yml](../examples/onebot11-napcat-demo/src/main/resources/app.yml)
- [examples/onebot11-napcat-demo/simbot-bots/napcat.bot.json](../examples/onebot11-napcat-demo/simbot-bots/napcat.bot.json)
- [examples/onebot11-napcat-demo/src/main/kotlin/love/forte/simbot/solon/onebot11/demo/OneBot11NapCatDemoApp.kt](../examples/onebot11-napcat-demo/src/main/kotlin/love/forte/simbot/solon/onebot11/demo/OneBot11NapCatDemoApp.kt)

## 1. 先理解这条链路

在这个组合里，各部分职责如下：

- `solon`：你的应用运行时
- `simbot-solon-starter`：负责把 simbot 接入 Solon
- `simbot-component-onebot-v11-core-jvm`：负责实现 OneBot v11 协议客户端
- `NapCat`：实际承载 QQ 登录和 OneBot 服务的宿主

也就是说，starter 不会替代 OneBot 组件，OneBot 组件也不会替代 NapCat。

## 2. NapCat 侧怎么配

建议直接按下面这组参数来做第一轮联调：

- HTTP API: `http://127.0.0.1:3000`
- WebSocket 事件: `ws://127.0.0.1:3001`
- `botUniqueId`: `sample-onebot`
- `accessToken`: 先留空

在 NapCat 的当前账号 OneBot 网络配置里，同时启用：

### HTTP Server

- host: `127.0.0.1`
- port: `3000`
- token: 留空

### WebSocket Server

- host: `127.0.0.1`
- port: `3001`
- token: 留空

最重要的注意点：

当前 NapCat 的最小联调里，HTTP API 和事件 WebSocket 要分开配置。
不要把事件流只指向 `http://127.0.0.1:3000`，否则你通常只能打通动作 API，拿不到真正的事件推送。

## 3. Maven 依赖怎么写

下面给的是一个可直接参考的 Maven 依赖组合，也是仓库内示例工程采用的做法。

版本说明：

- `simbot.solon.starter.version` 替换成你发布的 starter 版本
- `simbot.onebot.version` 按你实际使用的 OneBot 组件版本替换
- 下方示例中的 `1.8.2` 只是当前本机已验证可解析的坐标示例，不代表你必须固定用这个版本

```xml
<properties>
    <solon.version>3.4.1</solon.version>
    <kotlin.version>2.1.20</kotlin.version>
    <slf4j.version>2.0.17</slf4j.version>

    <simbot.solon.starter.version>0.1.0</simbot.solon.starter.version>
    <simbot.onebot.version>1.8.2</simbot.onebot.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.noear</groupId>
        <artifactId>solon</artifactId>
        <version>${solon.version}</version>
    </dependency>

    <dependency>
        <groupId>io.github.mengxin896</groupId>
        <artifactId>simbot-solon-starter</artifactId>
        <version>${simbot.solon.starter.version}</version>
    </dependency>

    <dependency>
        <groupId>love.forte.simbot.component</groupId>
        <artifactId>simbot-component-onebot-v11-core-jvm</artifactId>
        <version>${simbot.onebot.version}</version>
    </dependency>

    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>${slf4j.version}</version>
    </dependency>
</dependencies>
```

如果你已经有自己的日志体系，例如 Logback / Log4j2，也可以不用 `slf4j-simple`，换成你自己的 SLF4J provider。

## 4. 推荐的项目结构

```text
your-app/
├─ pom.xml
├─ src/main/kotlin/com/example/App.kt
├─ src/main/kotlin/com/example/PingListener.kt
├─ src/main/resources/app.yml
└─ simbot-bots/
   └─ napcat.bot.json
```

这里我建议把 bot JSON 放到外部目录 `simbot-bots/`，而不是 `src/main/resources/simbot-bots/`。

原因很简单：

- NapCat / OneBot 配置里经常带 token
- 外部文件更适合按环境区分
- 不会把敏感配置直接打进应用 jar

## 5. `app.yml` 怎么写

推荐先用下面这份：

```yaml
simbot:
  enabled: true
  components:
    autoInstallProviders: true
    autoInstallProviderConfigures: true
  plugins:
    autoInstallProviders: true
    autoInstallProviderConfigures: true
  bots:
    autoStartBots: true
    autoStartMode: ASYNC
    configurationJsonResources:
      - "file:./simbot-bots/*.bot.json"
    autoRegistrationResourceLoadFailurePolicy: ERROR
    autoRegistrationFailurePolicy: ERROR
    autoRegistrationMismatchConfigurableBotManagerPolicy: ERROR_LOG
```

说明：

- `configurationJsonResources` 指向外部目录
- `autoStartBots=true` 表示注册后自动启动
- `autoStartMode=ASYNC` 表示先注册完，再异步启动 bot

## 6. `napcat.bot.json` 怎么写

OneBot v11 的可序列化配置外层要带一个 `component` 字段，值为：

```json
"component": "simbot.onebot11"
```

下面这份是一个最小可用模板：

```json
{
  "component": "simbot.onebot11",
  "authorization": {
    "botUniqueId": "sample-onebot",
    "apiServerHost": "http://127.0.0.1:3000",
    "eventServerHost": "ws://127.0.0.1:3001",
    "accessToken": ""
  },
  "config": {
    "wsConnectMaxRetryTimes": 999999,
    "wsConnectRetryDelayMillis": 3000
  }
}
```

字段说明：

- `botUniqueId`：建议填机器人 QQ 号，或者一个稳定唯一值
- `apiServerHost`：NapCat 的 HTTP API 地址
- `eventServerHost`：NapCat 的 WebSocket 事件地址
- `accessToken`：如果 NapCat 配了 token，就填；没有就留空或直接删掉

如果你希望分别设置 HTTP / WebSocket token，也可以改成：

```json
{
  "component": "simbot.onebot11",
  "authorization": {
    "botUniqueId": "sample-onebot",
    "apiServerHost": "http://127.0.0.1:3000",
    "eventServerHost": "ws://127.0.0.1:3001",
    "apiAccessToken": "your-http-token",
    "eventAccessToken": "your-ws-token"
  }
}
```

## 7. 应用启动类怎么写

先从最简单的启动类开始：

```kotlin
package com.example

import org.noear.solon.Solon

object App {
    @JvmStatic
    fun main(args: Array<String>) {
        Solon.start(App::class.java, args)
    }
}
```

这里不需要手动注册 OneBot bot。
只要：

1. starter 在 classpath 里
2. OneBot 组件在 classpath 里
3. `simbot-bots/*.bot.json` 能被扫描到

starter 就会在启动时自动完成：

- 解析 JSON
- 找到匹配的 `BotManager`
- 注册 bot
- 启动 bot

## 8. `PingListener.kt` 怎么写

下面给一个最实用的入门监听器：收到 `ping` 回复 `pong`。

```kotlin
package com.example

import love.forte.simbot.event.MessageEvent
import love.forte.simbot.quantcat.common.annotations.Listener
import org.noear.solon.annotation.Component

@Component
class PingListener {
    @Listener
    suspend fun onMessage(event: MessageEvent) {
        val text = event.messageContent.plainText?.trim().orEmpty()
        if (text != "ping") {
            return
        }

        println("receive message from ${event.authorId}: $text")
        event.reply("pong")
    }
}
```

这个版本用的是通用 `MessageEvent`，先保证“能收能回”。
后续如果你要按群聊 / 私聊区分，再换成更具体的 OneBot 事件类型。

## 9. 启动顺序建议

为了少踩坑，建议按下面顺序做第一次联调：

1. 先启动 NapCat
2. 确认 QQ 已登录
3. 确认 OneBot HTTP / WebSocket 都已经启用
4. 再启动你的 Solon 应用
5. 给机器人发 `ping`
6. 观察是否回复 `pong`

## 10. 如何运行

如果你的工程也用了 `exec-maven-plugin`，可以直接：

```powershell
mvn -q compile exec:java
```

## 11. 成功标志

接入成功时，通常会出现这几类信号：

1. 启动日志中有 `SimbotSolonPlugin initialized. listeners=..., bots=...`
2. 没有出现 “找不到可注册该配置的 BotManager”
3. 没有出现 bot 配置解析失败
4. 给机器人发 `ping` 后，机器人回复 `pong`

## 12. 如果 NapCat 没准备好怎么办

这是实战里最容易碰到的情况。

需要明确一点：

当前 starter 的 bot 自动加载更适合“应用启动时，下游 OneBot 服务已经就绪”的场景。

如果应用启动时：

- NapCat 还没启动
- QQ 还没登录
- OneBot 服务端口还没起来

那么自动启动 bot 可能失败。

### 推荐做法

第一轮联调时，先保证：

1. NapCat 已经启动
2. QQ 已登录
3. HTTP / WebSocket 端口都已就绪

再启动你的 Solon 应用。

### 如果你就是想要“服务没好也持续重试”

那更适合采用“手动注册 + 外层重试”的方式。
这类方案的核心思路是：

1. 先正常启动 Solon + starter
2. 手动从 `simbotApplication` 取出应用实例
3. 代码里注册 OneBot bot
4. 如果启动失败，等待几秒后继续重试

当前仓库提供的 `examples/onebot11-napcat-demo` 走的是“自动加载 bot JSON”的方式，更适合作为用户工程模板。
如果你的场景必须依赖外层重试，可以在业务应用启动阶段按上面的思路自行扩展。

## 13. 常见问题

### 1) 日志里显示 `bots=0`

优先检查：

1. `simbot-bots/*.bot.json` 路径是否写对
2. JSON 文件是否真的存在
3. 文件内容是不是合法 JSON

### 2) 日志里显示找不到 `BotManager`

这通常意味着：

- 你只引入了 starter
- 但没有引入 OneBot 组件依赖

也就是少了：

```xml
<dependency>
    <groupId>love.forte.simbot.component</groupId>
    <artifactId>simbot-component-onebot-v11-core-jvm</artifactId>
    <version>...</version>
</dependency>
```

### 3) 只能调 HTTP API，收不到事件

这是 NapCat 联调里最高频的问题之一。

优先检查：

1. `eventServerHost` 是否真的写成了 `ws://...`
2. NapCat 是否单独启用了 WebSocket Server
3. 你是不是把事件地址误写成了 HTTP 地址

### 4) 为什么推荐外部 `bot.json`？

因为 OneBot 配置里通常带：

- token
- bot 唯一标识
- 不同环境下不同的地址

放在外部目录更适合部署和环境隔离。

## 14. 你接下来最该做什么

如果你已经把这篇文档跑通，下一步建议是：

1. 把 `botUniqueId` 改成真实机器人标识
2. 给 NapCat 配置好 token，并写入外部 `bot.json`
3. 把 `PingListener` 改成你自己的真实业务监听器
4. 再决定是否需要“手动注册 + 重试版”的启动模式
