package com.xin.aiagentbackend.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SessionChatRequest(
        @NotBlank @Size(max = 8000) String message,
        @NotBlank String conversationId) { }
