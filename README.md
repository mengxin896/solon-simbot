# simbot-solon-starter

为 [simbot](https://docs.simbot.forte.love) 提供的 Solon Starter（第三方实现）。

## 目标

提供一个 **可用的最小闭环**：

- 自动加载 Solon Plugin（`META-INF/solon/*.properties`）
- 启动时创建 Simbot `Application`
- 自动扫描并注册 `@Listener` 事件处理函数
- 支持在监听函数参数中通过 Solon 容器进行依赖注入（ParameterBinderFactory）

## 版本要求

- JDK 17+
- Solon 3.x
- Simbot 4.x（当前开发用 `4.13.0`）

## 使用

更完整的“发布后如何接入业务工程”的说明见：

- [docs/integration-guide.md](docs/integration-guide.md)
- [docs/user-app-from-zero.md](docs/user-app-from-zero.md)
- [docs/onebot11-napcat-guide.md](docs/onebot11-napcat-guide.md)
- [examples/README.md](examples/README.md)

### 1) 引入依赖

当前默认还是先走本地安装体验；仓库已经补齐 Maven Central 发布所需的构建与工作流配置，等版本号和凭据准备好后即可正式发版。

本地安装：

```bash
mvn -q -DskipTests install
```

然后在你的 Solon 应用里添加依赖（版本号按实际发布版本调整）：

```xml
<dependency>
  <groupId>love.forte.simbot</groupId>
  <artifactId>simbot-solon-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2) 配置项

Solon 配置文件（`app.properties` / `app.yml` 等）中可用：

```properties
simbot.enabled=true
simbot.components.autoInstallProviders=true
simbot.components.autoInstallProviderConfigures=true
simbot.plugins.autoInstallProviders=true
simbot.plugins.autoInstallProviderConfigures=true
```

### 3) 编写 Listener（示例）

监听函数必须在 **Solon Bean** 上（例如 `@Component`），并标注 `@Listener`：

```kotlin
import love.forte.simbot.event.Event
import love.forte.simbot.quantcat.common.annotations.Listener
import org.noear.solon.annotation.Component

@Component
class MyListeners {
    @Listener
    suspend fun onEvent(event: Event, myService: MyService) {
        // myService 将由 Solon 容器注入
    }
}

@Component
class MyService
```

## 验证

本仓库自带一个 smoke test，会启动 Solon（已禁用 HTTP）并断言 `simbotApplication` 已注册到容器：

```bash
mvn -q test
```

另外提供了一个本地启动入口（用于排障观察启动日志）：`love.forte.simbot.solon.SimbotSolonSmokeMain`。

### 第四档（真实 Solon 应用 demo）

仓库内提供了一个独立 demo 工程：`examples/demo-app`，用于模拟“用户工程通过依赖引入 starter 后的真实启动与事件触发”。

- 说明文档：[examples/demo-app/README.md](examples/demo-app/README.md)

```bash
# 在仓库根目录先安装 starter 到本地仓库
mvn -q -DskipTests install

# 进入 demo 工程并运行
cd examples/demo-app
mvn -q -DskipTests compile exec:java
```

### 第五档（OneBot11 + NapCat 实战示例）

如果你要验证真实机器人链路，仓库内还提供了：

- `examples/onebot11-napcat-demo`
- [examples/onebot11-napcat-demo/README.md](examples/onebot11-napcat-demo/README.md)

它演示了：

- 通过 `simbot.bots.configurationJsonResources` 自动加载 bot JSON
- 接入 `simbot-component-onebot-v11-core-jvm`
- 连接本机 NapCat
- 收到 `ping` 后回复 `pong`

```bash
# 在仓库根目录先安装 starter 到本地仓库
mvn -q -DskipTests install

# 进入示例工程并运行
cd examples/onebot11-napcat-demo
mvn -q -DskipTests compile exec:java
```

## 发布

### CI / Release 现状

- `CI` 工作流会执行 `mvn -B test`
- `CI` 还会执行 `mvn -B -DskipTests install`，确保主产物、`sources.jar`、`javadoc.jar` 都能打包
- `CI` 最后会运行 `examples/demo-app`，验证“用户工程依赖 starter 后的真实启动闭环”
- `Release` 工作流在推送 `v*` tag 或手动触发时执行，并通过 `central-release` profile 发布到 Maven Central

### 首次发布前需要准备

1. 将根目录 `.mvn/maven.config` 里的 `revision` 从 `*-SNAPSHOT` 改成正式版，例如 `-Drevision=0.1.0`
2. 在 GitHub Actions Secrets 中配置：
   - `CENTRAL_TOKEN_USERNAME`
   - `CENTRAL_TOKEN_PASSWORD`
   - `MAVEN_GPG_PRIVATE_KEY`
   - `MAVEN_GPG_PASSPHRASE`
3. 如果最终源码仓库是公开仓库，建议把 `pom.xml` 里的 `scm` 占位地址改成真实仓库地址；如果源码仓库保持私有，当前占位方式也符合 Sonatype 的最小要求
4. 推送形如 `v0.1.0` 的 tag，或手动触发 `Release` 工作流

### 本地手动发布

如果你想在本地直接走发布命令：

```bash
mvn -B -Pcentral-release -DskipTests deploy
```

要求：

- 版本号不能是 `-SNAPSHOT`
- `settings.xml` 里需要存在 `serverId=central` 的凭据
- 本机 GPG 已正确导入并可用于 Maven 签名

## 已知限制 / 待完善

- `@Listener` 扫描目前仅扫描所有 bean 的 `declaredMethods`；不包含父类方法、也不包含 Java 方法（`kotlinFunction == null` 会跳过）。
- 监听函数参数注入目前按“参数类型 -> Solon 容器”匹配；未支持按名称/限定符、泛型精确匹配等更复杂场景。
- 已提供 OneBot11 + NapCat 实战示例；其他平台组件 demo 仍待继续补充。
- `javadoc.jar` 当前是轻量占位文档包，后续如果接入 Dokka 可以无缝替换成完整 API 文档。

## License

本项目与上游 Simbot 保持一致：`LGPLv3`，见 `COPYING` 与 `COPYING.LESSER`。
