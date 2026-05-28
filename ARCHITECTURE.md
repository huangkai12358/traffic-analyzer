# 项目架构说明

## 项目定位

这是一个 Spring Boot CLI 项目，用来处理 GoReplay `.gor` 流量文件。它没有 Web 层，入口是命令行，核心能力是：

```text
.gor 文件 -> 解析请求 -> 分类 -> MySQL 入库 -> 统计 / 导出 / 拆分
```

## 整体分层

当前代码按职责分成这些包：

```text
com.example.gor
  cli          命令行入口
  service      业务编排
  parser       .gor 和 HTTP 请求解析
  classifier   请求分类规则
  mapper       MyBatis 数据库访问
  entity       数据库实体和枚举
  config       Spring 配置
```

核心依赖方向是：

```text
cli -> service -> parser / classifier / mapper
service -> entity
mapper -> entity
```

`cli` 不直接解析文件、不直接写数据库；`service` 不直接写 SQL；`mapper` 只负责数据库访问。

## 启动流程

入口文件：

```text
GorTrafficAnalyzerApplication.java
```

它启动 Spring Boot，然后把命令行参数交给 Picocli：

```text
main()
  -> SpringApplication.run()
  -> run(String... args)
  -> new CommandLine(GorCommand).execute(args)
```

`GorCommand` 是根命令，注册了 4 个子命令：

```text
import
clear
stats
export
split
```

例如执行：

```bash
./gor import sample.gor
```

也支持一次导入多个文件：

```bash
./gor import a.gor b.gor c.gor --clear
```

或导入目录下所有 `.gor` 文件：

```bash
./gor import --dir ./captures --clear
```

实际调用链是：

```text
GorTrafficAnalyzerApplication
  -> GorCommand
  -> ImportCommand
  -> ImportService
```

## 导入功能

命令：

```bash
./gor import sample.gor
```

调用链：

```text
ImportCommand
  -> ImportService.importFile() / importFiles() / importDirectory()
    -> GorRequestReader.readRequests()
    -> GorHttpParser.parse()
    -> TrafficClassifier.classify()
    -> TrafficRequestMapper.insert()
```

各文件职责：

```text
ImportCommand.java
```

只负责接收 CLI 参数、调用导入服务、输出成功或失败信息。

```text
ImportService.java
```

负责完整导入流程编排：

1. 校验输入文件存在
2. 流式读取 `.gor`
3. 解析 HTTP 请求字段
4. 分类
5. 组装 `TrafficRequest`
6. 调用 MyBatis Mapper 入库

```text
GorRequestReader.java
```

负责把 `.gor` 文件按 GoReplay 消息边界切成一条条请求。它是流式读取，不会一次性把整个文件读入内存。

输出对象是：

```text
GorRecord.java
```

里面有：

```text
requestNo
rawText
```

`rawText` 是原始 GoReplay 文本，后续导出必须用它。

```text
GorHttpParser.java
```

负责从单条 raw gor 请求中解析 HTTP 字段：

```text
method
host
url
path
query
headers
body
contentType
userAgent
```

解析结果放到：

```text
ParsedHttpRequest.java
```

```text
TrafficClassifier.java
```

负责根据 path、query、headers、body 打标签和主分类。

输出：

```text
ClassificationResult.java
```

包含：

```text
category
tags
riskLevel
```

```text
TrafficRequestMapper.java
```

负责把 `TrafficRequest` 插入 MySQL。

## 数据库模型

数据库实体：

```text
TrafficRequest.java
```

对应表：

```text
traffic_request
```

建表文件：

```text
src/main/resources/schema.sql
```

关键字段：

```text
source_file
request_no
method
host
url
path
query_string
headers_json
body
raw_gor_text
category
tags
risk_level
created_at
```

其中最重要的是：

```text
raw_gor_text
```

因为导出时不能用解析后的字段重新拼 HTTP 请求，否则可能破坏 GoReplay 回放格式。

## 统计功能

## 清空功能

命令：

```bash
./gor clear
```

调用链：

```text
ClearCommand
  -> ClearService.clearRequests()
    -> TrafficRequestMapper.deleteAll()
```

该命令用于清空 `traffic_request` 表，适合在处理新 `.gor` 文件前执行。`import` 命令也支持 `--clear` 参数：

```bash
./gor import input.gor --clear
```

它会在同一导入流程中先清空旧数据，再导入当前文件。

## 统计功能

命令：

```bash
./gor stats
```

调用链：

```text
StatsCommand
  -> StatsService.snapshot()
    -> TrafficRequestMapper.count()
    -> TrafficRequestMapper.countGroupedByCategory()
    -> TrafficRequestMapper.countByRiskLevel(HIGH)
    -> TrafficRequestMapper.topHosts(10)
    -> TrafficRequestMapper.topPaths(10)
```

职责：

```text
StatsCommand.java
```

负责把统计结果打印成终端文本。

```text
StatsService.java
```

负责组织统计数据。

```text
TrafficRequestMapper.java
```

执行具体 SQL 聚合查询。

## 导出功能

命令：

```bash
./gor export --category login --output login.gor
./gor export --risk high --output high-risk.gor
```

调用链：

```text
ExportCommand
  -> ExportService.exportByCategory()
    -> TrafficRequestMapper.findByCategoryOrderByIdAsc()
    -> ExportService.export()
```

或：

```text
ExportCommand
  -> ExportService.exportByRisk()
    -> TrafficRequestMapper.findByRiskLevelOrderByIdAsc()
    -> ExportService.export()
```

关键点：

```text
ExportService.java
```

使用 MyBatis `Cursor<TrafficRequest>` 流式读取数据库结果，避免一次性加载大量请求。

导出时写的是：

```java
request.getRawGorText()
```

不是重新拼接 method/path/header/body。

## 拆分功能

命令：

```bash
./gor split --input sample.gor --max-count 1000 --output-dir parts
./gor split --input sample.gor --max-size-mb 50 --output-dir parts
```

调用链：

```text
SplitCommand
  -> SplitService.splitByMaxCount()
    -> GorRequestReader.readRequests()
    -> SplitWriter.writeByCount()
```

或：

```text
SplitCommand
  -> SplitService.splitByMaxSizeMb()
    -> GorRequestReader.readRequests()
    -> SplitWriter.writeBySize()
```

拆分功能不依赖数据库。它直接读取原始 `.gor` 文件，并把 raw record 写入多个 part 文件。

## 配置

```text
application.yml
```

主配置，包含：

```text
MySQL 连接
MyBatis 下划线转驼峰
SQL 初始化
日志级别
```

数据库账号密码从环境变量读取：

```yaml
username: ${DB_USERNAME}
password: ${DB_PASSWORD}
```

```text
application-test.yml
```

测试配置，使用 H2 内存库，避免测试依赖本机 MySQL。

```text
JacksonConfig.java
```

手动提供 `ObjectMapper`，用于把 headers map 转成 JSON 字符串存入 `headers_json`。

## 测试结构

当前测试覆盖：

```text
GorRequestReaderTest      .gor 请求切分
GorHttpParserTest         HTTP method/path/header/body 解析
TrafficClassifierTest     分类规则
ExportServiceTest         按分类导出
SplitServiceTest          按数量拆分
```

测试运行：

```bash
mvn test
```

## 端到端流程

以导入后导出为例：

```text
1. 用户执行 ./gor import sample.gor
2. ImportCommand 接收命令
3. ImportService 读取文件
4. GorRequestReader 切分请求
5. GorHttpParser 解析 HTTP 字段
6. TrafficClassifier 分类
7. TrafficRequestMapper 写入 MySQL
8. 用户执行 ./gor export --category login --output login.gor
9. ExportService 从 MySQL 查 login 请求
10. 逐条写 raw_gor_text 到 login.gor
```

整个系统的核心设计是：

```text
解析字段用于查询、分类、统计
raw_gor_text 用于无损导出和回放
MyBatis Mapper 负责数据库访问
Service 负责业务流程编排
CLI 只负责命令参数和结果输出
```
