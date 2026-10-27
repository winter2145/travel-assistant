package com.xin.aiagentbackend.plan;

import com.fasterxml.jackson.databind.JsonNode;

public record PlanDto(
        String id,
        String conversationId,
        String title,
        String city,
        String startDate,
        int days,
        double budget,
        String pace,
        JsonNode itinerary,
        JsonNode budgetSummary,
        long createdAt,
        long updatedAt,
        int version) { }
