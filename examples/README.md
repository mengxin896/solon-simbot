# Examples

这里放的是“发布后用户可以直接照着抄”的示例工程。

## 示例列表

- `demo-app`
  - 最小闭环示例
  - 验证 starter 自动加载、`simbotApplication` 暴露、`@Listener` 注册和 Solon bean 参数注入
  - 不依赖外部机器人平台，适合做第一轮验收
- `onebot11-napcat-demo`
  - OneBot v11 + NapCat 实战示例
  - 验证 bot JSON 自动扫描、OneBot 组件接入、NapCat 联调与消息回复
  - 依赖本机 NapCat 和真实 QQ 登录状态

## 推荐使用顺序

1. 先运行 `demo-app`，确认 starter 最小闭环已经跑通。
2. 再运行 `onebot11-napcat-demo`，确认真实机器人接入链路也能工作。

## 通用前置步骤

两个示例都会依赖仓库当前版本的 starter。
在运行示例前，先回到仓库根目录执行：

```powershell
mvn -q -DskipTests install
```

当前仓库通过根目录的 `.mvn/maven.config` 统一维护版本号，所以主工程和示例工程会自动保持一致。
