# GoReplay .gor 流量处理工具

Java 21 + Spring Boot 3 + Maven CLI/MVP，用于导入、解析、分类、统计和导出 GoReplay `.gor` 文件。

## 运行要求

- JDK 21
- Maven 3.9+
- MySQL 8+

默认连接本机 MySQL：

```bash
url: jdbc:mysql://localhost:3306/traffic_analyzer?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC
username: ${DB_USERNAME}
password: ${DB_PASSWORD}
```

运行前设置环境变量：

```bash
export DB_USERNAME=root
export DB_PASSWORD=你的数据库密码
```

启动时会通过 `schema.sql` 自动创建 `traffic_request` 表。

## 常用命令

推荐使用项目根目录下的短命令脚本：

```bash
./gor import sample.gor
./gor stats
./gor export --category login --output login.gor
./gor export --risk high --output high-risk.gor
./gor split --input sample.gor --max-count 1000 --output-dir parts
./gor split --input sample.gor --max-size-mb 50 --output-dir parts
```

如果希望在当前机器上直接输入 `gor`，可以把项目目录加入 `PATH`：

```bash
export PATH="$PWD:$PATH"
gor stats
```

底层仍然支持 Maven 方式：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="import sample.gor"
mvn spring-boot:run -Dspring-boot.run.arguments="stats"
mvn spring-boot:run -Dspring-boot.run.arguments="export --category login --output login.gor"
mvn spring-boot:run -Dspring-boot.run.arguments="export --risk high --output high-risk.gor"
mvn spring-boot:run -Dspring-boot.run.arguments="split --input sample.gor --max-count 1000 --output-dir parts"
mvn spring-boot:run -Dspring-boot.run.arguments="split --input sample.gor --max-size-mb 50 --output-dir parts"
```

也可以先打包：

```bash
mvn package
java -jar target/traffic-analyzer-0.0.1-SNAPSHOT.jar import sample.gor
```

## 数据表

核心表：`traffic_request`

包含 `source_file`、`request_no`、`method`、`host`、`url`、`path`、`query_string`、`headers_json`、`body`、`raw_gor_text`、`category`、`tags`、`risk_level`、`created_at` 等字段。

导出使用 `raw_gor_text`，保证 GoReplay 原始文本尽量无损保留。

## sample.gor 里的三猴子分隔符

`🐵🙈🙉` 是老版本 GoReplay `.gor` 文件使用过的 payload 分隔符。当前 GoReplay 源码中的分隔符已经改为空行 `\n\n`，因此不同版本生成的 `.gor` 文件可能不同。本 MVP 先支持三猴子分隔符样例；后续可以把解析器扩展为同时兼容新版空行分隔格式。

## 分类

支持主分类和多 tag。疑似攻击类会优先成为主分类，并标记为 `HIGH` 风险：

- `static_resource`
- `login`
- `api`
- `upload`
- `download`
- `sql_injection`
- `xss`
- `path_traversal`
- `command_injection`
- `unknown`

## 测试

```bash
mvn test
```
