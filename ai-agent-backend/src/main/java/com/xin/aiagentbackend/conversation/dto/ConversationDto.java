package com.xin.aiagentbackend.conversation.dto;

/**
 * 会话数据传输对象（DTO）。
 * <p>用于会话创建、列表、详情、更新等接口的响应返回，
 * 包含会话唯一标识、标题以及创建/更新时间。</p>
 *
 * @param id        会话唯一标识
 * @param title     会话标题
 * @param createdAt 创建时间（毫秒时间戳）
 * @param updatedAt 最近更新时间（毫秒时间戳）
 */
public record ConversationDto(String id, String title, long createdAt, long updatedAt) { }
