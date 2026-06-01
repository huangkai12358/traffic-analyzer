# 功能说明

## 程序简介

`traffic-analyzer` 是一个 GoReplay `.gor` 流量处理命令行工具，用于对录制流量进行导入、解析、分类、统计、导出和拆分。

它适合用于以下场景：

- 分析 GoReplay 录制的请求流量
- 把 `.gor` 请求结构化保存到 MySQL
- 识别登录、上传、下载、静态资源、普通 API 和常见攻击请求
- 按分类或风险等级重新导出可回放的 `.gor` 文件
- 把大 `.gor` 文件拆成多个小文件，方便分批处理

## 支持的命令

当前支持 5 个核心命令：

```text
import   导入 .gor 文件
clear    清空已导入请求
stats    查看已入库流量统计
export   按条件导出 .gor 文件
split    拆分原始 .gor 文件
```

可以通过以下命令查看帮助：

```bash
./gor --help
```

## 1. 导入 .gor 文件

命令：

```bash
./gor import sample.gor
```

功能：

- 流式读取 `.gor` 文件
- 按请求维度切分 GoReplay 记录
- 解析 HTTP 请求字段
- 执行规则分类
- 保存到 MySQL 的 `traffic_request` 表
- 保存原始 `raw_gor_text`，用于后续无损导出

推荐每次处理新文件时使用：

```bash
./gor import sample.gor --clear
```

这样会先清空历史导入记录，再导入当前文件，避免后续统计和导出混入旧数据。

也可以单独清空：

```bash
./gor clear
```

批量导入多个文件：

```bash
./gor import a.gor b.gor c.gor --clear
```

导入目录下所有 `.gor` 文件：

```bash
./gor import --dir ./captures --clear
```

目录导入只扫描该目录当前层级下的 `.gor` 文件，不递归扫描子目录。

导入后会保存的核心字段包括：

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
content_type
user_agent
```

## 2. 请求分类

导入时程序会自动根据请求内容进行分类。

分类依据包括：

```text
path
query
headers
body
```

当前支持的主分类：

```text
static_resource
login
api
upload
download
sql_injection
xss
path_traversal
command_injection
unknown
```

具体分类规则：

| 分类 | 判断特征 |
| --- | --- |
| `static_resource` | `path` 以静态资源后缀结尾：`.js`、`.css`、`.png`、`.jpg`、`.jpeg`、`.gif`、`.svg`、`.ico`、`.woff`、`.woff2`、`.ttf`、`.map` |
| `login` | `path`、`query` 或 `body` 包含认证相关关键词：`login`、`auth`、`sso`、`token`、`oauth`。这里不会因为 `Authorization` header 里有 token 就误判为登录 |
| `api` | `path` 以 `/api/` 开头，或者 `path` 不包含文件后缀；并且没有命中攻击类标签 |
| `upload` | `path`、`query`、`headers` 或 `body` 包含上传相关关键词：`upload`、`import`、`file` |
| `download` | `path`、`query`、`headers` 或 `body` 包含下载相关关键词：`download`、`export` |
| `sql_injection` | `path`、`query`、`headers` 或 `body` 包含 SQL 注入特征：`union select`、`or 1=1`、`sleep(`、`benchmark(` |
| `xss` | `path`、`query`、`headers` 或 `body` 包含 XSS 特征：`<script`、`onerror=`、`javascript:` |
| `path_traversal` | `path`、`query`、`headers` 或 `body` 包含路径穿越特征：`../`、`..\\`、`/etc/passwd` |
| `command_injection` | `path`、`query`、`headers` 或 `body` 包含命令执行特征：`whoami`、独立的 `id`、`cmd=`、`bash`、`curl` |
| `unknown` | 没有命中以上任何规则 |

分类时会同时检查原始文本和 URL 解码后的文本，因此 `union%20select` 这类编码后的攻击特征也能被识别。

一个请求只能有一个主分类 `category`，但可以有多个标签 `tags`。

例如一个登录接口同时带有 SQL 注入特征，可能会得到：

```text
category = sql_injection
tags = login,sql_injection
risk_level = HIGH
```

攻击类分类会被标记为高风险：

```text
sql_injection
xss
path_traversal
command_injection
```

## 3. 查看统计

命令：

```bash
./gor stats
```

功能：

- 输出总请求数和高风险请求占比
- 输出分类分布表格
- 输出 Top Host 表格
- 输出 Top Path 表格
- 每个聚合项都会显示数量和占比

示例输出：

```text
GoReplay Traffic Statistics
========================================
Total Requests                   12
High Risk Requests                5  (41.7%)

Category Distribution
----------------------------------------
Category                        Count  Percent
api                                 2    16.7%
command_injection                   2    16.7%
static_resource                     2    16.7%
download                            1     8.3%
login                               1     8.3%
path_traversal                      1     8.3%
sql_injection                       1     8.3%
upload                              1     8.3%
xss                                 1     8.3%

Top Hosts
----------------------------------------
Host                            Count  Percent
demo.example.com                   10    83.3%
static.example.com                  2    16.7%

Top Paths
----------------------------------------
Path                            Count  Percent
/health                             1     8.3%
/assets/logo.svg                    1     8.3%
/assets/app.js                      1     8.3%
/api/users/page                     1     8.3%
/api/tools/ping                     1     8.3%
```

## 4. 按条件导出 .gor 文件

导出功能基于已入库的数据。

导出时程序不会重新拼接 HTTP 请求，而是直接写入数据库里的 `raw_gor_text`，尽量保证导出的 `.gor` 文件仍可被 GoReplay 回放。

### 按主分类导出

命令：

```bash
./gor export --category login --output login.gor
```

示例：

```bash
./gor export --category sql_injection --output sql-injection.gor
./gor export --category upload --output upload.gor
./gor export --category api --output api.gor
```

### 按风险等级导出

命令：

```bash
./gor export --risk high --output high-risk.gor
```

支持的风险等级：

```text
low
medium
high
```

注意：

```text
--category 和 --risk 必须二选一，不能同时使用。
```

## 5. 拆分 .gor 文件

拆分功能直接处理原始 `.gor` 文件，不要求文件已经导入数据库。

它只负责把大文件按请求边界拆成多个小文件，不解析 HTTP、不分类、不入库。

### 按请求数量拆分

命令：

```bash
./gor split --input input.gor --max-count 1000 --output-dir parts
```

含义：

```text
每个输出文件最多包含 1000 条请求。
```

输出示例：

```text
parts/part-00001.gor
parts/part-00002.gor
parts/part-00003.gor
```

### 按文件大小拆分

命令：

```bash
./gor split --input input.gor --max-size-mb 50 --output-dir parts
```

含义：

```text
每个输出文件尽量不超过 50 MiB。
```

如果单条请求本身超过最大大小限制，程序会把这条请求单独写入一个 part 文件，不会截断请求。

## 6. 数据库

程序默认使用 MySQL。

数据库连接配置在：

```text
src/main/resources/application.yml
```

账号密码从环境变量读取：

```bash
export DB_USERNAME=root
export DB_PASSWORD=你的数据库密码
```

默认连接：

```text
jdbc:mysql://localhost:3306/traffic_analyzer
```

启动时会自动执行：

```text
src/main/resources/schema.sql
```

用于创建 `traffic_request` 表。

## 7. 运行方式

推荐使用项目根目录下的短命令脚本：

```bash
./gor stats
```

如果项目还没有打包，脚本会自动使用 Maven 启动。

如果已经打包：

```bash
mvn package
```

脚本会优先使用 jar 启动：

```text
target/traffic-analyzer-0.0.1-SNAPSHOT.jar
```

也可以直接运行 jar：

```bash
java -jar target/traffic-analyzer-0.0.1-SNAPSHOT.jar stats
```

## 8. 当前限制

当前版本是 CLI/MVP，存在以下限制：

- 还没有 Web 管理页面
- 分类规则是代码内置规则，暂不支持数据库或配置文件动态维护
- 当前按 GoReplay `.gor` 的三猴子分隔符切分请求
- 导出条件当前只支持 category 和 risk
- 统计维度当前只支持总量、分类、攻击数、Top Host、Top Path

## 9. 后续可扩展方向

后续可以继续扩展：

- Web 管理平台
- 导入任务进度和任务历史
- 自定义分类规则
- 更多导出筛选条件，例如 host、path、method、时间范围
- 更细粒度的风险等级
- MySQL 集成测试
