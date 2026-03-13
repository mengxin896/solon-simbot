# Examples

这里放的是“发布后用户可以直接照着抄”的示例工程。

## 示例列表

- `demo-app`
  - Kotlin 最小闭环示例
  - 验证 starter 自动加载、`simbotApplication` 暴露、`@Listener` 注册和 Solon bean 参数注入
  - 不依赖外部机器人平台，适合做第一轮验收
- `demo-app-java`
  - Java 最小闭环示例
  - 验证 Java `@Listener` 注册、事件投递和 Solon bean 参数注入
  - 适合 Java 业务工程先确认 starter 接入链路
- `onebot11-napcat-demo`
  - Kotlin 版 OneBot v11 + NapCat 实战示例
  - 验证 bot JSON 自动扫描、OneBot 组件接入、NapCat 联调与消息回复
  - 依赖本机 NapCat 和真实 QQ 登录状态
- `onebot11-napcat-demo-java`
  - Java 版 OneBot v11 + NapCat 实战示例
  - 验证 Java `@Listener` 消息接收、消息回复和 bot JSON 自动加载
  - 依赖本机 NapCat 和真实 QQ 登录状态

## 推荐使用顺序

1. 如果你的业务工程是 Kotlin，先运行 `demo-app`；如果主要是 Java，先运行 `demo-app-java`。
2. 如果你的业务工程是 Kotlin，继续看 `onebot11-napcat-demo`；如果主要是 Java，继续看 `onebot11-napcat-demo-java`。

## 通用前置步骤

两个示例都会依赖仓库当前版本的 starter。
在运行示例前，先回到仓库根目录执行：

```powershell
mvn -q -DskipTests install
```

当前仓库通过根目录的 `.mvn/maven.config` 统一维护版本号，所以主工程和示例工程会自动保持一致。
