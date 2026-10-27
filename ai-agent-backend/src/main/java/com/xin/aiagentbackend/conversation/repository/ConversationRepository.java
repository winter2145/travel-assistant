package com.xin.aiagentbackend.conversation.repository;

import com.xin.aiagentbackend.conversation.dto.ConversationDto;
import com.xin.aiagentbackend.conversation.dto.MessageDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 会话数据访问仓库（Repository）。
 * <p>基于 JdbcTemplate 直接访问数据库中的会话表（travel_conversation）与消息表（travel_message），
 * 负责会话的增删改查以及消息的读写。当前数据仅归属本地用户（OWNER）。</p>
 */
@Repository
public class ConversationRepository {
    /** 当前数据归属的本地用户标识 */
    private static final String OWNER = "local";
    private final JdbcTemplate jdbc;

    /** 会话记录行映射器：将数据库行转换为会话 DTO */
    private final RowMapper<ConversationDto> conversationMapper = (rs, row) -> new ConversationDto(
            rs.getString("id"), rs.getString("title"), epoch(rs.getTimestamp("created_at")),
            epoch(rs.getTimestamp("updated_at")));
    /** 消息记录行映射器：将数据库行转换为消息 DTO */
    private final RowMapper<MessageDto> messageMapper = (rs, row) -> new MessageDto(
            rs.getString("id"), rs.getString("role"), rs.getString("content"), epoch(rs.getTimestamp("created_at")));

    public ConversationRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    /** 创建会话：生成唯一 ID 并写入数据库，随后返回完整会话记录 */
    public ConversationDto create(String title) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO travel_conversation(id, owner_id, title) VALUES (?, ?, ?)", id, OWNER, title);
        return get(id).orElseThrow();
    }

    /** 按 ID 查询会话记录（仅限当前归属用户） */
    public Optional<ConversationDto> get(String id) {
        return jdbc.query("SELECT id,title,created_at,updated_at FROM travel_conversation WHERE id=? AND owner_id=?",
                conversationMapper, id, OWNER).stream().findFirst();
    }

    /** 分页查询会话列表，按最近更新时间倒序 */
    public List<ConversationDto> list(int limit, int offset) {
        return jdbc.query("SELECT id,title,created_at,updated_at FROM travel_conversation WHERE owner_id=? ORDER BY updated_at DESC LIMIT ? OFFSET ?",
                conversationMapper, OWNER, limit, offset);
    }

    /** 统计当前归属用户的会话总数 */
    public long count() {
        Long result = jdbc.queryForObject("SELECT count(*) FROM travel_conversation WHERE owner_id=?", Long.class, OWNER);
        return result == null ? 0 : result;
    }

    /** 更新会话标题，返回是否更新成功（影响行数为 1 视为成功） */
    public boolean updateTitle(String id, String title) {
        return jdbc.update("UPDATE travel_conversation SET title=?, updated_at=CURRENT_TIMESTAMP WHERE id=? AND owner_id=?",
                title, id, OWNER) == 1;
    }

    /** 删除会话，返回是否删除成功 */
    public boolean delete(String id) {
        return jdbc.update("DELETE FROM travel_conversation WHERE id=? AND owner_id=?", id, OWNER) == 1;
    }

    /**
     * 向指定会话追加一条消息。
     * <p>若是该会话的第一条用户消息，则自动以消息内容生成会话标题；同时刷新会话的更新时间。</p>
     *
     * @param conversationId 会话唯一标识
     * @param role           消息角色（user / assistant）
     * @param content        消息内容
     */
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

    /** 分页查询会话下的消息列表，按创建时间与 ID 升序排列 */
    public List<MessageDto> messages(String conversationId, int limit, int offset) {
        return jdbc.query("SELECT id,role,content,created_at FROM travel_message WHERE conversation_id=? ORDER BY created_at,id LIMIT ? OFFSET ?",
                messageMapper, conversationId, limit, offset);
    }

    /** 统计指定会话下的消息总数 */
    public long messageCount(String conversationId) {
        Long result = jdbc.queryForObject("SELECT count(*) FROM travel_message WHERE conversation_id=?", Long.class, conversationId);
        return result == null ? 0 : result;
    }

    /** 查询指定会话最近 N 条消息（按时间倒序截取后再按正序返回） */
    public List<MessageDto> recentMessages(String conversationId, int limit) {
        List<MessageDto> result = jdbc.query("SELECT id,role,content,created_at FROM (SELECT id,role,content,created_at FROM travel_message WHERE conversation_id=? ORDER BY created_at DESC,id DESC LIMIT ?) recent ORDER BY created_at,id",
                messageMapper, conversationId, limit);
        return result;
    }

    /** 将消息内容压缩为会话标题：去除多余空白，超过 50 字截断并追加省略号 */
    private static String compactTitle(String content) {
        String title = content == null ? "新旅行计划" : content.strip().replaceAll("\\s+", " ");
        return title.length() <= 50 ? title : title.substring(0, 49) + "…";
    }

    /** 数据库时间戳转毫秒时间戳，空值返回 0 */
    private static long epoch(Timestamp timestamp) { return timestamp == null ? 0 : timestamp.getTime(); }
}
