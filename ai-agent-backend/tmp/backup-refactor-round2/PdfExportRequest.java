package com.xin.aiagentbackend.plan;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PdfExportRequest(@NotBlank @Pattern(regexp = "simple|magazine") String style, boolean includeCover,
                               boolean includeOverview, boolean includePhotos, boolean includeBudget,
                               boolean includeItinerary) { }
