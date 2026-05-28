package com.example.gor.mapper;

import com.example.gor.entity.RiskLevel;
import com.example.gor.entity.TrafficRequest;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.cursor.Cursor;

/**
 * traffic_request 表的数据访问 Mapper。
 *
 * <p>所有数据库读写都集中在该 Mapper 中，Service 层不直接编写 SQL。
 * 大批量导出方法返回 MyBatis Cursor，避免一次性把所有 rawGorText 加载到内存。</p>
 */
@Mapper
public interface TrafficRequestMapper {
    /**
     * 插入一条已解析、已分类的请求记录。
     *
     * @param request 待保存的请求实体；保存后会回填自增 id
     */
    @Insert("""
            INSERT INTO traffic_request (
                source_file, request_no, method, host, url, path, query_string,
                headers_json, body, raw_gor_text, category, tags, risk_level,
                created_at, content_type, user_agent
            ) VALUES (
                #{sourceFile}, #{requestNo}, #{method}, #{host}, #{url}, #{path}, #{queryString},
                #{headersJson}, #{body}, #{rawGorText}, #{category}, #{tags}, #{riskLevel},
                #{createdAt}, #{contentType}, #{userAgent}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(TrafficRequest request);

    /**
     * 查询请求总数。
     *
     * @return traffic_request 表中的总记录数
     */
    @Select("SELECT COUNT(*) FROM traffic_request")
    long count();

    /**
     * 查询指定主分类的请求数量。
     *
     * @param category 主分类
     * @return 命中该分类的请求数量
     */
    @Select("SELECT COUNT(*) FROM traffic_request WHERE category = #{category}")
    long countByCategory(@Param("category") String category);

    /**
     * 查询指定风险等级的请求数量。
     *
     * @param riskLevel 风险等级
     * @return 命中该风险等级的请求数量
     */
    @Select("SELECT COUNT(*) FROM traffic_request WHERE risk_level = #{riskLevel}")
    long countByRiskLevel(@Param("riskLevel") RiskLevel riskLevel);

    /**
     * 按主分类聚合请求数量。
     *
     * @return 分类名称和数量列表，按数量倒序
     */
    @Select("SELECT category AS name, COUNT(*) AS count_value FROM traffic_request GROUP BY category ORDER BY count_value DESC")
    List<CountRow> countGroupedByCategory();

    /**
     * 查询请求量最高的 Host。
     *
     * @param limit 返回数量上限
     * @return Host 名称和请求数量列表
     */
    @Select("SELECT host AS name, COUNT(*) AS count_value FROM traffic_request WHERE host IS NOT NULL GROUP BY host ORDER BY count_value DESC LIMIT #{limit}")
    List<CountRow> topHosts(@Param("limit") int limit);

    /**
     * 查询请求量最高的 Path。
     *
     * @param limit 返回数量上限
     * @return Path 名称和请求数量列表
     */
    @Select("SELECT path AS name, COUNT(*) AS count_value FROM traffic_request WHERE path IS NOT NULL GROUP BY path ORDER BY count_value DESC LIMIT #{limit}")
    List<CountRow> topPaths(@Param("limit") int limit);

    /**
     * 按主分类流式查询请求，用于导出。
     *
     * @param category 主分类
     * @return MyBatis Cursor，调用方必须在事务内关闭
     */
    @Select("SELECT * FROM traffic_request WHERE category = #{category} ORDER BY id ASC")
    Cursor<TrafficRequest> findByCategoryOrderByIdAsc(@Param("category") String category);

    /**
     * 按风险等级流式查询请求，用于导出。
     *
     * @param riskLevel 风险等级
     * @return MyBatis Cursor，调用方必须在事务内关闭
     */
    @Select("SELECT * FROM traffic_request WHERE risk_level = #{riskLevel} ORDER BY id ASC")
    Cursor<TrafficRequest> findByRiskLevelOrderByIdAsc(@Param("riskLevel") RiskLevel riskLevel);

    /**
     * 删除全部请求记录。
     *
     * <p>当前只在测试中使用，避免测试用例之间数据互相污染。</p>
     */
    @Delete("DELETE FROM traffic_request")
    void deleteAll();

    /**
     * 聚合查询返回行。
     *
     * @param name       聚合维度值，例如 category、host 或 path
     * @param countValue 该维度对应的请求数量
     */
    record CountRow(String name, long countValue) {
    }
}
