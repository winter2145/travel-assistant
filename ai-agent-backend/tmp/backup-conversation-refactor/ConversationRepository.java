package com.xin.aiagentbackend.conversation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ConversationRepository {
    private static final String OWNER = "local";
    private final JdbcTemplate jdbc;

    private final RowMapper<ConversationDto> conversationMapper = (rs, row) -> new ConversationDto(
            rs.getString("id"), rs.getString("title"), epoch(rs.getTimestamp("created_at")),
            epoch(rs.getTimestamp("updated_at")));
    private final RowMapper<MessageDto> messageMapper = (rs, row) -> new MessageDto(
            rs.getString("id"), rs.getString("role"), rs.getString("content"), epoch(rs.getTimestamp("created_at")));

    public ConversationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public ConversationDto create(String title) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO travel_conversation(id, owner_id, title) VALUES (?, ?, ?)", id, OWNER, title);
        return get(id).orElseThrow();
    }

    public Optional<ConversationDto> get(String id) {
        return jdbc.query("SELECT id,title,created_at,updated_at FROM travel_conversation WHERE id=? AND owner_id=?",
                conversationMapper, id, OWNER).stream().findFirst();
    }

    public List<ConversationDto> list(int limit, int offset) {
        return jdbc.query("SELECT id,title,created_at,updated_at FROM travel_conversation WHERE owner_id=? ORDER BY updated_at DESC LIMIT ? OFFSET ?",
                conversationMapper, OWNER, limit, offset);
    }

    public long count() {
        Long result = jdbc.queryForObject("SELECT count(*) FROM travel_conversation WHERE owner_id=?", Long.class, OWNER);
        return result == null ? 0 : result;
    }

    public boolean updateTitle(String id, String title) {
        return jdbc.update("UPDATE travel_conversation SET title=?, updated_at=CURRENT_TIMESTAMP WHERE id=? AND owner_id=?",
                title, id, OWNER) == 1;
    }

    public boolean delete(String id) {
        return jdbc.update("DELETE FROM travel_conversation WHERE id=? AND owner_id=?", id, OWNER) == 1;
    }

    public void appendMessage(String conversationId, String role, String content) {
        jdbc.update("INSERT INTO travel_message(id,conversation_id,role,content) VALUES (?,?,?,?)",
                UUID.randomUUID().toString(), conversationId, role, content);
        if ("user".equals(role)) {
            Integer count = jdbc.queryForObject("SELECT count(*) FROM travel_message WHERE conversation_id=? AND role='user'",
                    Integer.class, conversationId);
            String title = compactTitle(content);
            if (count != null && count == 1) {
                jdbc.update("UPDATE travel_conversation SET title=?, updated_at=CURRENT_TIMESTAMP WHERE id=? AND owner_id=?",
                        title, conversationId, OWNER);
                return;
            }
        }
        jdbc.update("UPDATE travel_conversation SET updated_at=CURRENT_TIMESTAMP WHERE id=? AND owner_id=?",
                conversationId, OWNER);
    }

    public List<MessageDto> messages(String conversationId, int limit, int offset) {
        return jdbc.query("SELECT id,role,content,created_at FROM travel_message WHERE conversation_id=? ORDER BY created_at,id LIMIT ? OFFSET ?",
                messageMapper, conversationId, limit, offset);
    }

    public long messageCount(String conversationId) {
        Long result = jdbc.queryForObject("SELECT count(*) FROM travel_message WHERE conversation_id=?", Long.class, conversationId);
        return result == null ? 0 : result;
    }

    public List<MessageDto> recentMessages(String conversationId, int limit) {
        List<MessageDto> result = jdbc.query("SELECT id,role,content,created_at FROM (SELECT id,role,content,created_at FROM travel_message WHERE conversation_id=? ORDER BY created_at DESC,id DESC LIMIT ?) recent ORDER BY created_at,id",
                messageMapper, conversationId, limit);
        return result;
    }

    private static String compactTitle(String content) {
        String title = content == null ? "新旅行计划" : content.strip().replaceAll("\\s+", " ");
        return title.length() <= 50 ? title : title.substring(0, 49) + "…";
    }

    private static long epoch(Timestamp timestamp) { return timestamp == null ? 0 : timestamp.getTime(); }
}
