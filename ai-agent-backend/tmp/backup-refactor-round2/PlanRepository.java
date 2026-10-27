package com.xin.aiagentbackend.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.aiagentbackend.api.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PlanRepository {
    private static final String OWNER = "local";
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

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

    public PlanDto create(String conversationId, String title, JsonNode itinerary, JsonNode budgetSummary) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO travel_plan(id,owner_id,conversation_id,title,city,start_date,day_count,budget,pace,itinerary,budget_summary) " +
                        "VALUES (?,?,?,?,?,CAST(? AS date),?,?,?,CAST(? AS jsonb),CAST(? AS jsonb))",
                id, OWNER, conversationId, title, city(itinerary), dateParam(itinerary),
                itinerary.path("days").size(), itinerary.path("budget").asDouble(0),
                itinerary.path("pace").asText(""), write(itinerary), writeNullable(budgetSummary));
        return get(id).orElseThrow();
    }

    public Optional<PlanDto> get(String id) {
        return jdbc.query("SELECT * FROM travel_plan WHERE id=? AND owner_id=?", rowMapper, id, OWNER)
                .stream().findFirst();
    }

    public Optional<PlanDto> getByConversation(String conversationId) {
        return jdbc.query("SELECT * FROM travel_plan WHERE conversation_id=? AND owner_id=? ORDER BY updated_at DESC LIMIT 1",
                rowMapper, conversationId, OWNER).stream().findFirst();
    }

    public List<PlanDto> list(int limit, int offset, String keyword) {
        String query = keyword == null ? "" : keyword.strip();
        return jdbc.query("SELECT * FROM travel_plan WHERE owner_id=? AND (?='' OR city ILIKE ? OR title ILIKE ?) " +
                        "ORDER BY updated_at DESC LIMIT ? OFFSET ?", rowMapper, OWNER, query,
                "%" + query + "%", "%" + query + "%", limit, offset);
    }

    public long count(String keyword) {
        String query = keyword == null ? "" : keyword.strip();
        Long result = jdbc.queryForObject("SELECT count(*) FROM travel_plan WHERE owner_id=? AND (?='' OR city ILIKE ? OR title ILIKE ?)",
                Long.class, OWNER, query, "%" + query + "%", "%" + query + "%");
        return result == null ? 0 : result;
    }

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

    public boolean delete(String id) {
        return jdbc.update("DELETE FROM travel_plan WHERE id=? AND owner_id=?", id, OWNER) == 1;
    }

    private String city(JsonNode itinerary) { return itinerary.path("city").asText(); }
    private String dateParam(JsonNode itinerary) {
        String value = itinerary.path("startDate").asText("");
        return value.isBlank() ? null : LocalDate.parse(value).toString();
    }
    private String write(JsonNode value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception e) { throw ApiException.badRequest("行程内容无法解析"); }
    }
    private String writeNullable(JsonNode value) { return value == null || value.isNull() ? null : write(value); }
    private JsonNode json(String value) {
        try { return value == null ? null : mapper.readTree(value); }
        catch (Exception e) { throw new IllegalStateException("Stored plan JSON is invalid", e); }
    }
    private String date(Date date) { return date == null ? "" : date.toLocalDate().toString(); }
    private long epoch(Timestamp timestamp) { return timestamp == null ? 0 : timestamp.getTime(); }
}
