# 数据库设计文档

## 1. 设计目标

本项目当前是 CLI/MVP，数据库只负责保存已导入的 GoReplay 请求记录，支撑后续统计和导出。

核心设计原则：

- 保存结构化字段，方便按分类、风险、Host、Path 做统计和筛选。
- 保存 `raw_gor_text` 原始文本，导出时直接写回，避免重新拼接 HTTP 请求导致 GoReplay 回放格式损坏。
- 当前不保存长期历史任务，推荐每次处理新文件前使用 `./gor import input.gor --clear` 清空旧数据。

## 2. 数据库

默认数据库：

```text
traffic_analyzer
```

默认连接配置在：

```text
src/main/resources/application.yml
```

账号密码通过环境变量读取：

```bash
export DB_USERNAME=root
export DB_PASSWORD=你的数据库密码
```

建表脚本：

```text
src/main/resources/schema.sql
```

Spring Boot 启动时会自动执行 `schema.sql`，创建缺失的数据表。

## 3. 表设计

### 3.1 traffic_request

`traffic_request` 是当前唯一核心业务表，用于保存每一条已解析的 GoReplay 请求。

一条 `.gor` 请求记录对应一行 `traffic_request`。

### 3.2 建表 SQL

```sql
CREATE TABLE IF NOT EXISTS traffic_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_file VARCHAR(1024) NOT NULL,
    request_no BIGINT NOT NULL,
    method VARCHAR(16),
    host VARCHAR(512),
    url VARCHAR(2048),
    path VARCHAR(512),
    query_string VARCHAR(2048),
    headers_json LONGTEXT,
    body LONGTEXT,
    raw_gor_text LONGTEXT NOT NULL,
    category VARCHAR(64),
    tags VARCHAR(512),
    risk_level VARCHAR(16),
    created_at TIMESTAMP NOT NULL,
    content_type VARCHAR(255),
    user_agent VARCHAR(1024),
    INDEX idx_traffic_category (category),
    INDEX idx_traffic_risk (risk_level),
    INDEX idx_traffic_host (host),
    INDEX idx_traffic_path (path)
);
```

### 3.3 字段说明

| 字段 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| `id` | `BIGINT` | 是 | 自增主键，用于唯一标识一条请求记录 |
| `source_file` | `VARCHAR(1024)` | 是 | 来源 `.gor` 文件的绝对路径 |
| `request_no` | `BIGINT` | 是 | 请求在来源文件内的序号，自增 |
| `method` | `VARCHAR(16)` | 否 | HTTP 方法，例如 `GET`、`POST`、`PUT` |
| `host` | `VARCHAR(512)` | 否 | HTTP `Host` header |
| `url` | `VARCHAR(2048)` | 否 | HTTP 请求行中的原始 URL，包含 path 和 query |
| `path` | `VARCHAR(512)` | 否 | URL path，不包含 query string |
| `query_string` | `VARCHAR(2048)` | 否 | 原始 query string，不包含 `?` |
| `headers_json` | `LONGTEXT` | 否 | HTTP headers 的 JSON 字符串，保留重复 header 值 |
| `body` | `LONGTEXT` | 否 | HTTP body 文本 |
| `raw_gor_text` | `LONGTEXT` | 是 | GoReplay 原始请求文本，用于无损导出和回放 |
| `category` | `VARCHAR(64)` | 否 | 主分类，一个请求只能有一个主分类 |
| `tags` | `VARCHAR(512)` | 否 | 所有命中的标签，当前使用英文逗号分隔 |
| `risk_level` | `VARCHAR(16)` | 否 | 风险等级，当前主要使用 `LOW`、`HIGH` |
| `created_at` | `TIMESTAMP` | 是 | 入库时间 |
| `content_type` | `VARCHAR(255)` | 否 | HTTP `Content-Type` header |
| `user_agent` | `VARCHAR(1024)` | 否 | HTTP `User-Agent` header |

## 4. 索引设计

当前索引围绕 CLI 的统计和导出查询设计。

| 索引 | 字段 | 用途 |
| --- | --- | --- |
| `PRIMARY KEY` | `id` | 唯一标识记录；导出时按 `id` 排序保证稳定输出 |
| `idx_traffic_category` | `category` | 支持按主分类统计和导出，例如 `export --category login` |
| `idx_traffic_risk` | `risk_level` | 支持按风险等级统计和导出，例如 `export --risk high` |
| `idx_traffic_host` | `host` | 支持 Top Host 聚合统计 |
| `idx_traffic_path` | `path` | 支持 Top Path 聚合统计 |

## 5. 数据写入流程

导入命令：

```bash
./gor import sample.gor --clear
```

数据流：

```text
.gor 文件
  -> GorRequestReader 按三猴子分隔符切分请求
  -> GorHttpParser 解析 method、host、url、path、query、headers、body
  -> TrafficClassifier 生成 category、tags、risk_level
  -> ImportService 组装 TrafficRequest
  -> TrafficRequestMapper.insert 写入 traffic_request
```

`--clear` 会在导入前执行：

```sql
DELETE FROM traffic_request;
```

这样可以避免本次统计和导出混入之前导入的数据。

## 6. 数据读取场景

### 6.1 stats

`stats` 使用聚合查询：

```sql
SELECT COUNT(*) FROM traffic_request;
SELECT category AS name, COUNT(*) AS count_value FROM traffic_request GROUP BY category ORDER BY count_value DESC;
SELECT COUNT(*) FROM traffic_request WHERE risk_level = ?;
SELECT host AS name, COUNT(*) AS count_value FROM traffic_request WHERE host IS NOT NULL GROUP BY host ORDER BY count_value DESC LIMIT ?;
SELECT path AS name, COUNT(*) AS count_value FROM traffic_request WHERE path IS NOT NULL GROUP BY path ORDER BY count_value DESC LIMIT ?;
```

### 6.2 export

按主分类导出：

```sql
SELECT * FROM traffic_request WHERE category = ? ORDER BY id ASC;
```

按风险等级导出：

```sql
SELECT * FROM traffic_request WHERE risk_level = ? ORDER BY id ASC;
```

导出时只写 `raw_gor_text`，不使用结构化字段重新拼接 HTTP 请求。

### 6.3 split

`split` 不访问数据库。

它直接读取原始 `.gor` 文件，按请求边界写出多个 part 文件。

## 7. MyBatis 映射

数据访问层：

```text
src/main/java/com/example/gor/mapper/TrafficRequestMapper.java
```

实体对象：

```text
src/main/java/com/example/gor/entity/TrafficRequest.java
```

项目开启了 MyBatis 下划线转驼峰：

```yaml
mybatis:
  configuration:
    map-underscore-to-camel-case: true
```

因此数据库字段 `source_file` 会自动映射到 Java 属性 `sourceFile`，`raw_gor_text` 会自动映射到 `rawGorText`。

## 8. 当前限制和后续建议

当前表结构满足 CLI/MVP 和演示需求，但如果后续要扩展成 Web 管理平台，建议考虑：

- 把 `url`、`query_string` 改为 `TEXT`，避免超长攻击 payload 插入失败。
- 把 `tags` 拆成单独的关联表，方便按任意 tag 查询。
- 增加 `import_batch` 或 `traffic_import_task` 表，用于保存导入任务、文件名、导入时间、导入数量和执行状态。
- 增加 `request_time` 字段，如果后续要基于 GoReplay 原始时间戳做时间范围查询。
- 增加 `(source_file, request_no)` 唯一索引，如果未来需要防止重复导入同一文件。
- 对 `raw_gor_text` 较大的场景评估压缩存储或对象存储方案。
