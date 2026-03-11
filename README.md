# simbot-solon-starter

为 [simbot](https://docs.simbot.forte.love) 提供的 Solon Starter（第三方实现）。

## 能做什么

- 自动加载 Solon Plugin（`META-INF/solon/*.properties`）
- 在应用启动时创建 Simbot `Application`
- 自动扫描并注册 `@Listener` 事件处理函数
- 支持在监听函数参数中通过 Solon 容器进行依赖注入
- 支持按配置自动扫描 bot JSON，并完成注册与自动启动

## 环境要求

- JDK 17+
- Solon 3.x
- Simbot 4.x

## 快速开始

### 1. 引入依赖

```xml
<dependency>
  <groupId>love.forte.simbot</groupId>
  <artifactId>simbot-solon-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

请把版本号替换成你实际使用的发布版本。

如果你是直接从源码运行当前仓库里的示例工程，可以先在仓库根目录执行：

```bash
mvn -q -DskipTests install
```

### 2. 配置 starter

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

如果你还要启用 bot JSON 自动加载，可以再补上：

```yaml
simbot:
  bots:
    autoStartBots: true
    autoStartMode: ASYNC
    configurationJsonResources:
      - "file:./simbot-bots/*.bot.json"
```

### 3. 编写 Listener

```kotlin
import love.forte.simbot.event.Event
import love.forte.simbot.quantcat.common.annotations.Listener
import org.noear.solon.annotation.Component

@Component
class MyListeners {
    @Listener
    suspend fun onEvent(event: Event, myService: MyService) {
        // myService 会由 Solon 容器自动注入
    }
}

@Component
class MyService
```

### 4. 运行与验证

你可以先直接运行仓库内的最小示例工程：

```bash
cd examples/demo-app
mvn -q -DskipTests compile exec:java
```

也可以执行仓库自带测试：

```bash
mvn -q test
```

## 文档

- [docs/integration-guide.md](docs/integration-guide.md)
- [docs/user-app-from-zero.md](docs/user-app-from-zero.md)
- [docs/onebot11-napcat-guide.md](docs/onebot11-napcat-guide.md)

## 示例工程

- [examples/README.md](examples/README.md)
- [examples/demo-app/README.md](examples/demo-app/README.md)
- [examples/onebot11-napcat-demo/README.md](examples/onebot11-napcat-demo/README.md)

## 当前限制

- `@Listener` 扫描目前仅扫描所有 bean 的 `declaredMethods`，不包含父类方法。
- Java 方法会因为拿不到 `kotlinFunction` 而被跳过，当前仅支持 Kotlin 监听函数。
- 监听函数参数注入目前按“参数类型 -> Solon 容器”匹配，暂未支持更复杂的名称或限定符场景。
- 已提供 OneBot11 + NapCat 实战示例；其他平台组件示例仍可继续补充。

## License

本项目与上游 Simbot 保持一致：`LGPLv3`，见 `COPYING` 与 `COPYING.LESSER`。
