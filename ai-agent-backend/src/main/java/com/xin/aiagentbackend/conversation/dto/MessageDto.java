package com.xin.aiagentbackend.conversation.dto;

/**
 * 会话消息数据传输对象（DTO）。
 * <p>表示会话中的一条消息记录，用于消息列表、最近消息等接口的响应返回。</p>
 *
 * @param id        消息唯一标识
 * @param role      消息角色（user / assistant）
 * @param content   消息内容
 * @param createdAt 创建时间（毫秒时间戳）
 */
public record MessageDto(String id, String role, String content, long createdAt) { }
