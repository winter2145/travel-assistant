package com.xin.aiagentbackend.plan.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.aiagentbackend.api.ApiException;
import com.xin.aiagentbackend.plan.dto.PlanDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 旅行计划数据访问仓库（Repository）。
 * <p>基于 JdbcTemplate 直接访问数据库中的旅行计划表（travel_plan），
 * 负责旅行计划的增删改查、按会话查询以及关键词分页检索。当前数据仅归属本地用户（OWNER）。</p>
 */
@Repository
public class PlanRepository {
    /** 当前数据归属的本地用户标识 */
    private static final String OWNER = "local";
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    /** 计划记录行映射器：将数据库行转换为计划 DTO */
    private final RowMapper<PlanDto> rowMapper = (rs, row) -> new PlanDto(
            rs.getString("id"), rs.getString("conversation_id"), rs.getString("title"),
            rs.getString("city"), date(rs.getDate("start_date")), rs.getInt("day_count"),
            rs.getBigDecimal("budget").doubleValue(), rs.getString("pace"),
            json(rs.getString("itinerary")), json(rs.getString("budget_summary")),
            epoch(rs.getTimestamp("created_at")), epoch(rs.getTimestamp("updated_at")), rs.getInt("version"));

    public PlanRepository(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    /** 创建旅行计划：生成唯一 ID 并写入数据库，随后返回完整计划记录 */
    public PlanDto create(String conversationId, String title, JsonNode itinerary, JsonNode budgetSummary) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO travel_plan(id,owner_id,conversation_id,title,city,start_date,day_count,budget,pace,itinerary,budget_summary) " +
                        "VALUES (?,?,?,?,?,CAST(? AS date),?,?,?,CAST(? AS jsonb),CAST(? AS jsonb))",
                id, OWNER, conversationId, title, city(itinerary), dateParam(itinerary),
                itinerary.path("days").size(), itinerary.path("budget").asDouble(0),
                itinerary.path("pace").asText(""), write(itinerary), writeNullable(budgetSummary));
        return get(id).orElseThrow();
    }

    /** 按 ID 查询旅行计划（仅限当前归属用户） */
    public Optional<PlanDto> get(String id) {
        return jdbc.query("SELECT * FROM travel_plan WHERE id=? AND owner_id=?", rowMapper, id, OWNER)
                .stream().findFirst();
    }

    /** 按会话标识查询最近保存的旅行计划 */
    public Optional<PlanDto> getByConversation(String conversationId) {
        return jdbc.query("SELECT * FROM travel_plan WHERE conversation_id=? AND owner_id=? ORDER BY updated_at DESC LIMIT 1",
                rowMapper, conversationId, OWNER).stream().findFirst();
    }

    /** 分页查询旅行计划列表，支持按城市/标题关键词模糊检索，按最近更新时间倒序 */
    public List<PlanDto> list(int limit, int offset, String keyword) {
        String query = keyword == null ? "" : keyword.strip();
        return jdbc.query("SELECT * FROM travel_plan WHERE owner_id=? AND (?='' OR city ILIKE ? OR title ILIKE ?) " +
                        "ORDER BY updated_at DESC LIMIT ? OFFSET ?", rowMapper, OWNER, query,
                "%" + query + "%", "%" + query + "%", limit, offset);
    }

    /** 统计符合条件的旅行计划总数（支持关键词过滤） */
    public long count(String keyword) {
        String query = keyword == null ? "" : keyword.strip();
        Long result = jdbc.queryForObject("SELECT count(*) FROM travel_plan WHERE owner_id=? AND (?='' OR city ILIKE ? OR title ILIKE ?)",
                Long.class, OWNER, query, "%" + query + "%", "%" + query + "%");
        return result == null ? 0 : result;
    }

    /**
     * 更新旅行计划（乐观锁）。
     * <p>通过 version 字段做并发控制：期望版本与数据库版本不一致时更新影响行数为 0，
     * 此时若记录已不存在抛 404，否则抛冲突异常提示用户刷新。</p>
     */
    public PlanDto update(String id, String title, JsonNode itinerary, JsonNode budgetSummary, int expectedVersion) {
        int updated = jdbc.update("UPDATE travel_plan SET title=?,city=?,start_date=CAST(? AS date),day_count=?,budget=?,pace=?," +
                        "itinerary=CAST(? AS jsonb),budget_summary=CAST(? AS jsonb),version=version+1,updated_at=CURRENT_TIMESTAMP " +
                        "WHERE id=? AND owner_id=? AND version=?",
                title, city(itinerary), dateParam(itinerary), itinerary.path("days").size(), itinerary.path("budget").asDouble(0),
                itinerary.path("pace").asText(""), write(itinerary), writeNullable(budgetSummary), id, OWNER, expectedVersion);
        if (updated == 0) {
            if (get(id).isEmpty()) throw ApiException.notFound("旅行计划不存在");
            throw ApiException.conflict("这份行程已在其他位置更新，请刷新后再保存");
        }
        return get(id).orElseThrow();
    }

    /** 删除旅行计划，返回是否删除成功 */
    public boolean delete(String id) {
        return jdbc.update("DELETE FROM travel_plan WHERE id=? AND owner_id=?", id, OWNER) == 1;
    }

    /** 从行程 JSON 中读取目的地城市 */
    private String city(JsonNode itinerary) { return itinerary.path("city").asText(); }
    /** 从行程 JSON 中读取出发日期，空值返回 null */
    private String dateParam(JsonNode itinerary) {
        String value = itinerary.path("startDate").asText("");
        return value.isBlank() ? null : LocalDate.parse(value).toString();
    }
    /** 将 JSON 节点序列化为字符串，失败时抛出参数错误 */
    private String write(JsonNode value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception e) { throw ApiException.badRequest("行程内容无法解析"); }
    }
    /** 可空 JSON 序列化：null 节点返回 null */
    private String writeNullable(JsonNode value) { return value == null || value.isNull() ? null : write(value); }
    /** 将数据库字符串解析为 JSON 节点 */
    private JsonNode json(String value) {
        try { return value == null ? null : mapper.readTree(value); }
        catch (Exception e) { throw new IllegalStateException("Stored plan JSON is invalid", e); }
    }
    /** 数据库日期转字符串，空值返回空串 */
    private String date(Date date) { return date == null ? "" : date.toLocalDate().toString(); }
    /** 数据库时间戳转毫秒时间戳，空值返回 0 */
    private long epoch(Timestamp timestamp) { return timestamp == null ? 0 : timestamp.getTime(); }
}
