package com.xin.aiagentbackend.conversation;

public record MessageDto(String id, String role, String content, long createdAt) { }
