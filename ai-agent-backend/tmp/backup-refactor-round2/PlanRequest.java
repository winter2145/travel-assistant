package com.xin.aiagentbackend.plan;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanRequest(
        String conversationId,
        @NotBlank @Size(max = 200) String title,
        @NotNull JsonNode itinerary,
        JsonNode budgetSummary) { }
