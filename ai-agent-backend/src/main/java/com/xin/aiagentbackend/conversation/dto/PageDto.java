package com.xin.aiagentbackend.conversation.dto;

import java.util.List;

/**
 * 通用分页结果封装（DTO）。
 * <p>承载分页查询返回的数据列表、总条数及当前分页参数。</p>
 *
 * @param <T>      分页列表元素类型
 * @param list     当前页数据列表
 * @param total    符合条件的总记录数
 * @param page     当前页码（从 1 开始）
 * @param pageSize 每页条数
 */
public record PageDto<T>(List<T> list, long total, int page, int pageSize) { }
