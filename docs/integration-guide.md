# simbot-solon-starter 接入指南

本文档面向“`simbot-solon-starter` 已经发布后，业务方如何在自己的 Solon 工程中接入”的场景。

如果你更希望直接照着实战模板搭工程，可以先看：

- [user-app-from-zero.md](user-app-from-zero.md)
- [onebot11-napcat-guide.md](onebot11-napcat-guide.md)

仓库内对应的真实示例工程：

- [examples/demo-app/pom.xml](../examples/demo-app/pom.xml)
- [examples/demo-app/README.md](../examples/demo-app/README.md)
- [examples/onebot11-napcat-demo/pom.xml](../examples/onebot11-napcat-demo/pom.xml)
- [examples/onebot11-napcat-demo/README.md](../examples/onebot11-napcat-demo/README.md)

## 1. 这个 starter 会帮你做什么

当应用启动时，starter 会自动完成下面几件事：

1. 通过 `META-INF/solon/*.properties` 被 Solon 自动发现并加载
2. 读取 `simbot.*` 配置并决定是否启用 starter
3. 构建并注册 Simbot `Application`
4. 扫描 Solon 容器中的 bean，自动注册标注了 `@Listener` 的 Kotlin 监听函数
5. 根据配置自动安装可发现的 component / plugin providers
6. 按配置扫描 bot JSON 资源，完成 bot 注册与自动启动

如果你只是想把 simbot 的监听能力接进 Solon，这个 starter 已经够用。
如果你还要真正接入 OneBot、QQ、KOOK 等平台，除了 starter 之外，还需要继续引入对应的 simbot component 依赖。

## 2. 环境要求

- JDK 17+
- Solon 3.x
- simbot 4.x

## 3. 引入依赖

### Maven

```xml
<dependencies>
    <dependency>
        <groupId>org.noear</groupId>
        <artifactId>solon</artifactId>
        <version>3.4.1</version>
    </dependency>

    <dependency>
        <groupId>io.github.mengxin896</groupId>
        <artifactId>simbot-solon-starter</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### Gradle Kotlin DSL

```kotlin
dependencies {
    implementation("org.noear:solon:3.4.1")
    implementation("io.github.mengxin896:simbot-solon-starter:0.1.0")
}
```

说明：

- `simbot-solon-starter` 负责把 simbot 接入 Solon
- `solon` 仍然应该由你的业务应用自己声明
- 如果你要接入真实平台，还要额外添加相应的 simbot component 依赖

## 4. 最小项目结构

建议先从下面这个最小结构开始：

```text
your-app/
├─ src/main/kotlin/com/example/App.kt
├─ src/main/kotlin/com/example/listener/MyListeners.kt
├─ src/main/resources/app.yml
└─ src/main/resources/simbot-bots/
   └─ demo.bot.json
```

其中：

- `simbot-bots/` 目录是可选的，只在你要使用 bot 自动加载时需要
- 如果 bot 配置包含敏感信息，更推荐使用外部目录，例如 `file:./simbot-bots/*.bot.json`

## 5. 启动 Solon 应用

你只需要正常启动自己的 Solon 应用，不需要额外写 `@Enable...` 注解：

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

starter 会在 Solon 启动过程中自动完成 simbot 的初始化。

## 6. 写第一个监听器

监听器函数必须满足下面两个条件：

1. 所在类必须是 Solon bean，例如 `@Component`
2. 方法必须是 Kotlin 函数，并标注 `@Listener`

示例：

```kotlin
package com.example.listener

import love.forte.simbot.event.Event
import love.forte.simbot.quantcat.common.annotations.Listener
import org.noear.solon.annotation.Component

@Component
class MyService {
    fun ping(): String = "pong"
}

@Component
class MyListeners {
    @Listener
    fun onEvent(event: Event, service: MyService) {
        println("receive event=$event, service=${service.ping()}")
    }
}
```

这个例子里：

- `event` 会由 simbot 事件调度器提供
- `service` 会按“参数类型 -> Solon 容器”的方式自动注入

## 7. 配置 starter

starter 的配置前缀是 `simbot`。

### 最小配置

```yaml
simbot:
  enabled: true
  components:
    autoInstallProviders: true
    autoInstallProviderConfigures: true
  plugins:
    autoInstallProviders: true
    autoInstallProviderConfigures: true
```

### 启用 bot 自动加载

```yaml
simbot:
  bots:
    autoStartBots: true
    autoStartMode: ASYNC
    configurationJsonResources:
      - "file:./simbot-bots/*.bot.json"
```

### 主要配置项说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `simbot.enabled` | `true` | 是否启用 starter |
| `simbot.components.autoInstallProviders` | `true` | 是否自动安装可发现的 component providers |
| `simbot.components.autoInstallProviderConfigures` | `true` | 自动安装 component provider 时，是否同时安装它们的 configure |
| `simbot.plugins.autoInstallProviders` | `true` | 是否自动安装可发现的 plugin providers |
| `simbot.plugins.autoInstallProviderConfigures` | `true` | 自动安装 plugin provider 时，是否同时安装它们的 configure |
| `simbot.bots.configurationJsonResources` | `classpath:simbot-bots/*.bot.json` | bot 配置文件扫描路径，支持 `classpath:` 与 `file:` |
| `simbot.bots.ignoreIOExceptionForResourcesLoad` | `true` | 扫描 bot 资源时遇到 `IOException` 是否忽略 |
| `simbot.bots.autoStartBots` | `true` | 注册完成后是否自动启动 bot |
| `simbot.bots.autoStartMode` | `ASYNC` | bot 自动启动模式，支持 `SYNC` / `ASYNC` |
| `simbot.bots.autoRegistrationResourceLoadFailurePolicy` | `ERROR` | bot 配置资源加载/解析失败时的处理策略 |
| `simbot.bots.autoRegistrationFailurePolicy` | `ERROR` | bot 注册或启动失败时的处理策略 |
| `simbot.bots.autoRegistrationMismatchConfigurableBotManagerPolicy` | `ERROR_LOG` | 找不到可注册该配置的 `BotManager` 时的处理策略 |

## 8. bot 配置文件如何放

starter 默认会扫描：

```text
classpath:simbot-bots/*.bot.json
```

这意味着你可以把 bot 配置放在：

```text
src/main/resources/simbot-bots/
```

如果配置中带有密钥、token、secret，推荐改成外部文件：

```yaml
simbot:
  bots:
    configurationJsonResources:
      - "file:./simbot-bots/*.bot.json"
```

这样你可以把 bot 配置放在应用运行目录旁边，而不是打进 jar 包里。

## 9. starter 暴露了哪些对象

启动成功后，starter 至少会向 Solon 容器中注册这些对象：

- `simbotApplication`
- `simbotBinderManager`

其中 `simbotApplication` 是最核心的对象。当前仓库里的 smoke test 已验证可以通过名称从容器中取到它：

```kotlin
val application = solonApp.context().getBean("simbotApplication")
```

如果你需要做启动自检、主动投递事件、或者调试当前应用状态，可以先从这里拿到 `Application`。

## 10. 如何接入真实平台组件

starter 本身不提供 OneBot、QQ、KOOK 等协议实现，它只负责：

- 与 Solon 的生命周期集成
- 监听器扫描与参数注入
- bot 配置自动加载
- component / plugin 的容器桥接

真正接入真实平台时，通常还需要：

1. 引入目标平台对应的 simbot component 依赖
2. 按该 component 的要求编写 bot 配置 JSON
3. 让 starter 扫描到这些 bot 配置并完成注册

平台相关的坐标与 bot JSON 结构，请以 simbot 官方文档为准：

- [simbot 文档首页](https://docs.simbot.forte.love)
- [Simple Robot / simbot 仓库](https://github.com/simple-robot/simpler-robot)

## 11. 如何验证接入成功

建议按下面顺序检查：

1. 应用启动日志中是否出现 `SimbotSolonPlugin initialized. listeners=..., bots=...`
2. 是否能从 Solon 容器中取到 `simbotApplication`
3. 自定义 `Event` 后，`@Listener` 方法是否能被触发
4. 如果启用了 bot 自动加载，日志里是否出现 bot 注册或启动结果

你也可以直接参考仓库中的 demo 工程：

- [examples/demo-app/pom.xml](../examples/demo-app/pom.xml)
- [examples/demo-app/src/main/kotlin/love/forte/simbot/solon/demo/DemoApp.kt](../examples/demo-app/src/main/kotlin/love/forte/simbot/solon/demo/DemoApp.kt)

## 12. 常见坑

### 1) 为什么 `@Listener` 没生效？

优先检查：

1. 类上是否有 `@Component`
2. 方法是否真的是 Kotlin 函数，而不是 Java 方法
3. 方法上是否有 `@Listener`
4. 应用是否真的把该类扫进了 Solon 容器

### 2) 为什么 bot 没自动注册？

优先检查：

1. `configurationJsonResources` 路径是否写对
2. 配置文件是否真的存在于 classpath 或外部目录
3. 对应平台的 component 依赖是否已经加入工程
4. 当前 bot 配置是否有可匹配的 `BotManager`

### 3) 为什么建议把 bot JSON 放到外部文件？

因为很多 bot 配置都包含 token、secret 或账号信息。
如果直接打进业务 jar，会更难做环境隔离与密钥治理。

## 13. 当前限制

- `@Listener` 扫描目前基于 bean 的 `declaredMethods`
- 父类方法不会被扫描到
- Java 方法会因为拿不到 `kotlinFunction` 而被跳过
- 参数注入当前按“参数类型 -> Solon 容器”匹配，暂不支持更复杂的名称/限定符/泛型精确匹配
- 仓库已提供 OneBot11 + NapCat 的端到端示例；其他平台 demo 仍待补充

## 14. 参考实现

如果你想看一个最小可运行样例，优先看这个文件：

- [examples/demo-app/src/main/kotlin/love/forte/simbot/solon/demo/DemoApp.kt](../examples/demo-app/src/main/kotlin/love/forte/simbot/solon/demo/DemoApp.kt)
- [examples/demo-app/README.md](../examples/demo-app/README.md)

如果你要看“真实机器人平台接入”的完整模板，再继续看：

- [examples/onebot11-napcat-demo/README.md](../examples/onebot11-napcat-demo/README.md)
- [examples/onebot11-napcat-demo/src/main/kotlin/love/forte/simbot/solon/onebot11/demo/OneBot11NapCatDemoApp.kt](../examples/onebot11-napcat-demo/src/main/kotlin/love/forte/simbot/solon/onebot11/demo/OneBot11NapCatDemoApp.kt)

它演示了：

- 正常启动 Solon
- 自动获得 `simbotApplication`
- 定义自定义事件
- 通过 `@Listener` 监听事件
- 让监听器参数自动注入 Solon bean
