package com.xin.aiagentbackend.plan;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdatePlanRequest(
        @Size(max = 200) String title,
        @NotNull JsonNode itinerary,
        JsonNode budgetSummary,
        @NotNull Integer version) { }
