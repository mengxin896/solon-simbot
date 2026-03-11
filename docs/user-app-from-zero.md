# 从 0 到 1 创建一个可运行的 Solon 用户工程

本文档面向第一次接触 `simbot-solon-starter` 的用户。

目标不是接入真实机器人平台，而是先验证三件事：

1. starter 能被 Solon 自动加载
2. `simbotApplication` 能成功注册到容器
3. `@Listener` 与参数注入都能正常工作

如果你先把这一档跑通，后面再接 OneBot / QQ / KOOK 时，排障会轻松很多。

## 1. 准备环境

- JDK 17+
- Maven 3.9+
- 一个空白目录，例如 `demo-solon-simbot`

## 2. 创建项目结构

建议直接按下面这个结构创建：

```text
demo-solon-simbot/
├─ pom.xml
└─ src/
   └─ main/
      ├─ kotlin/
      │  └─ com/example/
      │     ├─ App.kt
      │     └─ DemoListeners.kt
      └─ resources/
         └─ app.yml
```

## 3. 写 `pom.xml`

下面这个 `pom.xml` 是一个可以直接跑起来的最小版本。

注意把 `0.1.0` 替换成你实际发布的 starter 版本。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>demo-solon-simbot</artifactId>
    <version>1.0.0-SNAPSHOT</version>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.release>17</maven.compiler.release>

        <kotlin.version>2.1.20</kotlin.version>
        <solon.version>3.4.1</solon.version>
        <simbot.solon.starter.version>0.1.0</simbot.solon.starter.version>
        <slf4j.version>2.0.17</slf4j.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.noear</groupId>
            <artifactId>solon</artifactId>
            <version>${solon.version}</version>
        </dependency>

        <dependency>
            <groupId>love.forte.simbot</groupId>
            <artifactId>simbot-solon-starter</artifactId>
            <version>${simbot.solon.starter.version}</version>
        </dependency>

        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
            <version>${kotlin.version}</version>
        </dependency>

        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-reflect</artifactId>
            <version>${kotlin.version}</version>
        </dependency>

        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-simple</artifactId>
            <version>${slf4j.version}</version>
        </dependency>
    </dependencies>

    <build>
        <sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>

        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${kotlin.version}</version>
                <executions>
                    <execution>
                        <id>compile</id>
                        <phase>compile</phase>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                        <configuration>
                            <jvmTarget>${maven.compiler.release}</jvmTarget>
                        </configuration>
                    </execution>
                </executions>
            </plugin>

            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.3.0</version>
                <configuration>
                    <mainClass>com.example.App</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

## 4. 写 `app.yml`

最小配置如下：

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

这一档先不接真实平台，因此不需要 `simbot.bots.*` 配置。

## 5. 写 `DemoListeners.kt`

这个文件会同时定义：

- 一个自定义事件 `BootEvent`
- 一个可注入的业务 bean `GreetingService`
- 一个 `@Listener` 监听器

```kotlin
package com.example

import love.forte.simbot.common.id.ID
import love.forte.simbot.common.id.UUID
import love.forte.simbot.common.time.Timestamp
import love.forte.simbot.event.Event
import love.forte.simbot.quantcat.common.annotations.Listener
import org.noear.solon.annotation.Component

class BootEvent : Event {
    override val id: ID = UUID.random()
    override val time: Timestamp = Timestamp.now()
}

@Component
class GreetingService {
    fun message(): String = "hello from solon bean"
}

@Component
class DemoListeners {
    @Listener
    fun onBoot(event: BootEvent, service: GreetingService) {
        println("listener invoked. eventId=${event.id}, message=${service.message()}")
    }
}
```

## 6. 写 `App.kt`

这个启动类会在 Solon 启动完成后：

1. 从容器中拿到 `simbotApplication`
2. 主动推送一个自定义事件 `BootEvent`
3. 用事件回流来验证监听器是否真的生效

```kotlin
package com.example

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import love.forte.simbot.application.Application
import org.noear.solon.Solon

object App {
    @JvmStatic
    fun main(args: Array<String>) {
        val solonApp = Solon.start(App::class.java, args)

        val simbotApplication = solonApp.context().getBean("simbotApplication") as? Application
            ?: error("simbotApplication bean not found")

        println("starter loaded. simbotApplication=$simbotApplication")

        runBlocking {
            withTimeout(5_000) {
                simbotApplication.eventDispatcher.push(BootEvent()).collect { }
            }
        }
    }
}
```

## 7. 运行

在项目根目录执行：

```powershell
mvn -q compile exec:java
```

## 8. 成功标志

如果接入成功，你通常会看到类似输出：

```text
[Solon] SimbotSolonPlugin initialized. listeners=1, bots=0
starter loaded. simbotApplication=...
listener invoked. eventId=..., message=hello from solon bean
```

这说明下面这些链路都已经打通：

- Solon 成功发现 starter
- starter 成功创建 `simbotApplication`
- `@Listener` 成功被扫描并注册
- 监听器参数中的 `GreetingService` 成功从 Solon 容器注入

## 9. 这一档常见问题

### 1) 看不到 `SimbotSolonPlugin initialized`

优先检查：

1. `pom.xml` 里的 starter 版本是否真的存在
2. 是否引入了 `org.noear:solon`
3. 主类是否真的执行到了 `Solon.start(...)`

### 2) `simbotApplication bean not found`

优先检查：

1. starter 是否已经被加载
2. `simbot.enabled` 是否被错误地配成了 `false`
3. 依赖冲突是否导致 starter 初始化失败

### 3) listener 没打印

优先检查：

1. 监听器类上是否有 `@Component`
2. 监听方法上是否有 `@Listener`
3. 方法是否是 Kotlin 方法，而不是 Java 方法

## 10. 下一步做什么

这一档跑通后，下一步通常是两条路：

1. 接入真实平台组件，例如 OneBot v11 / NapCat
2. 把 bot 配置改成 `simbot.bots.configurationJsonResources` 自动加载

继续往真实机器人联调走时，直接看下一篇：

- [onebot11-napcat-guide.md](onebot11-napcat-guide.md)
