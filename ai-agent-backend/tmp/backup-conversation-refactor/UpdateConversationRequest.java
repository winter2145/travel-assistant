package com.xin.aiagentbackend.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateConversationRequest(@NotBlank @Size(max = 200) String title) { }
