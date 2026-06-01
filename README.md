# GoReplay .gor 流量处理工具

Java 21 + Spring Boot 3 + Maven CLI/MVP，用于导入、解析、分类、统计、导出和拆分 GoReplay `.gor` 文件。

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
./gor import sample.gor --clear
./gor import a.gor b.gor --clear
./gor import --dir ./captures --clear
./gor clear
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
mvn spring-boot:run -Dspring-boot.run.arguments="import sample.gor --clear"
mvn spring-boot:run -Dspring-boot.run.arguments="import a.gor b.gor --clear"
mvn spring-boot:run -Dspring-boot.run.arguments="import --dir ./captures --clear"
mvn spring-boot:run -Dspring-boot.run.arguments="clear"
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

推荐每次处理新文件时使用：

```bash
./gor import input.gor --clear
```

这样会先清空之前导入的请求，避免 `stats` 和 `export` 混入历史数据。

批量导入多个文件：

```bash
./gor import a.gor b.gor c.gor --clear
```

导入目录下所有 `.gor` 文件：

```bash
./gor import --dir ./captures --clear
```

## 数据表

核心表：`traffic_request`

包含 `source_file`、`request_no`、`method`、`host`、`url`、`path`、`query_string`、`headers_json`、`body`、`raw_gor_text`、`category`、`tags`、`risk_level`、`created_at` 等字段。

导出使用 `raw_gor_text`，保证 GoReplay 原始文本尽量无损保留。

详细数据库设计见 [DATABASE.md](DATABASE.md)。

## sample.gor 里的三猴子分隔符

`🐵🙈🙉` 是 GoReplay `.gor` 文件中用于分隔 payload 的标记。本工具按该分隔符流式切分请求，并保存每条请求的 `raw_gor_text`，导出和拆分时直接写回原始文本。

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

分类规则基于 `path`、`query`、`headers`、`body`，并会同时检查 URL 解码后的文本：

| 分类 | 主要特征 |
| --- | --- |
| `static_resource` | 静态资源后缀：`.js`、`.css`、`.png`、`.jpg`、`.svg`、`.ico`、`.woff` 等 |
| `login` | `login`、`auth`、`sso`、`token`、`oauth` |
| `api` | `/api/` 开头，或 path 不像文件路径，且未命中攻击规则 |
| `upload` | `upload`、`import`、`file` |
| `download` | `download`、`export` |
| `sql_injection` | `union select`、`or 1=1`、`sleep(`、`benchmark(` |
| `xss` | `<script`、`onerror=`、`javascript:` |
| `path_traversal` | `../`、`..\\`、`/etc/passwd` |
| `command_injection` | `whoami`、独立 `id`、`cmd=`、`bash`、`curl` |
| `unknown` | 没有命中以上规则 |

## 测试

```bash
mvn test
```
