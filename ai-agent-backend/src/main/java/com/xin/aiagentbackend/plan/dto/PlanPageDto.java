package com.xin.aiagentbackend.plan.dto;

import java.util.List;

/**
 * 旅行计划分页结果封装（DTO）。
 * <p>承载旅行计划分页查询返回的数据列表、总条数及当前分页参数。</p>
 *
 * @param list     当前页计划列表
 * @param total    符合条件的总记录数
 * @param page     当前页码（从 1 开始）
 * @param pageSize 每页条数
 */
public record PlanPageDto(List<PlanDto> list, long total, int page, int pageSize) { }
