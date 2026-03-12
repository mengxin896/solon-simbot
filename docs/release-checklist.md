# 发布前维护者清单

本文档面向维护者，不面向业务接入方。

## 当前已使用的真实公开信息

当前仓库已经替换为下面这些真实对外信息：

1. 项目主页
   - `https://github.com/mengxin896/solon-simbot`
2. 源码仓库信息
   - `scm.url`: `https://github.com/mengxin896/solon-simbot`
   - `scm.connection`: `scm:git:https://github.com/mengxin896/solon-simbot.git`
   - `scm.developerConnection`: `scm:git:https://github.com/mengxin896/solon-simbot.git`
3. 维护者信息
   - `id`: `mengxin896`
   - `name`: `mx14`
   - `email`: 当前 GitHub 主页未公开邮箱，`pom.xml` 暂不填写
   - `url`: `https://github.com/mengxin896`

## 真实公开信息通常需要哪些

在正式发布前，至少要确认下面这些信息是真实、可公开、可长期维护的：

1. 项目主页
   - 对应 `pom.xml` 里的 `<url>`
   - 应该指向项目主页、公开源码仓库或正式文档站，而不是占位地址或第三方项目主页
2. 源码仓库信息
   - 对应 `pom.xml` 里的 `<scm>`
   - `url`、`connection`、`developerConnection` 都应指向真实仓库
3. 维护者信息
   - 对应 `pom.xml` 里的 `<developers>`
   - 建议使用真实维护者名称、组织和联系邮箱
4. 发布命名空间
   - 对应 `pom.xml` 里的 `<groupId>`
   - 必须使用你实际拥有发布权限的命名空间
5. 正式版本号
   - 对应 `.mvn/maven.config` 里的 `revision`
   - 正式发布时不能继续使用 `-SNAPSHOT`

## 当前仓库发布前还需要人工确认的事项

1. 确认最终 Maven 坐标
   - 当前仓库已切到 `io.github.mengxin896:simbot-solon-starter`
   - 正式发布前再确认 Central Portal 中对应 namespace 仍为 `Verified`
2. 确认当前 `groupId` 是否具备发布权限
   - 当前 `groupId` 为 `io.github.mengxin896`
   - 需要与你已验证通过的 Central namespace 保持一致
3. 准备 Maven Central 发布凭据
   - `CENTRAL_TOKEN_USERNAME`
   - `CENTRAL_TOKEN_PASSWORD`
4. 准备 GPG 签名材料
   - 私钥
   - passphrase
5. 选择发布执行方式
   - GitHub Actions 自动发布
   - 或本地手动执行 `mvn -B -Pcentral-release -DskipTests deploy`

## 建议的发布前自查

1. 将 `.mvn/maven.config` 中的 `revision` 改成正式版，例如 `0.1.0`
2. 执行：

```bash
mvn -q -DskipTests package
mvn -q -Pcentral-release -DskipTests verify
```

3. 确认生成物包含：
   - 主 jar
   - `sources.jar`
   - `javadoc.jar`
4. 再检查一遍 README、接入文档、示例工程里的版本号和 Maven 坐标是否一致
5. 如果改过 `groupId`，记得同步检查：
   - `README.md`
   - `docs/integration-guide.md`
   - `docs/user-app-from-zero.md`
   - `docs/onebot11-napcat-guide.md`
   - `examples/*/pom.xml`
