package com.xin.aiagentbackend.conversation.service;

import com.xin.aiagentbackend.conversation.dto.ConversationDto;
import com.xin.aiagentbackend.conversation.dto.CreateConversationRequest;
import com.xin.aiagentbackend.conversation.dto.MessageDto;
import com.xin.aiagentbackend.conversation.dto.PageDto;
import com.xin.aiagentbackend.conversation.dto.UpdateConversationRequest;

import java.util.List;

/**
 * 会话业务服务接口（Service）。
 * <p>定义会话与消息相关的业务能力：创建、列表、详情、更新、删除，
 * 以及消息分页、最近消息读取和消息追加。具体实现见 {@code impl.ConversationServiceImpl}。</p>
 */
public interface ConversationService {

    /** 创建会话 */
    ConversationDto create(CreateConversationRequest request);

    /** 分页查询会话列表 */
    PageDto<ConversationDto> list(int page, int pageSize);

    /** 查询会话详情 */
    ConversationDto get(String id);

    /** 更新会话标题 */
    ConversationDto update(String id, UpdateConversationRequest request);

    /** 删除会话 */
    void delete(String id);

    /** 分页查询会话下的消息 */
    PageDto<MessageDto> messages(String id, int page, int pageSize);

    /** 查询会话最近 N 条消息 */
    List<MessageDto> recentMessages(String id, int limit);

    /** 追加一条用户消息 */
    void appendUserMessage(String id, String content);

    /** 追加一条助手消息 */
    void appendAssistantMessage(String id, String content);
}
