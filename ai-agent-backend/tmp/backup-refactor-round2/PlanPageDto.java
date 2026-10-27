package com.xin.aiagentbackend.plan;

import java.util.List;

public record PlanPageDto(List<PlanDto> list, long total, int page, int pageSize) { }
