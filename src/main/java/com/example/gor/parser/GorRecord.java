package com.example.gor.parser;

/**
 * 表示从 .gor 文件中切分出来的一条 GoReplay 请求记录。
 *
 * @param requestNo 当前导入文件内的请求序号，从 1 开始递增
 * @param rawText   包含 GoReplay header、HTTP payload 和分隔符的原始文本
 */
public record GorRecord(long requestNo, String rawText) {
}
