package com.xin.aiagentbackend.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * PDF 导出请求对象（Request）。
 * <p>作为旅行计划 PDF 导出接口的请求体，通过各布尔开关控制封面、概览、图片、预算、行程等章节的生成。</p>
 *
 * @param style            导出样式，仅支持 simple（简洁）或 magazine（杂志风）
 * @param includeCover     是否生成封面
 * @param includeOverview  是否生成旅行概览
 * @param includePhotos    是否嵌入景点图片
 * @param includeBudget    是否生成预算概览
 * @param includeItinerary 是否生成每日行程
 */
public record PdfExportRequest(@NotBlank @Pattern(regexp = "simple|magazine") String style, boolean includeCover,
                               boolean includeOverview, boolean includePhotos, boolean includeBudget,
                               boolean includeItinerary) { }
