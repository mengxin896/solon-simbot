# Demo App

这是仓库里最小、最适合先跑通的示例工程。

它演示的是：

- 业务应用像真实用户工程一样，通过 Maven 依赖引入 `simbot-solon-starter`
- Solon 启动后自动拿到 `simbotApplication`
- 主动推送一个自定义事件
- `@Listener` 成功接收事件，并自动注入 Solon bean

## 运行前准备

先在仓库根目录执行一次：

```powershell
mvn -q -DskipTests install
```

这一步会把当前 starter 安装到本机 Maven 仓库，示例工程随后会按同一版本号解析它。

## 运行

进入示例目录执行：

```powershell
cd examples/demo-app
mvn -q -DskipTests compile exec:java
```

## 成功标志

看到类似输出时，说明最小闭环已经打通：

```text
[Solon] SimbotSolonPlugin initialized. listeners=1, bots=0
onDemo invoked. eventId=..., service=pong
Demo done. invoked=1
```

这意味着以下链路都已经正常工作：

- starter 被 Solon 自动发现并初始化
- `simbotApplication` 已注册到容器
- 自定义事件成功进入 simbot 事件总线
- `@Listener` 监听器成功触发
- 监听器参数里的 `DemoService` 由 Solon 容器完成注入

## 什么时候先跑这个 demo

如果你正在排查“starter 到底有没有接进业务应用”，建议先跑这个 demo。
它不依赖外部机器人平台，也不要求额外账号或网络环境，比 OneBot / NapCat 实战示例更适合做第一轮验收。
